package com.botofholding.api.Security;


import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Guild;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.Domain.Enum.OwnerType;
import com.botofholding.api.Repository.OwnerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;
    private final OwnerRepository ownerRepository;

    public JwtAuthFilter(JwtService jwtService, OwnerRepository ownerRepository) {
        this.jwtService = jwtService;
        this.ownerRepository = ownerRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        // --- The Actor (WHO is doing this?) ---
        final String actorIdHeader = request.getHeader("X-On-Behalf-Of-User-ID");
        final String actorUserNameHeader = request.getHeader("X-On-Behalf-Of-User-Name");
        final String globalNameHeader = request.getHeader("X-On-Behalf-Of-Global-Name");

        // --- The Principal/Owner (WHOSE stuff is this?) ---
        final String targetOwnerIdHeader = request.getHeader("X-Target-Owner-ID");
        final String ownerTypeHeader = request.getHeader("X-Target-Owner-Type");
        final String ownerNameHeader = request.getHeader("X-Target-Owner-Name"); // Primarily for Guild name provisioning

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        if (jwtService.validateToken(jwt) && "bot-service-account".equals(jwtService.getPrincipalFromToken(jwt))) {
            // Token is valid and belongs to our bot. Now, check for impersonation.
            try {
                // [FIX] We must establish the ACTOR and set it in the context BEFORE
                // we attempt to provision a principal, which might trigger a database save.
                Owner actor = findAndProvisionActor(actorIdHeader, actorUserNameHeader, globalNameHeader);
                request.setAttribute("requestActor", actor); // Set the actor for the AuditorAware bean.

                // Scenario 1: Explicit Target (e.g., acting on a Guild or another User)
                if (targetOwnerIdHeader != null && !targetOwnerIdHeader.isBlank() && ownerTypeHeader != null && !ownerTypeHeader.isBlank()) {
                    log.debug("Processing request with explicit target owner.");

                    Long targetOwnerId = Long.parseLong(targetOwnerIdHeader);
                    OwnerType ownerType = OwnerType.valueOf(ownerTypeHeader.toUpperCase());

                    // Now, when this method calls .save(), the auditor will find the actor.
                    Owner principal = findAndProvisionPrincipal(targetOwnerId, ownerType, ownerNameHeader, actor);

                    setSecurityContext(request, principal);
                    log.debug("Successfully set principal to {} with Discord ID: {}", ownerType, targetOwnerId);
                }
                // Scenario 2: Implicit Target (e.g., /users/me)
                else {
                    log.debug("Processing request with implicit target owner (actor is principal).");
                    // In this case, the actor IS the principal.
                    setSecurityContext(request, actor);
                    log.debug("Successfully set principal to USER with Discord ID: {}", actorIdHeader);
                }

            } catch (IllegalArgumentException e) {
                log.warn("Invalid impersonation headers provided. Reason: {}", e.getMessage());
            }
        }


        filterChain.doFilter(request, response);
    }

    /**
     * [NEW] Finds an existing actor or provisions a new one.
     */
    private Owner findAndProvisionActor(String actorIdHeader, String actorUserNameHeader, String globalNameHeader) {
        Long actorDiscordId = Long.parseLong(actorIdHeader);
        return ownerRepository.findByDiscordId(actorDiscordId)
                .orElseGet(() -> {
                    log.info("Actor with Discord ID {} not found. Creating new BohUser.", actorDiscordId);
                    // Note: This save call is safe because BohUser does not have a @CreatedBy field.
                    return provisionNewOwner(OwnerType.USER, actorDiscordId, actorUserNameHeader, globalNameHeader);
                });
    }

    /**
     * [NEW] Finds an existing principal or provisions a new one. Optimizes for when actor and principal are the same.
     */
    private Owner findAndProvisionPrincipal(Long targetOwnerId, OwnerType ownerType, String ownerNameHeader, Owner actor) {
        // Optimization: If the target is the same user as the actor, reuse the fetched entity.
        if (ownerType == OwnerType.USER && targetOwnerId.equals(actor.getDiscordId())) {
            log.debug("Principal is the same as the actor. Reusing entity.");
            return actor;
        }
        // Otherwise, fetch or provision the target owner.
        return ownerRepository.findByDiscordId(targetOwnerId)
                .orElseGet(() -> {
                    String nameForNewPrincipal = (ownerType == OwnerType.GUILD) ? ownerNameHeader : actor.getDisplayName();
                    return provisionNewOwner(ownerType, targetOwnerId, nameForNewPrincipal, actor.getDisplayName()); // Pass actor's global name
                });
    }

    /**
     * [REFACTORED] Sets the Principal in the security context. The Actor is now set earlier.
     */
    private void setSecurityContext(HttpServletRequest request, Owner principal) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
    /**
     * A helper method to create a new Owner (BohUser or Guild) on-the-fly.
     * This is used for the principal/target owner.
     */
    private Owner provisionNewOwner(OwnerType type, Long discordId, String name, String globalName) {
        log.info("Principal of type {} with Discord ID {} not found. Creating new entity.", type, discordId);
        switch (type) {
            case USER:
                BohUser newUser = BohUser.builder()
                        .discordId(discordId)
                        .bohUserName(name)
                        .bohGlobalUserName(globalName) // Use the actor's global name
                        .build();
                return ownerRepository.save(newUser);
            case GUILD:
                Guild newGuild = Guild.builder()
                        .discordId(discordId)
                        .guildName(name) // Use the dedicated owner name header for the guild
                        .build();
                return ownerRepository.save(newGuild);
            default:
                throw new IllegalArgumentException("Unsupported OwnerType for provisioning: " + type);
        }
    }
}