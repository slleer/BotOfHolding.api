package com.botofholding.api.Domain.DTO.Request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for modifying an existing ContainerItem.
 * Fields are nullable to indicate that they are optional.
 * If a field is null in the request, it will not be updated.
 */
@Getter
@Setter
@NoArgsConstructor
public class ModifyItemRequestDto {
    private Long containerItemId;
    private String containerItemName;
    // The new note for the item. An empty string will clear the note.
    private String note;
    private Integer newQuantity;
    // The ID of the new parent ContainerItem.
    private Long newParentId;
    private String newParentName;
    // A flag to move the item to the container's root level.
    private Boolean moveToRoot;
}