package com.botofholding.api.Domain.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BohUserUpdateRequestDto {
    //TODO remove discordId from DTOs
    private Long bohUserDiscordId;
    private String bohUserName;
    private String bohGlobalUserName;
    private LocalDateTime lastActive;
}
