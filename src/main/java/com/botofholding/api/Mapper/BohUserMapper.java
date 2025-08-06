package com.botofholding.api.Mapper;

import com.botofholding.api.Domain.DTO.Request.BohUserRequestDto;
import com.botofholding.api.Domain.DTO.Response.BohUserSummaryDto;
import com.botofholding.api.Domain.DTO.Response.BohUserTestResponseDto;
import com.botofholding.api.Domain.DTO.Response.BohUserWithAllContainersDto;
import com.botofholding.api.Domain.DTO.Response.BohUserWithPrimaryContainerDto;
import com.botofholding.api.Domain.Entity.BohUser;
import org.mapstruct.*;

@Mapper(componentModel = "spring"
        , unmappedTargetPolicy = ReportingPolicy.IGNORE
        , uses = {ContainerMapper.class},
        builder = @Builder)
public interface BohUserMapper {


    BohUser requestToEntity(BohUserRequestDto request);

    @Mapping(source = "discordId", target = "discordId") // Explicitly map the discord ID
    BohUserSummaryDto toSummaryDto(BohUser entity);


    // Mapping to DTO with primary container (full object)
    @Mapping(source = "discordId", target = "discordId")
    @Mapping(source = "primaryContainer", target = "primaryContainer") // Map the full object
    BohUserWithPrimaryContainerDto toWithPrimaryContainerDto(BohUser entity);

    // Mapping to DTO with all containers
    @Mapping(source = "discordId", target = "discordId")
    @Mapping(source = "containers", target = "containers") // Map the collection
    BohUserWithAllContainersDto toWithAllContainersDto(BohUser entity);


    BohUser dtoToEntity(BohUserSummaryDto dto);
    BohUserRequestDto entityToRequest(BohUser entity);

    void updateEntityFromUpdateRequest(BohUserRequestDto updatedUser, @MappingTarget BohUser existingUser);

    BohUser dtoToEntity(BohUserTestResponseDto foundUser);
}
