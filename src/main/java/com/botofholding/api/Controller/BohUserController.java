package com.botofholding.api.Controller;

import com.botofholding.api.Domain.DTO.Request.BohUserRequestDto;
import com.botofholding.api.Domain.DTO.Response.BohUserSummaryDto;
import com.botofholding.api.Domain.DTO.Response.StandardApiResponse;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.Service.Interfaces.BohUserService;
import com.botofholding.api.Utility.ResponseBuilder;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Validated
public class BohUserController extends BaseController{

    private static final Logger logger = LoggerFactory.getLogger(BohUserController.class);
    private final BohUserService bohUserService;
    private final ResponseBuilder responseBuilder;

    public BohUserController(BohUserService bohUserService, ResponseBuilder responseBuilder) {
        this.bohUserService = bohUserService;
        this.responseBuilder = responseBuilder;
    }

    /**
     * Updates the profile for the currently authenticated user.
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<BohUserSummaryDto>> updateMyProfile(@Valid @RequestBody BohUserRequestDto updatedUserDto) {
        BohUser currentUser = getAuthenticatedBohUser();
        logger.info("Attempting to update profile for user: {}", currentUser.getDisplayName());

        BohUserSummaryDto userDto = bohUserService.updateUserProfile(currentUser, updatedUserDto);

        String message = responseBuilder.buildSuccessUpdateMessage("User profile", userDto.getDisplayName());
        StandardApiResponse<BohUserSummaryDto> response = new StandardApiResponse<>(true, message, userDto);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the details for the currently authenticated user.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<BohUserSummaryDto>> getMyProfile() {
        BohUser currentUser = getAuthenticatedBohUser();
        logger.info("Attempting to find details for current authenticated user: {}", currentUser.getDisplayName());

        BohUserSummaryDto userDto = bohUserService.getUserProfile(currentUser);

        String message = responseBuilder.buildCustomSuccessMessage("Current user details retrieved successfully.");
        StandardApiResponse<BohUserSummaryDto> response = new StandardApiResponse<>(true, message, userDto);
        return ResponseEntity.ok(response);
    }
}