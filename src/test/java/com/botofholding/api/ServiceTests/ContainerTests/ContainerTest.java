// H:/Bot-O-Holding_V2/api/src/test/java/com/botofholding/api/ServiceTests/ContainerTests/ContainerTest.java

package com.botofholding.api.ServiceTests.ContainerTests;

import com.botofholding.api.Domain.DTO.Request.ContainerRequestDto;
import com.botofholding.api.Domain.DTO.Response.ContainerSummaryDto;
import com.botofholding.api.Domain.DTO.Response.StandardApiResponse;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.ExceptionHandling.DuplicateResourceException;
import com.botofholding.api.Repository.OwnerRepository;
import com.botofholding.api.Service.Interfaces.ContainerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional // Ensures each test runs in its own transaction and is rolled back automatically.
public class ContainerTest {

    @Autowired
    private ContainerService containerService;

    @Autowired
    private OwnerRepository ownerRepository;

    /**
     * Helper method to create and save a BohUser for tests.
     * This is a standard way to set up prerequisite data in integration tests.
     */
    private Owner createTestOwner(String userName, Long discordId) {
        BohUser newUser = BohUser.builder()
                .discordId(discordId)
                .bohUserName(userName)
                .build();
        return ownerRepository.save(newUser);
    }

    /**
     * Because the class is marked @Transactional, we don't need to manually delete
     * created entities. The transaction rollback handles it. We only need to clear
     * the security context to ensure test isolation.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Helper method to set the Spring Security context for a test.
     * This "spoofs" a logged-in user, allowing the Auditing framework to work correctly.
     * @param owner The Owner entity to set as the current principal.
     */
    private void runAs(Owner owner) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                owner, null, owner.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("Add Container - Success")
    void addContainer_Success() {
        // Arrange: Create an owner and set the security context
        Owner owner = createTestOwner("ContainerOwner", 111222333L);
        runAs(owner);

        // The DTO is now much simpler and more secure
        ContainerRequestDto requestDto = new ContainerRequestDto();
        requestDto.setContainerName("Adventurer's Backpack");
        requestDto.setContainerDescription("Holds various treasures.");

        // Act: Call the service method with the new, explicit signature
        ContainerSummaryDto newContainer = containerService.addContainer(owner, requestDto);

        // Assert: Verify the result
        assertThat(newContainer).isNotNull();
        assertThat(newContainer.getContainerId()).isNotNull().isGreaterThan(0);
        assertThat(newContainer.isActive()).isNotNull();
        assertThat(newContainer.getContainerName()).isEqualTo("Adventurer's Backpack");
        assertThat(newContainer.getOwnerDisplayName()).isEqualTo(owner.getDisplayName());
    }

    @Test
    @DisplayName("Add Container - Fails on Duplicate Name for Same Owner")
    void addContainer_FailsOnDuplicateName() {
        // Arrange: Create an owner, set the security context, and create the first container
        Owner owner = createTestOwner("DuplicateContainerOwner", 444555666L);
        runAs(owner);

        ContainerRequestDto requestDto = new ContainerRequestDto();
        requestDto.setContainerName("Magic Pouch");

        // Create the first container successfully
        containerService.addContainer(owner, requestDto);

        // Act & Assert: Attempt to create a second container with the same name and owner
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> containerService.addContainer(owner, requestDto) // Use the same owner and DTO again
        );

        assertThat(exception.getMessage()).contains("A container named 'Magic Pouch' already exists for");
    }

    // NOTE: The tests 'addContainer_FailsWhenOwnerNotFound' and 'addContainer_FailsOnNullOwnerId'
    // have been removed. This is a positive result of your refactoring. The service layer
    // no longer performs this validation because the owner is now guaranteed to be valid
    // by the security context and the controller, simplifying the service's responsibilities.
}