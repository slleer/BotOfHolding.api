package com.botofholding.api.Domain.DTO.Request;


import com.botofholding.api.Domain.Enum.OwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerRequestDto {

    /**
     * Allows container names from 1-50 characters.
     * Can contain letters, numbers, spaces, hyphens, apostrophes, and periods.
     * As well as right and left single quote marks.
     */
    @Size(max = 50, message = "Container name must be between 1 and 50 characters.")
    @Pattern(regexp = "^[\\p{L}\\p{N} '-’‘.]+$", message = "Container name can only contain letters, numbers, spaces, hyphens, apostrophes, and periods.")
    @NotBlank(message = "Container name cannot be blank")
    private String containerName;

    private String containerDescription;

    // Optional: if you want to specify the type on creation
    private String containerTypeName;
    private boolean isActive;
    public void setContainerName(String containerName) {
        this.containerName = (containerName == null) ? null : containerName.trim();
    }
}
