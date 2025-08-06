package com.botofholding.api.Domain.Entity.Auditing;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * A base class for entities that need creation and modification timestamps.
 * Inherits the creation timestamp from CreatableEntity.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class ModifiableEntity extends CreatableEntity {

    @LastModifiedDate
    @Column(name = "LST_MDFD_DTTM")
    protected LocalDateTime lastModifiedDateTime;
}