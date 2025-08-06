package com.botofholding.api.Service.Implementations;

import com.botofholding.api.Domain.DTO.Request.AddItemRequestDto;
import com.botofholding.api.Domain.DTO.Request.ContainerRequestDto;
import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
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
import java.util.function.Function;
import java.util.function.Supplier;
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

    @Autowired
    public ContainerServiceImpl(ContainerRepository containerRepository, ContainerMapper containerMapper,
                                OwnerRepository ownerRepository, BohUserRepository bohUserRepository,
                                ItemRepository itemRepository, ContainerItemMapper containerItemMapper) {
        this.containerRepository = containerRepository;
        this.containerMapper = containerMapper;
        this.ownerRepository = ownerRepository;
        this.bohUserRepository = bohUserRepository;
        this.itemRepository = itemRepository;
        this.containerItemMapper = containerItemMapper;
    }

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

    @Override
    @Transactional
    public ContainerSummaryDto findContainerById(@NotNull @Min(1) Long id, Owner actor) {
        BohUser userContext = (actor instanceof BohUser) ? (BohUser) actor : null;
        return containerRepository.findByIdWithItems(id)
                .map(container -> containerMapper.toSummaryDto(container, userContext))
                .orElseThrow(() -> new ContainerNotFoundException("Container with id " + id + " not found."));
    }

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

    @Override
    @Transactional
    public ContainerSummaryDto findActiveContainerForUser(Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can activate containers.");
        }

        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        return containerMapper.toSummaryDto(activeContainer, user);
    }

    @Override
    @Transactional
    public ContainerSummaryDto addItemToActiveContainer(AddItemRequestDto addDto, Owner actor, Owner principal) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container to add items to.");
        }

        // 1. Find the active container for the user.
        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

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
        // Find and validate the parent item using a robust helper method.
        Optional<ContainerItem> parentItemOpt = findAndValidateParentContainerItem(addDto, activeContainer);

        // Correctly handle stackable vs. non-stackable (parent) items.
        if (!itemToAdd.isParent()) {
            // Item is stackable. Find if it exists in the correct location (i.e., with the same parent) to update it.
            ContainerItem parent = parentItemOpt.orElse(null); // null represents the container root

            ContainerItem containerItem = activeContainer.getContainerItems().stream()
                    .filter(ci -> ci.getItem().getItemId().equals(itemToAdd.getItemId()) && Objects.equals(ci.getParent(), parent))
                    .findFirst()
                    .orElseGet(() -> {
                        // No existing stack found in the target location, so create a new one.
                        ContainerItem newItem = new ContainerItem();
                        newItem.setItem(itemToAdd);
                        newItem.setContainer(activeContainer);
                        newItem.setQuantity(0); // Start at 0 before adding
                        if (parent != null) {
                            parent.addChild(newItem);
                        }
                        activeContainer.getContainerItems().add(newItem); // Add to the container's main list
                        String location = (parent != null) ? containerItemMapper.mapItemName(parent) : activeContainer.getContainerName();
                        logger.info("Creating new stack of '{}' inside '{}'", itemToAdd.getItemName(), location);
                        return newItem;
                    });

            // Now, update the found or newly created containerItem
            containerItem.setQuantity(containerItem.getQuantity() + addDto.getQuantity());
            if (addDto.getUserNote() != null && !addDto.getUserNote().isBlank()) {
                containerItem.setUserNote(addDto.getUserNote());
            }
            logger.info("Updated item '{}' in container '{}'. New quantity: {}", itemToAdd.getItemName(), activeContainer.getContainerName(), containerItem.getQuantity());
        } else {
            // Item is a parent (not stackable). Create a new instance for each quantity.
            logger.info("Adding {} new instance(s) of non-stackable item '{}' to container '{}'", addDto.getQuantity(), itemToAdd.getItemName(), activeContainer.getContainerName());
            for (int i = 0; i < addDto.getQuantity(); i++) {
                ContainerItem newContainerItem = new ContainerItem();
                newContainerItem.setItem(itemToAdd);
                newContainerItem.setContainer(activeContainer);
                newContainerItem.setQuantity(1); // Non-stackable items always have quantity 1
                if (addDto.getUserNote() != null && !addDto.getUserNote().isBlank()) {
                    newContainerItem.setUserNote(addDto.getUserNote());
                }
                activeContainer.getContainerItems().add(newContainerItem);
                parentItemOpt.ifPresent(parent -> parent.addChild(newContainerItem));
            }
        }
        // We must explicitly save and flush the container here.
        // This forces JPA to execute the SQL INSERT/UPDATE and trigger the auditing listeners (@PrePersist/@PreUpdate).
        // Without this, the lastModifiedDateTime on the ContainerItem would not be set before the mapping occurs,
        // resulting in a null value in the response DTO.
        Container savedContainer = containerRepository.saveAndFlush(activeContainer);

        // The savedContainer object is now up-to-date and can be mapped correctly.
        return containerMapper.toSummaryDto(savedContainer, user);
    }

    @Override
    @Transactional
    public ContainerSummaryDto dropItemFromActiveContainer(Long id, String name, Integer quantity, Boolean dropChildren, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container to add items to.");
        }

        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        // Find the specific ContainerItem to drop. Using the unique containerItemId is the most reliable way.
        ContainerItem foundContainerItem;
        if (id != null) {
            // The provided ID is the unique ContainerItem ID from the autocomplete selection.
            foundContainerItem = activeContainer.getContainerItems().stream()
                    .filter(ci -> id.equals(ci.getContainerItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ItemNotFoundException("Item with container item ID " + id + " not found in container."));
        } else if (name != null) {
            // Fallback to using the full display name, which can be ambiguous for non-stackable items.
            List<ContainerItem> potentialItems = activeContainer.getContainerItems().stream()
                    .filter(ci -> name.equalsIgnoreCase(containerItemMapper.mapItemName(ci)))
                    .toList();

            if (potentialItems.isEmpty()) {
                throw new ItemNotFoundException("Item named '" + name + "' not found in container '" + activeContainer.getContainerName() + "'.");
            }
            if (potentialItems.size() > 1) {
                throw new AmbiguousResourceException("Multiple items named '" + name + "' exist. Please use the item's unique ID to drop it.");
            }
            foundContainerItem = potentialItems.get(0);
        } else {
            throw new ValidationException("An item ID or name must be provided to drop an item.");
        }

        
        if (foundContainerItem.getQuantity() < quantity) {
            // by the client and should map to a 400 Bad Request, which is appropriate here.
            throw new ValidationException("The container only has " + foundContainerItem.getQuantity() + " of item '" + foundContainerItem.getItem().getItemName() + "', can't remove " + quantity +  ".");
        }
        
        if (foundContainerItem.getQuantity().equals(quantity)) {
            // If the quantity matches exactly, remove the item from the container.

            // [FIX] We must decouple children BEFORE removing the parent from the container's collection.
            // If we remove the parent first, orphanRemoval marks it for deletion, and when we then
            // modify the children, Hibernate gets confused about the state of the relationship,
            // leading to the "detached entity" error during flush.
            if(foundContainerItem.getItem().isParent()) {
                // Make a copy of the children list to avoid ConcurrentModificationException
                // as we will be modifying the original list via removeChild.
                List<ContainerItem> childrenItems = new ArrayList<>(foundContainerItem.getChildren());
                if (Boolean.TRUE.equals(dropChildren)) {
                    // If we are dropping the children, remove them from the container's main list.
                    // orphanRemoval will handle their deletion from the database.
                    activeContainer.getContainerItems().removeAll(childrenItems);
                } else {
                    // If we are keeping the children, we must properly sever the bidirectional relationship.
                    // This clears the parent's `children` list and sets the `parent` on each child to null.
                    childrenItems.forEach(foundContainerItem::removeChild);
                }
            }

            // Now that children are handled, remove the parent item itself.
            // orphanRemoval=true on the Container entity will ensure the ContainerItem is deleted from the DB.
            activeContainer.getContainerItems().remove(foundContainerItem);
            logger.info("Removed all of item '{}' from container '{}'", foundContainerItem.getItem().getItemName(), activeContainer.getContainerName());
        } else {
           foundContainerItem.setQuantity(foundContainerItem.getQuantity() - quantity);
            logger.info("Decreased quantity of item '{}' by {} in container '{}'. New quantity: {}",
                    foundContainerItem.getItem().getItemName(), quantity, activeContainer.getContainerName(), foundContainerItem.getQuantity());
        }
        // We must explicitly save and flush the container here to ensure the Auditable framework is triggered
        // and that any deletions (from orphanRemoval) are executed.
        Container savedContainer = containerRepository.saveAndFlush(activeContainer);

        // The savedContainer object is now up-to-date and can be mapped correctly.
        return containerMapper.toSummaryDto(savedContainer, user);
    }

    @Override
    @Transactional
    public List<AutoCompleteDto> autocompleteContainerItemsInActiveContainer(String prefix, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container.");
        }
        logger.info("Searching for items with prefix '{}' for actor: {}", prefix, actor.getDisplayName());
