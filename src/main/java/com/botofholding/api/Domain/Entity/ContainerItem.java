package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Entity.Auditing.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Table(name = "CNTNR_ITEM")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ContainerItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "CNTNR_ITEM_ID", nullable = false)
    private Long containerItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CNTNR_ID", nullable = false)
    private Container container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID", nullable = false)
    private Item item;

    @Column(name = "ITM_QTY")
    private Integer quantity;

    @Column(name = "USER_NOTE")
    private String userNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    private ContainerItem parent;

    @OneToMany(
            mappedBy = "parent",
            // [FIX] Remove orphanRemoval and CascadeType.REMOVE. The service layer now explicitly controls child deletion.
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH},
            fetch = FetchType.LAZY
    )
    private List<ContainerItem> children = new ArrayList<>();

    public void addChild(ContainerItem child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(ContainerItem child) {
        children.remove(child);
        child.setParent(null);
    }

    /**
     * Provides a human-readable representation of the ContainerItem,
     * which is invaluable for logging and debugging.
     * It safely handles potentially uninitialized lazy-loaded associations.
     * @return A string summary of the container item.
     */
    @Override
    public String toString() {
        // Safely access lazy-loaded properties to avoid NullPointerException or LazyInitializationException
        String itemName = (item != null) ? item.getItemName() : "N/A";
        String containerName = (container != null) ? container.getContainerName() : "N/A";

        return String.format("ContainerItem[ID: %d, Item: '%s', Quantity: %d, Container: '%s']",
                this.containerItemId, itemName, this.quantity, containerName);
    }
}
