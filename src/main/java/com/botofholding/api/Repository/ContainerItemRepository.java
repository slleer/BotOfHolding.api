package com.botofholding.api.Repository;

import com.botofholding.api.Domain.Entity.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerItemRepository extends JpaRepository<ContainerItem, Long> {

    Optional<ContainerItem> findByContainerAndItem(Container container, Item item);

    /**
     * Fetch all containerItems for a user's active container.
     * @param prefix the prefix to search for
     * @param user the user making the request who owns the container
     * @param pageable the pagination information
     * @return a list of containerItems that match the prefix and belong to the user's active container
     */
    @Query("SELECT ci FROM BohUser u JOIN u.primaryContainer pc JOIN pc.containerItems ci JOIN ci.item i " +
            "WHERE u = :user AND LOWER(i.itemName) LIKE CONCAT(LOWER(:prefix), '%') " +
            "ORDER BY ci.lastModifiedDateTime DESC")
    List<ContainerItem> findAllFromActiveContainerForUser(@Param("prefix") String prefix, @Param("user") BohUser user, Pageable pageable);

    /**
     * Fetch all parent containerItems for a user's active container.
     * @param prefix the prefix to search for
     * @param user the user making the request who owns the container
     * @param pageable the pagination information
     * @return a list of parent containerItems that match the prefix and belong to the user's active container
     */
    @Query("SELECT ci FROM BohUser u JOIN u.primaryContainer pc JOIN pc.containerItems ci JOIN ci.item i " +
            "WHERE u = :user AND LOWER(i.itemName) LIKE CONCAT(LOWER(:prefix), '%') " +
            "AND i.parent = true " +
            "ORDER BY ci.lastModifiedDateTime DESC")
    List<ContainerItem> findAllParentsFromActiveContainer(@Param("prefix") String prefix, @Param("user") BohUser user, Pageable pageable);
}