package com.workworth.preferences.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings, UUID> {
}
