package com.debtlens.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddRepositoriesRequestDTO(
        @NotEmpty(message = "At least one repository must be selected")
        List<SelectedRepoDTO> repositories
) {
}
