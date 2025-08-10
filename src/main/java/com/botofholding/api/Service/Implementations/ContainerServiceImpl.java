package com.botofholding.api.Service.Implementations;

import com.botofholding.api.Domain.DTO.Request.AddItemRequestDto;
import com.botofholding.api.Domain.DTO.Request.ContainerRequestDto;
import com.botofholding.api.Domain.DTO.Request.ModifyItemRequestDto;
import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.AutoCompleteProjection;
import com.botofholding.api.Domain.DTO.Response.ServiceResponse;
import com.botofholding.api.Domain.DTO.Response.ContainerSummaryDto;
import com.botofholding.api.Domain.DTO.Response.DeletedEntityDto;
import com.botofholding.api.Domain.Entity.*;
import com.botofholding.api.ExceptionHandling.*;
import com.botofholding.api.Mapper.ContainerItemMapper;
import com.botofholding.api.Mapper.ContainerMapper;
import com.botofholding.api.Repository.*;
import com.botofholding.api.Service.Interfaces.ContainerService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContainerServiceImpl implements ContainerService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerServiceImpl.class);

    private final ContainerRepository containerRepository;
    private final ContainerMapper containerMapper;
    private final OwnerRepository ownerRepository;
    private final BohUserRepository bohUserRepository;
    private final ItemRepository itemRepository;
    private final ContainerItemMapper containerItemMapper;
    private final ContainerItemRepository containerItemRepository;

    @Autowired
    public ContainerServiceImpl(ContainerRepository containerRepository, ContainerMapper containerMapper,
                                OwnerRepository ownerRepository, BohUserRepository bohUserRepository,
                                ItemRepository itemRepository, ContainerItemMapper containerItemMapper, ContainerItemRepository containerItemRepository) {
        this.containerRepository = containerRepository;
        this.containerMapper = containerMapper;
        this.ownerRepository = ownerRepository;
        this.bohUserRepository = bohUserRepository;
        this.itemRepository = itemRepository;
        this.containerItemMapper = containerItemMapper;
        this.containerItemRepository = containerItemRepository;
    }

    /**
     * Creates a new container for a given owner.
     * @param principal The owner for whom to create the container. Either the requesting user or the guild the command was sent from.
     * @param containerRequestDto The details of the container to create.
     * @return A DTO of the newly created container.
     */
    @Override
    @Transactional
    public ContainerSummaryDto addContainer(Owner principal, ContainerRequestDto containerRequestDto) {
        Owner managedOwner = ownerRepository.findByIdWithDetails(principal.getId())
                .orElseThrow(() -> new OwnerNotFoundException("Owner with ID " + principal.getId() + " not found."));

        if (containerRepository.existsByOwnerAndContainerName(managedOwner, containerRequestDto.getContainerName())) {
            throw new DuplicateResourceException(
                    "A container named '" + containerRequestDto.getContainerName() + "' already exists for " + managedOwner.getDisplayName() + "."
            );
        }

        Container newContainer = new Container();
        containerMapper.updateContainerFromDto(containerRequestDto, newContainer);
        newContainer.setOwner(managedOwner);

        BohUser userContext = (managedOwner instanceof BohUser) ? (BohUser) managedOwner : null;
        if (userContext != null) {
            // A user's primary container is their first one, OR any one they explicitly set as active.
            if (containerRequestDto.isActive() || userContext.getPrimaryContainer() == null) {
                logger.info("Setting new container as primary for user '{}'.", userContext.getDisplayName());
                userContext.setPrimaryContainer(newContainer);
                newContainer.setLastActiveDateTime(LocalDateTime.now());
            }
        }

        Container savedContainer = containerRepository.save(newContainer);

        ownerRepository.save(managedOwner);
        logger.info("Successfully created container '{}' for owner '{}'", savedContainer.getContainerName(), managedOwner.getDisplayName());
        return containerMapper.toSummaryDto(savedContainer, userContext);
    }

    /**
     * Finds a container by its ID.
     * @param id The ID of the container to find.
     * @param actor The user requesting the container.
     * @return A DTO of the found container.
     */
    @Override
    @Transactional
    public ContainerSummaryDto findContainerById(@NotNull @Min(1) Long id, Owner actor) {
        BohUser userContext = (actor instanceof BohUser) ? (BohUser) actor : null;
        return containerRepository.findByIdWithItems(id)
                .map(container -> containerMapper.toSummaryDto(container, userContext))
                .orElseThrow(() -> new ContainerNotFoundException("Container with id " + id + " not found."));
    }

    /**
     * Finds a list of containers by name or owners.
     * @param name The name of the container to find.
     * @param actor The user requesting the container and one of the owners to filter by.
     * @param principal One of the owners to filter by if it's a GUILD otherwise the requesting user.
     * @return A list of DTOs of the found containers.
     */
    @Override
    @Transactional
    public List<ContainerSummaryDto> findContainersForPrincipalAndActor(String name, Owner actor, Owner principal) {
        // Treat a blank name as a null filter, which the repository query understands.
        String effectiveName = (name != null && name.isBlank()) ? null : name;
        // [IMPROVEMENT] Always provide a sort order for predictable API results.
        Pageable sortByLastActive = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "lastActiveDateTime"));
        List<Container> containers =  containerRepository.findContainersForOwnersByName(effectiveName, actor, principal, sortByLastActive);

        // The 'actor' is the user whose context we need for the 'active' flag.
        BohUser userContext = (actor instanceof BohUser) ? (BohUser) actor : null;

        return containers.stream()
                .map(container -> containerMapper.toSummaryDto(container, userContext))
                .collect(Collectors.toList());
    }

    /**
     * provides autocomplete result set of containers for the given prefix and owners.
     * @param prefix The search string to filter by.
     * @param actor The user requesting the autocomplete and one of the owners to filter by.
     * @param principal One of the owners to filter by if it's a GUILD otherwise the requesting user.
     * @return A list of DTOs of the found containers.
     */
    @Override
    @Transactional
    public List<AutoCompleteDto> autocompleteContainersForPrincipalAndActor(String prefix, Owner actor, Owner principal) {
        logger.info("Attempting to find containers with name prefix: {}*", prefix);
        Pageable top25ByLastActive = PageRequest.of(0,25, Sort.by(Sort.Direction.DESC, "lastActiveDateTime"));
        List<Container> containers = containerRepository.autocompleteForOwnersByPrefix(prefix, actor, principal, top25ByLastActive);

        if (containers.isEmpty()) {
            logger.info("No containers found for autocomplete with prefix '{}'.", prefix);
        }

        return containers.stream()
                .map(containerMapper::toAutoCompleteDto)
                .collect(Collectors.toList());
    }


    /**
     * Activates a container for a given owner.
     * @param id The ID of the container to activate.
     * @param actor The user for whom to activate the container.
     * @return A DTO of the newly activated container.
     */
    // TODO combine activate by id and activate by name into a single activate method.
    @Override
    @Transactional
    public ContainerSummaryDto activateContainerById(Long id, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can activate containers.");
        }
        Container container = containerRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ContainerNotFoundException("Container with id " + id + " not found."));

        return activate(user, container);
    }

    /**
     * Activates a container for a given owner.
     * @param name The name of the container to activate.
     * @param ownerPriority used to prioritize a container owner if multiple containers returned for actor and principal
     * @param actor The user for whom to activate the container and one of the owners to filter by.
     * @param principal One of the owners to filter by if it's a GUILD otherwise the requesting user.
     * @return A DTO of the newly activated container.
     */
    @Override
    @Transactional
    public ContainerSummaryDto activateContainerByName(String name, String ownerPriority, Owner actor, Owner principal) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can activate containers.");
        }

        Optional<Container> userContainer = containerRepository.findByOwnerAndContainerName(actor, name);
        if (actor == principal && userContainer.isPresent()) {
            return activate(user, userContainer.get());
        }
        Optional<Container> guildContainer = containerRepository.findByOwnerAndContainerName(principal, name);

        Container containerToActivate;
        if ("GUILD".equalsIgnoreCase(ownerPriority)) {
            containerToActivate = guildContainer.or(() -> userContainer)
                    .orElseThrow(() -> new ContainerNotFoundException("Container with name '" + name + "' not found for user or their guild."));
        } else { // Default to USER priority
            containerToActivate = userContainer.or(() -> guildContainer)
                    .orElseThrow(() -> new ContainerNotFoundException("Container with name '" + name + "' not found for user or their guild."));
        }

        return activate(user, containerToActivate);
    }

    /**
     * Finds the active container for a given owner.
     * @param actor The owner for whom to find the active container.
     * @return A DTO of the found container.
     */
    @Override
    @Transactional
    public ContainerSummaryDto findActiveContainerForUser(Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can activate containers.");
        }

        // Step 1: Fetch the container and all its associated items in a single query.
        // This avoids the MultipleBagFetchException and Cartesian product issues.
        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        // Step 2: If there are items, fetch all their direct children and the children's associated item data
        // in a second, efficient query.
        if (!activeContainer.getContainerItems().isEmpty()) {
            containerItemRepository.fetchChildrenForContainerItems(activeContainer.getContainerItems());
        }

        return containerMapper.toSummaryDto(activeContainer, user);
    }

    /**
     * Adds an item to the actor's active container.
     * @param addDto The details of the item to add.
     * @param actor The requesting user for whom the container is active for, one of the item owners to filter by.
     * @param principal One of the item's owners to filter by if it's a GUILD otherwise the requesting user.
     * @return A DTO of the updated container.
     */
    @Override
    @Transactional
    public ServiceResponse<ContainerSummaryDto> addItemToActiveContainer(AddItemRequestDto addDto, Owner actor, Owner principal) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container to add items to.");
        }

        // 1. Find the active container for the user.
        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        if (!activeContainer.getContainerItems().isEmpty()) {
            containerItemRepository.fetchChildrenForContainerItems(activeContainer.getContainerItems());
        }
        // 2. Find the item to be added. Prefer the ID if provided, as it's unambiguous.
        Optional<Item> itemById = Optional.ofNullable(addDto.getItemId())
                .flatMap(itemRepository::findById);

        Item itemToAdd;
        if (itemById.isPresent()) {
            itemToAdd = itemById.get();
        } else {
            logger.info("Item couldn't be found by id, searching by name {}", addDto.getItemName());

            Pageable top3 = PageRequest.of(0, 3);
            List<Item> itemsByName = itemRepository.findAllByNameForOwners(addDto.getItemName(), actor, principal, top3);

            if (itemsByName.isEmpty()) {
                throw new ItemNotFoundException("Item '" + addDto.getItemName() + "' not found.");
            }
            if (itemsByName.size() > 1) {
                // Format a helpful error message listing the conflicting items.
                String conflictingItems = itemsByName.stream()
                        .map(item -> String.format("'%s' (ID: %d)\n", item.getItemName(), item.getItemId()))
                        .collect(Collectors.joining(", "));
                throw new AmbiguousResourceException(
                    "Multiple items found with the name '" + addDto.getItemName() + "': " + conflictingItems + ". Please be more specific or use the item's ID."
                );
            }
            itemToAdd = itemsByName.get(0);
        }
        logger.info("The item is {} with name '{}'.", itemToAdd.getItemId(), itemToAdd.getItemName());

        // 3. Check if the item already exists in the container to update its quantity.
        // Find the parent item if specified. This helper "soft-fails" by returning an empty Optional
        // if no parent is specified.
        Optional<ContainerItem> parentOpt;
        try {
            // This will "hard-fail" if an invalid or ambiguous parent is specified...
            parentOpt = findOptionalParentItem(addDto.getInsideId(), addDto.getInsideName(), activeContainer);
            // ...and this will "hard-fail" if the found parent is not a valid parent type.
            parentOpt.ifPresent(p -> validateParentage(null, p));
        } catch (ValidationException | ItemNotFoundException | AmbiguousResourceException e) {
            // [SOFT FAIL] Catch the failure, log it, and treat the parent as non-existent.
            logger.warn("Invalid parent specified when adding item. Defaulting to container root. Reason: {}", e.getMessage());
            parentOpt = Optional.empty();
        }
        ContainerItem parent = parentOpt.orElse(null);

        String message;

        // Correctly handle stackable vs. non-stackable (parent) items.
        if (!itemToAdd.isParent()) {
            Optional<ContainerItem> existingStackOpt = activeContainer.getContainerItems().stream()
                    .filter(ci -> ci.getItem().getItemId().equals(itemToAdd.getItemId()) && Objects.equals(ci.getParent(), parent))
                    .findFirst();

            ContainerItem containerItem;
            if (existingStackOpt.isPresent()) {
                containerItem = existingStackOpt.get();
                containerItem.setQuantity(containerItem.getQuantity() + addDto.getQuantity());
                message = String.format("Increased '%s' by %d.", itemToAdd.getItemName(), addDto.getQuantity());
            } else {
                containerItem = new ContainerItem();
                containerItem.setItem(itemToAdd);
                containerItem.setContainer(activeContainer);
                containerItem.setQuantity(addDto.getQuantity()); // Set initial quantity directly
                if (parent != null) {
                    parent.addChild(containerItem);
                }
                activeContainer.getContainerItems().add(containerItem);
                String location = (parent != null) ? containerItemMapper.mapItemName(parent) : activeContainer.getContainerName();
                message = String.format("Added %dx '%s' inside '%s'.", addDto.getQuantity(), itemToAdd.getItemName(), location);
            }
            if (addDto.getUserNote() != null && !addDto.getUserNote().isBlank()) {
                containerItem.setUserNote(addDto.getUserNote());
            }
        } else {
            // Item is a parent (not stackable). Create a new instance for each quantity.
            message = String.format("Added %dx '%s'.", addDto.getQuantity(), itemToAdd.getItemName());
            logger.info(message);
            for (int i = 0; i < addDto.getQuantity(); i++) {
                ContainerItem newContainerItem = new ContainerItem();
                newContainerItem.setItem(itemToAdd);
                newContainerItem.setContainer(activeContainer);
                newContainerItem.setQuantity(1); // Non-stackable items always have quantity 1
                if (addDto.getUserNote() != null && !addDto.getUserNote().isBlank()) {
                    newContainerItem.setUserNote(addDto.getUserNote());
                }
                activeContainer.getContainerItems().add(newContainerItem);
                if (parent != null) {
                    parent.addChild(newContainerItem);
                }
            }
        }
        // We must explicitly save and flush the container here.
        // This forces JPA to execute the SQL INSERT/UPDATE and trigger the auditing listeners (@PrePersist/@PreUpdate).
        // Without this, the lastModifiedDateTime on the ContainerItem would not be set before the mapping occurs,
        // resulting in a null value in the response DTO.
        Container savedContainer = containerRepository.saveAndFlush(activeContainer);

        ContainerSummaryDto summaryDto = containerMapper.toSummaryDto(savedContainer, user);
        return new ServiceResponse<>(summaryDto, message);
    }

    /**
     * Drops an item from the active container.
     * @param id The ID of the item to drop, nullable
     * @param name The name of the item to drop, a fallback if the id is null
     * @param quantity The quantity of the item to drop
     * @param dropChildren Indicates if any children should be dropped as well
     * @param actor The requesting user for whom the container is active for.
     * @return A DTO of the updated container.
     */
    @Override
    @Transactional
    public ServiceResponse<ContainerSummaryDto> dropItemFromActiveContainer(Long id, String name, Integer quantity, Boolean dropChildren, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container to add items to.");
        }

        logger.debug("Id: {}, name: {}, quantity: {}, dropChildren: {}", id, name, quantity, dropChildren);
        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        if (!activeContainer.getContainerItems().isEmpty()) {
            containerItemRepository.fetchChildrenForContainerItems(activeContainer.getContainerItems());
        }
        // Find the specific ContainerItem to drop. Using the unique containerItemId is the most reliable way.
        ContainerItem foundContainerItem = findContainerItem(id, name, activeContainer);


        if (foundContainerItem.getQuantity() < quantity) {
            throw new ValidationException("The container only has " + foundContainerItem.getQuantity() + " of item '" + foundContainerItem.getItem().getItemName() + "', can't remove " + quantity +  ".");
        }


        if (foundContainerItem.getQuantity().equals(quantity)) {
            // If the quantity matches exactly, remove the item from the container.
            if(foundContainerItem.getItem().isParent()) {
                List<ContainerItem> childrenItems = new ArrayList<>(foundContainerItem.getChildren());
                if (Boolean.TRUE.equals(dropChildren)) {
                    activeContainer.getContainerItems().removeAll(childrenItems);
                } else {
                    childrenItems.forEach(foundContainerItem::removeChild);
                }
            }

            activeContainer.getContainerItems().remove(foundContainerItem);
            logger.info("Removed all of item '{}' from container '{}'", foundContainerItem.getItem().getItemName(), activeContainer.getContainerName());
        } else {
           foundContainerItem.setQuantity(foundContainerItem.getQuantity() - quantity);
           logger.info("Decreased quantity of item '{}' by {} in container '{}'. New quantity: {}",
                    foundContainerItem.getItem().getItemName(), quantity, activeContainer.getContainerName(), foundContainerItem.getQuantity());
        }
        // We must explicitly save and flush the container here to ensure the Auditable framework is triggered
        // and that any deletions (from orphanRemoval) are executed.

        String message = "Removed " + quantity + "x '" + foundContainerItem.getItem().getItemName() + "'" + (dropChildren ? " and any children." : ".");
        Container savedContainer = containerRepository.saveAndFlush(activeContainer);
        ContainerSummaryDto summaryDto = containerMapper.toSummaryDto(savedContainer, user);
        return new ServiceResponse<>(summaryDto, message);
    }

    // TODO - update so guild owned container items can be deleted
    /**
     * Delete a container by name and id
     * @param id the id of the container to delete
     * @param name the name of the container to delete
     * @param actor the owner of the container to delete
     * @return A DTO of the deleted container.
     */
    @Override
    @Transactional
    public DeletedEntityDto deleteContainerByIdAndName(Long id, String name, Owner actor) {

        // 1. Find the container to be deleted. The security layer has already confirmed ownership.
        Container containerToDelete = containerRepository.findById(id)
                .orElseThrow(() -> new ContainerNotFoundException("Container with ID " + id + " not found."));

        // 2. Perform the name confirmation check as a business rule.
        if (!containerToDelete.getContainerName().equalsIgnoreCase(name)) {
            throw new ValidationException(
                    String.format("Provided name '%s' does not match the container's actual name '%s'.",
                            name, containerToDelete.getContainerName()));
        }
        // 3. Create the response DTO *before* deleting the entity.
        DeletedEntityDto responseDto = new DeletedEntityDto(
                containerToDelete.getContainerId(),
                containerToDelete.getContainerName(),
                "Container"
        );

        // 4. If the container is the primary for the user, we must nullify the reference.
        if (actor instanceof BohUser) {
            BohUser user = bohUserRepository.findByIdWithPrimaryContainer(actor.getId())
                    .orElseThrow(() -> new OwnerNotFoundException("User with ID " + actor.getId() + " not found during delete operation."));

            if (user.getPrimaryContainer() != null && user.getPrimaryContainer().getContainerId().equals(id)) {
                logger.warn("Deleting the primary container for user '{}'. Setting primary container to null.", user.getDisplayName());
                user.setPrimaryContainer(null);
                bohUserRepository.save(user);
            }
        }

        // 5. Delete the container.
        containerRepository.delete(containerToDelete);
        logger.info("Successfully deleted container '{}' (ID: {})", responseDto.getName(), responseDto.getId());
        return responseDto;
    }

    /**
     * modify an existing ContainerItem's details in an active container
     * @param modifyDto The details of the item to modify
     * @param actor The requesting user for whom the container is active for.
     * @return A DTO of the updated container.
     */
    @Override
    @Transactional
    public ServiceResponse<ContainerSummaryDto> modifyItemInActiveContainer(ModifyItemRequestDto modifyDto, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can modify items in an active container.");
        }

        // 1. Validate DTO to prevent ambiguous requests
        if ((modifyDto.getNewParentId() != null || modifyDto.getNewParentName() != null) && Boolean.TRUE.equals(modifyDto.getMoveToRoot())) {
            throw new ValidationException("Cannot specify both a new parent and move to root. Please choose one.");
        }

        // 2. Get active container
        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        if (!activeContainer.getContainerItems().isEmpty()) {
            containerItemRepository.fetchChildrenForContainerItems(activeContainer.getContainerItems());
        }
        // 3. Find the item to modify using the resilient finder.
        // This uses the ID if present, otherwise falls back to the name.
        ContainerItem itemToModify = findContainerItem(modifyDto.getContainerItemId(), modifyDto.getContainerItemName(), activeContainer);

        boolean modified = false;
        StringBuilder sb = new StringBuilder();
        sb.append("Modified field(s) [");

        // 4. Update the note if the 'note' field is present in the request body
        if (modifyDto.getNote() != null) {
            // Allow clearing the note by passing an empty or blank string
            sb.append("note");
            itemToModify.setUserNote(modifyDto.getNote().isBlank() ? null : modifyDto.getNote());
            logger.info("Updated note for item '{}' (ID: {})", containerItemMapper.mapItemName(itemToModify), itemToModify.getContainerItemId());
            modified = true;
        }

        // 5. Handle moving the item to a new parent
        // [FIX] Also check that the parent name is not blank to avoid attempting a move with an empty identifier.
        if (modifyDto.getNewParentId() != null || (modifyDto.getNewParentName() != null && !modifyDto.getNewParentName().isBlank())) {
            ContainerItem newParent = findContainerItem(modifyDto.getNewParentId(), modifyDto.getNewParentName(), activeContainer);

            // [HARD FAIL] Perform validation. If it fails, an exception is thrown, and the operation stops.
            validateParentage(itemToModify, newParent);

            // If there's an old parent, correctly sever the bidirectional link
            if (itemToModify.getParent() != null) {
                itemToModify.getParent().removeChild(itemToModify);
            }
            newParent.addChild(itemToModify);
            logger.info("Moved item '{}' into parent '{}'", containerItemMapper.mapItemName(itemToModify), containerItemMapper.mapItemName(newParent));
            sb.append(modified ? ", location" : "location");
            modified = true;

        } else if (Boolean.TRUE.equals(modifyDto.getMoveToRoot())) {
            if (itemToModify.getParent() != null) {
                // Move to root by severing the link with the current parent
                itemToModify.getParent().removeChild(itemToModify);
                logger.info("Moved item '{}' to the container root.", containerItemMapper.mapItemName(itemToModify));

                sb.append(modified ? ", location" : "location");
                modified = true;
            } else {
                logger.info("Item '{}' is already at the root. No move performed.", containerItemMapper.mapItemName(itemToModify));
            }
        }

        // 6. Update the quantity if the 'quantity' field is present in the request body
        if (modifyDto.getNewQuantity() != null) {
            // newQuantity must be > 0
            if (modifyDto.getNewQuantity() <= 0) {
                throw new ValidationException("New quantity must be greater than 0.");
            }
            logger.info("Updated quantity of item '{}' to {}.", containerItemMapper.mapItemName(itemToModify), modifyDto.getNewQuantity());
            itemToModify.setQuantity(modifyDto.getNewQuantity());
            sb.append(modified ? ", quantity" : "quantity");
            modified = true;
        }
        sb.append("] for item: ").append(itemToModify.getItem().getItemName()).append(".");
        if (!modified) {
            logger.warn("Modify item request received for item ID {}, but no changes were specified in the request body.", itemToModify.getContainerItemId());
            // No need to save if no changes were made, just return the current state
            ContainerSummaryDto summaryDto = containerMapper.toSummaryDto(activeContainer, user);
            return new ServiceResponse<>(summaryDto, "No changes were made to the item.");
        }

        // 6. Save the container to persist all changes and return the updated state
        Container savedContainer = containerRepository.saveAndFlush(activeContainer);
        ContainerSummaryDto summaryDto = containerMapper.toSummaryDto(activeContainer, user);
        return new ServiceResponse<>(summaryDto, sb.toString());
    }

    /**
     * generates autocomplete result set for given prefix of ContainerItems inside user's active container
     * @param prefix The search string to filter by
     * @param actor The requesting user for whom the container is active for.
     * @return A list of DTOs of the found containerItems.
     */
    @Override
    @Transactional
    public List<AutoCompleteDto> autocompleteContainerItemsInActiveContainer(String prefix, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container.");
        }
        logger.info("Searching for items with prefix '{}' for actor: {}", prefix, actor.getDisplayName());

        List<AutoCompleteProjection> projections = containerItemRepository.findItemsForAutocomplete(prefix, user.getId());

        if (projections.isEmpty()) {
            logger.info("No items found for autocomplete with prefix '{}'.", prefix);
        }

        return projections.stream()
                .map(p -> new AutoCompleteDto(p.getId(), p.getLabel(), p.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * generates autocomplete result set for given prefix of 'Parent' ContainerItems inside user's active container
     * @param prefix The search string to filter by
     * @param actor The requesting user for whom the container is active for.
     * @return A list of DTOs of the found containerItems.
     */
    @Override
    @Transactional
    public List<AutoCompleteDto> autocompleteParentContainerItemsInActiveContainer(String prefix, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container.");
        }
        logger.info("Searching for parent items with prefix '{}' for actor: {}", prefix, actor.getDisplayName());

        List<AutoCompleteProjection> projections = containerItemRepository.findParentItemsForAutocomplete(prefix, user.getId());

        if (projections.isEmpty()) {
            logger.info("No parent items found for autocomplete with prefix '{}'.", prefix);
        }

        return projections.stream()
                .map(p -> new AutoCompleteDto(p.getId(), p.getLabel(), p.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Activates a container for a given owner, setting it as the primary container.
     *
     * @param user The user for whom to activate the container.
     * @param containerToActivate The container to make active.
     * @return A DTO of the newly activated container.
     */
    private ContainerSummaryDto activate(BohUser user, Container containerToActivate) {
        BohUser managedUser = bohUserRepository.findByIdWithPrimaryContainer(user.getId())
                .orElseThrow(() -> new OwnerNotFoundException("User with ID " + user.getId() + " not found."));

        logger.info("Setting container '{}' as primary for user '{}'",
                containerToActivate.getContainerName(), managedUser.getDisplayName());

        containerToActivate.setLastActiveDateTime(LocalDateTime.now());
        managedUser.setPrimaryContainer(containerToActivate);
        ownerRepository.save(managedUser);


        return containerMapper.toSummaryDto(containerToActivate, managedUser);
    }

    /**
     * Finds a specific ContainerItem within a given container. This is a "hard-failing" method.
     * It will throw an exception if the item is not found or if the name is ambiguous.
     *
     * @param id The unique ID of the ContainerItem.
     * @param name The name of the ContainerItem (used as a fallback if id is null).
     * @param container The container to search within.
     * @return The found ContainerItem.
     * @throws ItemNotFoundException if the specified ContainerItem is not found.
     * @throws AmbiguousResourceException if the specified ContainerItem name matches multiple items.
     * @throws ValidationException if neither an ID nor a name is provided.
     */
    private ContainerItem findContainerItem(Long id, String name, Container container) {
        if (id == null && (name == null || name.isBlank())) {
            throw new ValidationException("An item ID or name must be provided to identify the item.");
        }

        if (id != null) {
            return container.getContainerItems().stream()
                    .filter(ci -> id.equals(ci.getContainerItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ItemNotFoundException("Item with ID " + id + " not found in container '" + container.getContainerName() + "'."));
        } else { // TODO fix find by name to check both item's name and item's fully qualified location name (mapItemName)
                //   because if no id, then autocomplete failed and might have just typed base name
            List<ContainerItem> potentialItems = container.getContainerItems().stream()
                    .filter(ci -> name.equalsIgnoreCase(containerItemMapper.mapItemName(ci)))
                    .toList();

            if (potentialItems.isEmpty()) {
                throw new ItemNotFoundException("Item named '" + name + "' not found in container '" + container.getContainerName() + "'.");
            }
            if (potentialItems.size() > 1) {
                throw new AmbiguousResourceException("Multiple items found with the name '" + name + "'. Please be more specific or use the item's unique ID.");
            }
            return potentialItems.get(0);
        }
    }

    /**
     * Finds a potential parent ContainerItem. This is a "soft-failing" method for optional parents.
     * It returns an empty Optional if no parent is specified.
     * It "hard-fails" by throwing an exception if an *invalid* or *ambiguous* parent is specified.
     */
    private Optional<ContainerItem> findOptionalParentItem(Long id, String name, Container container) {
        if (id == null && (name == null || name.isBlank())) {
            return Optional.empty(); // No parent was specified, which is valid.
        }
        // A parent was specified, so use the hard-failing finder to locate it.
        return Optional.of(findContainerItem(id, name, container));
    }

    /**
     * Runs a series of validation checks before changing an item's parent, throwing an exception on failure.
     * @param itemToMove The item that is being moved. Can be null when validating a new item.
     * @param newParent The potential new parent for the item.
     * @throws ValidationException if any parenting rule is violated.
     */
    private void validateParentage(ContainerItem itemToMove, ContainerItem newParent) {
        // Rule 1: A new parent must be a parent-type item.
        if (!newParent.getItem().isParent()) {
            throw new ValidationException("Item '" + containerItemMapper.mapItemName(newParent) + "' cannot contain other items.");
        }
        // Rule 2: An item cannot be its own parent.
        if (itemToMove != null && newParent.getContainerItemId().equals(itemToMove.getContainerItemId())) {
            throw new ValidationException("An item cannot be its own parent.");
        }
        // Rule 3: An item cannot be moved into one of its own descendants (circular dependency).
        ContainerItem current = newParent;
        while (current != null) {
            if (itemToMove != null && current.getContainerItemId().equals(itemToMove.getContainerItemId())) {
                throw new ValidationException("Cannot move item into one of its own descendants, as this would create a circular reference.");
            }
            current = current.getParent();
        }
    }
}
