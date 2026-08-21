package com.debtlens.backend.service;

import com.debtlens.backend.dto.request.CompanyRequestDTO;
import com.debtlens.backend.dto.request.SelectedRepoDTO;
import com.debtlens.backend.dto.response.CompanyAvailableRepoDTO;
import com.debtlens.backend.dto.response.CompanyResponseDTO;
import com.debtlens.backend.dto.response.RepositoryResponseDTO;

import java.util.List;

public interface CompanyService {

    CompanyResponseDTO createCompany(CompanyRequestDTO request);

    List<CompanyResponseDTO> getMyAdminCompanies();

    CompanyResponseDTO getCompanyById(Long companyId);

    CompanyResponseDTO addRepositoriesToCompany(Long companyId, List<SelectedRepoDTO> newRepos);

    List<CompanyAvailableRepoDTO> getAvailableRepositoriesForCompany(Long companyId);

    List<RepositoryResponseDTO> getCompanyRepositories(Long companyId);

    List<CompanyResponseDTO> getMyMemberCompanies();
}