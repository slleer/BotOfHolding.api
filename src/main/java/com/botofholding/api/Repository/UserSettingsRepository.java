package com.botofholding.api.Repository;

import com.botofholding.api.Domain.Entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByOwnerDiscordId(Long discordId);
}
