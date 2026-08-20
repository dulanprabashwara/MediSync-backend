package com.medisync.consultation.websocket;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthenticationInterceptorTest {

    @Mock JwtDecoder jwtDecoder;
    @Mock CurrentUserService currentUserService;
    @Mock DoctorProfileRepository doctorProfileRepository;

    private WebSocketAuthenticationInterceptor interceptor;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthenticationInterceptor(jwtDecoder, currentUserService, doctorProfileRepository);
        jwt = Jwt.withTokenValue("valid-token").header("alg", "ES256").subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
    }

    @Test
    void missingBearerTokenRejectsConnect() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.CONNECT, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invalidJwtRejectsConnect() {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new JwtException("invalid token"));

        assertThatThrownBy(() -> interceptor.preSend(
                message(StompCommand.CONNECT, "Bearer invalid-token"), null))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validActivePatientJwtAuthenticatesConnectUsingSubjectPrincipal() {
        AppUser patient = new AppUser(UUID.fromString(jwt.getSubject()), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
        when(currentUserService.requireCurrentUser(jwt)).thenReturn(patient);

        Message<?> authenticated = interceptor.preSend(
                message(StompCommand.CONNECT, "Bearer valid-token"), null);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(authenticated);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(jwt.getSubject());
    }

    @Test
    void pharmacistCannotConnectToConsultationEvents() {
        AppUser pharmacist = new AppUser(UUID.fromString(jwt.getSubject()), "pharmacist@example.com", "Ravi", "F", null,
                UserRole.PHARMACIST, AccountStatus.ACTIVE);
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
        when(currentUserService.requireCurrentUser(jwt)).thenReturn(pharmacist);

        assertThatThrownBy(() -> interceptor.preSend(
                message(StompCommand.CONNECT, "Bearer valid-token"), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activeVerifiedDoctorCanConnect() {
        AppUser doctorUser = new AppUser(UUID.fromString(jwt.getSubject()), "doctor@example.com", "Nimal", "P", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        DoctorProfile doctor = new DoctorProfile(doctorUser.getId());
        doctor.verify(UUID.randomUUID());
        assertThat(doctor.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
        when(currentUserService.requireCurrentUser(jwt)).thenReturn(doctorUser);
        when(doctorProfileRepository.findByUserId(doctorUser.getId())).thenReturn(java.util.Optional.of(doctor));

        Message<?> authenticated = interceptor.preSend(
                message(StompCommand.CONNECT, "Bearer valid-token"), null);

        assertThat(StompHeaderAccessor.wrap(authenticated).getUser()).isNotNull();
    }

    @Test
    void clientCannotPublishMessagesOverStomp() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, null), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("REST API");
    }

    private Message<byte[]> message(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
