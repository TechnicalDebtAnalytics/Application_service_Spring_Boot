package com.debtlens.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CompanyRequestDTO(
        @NotBlank(message = "Company name is required")
        String companyName,

        @NotBlank(message = "GitHub organization name is required")
        String githubOrganizationName,

        @NotEmpty(message = "At least one repository must be selected")
        List<SelectedRepoDTO> selectedRepositories
) {
}