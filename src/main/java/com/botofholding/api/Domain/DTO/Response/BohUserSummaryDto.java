package com.botofholding.api.Domain.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BohUserSummaryDto {
    private Long id;
    private Long discordId;
    private String bohUserName;
    private String bohUserTag;
    private String bohGlobalUserName;
    private LocalDateTime lastActive;

    public String getDisplayName() {
        return Optional.ofNullable(bohGlobalUserName).orElse(bohUserName);
    }
    @Override
    public String toString() {
        return "User: " + bohUserName
                + ", Tag: " + Optional.ofNullable(bohUserTag).orElse("N/A")
                + ", Global UserName: " + Optional.ofNullable(bohGlobalUserName).orElse("N/A")
                + ", discordId: " + discordId
                + ", Last Active: " + (lastActive != null ? lastActive.toString() : "N/A");
    }
}
        