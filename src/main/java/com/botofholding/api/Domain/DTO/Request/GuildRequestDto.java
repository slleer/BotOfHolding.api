package com.botofholding.api.Domain.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuildRequestDto {

    //TODO DO THIS AFTER todo in JwtAuthFilter, if header can include guildId, then do that and remove this.
    private Long guildDiscordId;
    private String guildName;
}
