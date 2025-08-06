package com.botofholding.api.Domain.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "USER_SETTINGS")
@Builder
public class UserSettings {

    @Id
    @Column(name = "STNG_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "STNG_ID")
    private Owner owner;

    @Column(name = "EPHML_CNTNR")
    @Builder.Default
    private boolean ephemeralContainer = true;

    @Column(name = "EPHML_USER")
    @Builder.Default
    private boolean ephemeralUser = true;

    @Column(name = "EPHML_ITEM")
    @Builder.Default
    private boolean ephemeralItem = true;


}
