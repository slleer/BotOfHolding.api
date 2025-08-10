package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Entity.Auditing.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "ITEM")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Item extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ITEM_ID")
    @EqualsAndHashCode.Include
    private Long itemId;

    @Column(name = "ITEM_NME")
    @EqualsAndHashCode.Include
    private String itemName;

    @Column(name = "ITEM_DESC")
    private String itemDescription;

    @Column(name = "WEIGHT")
    private Float weight;

    @Column(name = "WEIGHT_UNIT")
    private String weightUnit;

    @Column(name = "VALUE")
    private Float value;

    @Column(name = "VALUE_UNIT")
    private String valueUnit;

    // [FIX] Corrected column name typo from IS_PAET to IS_PARENT for clarity.
    @Column(name = "IS_PARENT")
    private boolean parent;

    @Override
    public String toString() {
        return "Item: " + itemName + " Description: " + itemDescription;
    }

    
}
