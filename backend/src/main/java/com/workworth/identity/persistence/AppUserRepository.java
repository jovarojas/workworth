package com.workworth.identity.persistence;

import com.workworth.identity.domain.AppUserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByIdentitySubjectAndStatus(String identitySubject, AppUserStatus status);

    List<AppUser> findAllByStatus(AppUserStatus status);
}
