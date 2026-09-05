package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminUserResponseDTO;

import java.util.List;

public interface AdminUserService {

    List<AdminUserResponseDTO> getAllUsers();
}
