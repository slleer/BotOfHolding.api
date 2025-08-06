package com.botofholding.api.Mapper;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ItemSummaryDto;
import com.botofholding.api.Domain.DTO.Seed.ItemSeedDto;
import com.botofholding.api.Domain.Entity.Item;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring"
        , unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder
)
public interface ItemMapper {

    @Mapping(source = "name", target = "itemName")
    @Mapping(source = "description", target = "itemDescription")
    Item toEntity(ItemSeedDto seedDto);

    @Mapping(source = "createdBy.displayName", target = "ownerDisplayName")
    ItemSummaryDto toSummaryDto(Item item);

    @Mapping(source = "itemId", target = "id")
    @Mapping(source = "itemName", target = "label")
    @Mapping(source = "createdBy.displayName", target = "description")
    AutoCompleteDto toAutoCompleteDto(Item item);

    default String trimString(String value) {
        return value != null ? value.trim() : null;
    }
}
