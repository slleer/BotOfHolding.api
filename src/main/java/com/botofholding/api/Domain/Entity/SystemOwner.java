package com.botofholding.api.Domain.Entity;

import com.botofholding.api.Domain.Enum.OwnerType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@DiscriminatorValue("SYSTEM")
@Table(name = "SYSTEM_OWNER_DATA")
@PrimaryKeyJoinColumn(name = "SYSTEM_OWNER_ID")
public class SystemOwner extends Owner{

    public static final Long SYSTEM_OWNER_DISCORD_ID = -1L;

    @CreatedDate
    @Column(name = "CRE_DTTM", nullable = false, updatable = false)
    private LocalDateTime creationDateTime;

    public SystemOwner() {
        super(null);
    }

    private SystemOwner(Long discordId) {
        super(discordId);
    }

    public static SystemOwner createInstance() {
        return new SystemOwner(SYSTEM_OWNER_DISCORD_ID);
    }

    @Override
    public OwnerType getOwnerType() {
        return OwnerType.SYSTEM;
    }

    @Override
    public String getDisplayName() {
        return "System";
    }
}
