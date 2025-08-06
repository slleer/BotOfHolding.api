package com.botofholding.api.Mapper;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Request.ContainerRequestDto;
import com.botofholding.api.Domain.DTO.Response.ContainerSummaryDto;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Container;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {ContainerItemMapper.class})
public interface ContainerMapper {

    @Mapping(source = "containerId", target = "containerId")
    @Mapping(source = "owner.displayName", target = "ownerDisplayName")
    @Mapping(source = "containerType.containerTypeName", target = "containerTypeName")
    @Mapping(source = "containerItems", target = "items")
    @Mapping(source = "container", target = "active", qualifiedByName = "mapIsActive")
    ContainerSummaryDto toSummaryDto(Container container, @Context BohUser currentUser);

    @Mapping(source = "containerId", target = "id")
    @Mapping(source = "containerName", target = "label")
    @Mapping(source = "owner.displayName", target = "description")
    AutoCompleteDto toAutoCompleteDto(Container container);

    @Mapping(target = "containerDescription", source = "containerDescription", conditionQualifiedByName = "isNonBlankString")
    @Mapping(target = "containerId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "containerItems", ignore = true)
    @Mapping(target = "creationDateTime", ignore = true)
    @Mapping(target = "lastModifiedDateTime", ignore = true)
    void updateContainerFromDto(ContainerRequestDto dto, @MappingTarget Container entity);

    @Named("isNonBlankString")
    @Condition
    default boolean isNonBlankString(String val) {
        return val != null && !val.isBlank();
    }

    /**
     * Determines if a container is the active (primary) container for its user owner.
     *
     * @param container The source container entity.
     * @return {@code true} if the container is its user's primary container, {@code false} otherwise.
     */
    @Named("mapIsActive")
    default boolean mapIsActive(Container container, @Context BohUser currentUser) {
        // If there's no user context or the container is invalid, it can't be active.
        if (currentUser == null || container == null || container.getContainerId() == null) {
            return false;
        }

        Container primaryContainer = currentUser.getPrimaryContainer();

        // It's active if the user has a primary container and its ID matches this container's ID.
        return primaryContainer != null
                && primaryContainer.getContainerId() != null
                && primaryContainer.getContainerId().equals(container.getContainerId());
    }
}
