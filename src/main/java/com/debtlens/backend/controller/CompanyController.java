package com.debtlens.backend.controller;

import com.debtlens.backend.dto.request.AddRepositoriesRequestDTO;
import com.debtlens.backend.dto.request.CompanyRequestDTO;
import com.debtlens.backend.dto.response.CompanyAvailableRepoDTO;
import com.debtlens.backend.dto.response.CompanyResponseDTO;
import com.debtlens.backend.dto.response.RepositoryResponseDTO;
import com.debtlens.backend.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * Create a new company from a verified GitHub organization with selected repositories.
     */
    @PostMapping
    public ResponseEntity<CompanyResponseDTO> createCompany(
            @Valid @RequestBody CompanyRequestDTO request
    ) {
        CompanyResponseDTO company = companyService.createCompany(request);
        return ResponseEntity.ok(company);
    }

    /**
     * Get all companies where the authenticated user is a Super Admin.
     */
    @GetMapping("/my-admin")
    public ResponseEntity<List<CompanyResponseDTO>> getMyAdminCompanies() {
        List<CompanyResponseDTO> companies = companyService.getMyAdminCompanies();
        return ResponseEntity.ok(companies);
    }

    /**
     * Get all companies where the authenticated user is a Member.
     */
    @GetMapping("/my-member")
    public ResponseEntity<List<CompanyResponseDTO>> getMyMemberCompanies() {
        List<CompanyResponseDTO> companies = companyService.getMyMemberCompanies();
        return ResponseEntity.ok(companies);
    }

    /**
     * Get company details by company ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> getCompanyById(
            @PathVariable Long id
    ) {
        CompanyResponseDTO company = companyService.getCompanyById(id);
        return ResponseEntity.ok(company);
    }

    /**
     * Add more repositories to an existing company (Super Admin only).
     */
    @PostMapping("/{companyId}/repositories")
    public ResponseEntity<CompanyResponseDTO> addRepositories(
            @PathVariable Long companyId,
            @Valid @RequestBody AddRepositoriesRequestDTO request
    ) {
        CompanyResponseDTO updated = companyService.addRepositoriesToCompany(companyId, request.repositories());
        return ResponseEntity.ok(updated);
    }

    /**
     * Fetch all organization repositories from GitHub with status indicating if already added to this company.
     */
    @GetMapping("/{companyId}/available-repositories")
    public ResponseEntity<List<CompanyAvailableRepoDTO>> getAvailableRepositories(
            @PathVariable Long companyId
    ) {
        List<CompanyAvailableRepoDTO> available = companyService.getAvailableRepositoriesForCompany(companyId);
        return ResponseEntity.ok(available);
    }

    /**
     * Fetch all currently imported repositories for this company.
     */
    @GetMapping("/{companyId}/repositories")
    public ResponseEntity<List<RepositoryResponseDTO>> getCompanyRepositories(
            @PathVariable Long companyId
    ) {
        List<RepositoryResponseDTO> repos = companyService.getCompanyRepositories(companyId);
        return ResponseEntity.ok(repos);
    }
}