package com.botofholding.api.Mapper;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ContainerItemSummaryDto;
import com.botofholding.api.Domain.Entity.ContainerItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContainerItemMapper {

    @Mapping(source = "item.itemId", target = "itemId")
    @Mapping(source = "containerItem", target = "itemName", qualifiedByName = "mapItemName")
    @Mapping(source = "lastModifiedDateTime", target = "lastModified")
    ContainerItemSummaryDto toDto(ContainerItem containerItem);

    @Mapping(source = "containerItem", target = "label", qualifiedByName = "mapItemName")
    @Mapping(source = "containerItemId", target = "id")
    @Mapping(source = "containerItem", target = "description", qualifiedByName = "mapDescription")
    AutoCompleteDto toAutoCompleteDto(ContainerItem containerItem);


    @Named("mapItemName")
    default String mapItemName(ContainerItem containerItem) {
        if (containerItem == null) {
            return ""; // Or some default like "N/A"
        }
        return buildRecursiveItemName(containerItem);
    }

    /**
     * Creates a description for an autocomplete entry, combining quantity and user note.
     * e.g., "(5) A special potion" or "(10)" if no note exists.
     * @param containerItem The item to describe.
     * @return A formatted description string.
     */
    @Named("mapDescription")
    default String mapDescription(ContainerItem containerItem) {
        if (containerItem == null) {
            return "";
        }
        StringBuilder description = new StringBuilder();
        if (containerItem.getQuantity() != null) {
            description.append("x").append(containerItem.getQuantity());
        }
        String note = containerItem.getUserNote();
        if (note != null && !note.isBlank()) {
            if (!description.isEmpty()) {
                description.append(" ");
            }
            description.append(note);
        }
        return description.toString();
    }
    /**
     * Recursively builds the full path of an item, including its parents.
     * e.g., "Backpack > Potion Pouch > Health Potion"
     * @param item The item to build the name for.
     * @return The full, nested item name.
 */
    default String buildRecursiveItemName(ContainerItem item) {
        // Base case: The item has no parent, so just return its name.
        if (item.getParent() == null) {
            return item.getItem() != null ? item.getItem().getItemName() : "Unnamed Item";
        }
        // Recursive step: Get the parent's full name, then append this item's name.
        return buildRecursiveItemName(item.getParent()) + " > " + (item.getItem() != null ? item.getItem().getItemName() : "Unnamed Item");
    }
}