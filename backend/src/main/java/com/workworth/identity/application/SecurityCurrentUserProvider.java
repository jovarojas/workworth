package com.workworth.identity.application;

import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    private final AppUserRepository users;
    private final AppUserProvisioningService provisioning;
    private final String emailClaim;

    public SecurityCurrentUserProvider(AppUserRepository users, AppUserProvisioningService provisioning,
                                       @Value("${workworth.identity.email-claim}") String emailClaim) {
        this.users = users;
        this.provisioning = provisioning;
        this.emailClaim = emailClaim;
    }

    @Override
    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("No authenticated WorkWorth user is available.");
        }
        return users.findByIdentitySubject(jwt.getSubject())
            .map(this::activeUser)
            .orElseGet(() -> provisionUser(jwt));
    }

    private AppUser provisionUser(Jwt jwt) {
        String email = jwt.getClaimAsString(emailClaim);
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("The authenticated identity is missing its verified email claim.");
        }

        try {
            return provisioning.provision(jwt.getSubject(), email.trim());
        } catch (DataIntegrityViolationException exception) {
            return users.findByIdentitySubject(jwt.getSubject())
                .map(this::activeUser)
                .orElseThrow(() -> exception);
        }
    }

    private AppUser activeUser(AppUser user) {
        if (user.getStatus() != AppUserStatus.ACTIVE) {
            throw new AccessDeniedException("The authenticated identity is not authorized for WorkWorth.");
        }
        return user;
    }
}
