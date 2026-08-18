package com.workworth.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.application.AppUserProvisioningService;
import com.workworth.identity.application.SecurityCurrentUserProvider;
import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigurationTest.ProtectedController.class)
@Import({SecurityConfiguration.class, SecurityCurrentUserProvider.class,
    SecurityConfigurationTest.ProtectedController.class})
@ActiveProfiles("security-test")
@TestPropertySource(properties = {
    "workworth.security.enabled=true",
    "workworth.identity.email-claim=https://workworth.app/email",
    "workworth.identity.default-time-zone=Europe/Madrid"
})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AppUserRepository users;

    @MockBean
    private AppUserProvisioningService provisioning;

    @Test
    void rejectsRequestsWithoutAnAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/security-test"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsInvalidAccessTokens() throws Exception {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(get("/api/v1/security-test").header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void permitsAnAuthenticatedIdentityWithAnExistingActiveUser() throws Exception {
        AppUser user = new AppUser(UUID.randomUUID(), "auth0|allowed", "allowed@example.test", "Europe/Madrid", Instant.EPOCH);
        when(users.findByIdentitySubject("auth0|allowed")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/security-test").with(jwt().jwt(token -> token.subject("auth0|allowed"))))
            .andExpect(status().isOk());
    }

    @Test
    void provisionsAnAuthenticatedIdentityFromItsVerifiedEmailClaim() throws Exception {
        AppUser user = new AppUser(UUID.randomUUID(), "google-oauth2|new-user", "new@example.test", "Europe/Madrid", Instant.EPOCH);
        when(users.findByIdentitySubject("google-oauth2|new-user")).thenReturn(Optional.empty());
        when(provisioning.provision("google-oauth2|new-user", "new@example.test")).thenReturn(user);

        mockMvc.perform(get("/api/v1/security-test").with(jwt().jwt(token -> token
                .subject("google-oauth2|new-user")
                .claim("https://workworth.app/email", "new@example.test"))))
            .andExpect(status().isOk());
    }

    @Test
    void rejectsAnAuthenticatedIdentityWithoutTheVerifiedEmailClaim() throws Exception {
        when(users.findByIdentitySubject("auth0|unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/security-test").with(jwt().jwt(token -> token.subject("auth0|unknown"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void rejectsDisabledUsers() throws Exception {
        AppUser user = new AppUser(UUID.randomUUID(), "auth0|disabled", "disabled@example.test", "Europe/Madrid", Instant.EPOCH);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "status", AppUserStatus.DISABLED);
        when(users.findByIdentitySubject("auth0|disabled")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/security-test").with(jwt().jwt(token -> token.subject("auth0|disabled"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @RestController
    static class ProtectedController {
        private final CurrentUserProvider currentUser;

        ProtectedController(CurrentUserProvider currentUser) {
            this.currentUser = currentUser;
        }

        @GetMapping("/api/v1/security-test")
        String currentIdentity() {
            return currentUser.currentUser().getIdentitySubject();
        }
    }

}
