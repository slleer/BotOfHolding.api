package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Enum.OwnerType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "GUILD_DATA") // Table for guild-specific columns
@DiscriminatorValue("GUILD")
@Getter
@Setter
@PrimaryKeyJoinColumn(name = "GUILD_ID")
public class Guild extends Owner {

    @Column(name = "GUILD_NME")
    private String guildName;


    @CreatedDate
    @Column(name = "CRE_DTTM", nullable = false, updatable = false)
    private LocalDateTime creationDateTime;

    @LastModifiedDate
    @Column(name = "LST_MDFD_DTTM")
    private LocalDateTime lastModifiedDateTime;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CRE_ID", updatable = false)
    protected Owner createdBy;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LST_MDFD_ID")
    protected Owner lastModifiedBy;
    // --- End Auditing Fields ---

    public Guild() {
        super(null);
    }

    @Builder
    public Guild(Long discordId, String guildName) {
        super(discordId);
        this.guildName = guildName;
    }

    @Override
    public OwnerType getOwnerType() {
        return OwnerType.GUILD;
    }

    @Override
    public String getDisplayName() {
        return this.guildName != null ? this.guildName : "NA";
    }
}