package com.botofholding.api.Repository;

import com.botofholding.api.Domain.Entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {

    /**
     * Finds an Owner by their Discord ID.
     * @param discordId The Discord ID to search for.
     * @return An Optional containing the Owner if found, or empty if not.
     */
    Optional<Owner> findByDiscordId(Long discordId);

    /**
     * Finds an Owner by ID and eagerly fetches all owned Containers and, if it's a BohUser, the primaryContainer in a single query.
     * @param id The id of the owner
     * @return An Optional containing the Owner if found, or empty if not.
     */
    @Query("SELECT o FROM Owner o LEFT JOIN FETCH o.containers LEFT JOIN FETCH TREAT(o AS BohUser).primaryContainer WHERE o.id = :id")
    Optional<Owner> findByIdWithDetails(@Param("id") Long id);
}
