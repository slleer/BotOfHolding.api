package com.botofholding.api.Domain.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemSummaryDto {
    private Long itemId;
    private String itemName;
    private String itemDescription;
    private Float weight;
    private String weightUnit;
    private Float value;
    private String valueUnit;
    private String ownerDisplayName;
}
