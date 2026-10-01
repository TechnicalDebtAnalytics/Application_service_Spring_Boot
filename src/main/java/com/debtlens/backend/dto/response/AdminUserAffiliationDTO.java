package com.debtlens.backend.dto.response;

public record AdminUserAffiliationDTO(
        Long companyId,
        String companyName,
        String role
) {
}
