package com.botofholding.api.Security;

import com.botofholding.api.Domain.Entity.Container;
import com.botofholding.api.Domain.Entity.Item;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.Domain.Entity.SystemOwner;
import com.botofholding.api.Domain.Enum.OwnerType;
import com.botofholding.api.Repository.ContainerRepository;
import com.botofholding.api.Repository.ItemRepository;
import com.botofholding.api.Repository.OwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Service("securityService") // The name "securityService" is used in the @PreAuthorize annotation
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    private final ContainerRepository containerRepository;
    private final OwnerRepository ownerRepository;
    private final ItemRepository itemRepository;


    public SecurityService(ContainerRepository containerRepository, OwnerRepository ownerRepository, ItemRepository itemRepository) {
        this.containerRepository = containerRepository;
        this.ownerRepository = ownerRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Checks if the authenticated principal is the owner of a given container.
     * This is the core of our resource-based authorization.
     *
     * @param containerId The ID of the container to check.
     * @param principal The authenticated principal (the impersonated Owner).
     * @return true if the principal owns the container, false otherwise.
     */
    public boolean isContainerOwner(Long containerId, Owner principal) {
        if (principal == null || containerId == null) {
            return false;
        }

        // Find the container and check if its owner's ID matches the principal's ID.
        return containerRepository.findById(containerId)
                .map(container -> container.getOwner().getId().equals(principal.getId()))
                .orElse(false); // If container doesn't exist, deny access.
    }

    /**
     * [NEW & WORLD-CLASS]
     * Checks if the current actor has permission to view a given container.
     * Access is granted if:
     * 1. The actor is the direct owner of the container.
     * 2. The container is owned by a Guild, and the request's principal is that Guild
     *    (implying the bot has already authorized the user for that guild context).
     *
     * @param containerId The ID of the container to check.
     * @param principal The authenticated principal (the context of the request).
     * @return true if the actor can view the container, false otherwise.
     */
    // [FIX] This method must be transactional to allow lazy-loading of the container's 'owner'
    // association during the @PreAuthorize check, which runs before the controller's transaction begins.
    // Without this, a LazyInitializationException occurs, which Spring Security treats as a 403 Forbidden.
    @Transactional(readOnly = true)
    public boolean canModifyContainer(Long containerId, Owner principal) {
        logger.info("Verifying principal {} can modify container {}.", principal.getDisplayName(), containerId);
        if (principal == null || containerId == null) {
            return false;
        }

        Optional<Container> containerOpt = containerRepository.findById(containerId);
        return validateContainerAccess(containerOpt, principal);
    }

    // This method has the same potential for LazyInitializationException and also needs to be transactional.
    @Transactional(readOnly = true)
    public boolean canAccessItem(Long itemId, Owner principal) {
        if (principal == null || itemId == null) {
            return false;
        }

        Optional<Item> itemOptional = itemRepository.findById(itemId);
        if (itemOptional.isEmpty()) {
            return false;
        }

        Owner systemOwner = ownerRepository.findByDiscordId(SystemOwner.SYSTEM_OWNER_DISCORD_ID).orElseGet(() -> {
            SystemOwner so = SystemOwner.createInstance();
            return ownerRepository.save(so);
        });

        Item item = itemOptional.get();
        Owner owner = item.getCreatedBy();

        //Rule 1: item was created by the system
        if (owner.getId().equals(systemOwner.getId())) {
            return true;
        }

        return validateOwnership(owner, getRequestActor(), principal);
    }

    /**
     * Helper to validate container access for actor and principal
     */
    private boolean validateContainerAccess(Optional<Container> containerOptional, Owner principal) {
        if (containerOptional.isEmpty()) {
            logger.info("Container not found.");
            return false;
        }

        Container container = containerOptional.get();
        Owner owner = container.getOwner();
        Owner actor = getRequestActor();

        return validateOwnership(owner, actor, principal);
    }

    private boolean validateOwnership(Owner owner, Owner actor, Owner principal) {
        // [IMPROVEMENT] Clarified log message to be more descriptive.
        logger.info("Validating ownership for resource owned by '{}'. Actor: '{}', Principal: '{}'",
                owner.getDisplayName(), actor.getDisplayName(), principal.getDisplayName());
        // Rule 1: Actor is the direct owner.
        if (owner.getId().equals(actor.getId())) {
            return true;
        }
        // Rule 2: Owner is a Guild
        return owner.getOwnerType() == OwnerType.GUILD && owner.getId().equals(principal.getId());
    }

    /**
     * Helper to retrieve the actor from the current request context.
     */
    private Owner getRequestActor() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        Owner actor = (Owner) requestAttributes.getAttribute("requestActor", RequestAttributes.SCOPE_REQUEST);
        if (actor == null) {
            // This indicates a server-side configuration error, as the JwtAuthFilter should always set this.
            throw new IllegalStateException("Request actor not found in context. This indicates a filter configuration issue.");
        }
        return actor;
    }

}