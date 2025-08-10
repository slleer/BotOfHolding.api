package com.botofholding.api.Domain.DTO.Response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerItemSummaryDto {
    private Long containerItemId;
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private String userNote;
    private LocalDateTime lastModified;
    private List<ContainerItemSummaryDto> children;
    private ContainerItemSummaryDto parent;
}
