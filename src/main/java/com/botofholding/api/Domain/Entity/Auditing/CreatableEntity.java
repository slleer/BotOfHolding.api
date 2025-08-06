package com.botofholding.api.Domain.Entity.Auditing;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A base class for entities that only need to track their creation timestamp.
 */
@Getter
@Setter
@MappedSuperclass // Declares this as a base class for entities, not an entity itself.
@EntityListeners(AuditingEntityListener.class) // Enables Spring Data JPA auditing features.
public abstract class CreatableEntity {

    @CreatedDate
    @Column(name = "CRE_DTTM", nullable = false, updatable = false)
    protected LocalDateTime creationDateTime;
}