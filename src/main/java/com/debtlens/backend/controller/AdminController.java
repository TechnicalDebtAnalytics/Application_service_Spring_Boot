package com.debtlens.backend.controller;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.AdminCompanyService;
import com.debtlens.backend.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RepositoryRepository repositoryRepository;
    private final AdminCompanyService adminCompanyService;
    private final AdminUserService adminUserService;

    public AdminController(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            RepositoryRepository repositoryRepository,
            AdminCompanyService adminCompanyService,
            AdminUserService adminUserService
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.repositoryRepository = repositoryRepository;
        this.adminCompanyService = adminCompanyService;
        this.adminUserService = adminUserService;
    }

    @GetMapping("/companies")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<AdminCompanyResponseDTO>> getAllCompanies() {

        return ResponseEntity.ok(
            adminCompanyService.getAllCompanies()
        );
    }

    @GetMapping("/companies/{companyId}/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<AdminUserResponseDTO>> getCompanyUsers(
            @PathVariable Long companyId
    ) {

        return ResponseEntity.ok(
            adminCompanyService.getCompanyUsers(companyId)
        );
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<AdminUserResponseDTO>> getAllUsers() {

        return ResponseEntity.ok(
            adminUserService.getAllUsers()
        );
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Map<String, Object> getSystemStats() {

        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        long totalRepositories = repositoryRepository.count();

        return Map.of(
                "totalUsers", totalUsers,
                "totalCompanies", totalCompanies,
                "totalRepositories", totalRepositories
        );
    }
}