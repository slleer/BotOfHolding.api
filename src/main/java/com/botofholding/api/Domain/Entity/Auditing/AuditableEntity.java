package com.botofholding.api.Domain.Entity.Auditing;

import com.botofholding.api.Domain.Entity.Owner;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * A base class for entities that require a full audit trail, including
 * who created and last modified the entity. Inherits timestamps from ModifiableEntity.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditableEntity extends ModifiableEntity {

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CRE_ID", updatable = false)
    protected Owner createdBy;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LST_MDFD_ID") // Assuming you add a LST_MDFD_ID column
    protected Owner lastModifiedBy;
}