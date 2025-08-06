package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Entity.Auditing.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table(name = "CNTNR_TYPE")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ContainerType extends AuditableEntity {

    @Column(name = "CNTNR_TYPE_ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long containerTypeId;

    @Column(name = "CNTNR_TYPE_NME")
    @EqualsAndHashCode.Include
    private String containerTypeName;

    @Column(name = "CNTNR_TYPE_DESC")
    private String containerTypeDescription;

    @Override
    public String toString() {
        return "ContainerType: " + containerTypeName + (containerTypeDescription != null && !containerTypeDescription.strip().isEmpty() ? " Description: " + containerTypeDescription : "");
    }

}
