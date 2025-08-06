package com.botofholding.api.Domain.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetContainerRequestDto {


    @NotBlank(message = "Container name cannot be blank")
    private String containerName;

    private String containerDescription;

    private Long guildDiscordId;

    // Optional: if you want to specify the type on creation
    private String containerTypeName;
}
