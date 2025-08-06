package com.botofholding.api.Domain.DTO.Response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BohUserWithPrimaryContainerDto extends BohUserSummaryDto {
    private ContainerSummaryDto primaryContainer; // Full ContainerSummaryDto object
}
