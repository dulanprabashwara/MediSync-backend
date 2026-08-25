package com.medisync.consultation.service;

import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.consultation.dto.VideoTokenResponse;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.entity.ConsultationVideoSession;
import com.medisync.consultation.repository.ConsultationVideoSessionRepository;
import com.medisync.consultation.service.ConsultationAccessService.ConsultationContext;
import com.medisync.exception.ResourceConflictException;
import com.medisync.notification.NotificationType;
import com.medisync.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Handles video consultation token generation with enforcement of all
 * MediSync video rules:
 * <ol>
 *   <li>Only the Doctor can start the video call.</li>
 *   <li>Doctor can start only after the scheduled consultation time.</li>
 *   <li>Patient can join only after the Doctor has started it (room exists in DB).</li>
 *   <li>Only the assigned doctor and patient may join.</li>
 *   <li>Video does not replace the consultation lifecycle.</li>
 *   <li>No clinical data is sent to LiveKit. Room name is randomly generated UUID.</li>
 * </ol>
 */
@Service
public class VideoConsultationService {

    private static final Logger log = LoggerFactory.getLogger(VideoConsultationService.class);

    private final ConsultationAccessService accessService;
    private final LiveKitTokenService liveKitTokenService;
    private final NotificationService notificationService;
    private final ConsultationVideoSessionRepository videoSessionRepository;

    public VideoConsultationService(ConsultationAccessService accessService,
                                    LiveKitTokenService liveKitTokenService,
                                    NotificationService notificationService,
                                    ConsultationVideoSessionRepository videoSessionRepository) {
        this.accessService = accessService;
        this.liveKitTokenService = liveKitTokenService;
        this.notificationService = notificationService;
        this.videoSessionRepository = videoSessionRepository;
    }

    /**
     * Doctor starts a video call for a consultation.
     * Enforces: consultation must be IN_PROGRESS and scheduled time must have arrived.
     */
    @Transactional
    public VideoTokenResponse doctorStartVideo(Jwt jwt, UUID consultationId) {
        ConsultationContext ctx = accessService.requireDoctor(jwt, consultationId, false);

        requireInProgressAndTimeArrived(ctx);

        ConsultationVideoSession session = videoSessionRepository.findByConsultationId(consultationId)
                .orElse(null);

        boolean isFirstStart = false;
        if (session == null) {
            String randomRoomName = "medisync-v-" + UUID.randomUUID().toString();
            session = new ConsultationVideoSession(consultationId, randomRoomName, ctx.doctorUser().getId());
            try {
                videoSessionRepository.saveAndFlush(session);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                throw new ResourceConflictException("A video session already exists for this consultation.");
            }
            isFirstStart = true;
        }

        if (session.getStatus() != ConsultationVideoSession.VideoSessionStatus.ACTIVE) {
            throw new ResourceConflictException("The video session is no longer active.");
        }

        if (isFirstStart) {
            // Notify the patient only on first start
            String doctorName = ctx.doctorUser().getFirstName() + " " + ctx.doctorUser().getLastName();
            notificationService.createNotification(
                    ctx.patientUser().getId(),
                    ctx.doctorUser().getId(),
                    NotificationType.VIDEO_CALL_STARTED,
                    "Video call started",
                    "Dr. " + doctorName + " has started a video consultation. Join now.",
                    "/patient/consultations/" + consultationId,
                    "CONSULTATION",
                    consultationId,
                    "video-start-" + consultationId
            );
        }

        return new VideoTokenResponse(
                liveKitTokenService.generateToken(
                        session.getProviderRoomName(),
                        ctx.doctorUser().getId(),
                        "Doctor",
                        true),
                liveKitTokenService.serverUrl()
        );
    }

    /**
     * Doctor rejoins an already-started video call.
     */
    @Transactional(readOnly = true)
    public VideoTokenResponse doctorRejoinVideo(Jwt jwt, UUID consultationId) {
        ConsultationContext ctx = accessService.requireDoctor(jwt, consultationId, false);
        requireInProgressAndTimeArrived(ctx);

        ConsultationVideoSession session = videoSessionRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new ResourceConflictException("No active video call. Use 'Start Video Call' to begin one."));

        if (session.getStatus() != ConsultationVideoSession.VideoSessionStatus.ACTIVE) {
            throw new ResourceConflictException("The video session is no longer active.");
        }

        return new VideoTokenResponse(
                liveKitTokenService.generateToken(
                        session.getProviderRoomName(),
                        ctx.doctorUser().getId(),
                        "Doctor",
                        true),
                liveKitTokenService.serverUrl()
        );
    }

    /**
     * Patient joins a video call that the doctor has already started.
     */
    @Transactional(readOnly = true)
    public VideoTokenResponse patientJoinVideo(Jwt jwt, UUID consultationId) {
        ConsultationContext ctx = accessService.requirePatient(jwt, consultationId, false);
        requireInProgress(ctx);

        ConsultationVideoSession session = videoSessionRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new ResourceConflictException("The doctor has not started a video call yet. Please wait for the doctor to initiate."));

        if (session.getStatus() != ConsultationVideoSession.VideoSessionStatus.ACTIVE) {
            throw new ResourceConflictException("The video session is no longer active.");
        }

        return new VideoTokenResponse(
                liveKitTokenService.generateToken(
                        session.getProviderRoomName(),
                        ctx.patientUser().getId(),
                        "Patient",
                        true),
                liveKitTokenService.serverUrl()
        );
    }

    /**
     * Checks whether a video call is currently active for this consultation.
     */
    public boolean isVideoActive(UUID consultationId) {
        return videoSessionRepository.findByConsultationId(consultationId)
                .map(session -> session.getStatus() == ConsultationVideoSession.VideoSessionStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * Called by ConsultationService to mark the video session as ended
     * when the overall consultation ends. We don't expose this as an API
     * endpoint for the doctor to arbitrarily end the video stream alone.
     */
    @Transactional
    public void markSessionEnded(UUID consultationId) {
        videoSessionRepository.findByConsultationId(consultationId).ifPresent(session -> {
            if (session.getStatus() == ConsultationVideoSession.VideoSessionStatus.ACTIVE) {
                session.end();
                videoSessionRepository.save(session);
                String roomName = session.getProviderRoomName();
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            liveKitTokenService.deleteRoom(roomName);
                        }
                    });
                } else {
                    liveKitTokenService.deleteRoom(roomName);
                }
                log.info("Marked video session for consultation {} as ENDED", consultationId);
            }
        });
    }

    private void requireInProgressAndTimeArrived(ConsultationContext ctx) {
        requireInProgress(ctx);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (now.isBefore(ctx.appointment().getScheduledStart())) {
            throw new ResourceConflictException(
                    "The video call cannot be started before the scheduled consultation time.");
        }
    }

    private void requireInProgress(ConsultationContext ctx) {
        ConsultationStatus status = ctx.consultation().getStatus();
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new ResourceConflictException(
                    "Video calls are only available for consultations that are in progress.");
        }
        if (ctx.appointment().getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResourceConflictException(
                    "Video calls require a confirmed appointment.");
        }
    }
}
