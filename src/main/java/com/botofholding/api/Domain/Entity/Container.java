package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Entity.Auditing.AuditableEntity;
import com.botofholding.api.Domain.Enum.OwnerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Table(name = "CNTNR")
@Entity
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Container extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CNTNR_ID")
    @EqualsAndHashCode.Include
    private Long containerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OWNER_ID", nullable = false)
    private Owner owner;

    @Column(name = "CNTNR_NME")
    @EqualsAndHashCode.Include
    private String containerName;

    @Column(name = "CNTNR_DESC")
    private String containerDescription;

    @OneToMany(mappedBy = "container", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContainerItem> containerItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CNTNR_TYPE_ID")
    private ContainerType containerType;

    @Column(name = "LST_ACTV_DTTM")
    private LocalDateTime lastActiveDateTime;

    public Container() {
        this.lastActiveDateTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Container: " + containerName + " (Type: " + (containerType != null ? containerType.getContainerTypeName() : "N/A")
                + ", " + (owner.getOwnerType() == OwnerType.USER ? "User" : "Guild") + ": "
                + (owner != null ? owner.getDisplayName() : "N/A")
                + ")";
    }

}
