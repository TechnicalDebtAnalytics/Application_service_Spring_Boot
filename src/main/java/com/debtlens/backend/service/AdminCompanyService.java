package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.dto.response.RepositoryResponseDTO;

import java.util.List;
import java.time.LocalDate;
import com.debtlens.backend.dto.response.AdminAnalysisJobDetailDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCompanyService {

    Page<AdminCompanyResponseDTO> getAllCompanies(String query, LocalDate createdFrom, LocalDate createdTo, Pageable pageable);

    AdminCompanyResponseDTO getCompany(Long companyId);

    List<RepositoryResponseDTO> getCompanyRepositories(Long companyId);

    Page<AdminUserResponseDTO> getCompanyUsers(Long companyId, String query, Pageable pageable);

    Page<AnalysisResponseDTO> getCompanyAnalysisJobs(Long companyId, String query, String status, Pageable pageable);

    Page<AnalysisResponseDTO> getAllAnalysisJobs(
            String query,
            Long companyId,
            Long repositoryId,
            String status,
            LocalDate startedFrom,
            LocalDate startedTo,
            Pageable pageable
    );

    AdminAnalysisJobDetailDTO getAnalysisJob(Long analysisId);
}
