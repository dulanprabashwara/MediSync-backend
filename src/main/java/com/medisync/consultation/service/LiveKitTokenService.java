package com.medisync.consultation.service;

import com.medisync.config.LiveKitProperties;
import io.livekit.server.AccessToken;
import org.springframework.stereotype.Service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.livekit.server.RoomServiceClient;

/**
 * Generates opaque LiveKit room names and short-lived access tokens.
 *
 * <ul>
 *   <li>Room names are derived solely from the consultation ID — no PII.</li>
 *   <li>Participant identities are opaque UUIDs — no PII.</li>
 *   <li>The API secret never leaves this service.</li>
 * </ul>
 */
@Service
public class LiveKitTokenService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitTokenService.class);

    private final LiveKitProperties properties;

    public LiveKitTokenService(LiveKitProperties properties) {
        this.properties = properties;
    }

    /**
     * Generates a short-lived LiveKit JWT for the given participant.
     *
     * @param roomName the opaque, persistent provider room name
     * @param participantUserId the app user's UUID — used as the opaque identity
     * @param participantDisplayName an opaque label like "Doctor" or "Patient"
     * @param canPublish whether the participant can publish media tracks
     */
    public String generateToken(String roomName,
                                UUID participantUserId,
                                String participantDisplayName,
                                boolean canPublish) {

        AccessToken token = new AccessToken(properties.apiKey(), properties.apiSecret());
        token.setIdentity(participantUserId.toString());
        token.setName(participantDisplayName);
        token.addGrants(
                new io.livekit.server.RoomJoin(true),
                new io.livekit.server.RoomName(roomName),
                new io.livekit.server.CanPublish(canPublish),
                new io.livekit.server.CanSubscribe(true)
        );
        token.setTtl(4 * 60 * 60); // 4 hours in seconds

        return token.toJwt();
    }

    /**
     * Returns the LiveKit WebSocket URL that clients connect to.
     * This is NOT the API secret — safe to return to the frontend.
     */
    public String serverUrl() {
        return properties.url();
    }

    /**
     * Best-effort API call to LiveKit to shut down the room.
     */
    public void deleteRoom(String roomName) {
        try {
            RoomServiceClient client = RoomServiceClient.createClient(
                    properties.url(), properties.apiKey(), properties.apiSecret());
            retrofit2.Call<Void> call = client.deleteRoom(roomName);
            call.execute();
            log.info("LiveKit room {} deleted", roomName);
        } catch (Exception e) {
            log.warn("Best-effort LiveKit room shutdown failed for room {}", roomName, e.getMessage());
        }
    }
}
