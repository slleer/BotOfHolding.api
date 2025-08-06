package com.botofholding.api.Config;

import com.botofholding.api.Domain.Entity.Owner;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<Owner> {

    @Override
    public Optional<Owner> getCurrentAuditor() {
        // First, try to get the "Actor" from the request attributes set by our filter.
        // This is the most reliable source for whom *performed* the action.
        Optional<Owner> actorFromRequest = Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> (Owner) attributes.getAttribute("requestActor", RequestAttributes.SCOPE_REQUEST));

        if (actorFromRequest.isPresent()) {
            return actorFromRequest;
        }

        // If no actor is in the request (e.g., for system processes like data initializers),
        // fall back to using the principal from the security context.
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .flatMap(principal -> {
                    if (principal instanceof Owner) {
                        return Optional.of((Owner) principal);
                    }
                    // Could be an anonymous user or system principal, which is fine to be empty.
                    return Optional.empty();
                });
    }
}
