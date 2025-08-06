package com.botofholding.api.Controller;

import com.botofholding.api.Domain.DTO.Request.UserSettingsUpdateRequestDto;
import com.botofholding.api.Domain.DTO.Response.StandardApiResponse;
import com.botofholding.api.Domain.DTO.Response.UserSettingsDto;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Service.Interfaces.UserSettingsService;
import com.botofholding.api.Utility.ResponseBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserSettingsController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsController.class);
    private final UserSettingsService userSettingsService;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public UserSettingsController(UserSettingsService userSettingsService, ResponseBuilder responseBuilder) {
        this.userSettingsService = userSettingsService;
        this.responseBuilder = responseBuilder;
    }

    @GetMapping("/me/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<UserSettingsDto>> getMySettings() {
        BohUser currentUser = getAuthenticatedBohUser();
        logger.info("Attempting to find settings for current user: {}", currentUser.getDisplayName());
        UserSettingsDto settingsDto = userSettingsService.getUserSettings(currentUser);

        String message = responseBuilder.buildSuccessFoundMessage("User Settings", "current user");
        StandardApiResponse<UserSettingsDto> response = new StandardApiResponse<>(true, message, settingsDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping(path = "/me/settings", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<UserSettingsDto>> updateMySettings(
            @Valid @RequestBody UserSettingsUpdateRequestDto updateRequestDto) {
        BohUser currentUser = getAuthenticatedBohUser();
        logger.info("Attempting to update settings for user: {}", currentUser.getDisplayName());
        UserSettingsDto updatedSettings = userSettingsService.updateUserSettings(currentUser, updateRequestDto);

        String message = responseBuilder.buildSuccessUpdateMessage("User Settings", "current user");
        StandardApiResponse<UserSettingsDto> response = new StandardApiResponse<>(true, message, updatedSettings);
        return ResponseEntity.ok(response);
    }
}
