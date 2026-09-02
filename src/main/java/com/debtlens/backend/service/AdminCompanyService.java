package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;

import java.util.List;

public interface AdminCompanyService {

    List<AdminCompanyResponseDTO> getAllCompanies();
}