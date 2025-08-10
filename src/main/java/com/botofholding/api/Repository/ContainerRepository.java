package com.botofholding.api.Repository;

import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Container;
import com.botofholding.api.Domain.Entity.Owner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
// In ContainerRepository.java
@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    /**
     * Finds all containers with a given name that belong to either the actor or principal
     * @param name The name of the container to find
     * @param actor The user making the request
     * @param principal Either a guild or the requesting user if request not made from a guild
     * @param pageable The pagination information
     * @return A list of matching containers or an empty list if none found.
     */
    @Query("SELECT c FROM Container c WHERE (c.owner = :principal OR c.owner = :actor) AND (:name IS NULL OR c.containerName = :name)")
    List<Container> findContainersForOwnersByName(@Param("name") String name, @Param("actor") Owner actor, @Param("principal") Owner principal, Pageable pageable);

    /**
     *  Used for Autocomplete results, finds containers owned by EITHER the principal OR the actor that start with the given prefix.
     * @param prefix The prefix to search for.
     * @param actor The user making the request
     * @param principal Either a guild or the requesting user if request not made from a guild
     * @param pageable The pagination information
     * @return A list of matching containers or an empty list if none found.
     */
    @Query("SELECT c FROM Container c WHERE (c.owner = :principal OR c.owner = :actor) AND LOWER(c.containerName) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<Container> autocompleteForOwnersByPrefix(@Param("prefix") String prefix, @Param("actor") Owner actor, @Param("principal") Owner principal, Pageable pageable);

    /**
     * Checks for the existence of a container with the given owner and name.
     * @param owner The owner of the container.
     * @param containerName The name of the container to check.
     * @return true if a container with the given owner and name exists, false otherwise.
     */
    boolean existsByOwnerAndContainerName(Owner owner, String containerName);

    /**
     * Find a container by its owner and name
     * @param principal The owner of the container
     * @param name The name of the container
     * @return An Optional containing the container if found, or empty if not
     */
    Optional<Container> findByOwnerAndContainerName(Owner principal, String name);

    /**
     * Finds the active container for a given user and eagerly fetches its items and their
     * master item data in a single, efficient query.
     * @param user The user whose active container is to be found.
     * @return An Optional containing the active Container, or empty if none is set.
     */
    @Query("SELECT DISTINCT pc FROM BohUser u JOIN u.primaryContainer pc LEFT JOIN FETCH pc.containerItems ci LEFT JOIN FETCH ci.item i WHERE u = :user")
    Optional<Container> findActiveContainerWithItemsForUser(@Param("user") BohUser user);

    /**
     * Finds the active container for a given user.
     * The active container is referenced by the primaryContainer field of the given BohUser.
     * @param user The user whose active container is to be found.
     * @return An Optional containing the active Container, or empty if none is set.
     */
    @Query("SELECT u.primaryContainer FROM BohUser u WHERE u = :user")
    Optional<Container> findActiveContainerForUser(@Param("user") BohUser user);

    /**
     * Finds a container by id and fetches all it's containerItems and items
     * @param id The id of the container to find
     * @return An Optional containing the container if found, or empty if not
     */
    @Query("SELECT c FROM Container c LEFT JOIN FETCH c.containerItems ci LEFT JOIN FETCH ci.item i WHERE c.containerId = :id")
    Optional<Container> findByIdWithItems(@Param("id") Long id);

}