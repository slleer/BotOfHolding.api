package com.botofholding.api.Controller;

import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Owner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * An abstract base class for controllers to inherit common functionality,
 * such as retrieving authenticated principals and actors from the security context.
 * This promotes DRY principles and consistency across the API.
 */
public abstract class BaseController {

    /**
     * Retrieves the authenticated principal (the owner of the resource) from the security context.
     * @return The authenticated {@link Owner}.
     * @throws IllegalStateException if the principal is not found or is of an incorrect type.
     */
    protected Owner getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Owner)) {
            throw new IllegalStateException("Principal not found or is of an incorrect type in Security Context.");
        }
        return (Owner) authentication.getPrincipal();
    }

    /**
     * A specialized helper that retrieves the principal and safely casts it to a {@link BohUser}.
     * @return The authenticated {@link BohUser}.
     * @throws IllegalStateException if the principal is not a BohUser.
     */
    protected BohUser getAuthenticatedBohUser() {
        Owner principal = getAuthenticatedPrincipal();
        if (!(principal instanceof BohUser)) {
            // This should be caught by the security filter, but it's a good safeguard.
            throw new IllegalStateException("Principal is not a BohUser. This endpoint is for user operations only.");
        }
        return (BohUser) principal;
    }

    /**
     * Retrieves the request actor (the user performing the action) from the request attributes.
     * @return The actor {@link Owner}.
     * @throws IllegalStateException if the actor is not found in the request context.
     */
    protected Owner getRequestActor() {
        Owner actor = (Owner) RequestContextHolder.currentRequestAttributes()
                .getAttribute("requestActor", RequestAttributes.SCOPE_REQUEST);
        if (actor == null) {
            // This indicates a server-side configuration error, as the JwtAuthFilter should always set this.
            throw new IllegalStateException("Request actor not found in context. This indicates a filter configuration issue.");
        }
        return actor;
    }
}