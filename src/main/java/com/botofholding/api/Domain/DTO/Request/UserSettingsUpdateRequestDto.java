package com.botofholding.api.Domain.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSettingsUpdateRequestDto {
    private Boolean ephemeralContainer;
    private Boolean ephemeralUser;
    private Boolean ephemeralItem;
}
