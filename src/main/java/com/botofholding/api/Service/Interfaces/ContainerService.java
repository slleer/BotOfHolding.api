package com.botofholding.api.Service.Interfaces;

import com.botofholding.api.Domain.DTO.Request.AddItemRequestDto;
import com.botofholding.api.Domain.DTO.Request.ContainerRequestDto;
import com.botofholding.api.Domain.DTO.Request.ModifyItemRequestDto;
import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ContainerSummaryDto;
import com.botofholding.api.Domain.DTO.Response.DeletedEntityDto;
import com.botofholding.api.Domain.Entity.Owner;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface ContainerService {

    ContainerSummaryDto addContainer(Owner principal, ContainerRequestDto containerRequestDto);

    List<ContainerSummaryDto> findContainersForPrincipalAndActor(String name, Owner actor, Owner principal);

    List<AutoCompleteDto> autocompleteContainersForPrincipalAndActor(@NotNull String prefix, Owner actor, Owner principal);

    ContainerSummaryDto findContainerById(@NotNull @Min(1) Long id, Owner actor);

    ContainerSummaryDto activateContainerById(@NotNull @Min(1) Long id, Owner actor);

    ContainerSummaryDto activateContainerByName(String name, String ownerPriority, Owner actor, Owner principal);

    ContainerSummaryDto findActiveContainerForUser(Owner actor);

    ContainerSummaryDto addItemToActiveContainer(AddItemRequestDto addDto, Owner actor, Owner principal);

    ContainerSummaryDto dropItemFromActiveContainer(Long id, String name, Integer quantity, Boolean dropChildren, Owner actor);

    List<AutoCompleteDto> autocompleteContainerItemsInActiveContainer(String prefix, Owner actor);

    List<AutoCompleteDto> autocompleteParentContainerItemsInActiveContainer(String prefix, Owner actor);

    DeletedEntityDto deleteContainerByIdAndName(@NotNull @Min(1) Long id, @NotNull String name, Owner actor);

    ContainerSummaryDto modifyItemInActiveContainer(ModifyItemRequestDto modifyItemRequestDto, Owner actor);
}
