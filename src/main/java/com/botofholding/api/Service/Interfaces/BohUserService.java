package com.botofholding.api.Service.Interfaces;

import com.botofholding.api.Domain.DTO.Request.BohUserRequestDto;
import com.botofholding.api.Domain.DTO.Response.BohUserSummaryDto;
import com.botofholding.api.Domain.Entity.BohUser;

public interface BohUserService {

    /**
     * Updates the profile information for a given BohUser.
     * @param userToUpdate The BohUser entity to be updated.
     * @param dto The DTO containing the new profile information.
     * @return A summary DTO of the updated user.
     */
    BohUserSummaryDto updateUserProfile(BohUser userToUpdate, BohUserRequestDto dto);

    /**
     * Retrieves the user summary for the given BohUser.
     * @param user The BohUser entity to get the summary for.
     * @return A summary DTO of the user.
     */
    BohUserSummaryDto getUserProfile(BohUser user);

    /**
     * Deletes a user by their internal database ID.
     * @param userId The ID of the user to delete.
     */
    void deleteBohUser(Long userId);
}