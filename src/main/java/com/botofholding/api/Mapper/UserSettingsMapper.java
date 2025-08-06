package com.botofholding.api.Mapper;

import com.botofholding.api.Domain.DTO.Request.UserSettingsUpdateRequestDto;
import com.botofholding.api.Domain.DTO.Response.UserSettingsDto;
import com.botofholding.api.Domain.Entity.UserSettings;
import com.botofholding.api.Domain.Enum.OwnerType;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserSettingsMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "owner.discordId", target = "ownerDiscordId")
    @Mapping(source = "owner.displayName", target = "ownerDisplayName")
    @Mapping(source = "owner.ownerType", target = "ownerType", qualifiedByName = "ownerTypeToString")
    UserSettingsDto toDto(UserSettings userSettings);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UserSettingsUpdateRequestDto dto, @MappingTarget UserSettings entity);

    @Named("ownerTypeToString")
    default String ownerTypeToString(OwnerType ownerType) {
        if(ownerType == null)
            return null;
        return ownerType.name();
    }

}
