package com.botofholding.api.Controller;

import com.botofholding.api.Domain.DTO.Request.AddItemRequestDto;
import com.botofholding.api.Domain.DTO.Request.ContainerRequestDto;
import com.botofholding.api.Domain.DTO.Request.ModifyItemRequestDto;
import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ContainerSummaryDto;
import com.botofholding.api.Domain.DTO.Response.DeletedEntityDto;
import com.botofholding.api.Domain.DTO.Response.ServiceResponse;
import com.botofholding.api.Domain.DTO.Response.StandardApiResponse;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.Service.Interfaces.ContainerService;
import com.botofholding.api.Utility.ResponseBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/containers")
@Validated
public class ContainerController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ContainerController.class);

    private final ContainerService containerService;
    private final ResponseBuilder responseBuilder;

    public ContainerController(ContainerService containerService, ResponseBuilder responseBuilder) {
        this.containerService = containerService;
        this.responseBuilder = responseBuilder;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> createContainer(@Valid @RequestBody ContainerRequestDto containerRequestDto) {
        logger.info("Attempting to create container: {}", containerRequestDto.getContainerName());
        Owner principal = getAuthenticatedPrincipal();
        ContainerSummaryDto newContainer = containerService.addContainer(principal, containerRequestDto);

        // Build the URI for the newly created resource, e.g., /api/containers/123
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newContainer.getContainerId())
                .toUri();

        String message = responseBuilder.buildSuccessCreationMessage("Container", newContainer.getContainerName());
        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, message, newContainer);
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Finds containers for the authenticated principal. Can be filtered by exact name.
     * If no name is provided, it returns all containers for the principal.
     * @param name (Optional) The exact name of the container to find.
     * @return A list of matching container summaries.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<List<ContainerSummaryDto>>> findContainersByName(@RequestParam(required = false) String name) {
        Owner principal = getAuthenticatedPrincipal();
        Owner actor = getRequestActor();
        logger.info("Attempting to find containers for principal '{}' and actor '{}' with filter [name={}]", principal.getDisplayName(), actor.getDisplayName(), name);
        List<ContainerSummaryDto> dtoList = containerService.findContainersForPrincipalAndActor(name, actor, principal);
        String message = responseBuilder.buildSuccessFoundMessage( "Containers", name);
        StandardApiResponse<List<ContainerSummaryDto>> response = new StandardApiResponse<>(true, message, dtoList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<List<AutoCompleteDto>>> autocompleteContainerByName(@RequestParam String prefix) {
        Owner principal = getAuthenticatedPrincipal();
        Owner actor = getRequestActor();
        logger.info("Performing autocomplete for containers with prefix '{}' for principal '{}' and actor '{}'."
                , prefix
                , principal.getDisplayName()
                , actor.getDisplayName());
        List<AutoCompleteDto> dtoList = containerService.autocompleteContainersForPrincipalAndActor(prefix, actor, principal);
        String message = responseBuilder.buildSuccessFoundMessage( "Containers", prefix);
        StandardApiResponse<List<AutoCompleteDto>> response = new StandardApiResponse<>(true, message, dtoList);
        return ResponseEntity.ok(response);
    }

    //@PreAuthorize("@securityService.canModifyContainer(#id, principal)")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> getContainerById(
            @PathVariable("id") @NotNull @Min(1) Long id) {
        logger.info("Attempting to find container by containerId: {}. ", id);
        Owner actor = getRequestActor();
        ContainerSummaryDto  foundContainer = containerService.findContainerById(id, actor);
        String message = responseBuilder.buildSuccessFoundMessage("Container", foundContainer.getContainerName());
        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, message, foundContainer);
        return ResponseEntity.ok(response);
    }

    // TODO combine activate by id and activate by name into a single activate method. use query parameters for id
    //  since it cna be null, unless can keep it a path variable and still have it be nullable.
    @PutMapping("/{id}/activate")
    @PreAuthorize("@securityService.canModifyContainer(#id, principal)")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> activateContainerById(
            @PathVariable("id") @NotNull @Min(1) Long id) {
        logger.info("Attempting to activate container by containerId: {}. ", id);
        Owner actor = getRequestActor();
        ContainerSummaryDto activeContainer = containerService.activateContainerById(id, actor);
        String message = responseBuilder.buildSuccessActivateMessage(activeContainer.getContainerName());
        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, message, activeContainer);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> activateContainerByName(
            @RequestParam(required = true) String name,
            @RequestParam(required = false) String ownerPriority) {
        Owner principal = getAuthenticatedPrincipal();
        Owner actor = getRequestActor();
        logger.info("Attempting to activate container by name '{}' for ownerType: {}.", name, ownerPriority == null ? "N/A" : ownerPriority);
        ContainerSummaryDto activeContainer = containerService.activateContainerByName(name, ownerPriority, actor, principal);
        String message = responseBuilder.buildSuccessActivateMessage(activeContainer.getContainerName());
        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, message, activeContainer);
        return  ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> getActiveContainer() {
        Owner actor = getRequestActor();
        ContainerSummaryDto activeContainer = containerService.findActiveContainerForUser(actor);
        String message = responseBuilder.buildSuccessFoundMessage("Active Container", activeContainer.getContainerName());
        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, message, activeContainer);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/active/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> addItemToActiveContainer(
            @Valid @RequestBody AddItemRequestDto addItemRequestDto) {
        Owner actor = getRequestActor();
        Owner principal = getAuthenticatedPrincipal();
        String itemIdentifier = (addItemRequestDto.getItemName() != null) ? "named '" + addItemRequestDto.getItemName()
                + "'" : "with ID " + addItemRequestDto.getItemId();
        logger.info("Attempting to add item {} (quantity: {}) to active container for user '{}'",
                itemIdentifier,
                addItemRequestDto.getQuantity(),
                actor.getDisplayName());

        ServiceResponse<ContainerSummaryDto> serviceResponse = containerService.addItemToActiveContainer(addItemRequestDto, actor, principal);

        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, serviceResponse.message(), serviceResponse.data());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/active/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> dropItemFromActiveContainer(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean dropChildren,
            @RequestParam(required = true) Integer quantity) {
        Owner actor = getRequestActor();
        // [FIX] Log a clear identifier, as 'name' can be null if 'id' is used.
        String itemIdentifier = (name != null) ? "named '" + name + "'" : "with ID " + id;
        logger.info("Attempting to drop item {} (quantity: {}) from active container.", itemIdentifier, quantity);

        ServiceResponse<ContainerSummaryDto> serviceResponse  = containerService.dropItemFromActiveContainer(id, name, quantity, dropChildren, actor);

        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, serviceResponse.message(), serviceResponse.data());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/active/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<ContainerSummaryDto>> modifyItemInActiveContainer(
            @Valid @RequestBody ModifyItemRequestDto modifyDto) {
        Owner actor = getRequestActor();
        String itemIdentifier = (modifyDto.getContainerItemName() != null)
                ? "named '" + modifyDto.getContainerItemName() + "'"
                : "with ID " + modifyDto.getContainerItemId();
        logger.info("Attempting to modify item {} in active container for user '{}'", itemIdentifier, actor.getDisplayName());

        ServiceResponse<ContainerSummaryDto> serviceResponse  = containerService.modifyItemInActiveContainer(modifyDto, actor);
        logger.info("Returning message: {}", serviceResponse.message());
        StandardApiResponse<ContainerSummaryDto> response = new StandardApiResponse<>(true, serviceResponse.message(), serviceResponse.data());
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("@securityService.canModifyContainer(#id, principal)")
    public ResponseEntity<StandardApiResponse<DeletedEntityDto>> deleteContainerById(
            @PathVariable("id") @NotNull @Min(1) Long id,
            @RequestParam("name") String name) {
        Owner actor = getRequestActor();

        logger.info("User '{}' attempting to delete container with ID: {} and confirmation name: '{}'",
                actor.getDisplayName(), id, name);

        DeletedEntityDto deletedEntity = containerService.deleteContainerByIdAndName(id, name, actor);
        String message = responseBuilder.buildSuccessDeleteMessage(deletedEntity.getEntityType(), deletedEntity.getName());
        StandardApiResponse<DeletedEntityDto> response = new StandardApiResponse<>(true, message, deletedEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active/items/autocomplete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<List<AutoCompleteDto>>> autocompleteItemsInActiveContainer(@RequestParam String prefix) {
        Owner actor = getRequestActor();
        logger.info("Performing autocomplete for items in active container with prefix '{}' for actor '{}'."
                , prefix
                , actor.getDisplayName());

        List<AutoCompleteDto> dtoList = containerService.autocompleteContainerItemsInActiveContainer(prefix, actor);

        String message = responseBuilder.buildSuccessFoundMessage("Items in active container", prefix);
        StandardApiResponse<List<AutoCompleteDto>> response = new StandardApiResponse<>(true, message, dtoList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active/parents/autocomplete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<List<AutoCompleteDto>>> autocompleteParentItemsActiveContainer(@RequestParam String prefix) {
        Owner actor = getRequestActor();
        logger.info("Performing autocomplete for parent items with prefix '{}' and actor {}",prefix, actor.getDisplayName());

        List<AutoCompleteDto> dtoList = containerService.autocompleteParentContainerItemsInActiveContainer(prefix, actor);

        String message = responseBuilder.buildSuccessFoundMessage("Parent Items in active container", prefix);
        StandardApiResponse<List<AutoCompleteDto>> response = new StandardApiResponse<>(true, message, dtoList);
        return ResponseEntity.ok(response);
    }

}
