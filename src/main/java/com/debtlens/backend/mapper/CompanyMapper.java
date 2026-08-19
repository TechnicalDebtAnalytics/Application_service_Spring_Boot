package com.debtlens.backend.mapper;

import com.debtlens.backend.dto.response.CompanyResponseDTO;
import com.debtlens.backend.dto.response.RepositoryResponseDTO;
import com.debtlens.backend.entity.Company;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CompanyMapper {

    private final RepositoryMapper repositoryMapper;

    public CompanyMapper(RepositoryMapper repositoryMapper) {
        this.repositoryMapper = repositoryMapper;
    }

    public CompanyResponseDTO toDTO(Company company) {
        if (company == null) {
            return null;
        }

        List<RepositoryResponseDTO> repoDTOs = company.getRepositories() != null
                ? company.getRepositories().stream().map(repositoryMapper::toDTO).toList()
                : Collections.emptyList();

        String createdByName = company.getCreatedBy() != null
                ? (company.getCreatedBy().getFirstName() != null ? company.getCreatedBy().getFirstName() + " " : "")
                + (company.getCreatedBy().getLastName() != null ? company.getCreatedBy().getLastName() : "")
                : "Unknown";

        Long createdByUserId = company.getCreatedBy() != null
                ? company.getCreatedBy().getUserId()
                : null;

        String orgUrl = company.getGithubOrganizationUrl();
        String orgName = orgUrl != null && orgUrl.contains("/")
                ? orgUrl.substring(orgUrl.lastIndexOf('/') + 1)
                : orgUrl;

        return new CompanyResponseDTO(
                company.getCompanyId(),
                company.getCompanyName(),
                orgUrl,
                orgName,
                createdByUserId,
                createdByName.trim(),
                repoDTOs.size(),
                repoDTOs,
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
