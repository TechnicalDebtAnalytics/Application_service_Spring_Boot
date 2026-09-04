package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;

import java.util.List;

public interface AdminCompanyService {

    List<AdminCompanyResponseDTO> getAllCompanies();

    List<AdminUserResponseDTO> getCompanyUsers(Long companyId);

    List<AnalysisResponseDTO> getCompanyAnalysisJobs(Long companyId);

    List<AnalysisResponseDTO> getAllAnalysisJobs();
}