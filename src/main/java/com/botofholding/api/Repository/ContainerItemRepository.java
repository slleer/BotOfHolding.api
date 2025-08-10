package com.botofholding.api.Repository;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteProjection;
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

    @Query("SELECT DISTINCT ci FROM ContainerItem ci LEFT JOIN FETCH ci.item i WHERE ci.parent in :containerItems")
    List<ContainerItem> fetchChildrenForContainerItems(@Param("containerItems") List<ContainerItem> containerItems);

    String AUTOCOMPLETE_CTE = """
            WITH ItemPath AS (
                -- Anchor: items at the root of the container
                SELECT
                    ci.cntnr_item_id,
                    ci.itm_qty,
                    ci.user_note,
                    i.item_nme,
                    CAST(i.item_nme AS VARCHAR(MAX)) AS full_path,
                    i.is_parent
                FROM
                    cntnr_item ci
                JOIN
                    item i ON ci.item_id = i.item_id
                WHERE
                    ci.cntnr_id = (SELECT prmy_cntnr_id FROM user_data WHERE user_id = :userId)
                    AND ci.parent_id IS NULL
            
                UNION ALL
            
                -- Recursive member: children of items already in the path
                SELECT
                    child.cntnr_item_id,
                    child.itm_qty,
                    child.user_note,
                    child_item.item_nme,
                    CAST(parent.full_path + ' > ' + child_item.item_nme AS VARCHAR(MAX)),
                    child_item.is_parent
                FROM
                    cntnr_item child
                JOIN
                    item child_item ON child.item_id = child_item.item_id
                JOIN
                    ItemPath parent ON child.parent_id = parent.cntnr_item_id
            )
            """;

    @Query(value = AUTOCOMPLETE_CTE + """
            SELECT TOP 25 ip.cntnr_item_id as id, ip.full_path as label, CONCAT('[id:', ip.cntnr_item_id, '] ', COALESCE('x' + ip.itm_qty + ' ', ''), COALESCE(ip.user_note, '')) as description FROM ItemPath ip WHERE LOWER(ip.full_path) LIKE LOWER(CONCAT('%', :prefix, '%')) ORDER BY label""", nativeQuery = true)
    List<AutoCompleteProjection> findItemsForAutocomplete(@Param("prefix") String prefix, @Param("userId") Long userId);

    @Query(value = AUTOCOMPLETE_CTE + """
            SELECT TOP 25 ip.cntnr_item_id as id, ip.full_path as label, CONCAT('[id:', ip.cntnr_item_id, '] ', COALESCE('x' + ip.itm_qty + ' ', ''), COALESCE(ip.user_note, '')) as description FROM ItemPath ip WHERE ip.is_parent = 1 AND LOWER(ip.full_path) LIKE LOWER(CONCAT('%', :prefix, '%')) ORDER BY label""", nativeQuery = true)
    List<AutoCompleteProjection> findParentItemsForAutocomplete(@Param("prefix") String prefix, @Param("userId") Long userId);

}