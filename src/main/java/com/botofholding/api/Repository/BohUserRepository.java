package com.botofholding.api.Repository;

import com.botofholding.api.Domain.Entity.Container;
import org.springframework.stereotype.Repository;
import com.botofholding.api.Domain.Entity.BohUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BohUserRepository extends JpaRepository<BohUser, Long> {

    /**
     * Finds a user by their Discord ID.
     * Since Discord IDs are typically unique, this returns an Optional.
     * @param userDiscordId the Discord ID of the user
     * @return an Optional containing the user if found, or an empty Optional otherwise
     */
    Optional<BohUser> findByDiscordId(Long userDiscordId);

    /**
     * Finds a BohUser by their discordId and fetches all their containers in a single query.
     * @param discordId The Discord ID of the user to find.
     * @return An Optional containing the BohUser with their containers, if found.
     */
    @Query("SELECT u FROM BohUser u JOIN FETCH u.containers c WHERE u.discordId = :discordId")
    Optional<BohUser> findByDiscordIdAndContainers(@Param("discordId") Long discordId);

    /**
     * Finds a BohUser by their discordId and fetches their primaryContainer in a single query.
     * @param discordId The Discord ID of the user to find.
     * @return An Optional containing the BohUser with their primary container, if found.
     */
    @Query("SELECT u FROM BohUser u JOIN FETCH u.primaryContainer c WHERE u.discordId = :discordId")
    Optional<BohUser> findByDiscordIdAndPrimaryContainer(@Param("discordId") Long discordId);

    boolean existsByDiscordId(Long discordId);

    /**
     * Finds a BohUser by their ID and eagerly fetches their primaryContainer
     * in a single query to prevent lazy-loading issues.
     * @param id The ID of the user to find.
     * @return An Optional containing the BohUser with their primary container, if found.
     */
    @Query("SELECT u FROM BohUser u LEFT JOIN FETCH u.primaryContainer WHERE u.id = :id")
    Optional<BohUser> findByIdWithPrimaryContainer(@Param("id") Long id);

}
