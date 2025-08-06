package com.botofholding.api.Service.Interfaces;

import com.botofholding.api.Domain.DTO.Request.UserSettingsUpdateRequestDto;
import com.botofholding.api.Domain.DTO.Response.UserSettingsDto;
import com.botofholding.api.Domain.Entity.BohUser;

public interface UserSettingsService {

    /**
     * Retrieves the settings for a given BohUser.
     * @param user The BohUser whose settings are to be retrieved.
     * @return A DTO representing the user's settings.
     */
    UserSettingsDto getUserSettings(BohUser user);

    /**
     * Updates the settings for a given BohUser.
     * @param user The BohUser whose settings are to be updated.
     * @param updateRequestDto The DTO containing the new settings values.
     * @return A DTO representing the updated settings.
     */
    UserSettingsDto updateUserSettings(BohUser user, UserSettingsUpdateRequestDto updateRequestDto);
}