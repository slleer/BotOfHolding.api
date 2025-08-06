package com.botofholding.api.Domain.DTO.Response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BohUserWithAllContainersDto extends BohUserSummaryDto {
    private List<ContainerSummaryDto> containers; // List of full ContainerSummaryDto objects
}
