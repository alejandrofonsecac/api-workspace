package com.workspace.api.user.preferences.repository;

import com.workspace.api.user.preferences.domain.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, String> {
}
