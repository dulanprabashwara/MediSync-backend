package com.medisync.consultation.websocket;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private static final Set<String> ALLOWED_DESTINATIONS = Set.of(
            "/user/queue/consultation-events",
            "/user/queue/notifications"
    );

    private final JwtDecoder jwtDecoder;
    private final CurrentUserService currentUserService;
    private final DoctorProfileRepository doctorProfileRepository;

    public WebSocketAuthenticationInterceptor(JwtDecoder jwtDecoder,
                                              CurrentUserService currentUserService,
                                              DoctorProfileRepository doctorProfileRepository) {
        this.jwtDecoder = jwtDecoder;
        this.currentUserService = currentUserService;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (accessor.getUser() == null
                    || destination == null
                    || !ALLOWED_DESTINATIONS.contains(destination)) {
                throw new AccessDeniedException("WebSocket subscription is not allowed");
            }
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new AccessDeniedException("Consultation messages must be sent through the REST API");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("A valid access token is required for WebSocket connections");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new AccessDeniedException("A valid access token is required for WebSocket connections");
        }

        Jwt jwt = jwtDecoder.decode(token);
        AppUser user = currentUserService.requireCurrentUser(jwt);
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException("This account cannot connect to consultation events");
        }
        if (user.getRole() == UserRole.DOCTOR) {
            DoctorProfile doctor = doctorProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));
            if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
                throw new AccessDeniedException("Only verified doctors can connect to consultation events");
            }
        }
        accessor.setUser(new JwtAuthenticationToken(jwt));
    }
}
