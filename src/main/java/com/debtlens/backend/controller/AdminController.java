package com.debtlens.backend.controller;

import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.UserRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RepositoryRepository repositoryRepository;

    public AdminController(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            RepositoryRepository repositoryRepository
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.repositoryRepository = repositoryRepository;
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