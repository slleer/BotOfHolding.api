package com.botofholding.api.Domain.DTO.Request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddItemRequestDto {

    @Min(value = 1, message = "must be a positive number.")
    private Long itemId;

    private String itemName;
    private String itemDescription;

    @Size(max = 350, message = "can only be 350 characters long.")
    private String userNote;

    @NotNull(message = "Quantity cannot be null.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity;

    private Long insideId;
    private String insideName;
}