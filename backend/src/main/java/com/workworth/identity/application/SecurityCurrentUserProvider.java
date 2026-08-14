package com.workworth.identity.application;

import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    private final AppUserRepository users;

    public SecurityCurrentUserProvider(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("No authenticated WorkWorth user is available.");
        }
        return users.findByIdentitySubjectAndStatus(jwt.getSubject(), AppUserStatus.ACTIVE)
            .orElseThrow(() -> new AccessDeniedException("The authenticated identity is not authorized for WorkWorth."));
    }
}