//        Pageable top25 = PageRequest.of(0, 25);
//
//        return searchAndMapItems(
//                () -> containerItemRepository.findAllFromActiveContainerForUser(prefix, user, top25),
//                prefix,
//                containerItemMapper::toAutoCompleteDto);
        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        List<AutoCompleteDto> results = activeContainer.getContainerItems().stream()
                .filter(ci -> {
                    // Use the fully-qualified mapped name for filtering to match what the user sees.
                    String fullName = containerItemMapper.mapItemName(ci);
                    // Case-insensitive "contains" search is more user-friendly than "startsWith".
                    return fullName.toLowerCase().contains(prefix.toLowerCase());
                })
                .sorted(Comparator.comparing(containerItemMapper::mapItemName)) // Sort alphabetically by full name
                .limit(25) // Apply pagination
                .map(containerItemMapper::toAutoCompleteDto)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            logger.info("No items found for autocomplete with prefix '{}'.", prefix);
        }
        return results;
    }

    @Override
    @Transactional
    public List<AutoCompleteDto> autocompleteParentContainerItemsInActiveContainer(String prefix, Owner actor) {
        if (!(actor instanceof BohUser user)) {
            throw new UnsupportedOperationException("Only users can have an active container.");
        }
        logger.info("Searching for parent items with prefix '{}' for actor: {}", prefix, actor.getDisplayName());
//        Pageable top25 = PageRequest.of(0, 25);
//
//        return searchAndMapItems(
//                () -> containerItemRepository.findAllParentsFromActiveContainer(prefix, user, top25),
//                prefix,
//                containerItemMapper::toAutoCompleteDto);
        Container activeContainer = containerRepository.findActiveContainerWithItemsForUser(user)
                .orElseThrow(() -> new ContainerNotFoundException("No active container found for user " + user.getDisplayName()));

        List<AutoCompleteDto> results = activeContainer.getContainerItems().stream()
                .filter(ci -> ci.getItem() != null && ci.getItem().isParent()) // Filter for parents first
                .filter(ci -> {
                    String fullName = containerItemMapper.mapItemName(ci);
                    return fullName.toLowerCase().contains(prefix.toLowerCase());
                })
                .sorted(Comparator.comparing(containerItemMapper::mapItemName))
                .limit(25)
                .map(containerItemMapper::toAutoCompleteDto)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            logger.info("No parent items found for autocomplete with prefix '{}'.", prefix);
        }
        return results;
    }

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
     * Finds and validates a potential parent ContainerItem based on the AddItemRequestDto.
     * This helper method encapsulates the logic for finding by ID or name, handling ambiguity,
     * and ensuring the found item is capable of being a parent.
     *
     * @param addDto The request DTO containing the parent's ID or name.
     * @param activeContainer The container to search within.
     * @return An Optional containing the validated parent ContainerItem, or an empty Optional if no parent was specified.
     * @throws ItemNotFoundException if the specified parent is not found.
     * @throws AmbiguousResourceException if the specified parent name matches multiple items.
     * @throws ValidationException if the found item is not a valid parent (i.e., its `isParent` flag is false).
     */
    private Optional<ContainerItem> findAndValidateParentContainerItem(AddItemRequestDto addDto, Container activeContainer) {
        if (addDto.getInsideId() == null && (addDto.getInsideName() == null || addDto.getInsideName().isBlank())) {
            return Optional.empty();
        }

        // [IMPROVEMENT] Implement "soft-fail" for parent assignment per the TODO.
        // If the parent is invalid, log a warning and add the item to the root instead of throwing an error.
        try {
            Optional<ContainerItem> parentOpt;
            if (addDto.getInsideId() != null) {
                // The provided ID is the unique ContainerItem ID from the autocomplete selection.
                parentOpt = activeContainer.getContainerItems().stream()
                        .filter(ci -> addDto.getInsideId().equals(ci.getContainerItemId()))
                        .findFirst();
                if (parentOpt.isEmpty()) {
                    throw new ItemNotFoundException("The specified parent item (container item ID: " + addDto.getInsideId() + ") was not found in the container.");
                }
            } else { // Find by name
                // Fallback to using the full display name, which can be ambiguous for non-stackable items.
                List<ContainerItem> potentialParents = activeContainer.getContainerItems().stream()
                        .filter(ci -> addDto.getInsideName().equalsIgnoreCase(containerItemMapper.mapItemName(ci)))
                        .toList();

                if (potentialParents.isEmpty()) {
                    throw new ItemNotFoundException("The specified parent item ('" + addDto.getInsideName() + "') was not found in the container.");
                }
                if (potentialParents.size() > 1) {
                    throw new AmbiguousResourceException("Multiple potential parent items found with the name '" + addDto.getInsideName() + "'. Please use the parent's unique ID instead.");
                }
                parentOpt = Optional.of(potentialParents.get(0));
            }

            ContainerItem parentItem = parentOpt.get();

            if (!parentItem.getItem().isParent()) {
                throw new ValidationException("The item '" + parentItem.getItem().getItemName() + "' cannot contain other items.");
            }

            return parentOpt;
        } catch (ItemNotFoundException | AmbiguousResourceException | ValidationException e) {
            logger.warn("Could not set parent for item. Reason: {}. The item will be added to the container root.", e.getMessage());
            return Optional.empty();
        }
    }
}
