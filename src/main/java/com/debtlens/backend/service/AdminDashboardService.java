package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminActivityResponseDTO;
import com.debtlens.backend.dto.response.AdminSearchResultDTO;
import com.debtlens.backend.dto.response.AdminStatsResponseDTO;

import java.util.List;

public interface AdminDashboardService {
    AdminStatsResponseDTO getStats();
    List<AdminActivityResponseDTO> getRecentActivity(int limit);
    List<AdminSearchResultDTO> search(String query, int limit);
}
