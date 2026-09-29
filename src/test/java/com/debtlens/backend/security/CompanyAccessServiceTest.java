package com.debtlens.backend.security;

import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyAccessServiceTest {

    private CompanyRepository companyRepository;
    private Super_AdminRepository superAdminRepository;
    private Auth0UserService auth0UserService;
    private CompanyAccessService service;
    private User currentUser;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        superAdminRepository = mock(Super_AdminRepository.class);
        auth0UserService = mock(Auth0UserService.class);
        service = new CompanyAccessService(companyRepository, superAdminRepository, auth0UserService);

        currentUser = new User();
        currentUser.setUserId(7L);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(currentUser);
    }

    @Test
    void requireCompanyAccessReturnsOnlyTenantScopedCompany() {
        Company company = company(10L);
        when(companyRepository.existsById(10L)).thenReturn(true);
        when(companyRepository.findAccessibleByCompanyIdAndUserId(10L, 7L))
                .thenReturn(Optional.of(company));

        assertSame(company, service.requireCompanyAccess(10L));
        verify(companyRepository).findAccessibleByCompanyIdAndUserId(10L, 7L);
    }

    @Test
    void requireCompanyAccessRejectsOutsider() {
        when(companyRepository.existsById(10L)).thenReturn(true);
        when(companyRepository.findAccessibleByCompanyIdAndUserId(10L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.requireCompanyAccess(10L));
    }

    @Test
    void requireCompanyAccessReturnsNotFoundWithoutScopedLookup() {
        when(companyRepository.existsById(404L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.requireCompanyAccess(404L));
        verify(companyRepository, never()).findAccessibleByCompanyIdAndUserId(404L, 7L);
    }

    @Test
    void requireSuperAdminAccessUsesCompanyRoleAssignment() {
        Company company = company(10L);
        Super_Admin assignment = new Super_Admin();
        assignment.setUser(currentUser);
        assignment.setCompany(company);
        when(companyRepository.existsById(10L)).thenReturn(true);
        when(superAdminRepository.findByUserUserIdAndCompanyCompanyId(7L, 10L))
                .thenReturn(Optional.of(assignment));

        assertSame(company, service.requireSuperAdminAccess(10L));
        assertSame(assignment, service.requireSuperAdminAssignment(10L));
    }

    @Test
    void requireSuperAdminAccessRejectsNonAdmin() {
        when(companyRepository.existsById(10L)).thenReturn(true);
        when(superAdminRepository.findByUserUserIdAndCompanyCompanyId(7L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.requireSuperAdminAccess(10L));
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setCompanyId(id);
        return company;
    }
}
