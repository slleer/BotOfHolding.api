package com.botofholding.api.Domain.DTO.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BohUserRequestDto {

//    @NotNull(message = "Discord ID cannot be null")
//    private Long discordId;

    @NotNull(message = "Boh User Name cannot be null")
    @Size(min = 2, max = 32, message = "Username must be between 2 and 32 characters.")
    private String bohUserName;

    private String bohUserTag;

    private String bohGlobalUserName;

    public void setBohUserName(String bohUserName) {
        this.bohUserName = (bohUserName == null) ? null : bohUserName.trim();
    }
}
