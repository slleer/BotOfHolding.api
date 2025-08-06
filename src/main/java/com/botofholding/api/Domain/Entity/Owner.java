package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Enum.OwnerType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "OWNER") // The base table for all owners
@Inheritance(strategy = InheritanceType.JOINED) // This is the key!
@DiscriminatorColumn(name = "OWNER_TYPE", discriminatorType = DiscriminatorType.STRING) // Column to identify subtype
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class Owner implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OWNER_ID")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "DSCD_ID", unique = true, nullable = false, updatable = false) // Discord IDs are globally unique
    @EqualsAndHashCode.Include // Include in equals/hashCode as it's a unique identifier
    private Long discordId;

    // This defines the "other side" of the relationship from Container
    // It allows you to easily get all containers for any owner.
    @Setter
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Container> containers;

    public abstract OwnerType getOwnerType();

    // An abstract method to get a display name, forcing subtypes to implement it.
    public abstract String getDisplayName();

    protected Owner(Long discordId) {
        this.discordId = discordId;
    }

    /**
     * Safely adds a container to this owner's collection.
     * This method maintains the bidirectional relationship.
     * @param container The container to add.
     */
    public void addContainer(Container container) {
        if (this.containers == null) {
            this.containers = new ArrayList<>();
        }
        if (!this.containers.contains(container)) {
            this.containers.add(container);
            container.setOwner(this); // Set the "other side" of the relationship
        }
    }

    /**
     * Safely removes a container from this owner's collection.
     * This method maintains the bidirectional relationship.
     * @param container The container to remove.
     */
    public void removeContainer(Container container) {
        if (this.containers != null) {
            this.containers.remove(container);
            container.setOwner(null); // Break the "other side" of the relationship
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // For now, we don't have roles. Return an empty list.
        // This is where you would add roles like "ROLE_USER", "ROLE_ADMIN".
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        // We are not using password-based authentication.
        return null;
    }

    @Override
    public String getUsername() {
        // Use the unique Discord ID as the "username".
        return this.discordId.toString();
    }

    // For our purposes, accounts are always active and enabled.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}