package com.botofholding.api.Domain.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDto {
    private Long id;
    private Long ownerDiscordId;
    private String ownerDisplayName;
    private String ownerType;
    private boolean ephemeralContainer;
    private boolean ephemeralUser;
    private boolean ephemeralItem;

}
