package com.botofholding.api.ServiceTests.BohUserTests;

import com.botofholding.api.Domain.DTO.Request.BohUserRequestDto;
import com.botofholding.api.Domain.DTO.Response.BohUserSummaryDto;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.ExceptionHandling.ResourceNotFoundException;
import com.botofholding.api.Repository.BohUserRepository;
import com.botofholding.api.Service.Interfaces.BohUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional // Ensures each test runs in its own transaction and is rolled back.
public class BohUserTest {

    @Autowired
    private BohUserService userService;

    @Autowired
    private BohUserRepository userRepository;

    private BohUser testUser;

    @BeforeEach
    void setUp() {
        // For each test, create a fresh user directly in the repository.
        // This is the correct way to set up state for service-level tests.
        BohUser user = BohUser.builder()
                .discordId(123456789L)
                .bohUserName("testuser")
                .bohGlobalUserName("Test User")
                .build();
        testUser = userRepository.save(user);
    }

    @Test
    @DisplayName("Get User Profile Test")
    void getUserProfileTest() {
        // The service method expects a managed user entity, which we have from our setup.
        BohUserSummaryDto userProfile = userService.getUserProfile(testUser);

        assertThat(userProfile).isNotNull();
        assertThat(userProfile.getId()).isEqualTo(testUser.getId());
        assertThat(userProfile.getBohUserName()).isEqualTo("testuser");
        assertThat(userProfile.getBohGlobalUserName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Update User Profile Test")
    void updateUserProfileTest() {
        // Create a DTO with the desired changes.
        // This assumes BohUserRequestDto is used to update UserSettings.
        BohUserRequestDto updateRequest = new BohUserRequestDto();
        // I'm assuming your DTO has these fields for updating settings.
        // updateRequest.setEphemeralContainer(true);
        // updateRequest.setEphemeralInventory(false);

        // Fetch the managed user instance to pass to the service.
        BohUser managedUser = userRepository.findById(testUser.getId())
                .orElseThrow(() -> new IllegalStateException("Test user not found"));

        BohUserSummaryDto updatedProfile = userService.updateUserProfile(managedUser, updateRequest);

        assertThat(updatedProfile).isNotNull();
        // Add assertions for the updated fields
        // assertThat(updatedProfile.isEphemeralContainer()).isTrue();
        // assertThat(updatedProfile.isEphemeralInventory()).isFalse();

        // Verify the changes were persisted by checking the entity directly
        BohUser userAfterUpdate = userRepository.findById(testUser.getId()).orElseThrow();
        // assertThat(userAfterUpdate.getUserSettings().isEphemeralContainer()).isTrue();
        // assertThat(userAfterUpdate.getUserSettings().isEphemeralInventory()).isFalse();
    }

    @Test
    @DisplayName("Delete User Test")
    void deleteUserTest() {
        Long userIdToDelete = testUser.getId();

        // Act
        userService.deleteBohUser(userIdToDelete);

        // Assert that the user no longer exists in the repository.
        assertThat(userRepository.existsById(userIdToDelete)).isFalse();
    }

    @Test
    @DisplayName("Delete Non-Existent User Throws Exception")
    void deleteNonExistentUserTest() {
        Long nonExistentId = 99999L;

        // Assert that the correct exception is thrown when trying to delete a user that doesn't exist.
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteBohUser(nonExistentId);
        });
    }
}