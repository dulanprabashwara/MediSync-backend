package com.medisync.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisync.config.CorsProperties;
import com.medisync.config.SecurityProperties;
import com.medisync.exception.ApiErrorResponse;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    public SecurityConfig(AppUserRepository appUserRepository, ObjectMapper objectMapper) {
        this.appUserRepository = appUserRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/api/patient/profile")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.PATIENT))
                        .requestMatchers("/api/pharmacist/profile")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.PHARMACIST, true))
                        .requestMatchers("/api/pharmacist/professional-profile", "/api/pharmacist/professional-profile/**")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.PHARMACIST, true))
                        .requestMatchers("/api/admin/profile")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.ADMIN))
                        .requestMatchers("/api/reference/**").authenticated()
                        .requestMatchers("/api/doctor/profile", "/api/doctor/profile/**")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.DOCTOR, true))
                        .requestMatchers("/api/patient/**")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.PATIENT))
                        .requestMatchers("/api/doctor/**")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.DOCTOR))
                        .requestMatchers("/api/pharmacist/**")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.PHARMACIST))
                        .requestMatchers("/api/admin/**")
                        .access(new ApplicationRoleAuthorizationManager(appUserRepository, UserRole.ADMIN))
                        .requestMatchers("/api/users/me", "/api/users/onboarding").authenticated()
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, request.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "A valid access token is required"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, request.getRequestURI(), HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN", "You do not have permission to access this resource"))
                );
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUrl())
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audience = new SupabaseJwtAudienceValidator(properties.audience());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(properties.frontendUrl()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeSecurityError(HttpServletResponse response, String path, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(status, code, message, path));
    }
}
