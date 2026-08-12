package com.workworth.preferences.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings, Short> {
}
