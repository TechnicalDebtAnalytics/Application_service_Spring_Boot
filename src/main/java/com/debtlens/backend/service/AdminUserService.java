package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminUserResponseDTO;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponseDTO> getAllUsers(
            String query,
            Long companyId,
            String companyRole,
            Boolean emailVerified,
            Pageable pageable
    );
}
