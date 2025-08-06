package com.botofholding.api.Repository;

import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Item;
import com.botofholding.api.Domain.Entity.Owner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Finds all items with an exact (case-insensitive) name that are accessible within a given scope.
     * The scope includes items owned by any of the provided owner IDs, plus any globally available
     * items owned by the SystemOwner.
     * Results are ordered to prioritize user-owned items, then guild-owned, then system-owned.
     *
     * @param name The exact item name to search for.
     * @param actor the user making the request
     * @param principal the guild the request was made in or the actor if not in a guild
     * @param pageable the pagination information
     * @return A sorted list of matching items.
     */
    @Query("SELECT i FROM Item i JOIN i.createdBy o " +
            "WHERE LOWER(i.itemName) = LOWER(:name) " +
            "AND (o = :actor OR o = :principal OR TYPE(o) = SystemOwner) " +
            "ORDER BY CASE WHEN TYPE(o) = BohUser THEN 1 " +
            "WHEN TYPE(o) = Guild THEN 2 ELSE 3 END")
    List<Item> findAllByNameForOwners(@Param("name") String name, @Param("actor") Owner actor, @Param("principal") Owner principal, Pageable pageable);


    /**
     * Finds all items with a name starting with a given prefix (case-insensitive) that are accessible
     * within a given scope. The scope includes items owned by any of the provided owner IDs, plus any
     * globally available items owned by the SystemOwner.
     * Results are ordered alphabetically by item name, then by owner type.
     *
     * @param prefix The prefix to search for in the item name.
     * @param actor the user making the request
     * @param principal the guild the request was made in or the actor if not in a guild
     * @param pageable the pagination information
     * @return A sorted list of matching items.
     */
    @Query("SELECT i FROM Item i JOIN i.createdBy o " +
            "WHERE LOWER(i.itemName) LIKE CONCAT('%', LOWER(:prefix), '%') " +
            "AND (o = :actor OR o = :principal OR TYPE(o) = SystemOwner) " +
            "ORDER BY i.itemName, " +
            "CASE WHEN TYPE(o) = BohUser THEN 1 " +
            "WHEN TYPE(o) = Guild THEN 2 ELSE 3 END")
    List<Item> findAllByNameLikeForOwners(@Param("prefix") String prefix, @Param("actor") Owner actor, @Param("principal") Owner principal, Pageable pageable);

    // [FIX] Corrected parameter name from :actor to :user.
    // [IMPROVEMENT] Changed to a "starts with" search (LIKE 'prefix%') for better autocomplete performance and behavior.
    // [IMPROVEMENT] Added explicit sorting by the ContainerItem's modification date to get most-recently-used items.
    @Query("SELECT i FROM BohUser u JOIN u.primaryContainer pc JOIN pc.containerItems ci JOIN ci.item i " +
           "WHERE u = :user AND LOWER(i.itemName) LIKE CONCAT(LOWER(:prefix), '%') " +
           "ORDER BY ci.lastModifiedDateTime DESC")
    List<Item> findAllFromActiveContainerForUser(@Param("prefix") String prefix, @Param("user") BohUser user, Pageable pageable);


    @Query("SELECT i FROM BohUser u JOIN u.primaryContainer pc JOIN pc.containerItems ci JOIN ci.item i " +
            "WHERE u = :user AND LOWER(i.itemName) LIKE CONCAT(LOWER(:prefix), '%') " +
            "AND i.parent = true " +
            "ORDER BY ci.lastModifiedDateTime DESC")
    List<Item> findAllParentsFromActiveContainer(@Param("prefix") String prefix, @Param("user") BohUser user, Pageable pageable);
}
