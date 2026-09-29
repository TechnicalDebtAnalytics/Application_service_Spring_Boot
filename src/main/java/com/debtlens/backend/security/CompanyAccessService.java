package com.debtlens.backend.security;

import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CompanyAccessService {

    private final CompanyRepository companyRepository;
    private final Super_AdminRepository superAdminRepository;
    private final Auth0UserService auth0UserService;

    public CompanyAccessService(
            CompanyRepository companyRepository,
            Super_AdminRepository superAdminRepository,
            Auth0UserService auth0UserService
    ) {
        this.companyRepository = companyRepository;
        this.superAdminRepository = superAdminRepository;
        this.auth0UserService = auth0UserService;
    }

    /**
     * Returns a company only when the authenticated user is one of its Super Admins or Members.
     */
    public Company requireCompanyAccess(Long companyId) {
        User currentUser = auth0UserService.getAuthenticatedUser();
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found with ID: " + companyId);
        }

        return companyRepository.findAccessibleByCompanyIdAndUserId(companyId, currentUser.getUserId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Access denied: You are not a member or Super Admin of this company"
                ));
    }

    /**
     * Returns the authenticated user's Super Admin assignment for the requested company.
     */
    public Super_Admin requireSuperAdminAssignment(Long companyId) {
        User currentUser = auth0UserService.getAuthenticatedUser();
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found with ID: " + companyId);
        }

        return superAdminRepository
                .findByUserUserIdAndCompanyCompanyId(currentUser.getUserId(), companyId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Access denied: You are not a Super Admin of this company"
                ));
    }

    public Company requireSuperAdminAccess(Long companyId) {
        return requireSuperAdminAssignment(companyId).getCompany();
    }
}
