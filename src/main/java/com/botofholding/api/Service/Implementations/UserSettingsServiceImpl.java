package com.botofholding.api.Service.Implementations;

import com.botofholding.api.Domain.DTO.Request.UserSettingsUpdateRequestDto;
import com.botofholding.api.Domain.DTO.Response.UserSettingsDto;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.UserSettings;
import com.botofholding.api.Mapper.UserSettingsMapper;
import com.botofholding.api.Repository.UserSettingsRepository;
import com.botofholding.api.Service.Interfaces.UserSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsServiceImpl implements UserSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsServiceImpl.class);
    private final UserSettingsRepository userSettingsRepository;
    private final UserSettingsMapper userSettingsMapper;

    public UserSettingsServiceImpl(UserSettingsRepository userSettingsRepository, UserSettingsMapper userSettingsMapper) {
        this.userSettingsRepository = userSettingsRepository;
        this.userSettingsMapper = userSettingsMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserSettingsDto getUserSettings(BohUser user) {
        logger.debug("Fetching settings for user: {}", user.getDisplayName());
        // The BohUser entity should already have its settings loaded or accessible.
        UserSettings settings = user.getUserSettings();
        if (settings == null) {
            // This is a data integrity issue, but we can handle it gracefully.
            // A user should ALWAYS have settings upon creation.
            logger.warn("User {} found but has null UserSettings. Creating default settings.", user.getDisplayName());
            settings = new UserSettings();
            user.setUserSettings(settings);
            userSettingsRepository.save(settings);
        }
        return userSettingsMapper.toDto(settings);

    }

    @Override
    @Transactional
    public UserSettingsDto updateUserSettings(BohUser user, UserSettingsUpdateRequestDto updateRequestDto) {
        logger.info("Attempting to update settings for user: {}", user.getDisplayName());
        UserSettings existingSettings = user.getUserSettings();
        userSettingsMapper.updateEntityFromDto(updateRequestDto, existingSettings);
        UserSettings savedSettings = userSettingsRepository.save(existingSettings);
        return userSettingsMapper.toDto(savedSettings);
    }
}
