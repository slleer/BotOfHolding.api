package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Enum.OwnerType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Optional;



@Entity
@Table(name = "USER_DATA")
@DiscriminatorValue("USER")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@PrimaryKeyJoinColumn(name = "USER_ID")
@NoArgsConstructor
public class BohUser extends Owner {

    @Column(name = "USR_NME")
    @EqualsAndHashCode.Include
    private String bohUserName;

    @Column(name = "USR_TAG")
    private String bohUserTag;

    @Column(name = "GLBL_USR_NME")
    private String bohGlobalUserName;

    // --- Auditing Fields (Timestamps Only) ---
    @CreatedDate
    @Column(name = "CRE_DTTM", nullable = false, updatable = false)
    private LocalDateTime creationDateTime;

    @LastModifiedDate
    @Column(name = "LST_MDFD_DTTM")
    private LocalDateTime lastModifiedDateTime;
    // --- End Auditing Fields ---

    @OneToOne(
            mappedBy = "owner",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private UserSettings userSettings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRMY_CNTNR_ID", referencedColumnName = "CNTNR_ID") // Added referencedColumnName
    private Container primaryContainer;

    @Column(name = "LST_ACTV_DTTM")
    private LocalDateTime lastActive;



    @Builder
    public BohUser(Long discordId, String bohUserName, String bohUserTag, String bohGlobalUserName, Container primaryContainer) {
        super(discordId);
        this.bohUserName = bohUserName;
        this.bohUserTag = bohUserTag;
        this.bohGlobalUserName = bohGlobalUserName;
        this.primaryContainer = primaryContainer;
        this.lastActive = LocalDateTime.now();
        this.setUserSettings(new UserSettings());
    }

    public void setUserSettings(UserSettings userSettings) {
        if (userSettings == null) {
            throw new IllegalArgumentException("UserSettings cannot be null.");
        }
        this.userSettings = userSettings;
        userSettings.setOwner(this);
    }

    @Override
    public OwnerType getOwnerType() {
        return OwnerType.USER;
    }

    @Override
    public String getDisplayName() {
        return Optional.ofNullable(bohGlobalUserName).orElse(bohUserName);
    }

    @Override
    public String toString() {
        return "User: " + bohUserName
                + ", Global UserName: " + Optional.ofNullable(bohGlobalUserName).orElse("N/A")
                + ", Discord Id: " + this.getDiscordId().toString()
                + ", Last Active: " + lastActive.toString();
    }

}
