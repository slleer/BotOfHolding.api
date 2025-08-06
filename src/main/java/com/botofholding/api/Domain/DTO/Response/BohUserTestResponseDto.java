package com.botofholding.api.Domain.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BohUserTestResponseDto {
    private String message;
    private Integer bohUserId;
    private String bohUserName;
    private String bohUserTag;
    private LocalDateTime lastActive;
    private Long discordId;
}
        