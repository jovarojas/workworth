package com.workworth.config;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "workworth.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint((request, response, exception) -> writeProblem(response,
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required or the access token is invalid.")))
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, exception) -> writeProblem(response,
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "The authenticated identity is not authorized for this resource.")))
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${workworth.security.jwt.jwk-set-uri}") String jwkSetUri,
                          @Value("${workworth.security.jwt.issuer}") String issuer,
                          @Value("${workworth.security.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> audienceValidator =
            new JwtClaimValidator<java.util.List<String>>("aud", values -> values != null && values.contains(audience));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            audienceValidator));
        return decoder;
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getOutputStream(), java.util.Map.of(
            "type", "about:blank", "title", status.getReasonPhrase(), "status", status.value(),
            "detail", detail, "code", code));
    }
}
