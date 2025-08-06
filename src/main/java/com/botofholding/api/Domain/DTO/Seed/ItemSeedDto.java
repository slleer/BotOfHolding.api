package com.botofholding.api.Domain.DTO.Seed;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItemSeedDto {
    private String name;
    private String description;
    private Float weight;
    private String weightUnit;
    private Float value;
    private String valueUnit;
    private boolean parent;
}
