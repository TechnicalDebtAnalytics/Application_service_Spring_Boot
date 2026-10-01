package com.debtlens.backend.controller;

import com.debtlens.backend.config.SecurityConfig;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.dto.response.CompanyResponseDTO;
import com.debtlens.backend.dto.response.InvitationResponseDTO;
import com.debtlens.backend.dto.response.RegistrationResponse;
import com.debtlens.backend.dto.response.AdminStatsResponseDTO;
import com.debtlens.backend.entity.AnalysisJobStatus;
import com.debtlens.backend.entity.InvitationStatus;
import com.debtlens.backend.exception.GlobalExceptionHandler;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.AdminCompanyService;
import com.debtlens.backend.service.AdminUserService;
import com.debtlens.backend.service.AdminDashboardService;
import com.debtlens.backend.service.AnalysisService;
import com.debtlens.backend.service.AuthService;
import com.debtlens.backend.service.CompanyService;
import com.debtlens.backend.service.InvitationService;
import com.debtlens.backend.service.ReportService;
import com.debtlens.backend.service.SystemHealthService;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        CompanyController.class,
        InvitationController.class,
        AnalysisController.class,
        ReportController.class,
        AdminController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ApiFunctionTest {

    private static final String USER_TOKEN = "user-token";
    private static final String ADMIN_TOKEN = "admin-token";
    private static final String ROLES_CLAIM = "https://debtlens.example.com/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CompanyRepository companyRepository;

    @MockitoBean
    private RepositoryRepository repositoryRepository;

    @MockitoBean
    private AdminCompanyService adminCompanyService;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private SystemHealthService systemHealthService;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void configureSyntheticJwtDecoding() {
        when(jwtDecoder.decode(USER_TOKEN)).thenReturn(jwt(USER_TOKEN, "auth0|user", List.of()));
        when(jwtDecoder.decode(ADMIN_TOKEN)).thenReturn(jwt(
                ADMIN_TOKEN,
                "auth0|system-admin",
                List.of("SYSTEM_ADMIN")
        ));
    }

    @Test
    void registration_validPublicRequestReturnsRegistration() throws Exception {
        when(authService.register(any())).thenReturn(new RegistrationResponse(
                "Registration successful",
                "auth0|new-user",
                "ada@example.test",
                "SYSTEM_USER",
                "ada-lovelace"
        ));

        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ada@example.test",
                                  "password": "correct-password",
                                  "firstName": "Ada",
                                  "lastName": "Lovelace",
                                  "githubUsername": "ada-lovelace"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.auth0UserId").value("auth0|new-user"))
                .andExpect(jsonPath("$.email").value("ada@example.test"));
    }

    @Test
    void registration_invalidRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "firstName": "",
                                  "lastName": "Lovelace",
                                  "githubUsername": "ada-lovelace"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email format"))
                .andExpect(jsonPath("$.errors.password").value("Password must be between 8 and 128 characters"))
                .andExpect(jsonPath("$.errors.firstName").value("First name is required"));

        verifyNoInteractions(authService);
    }

    @Test
    void company_authenticatedValidRequestReturnsCompany() throws Exception {
        when(companyService.createCompany(any())).thenReturn(companyResponse());

        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "DebtLens Labs",
                                  "githubOrganizationName": "debtlens-labs",
                                  "selectedRepositories": [
                                    {
                                      "githubRepositoryId": 1001,
                                      "repositoryName": "backend",
                                      "repositoryUrl": "https://github.com/debtlens-labs/backend",
                                      "defaultBranch": "main"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(10))
                .andExpect(jsonPath("$.companyName").value("DebtLens Labs"))
                .andExpect(jsonPath("$.totalRepositories").value(1));
    }

    @Test
    void company_invalidRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "",
                                  "githubOrganizationName": "",
                                  "selectedRepositories": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.companyName").value("Company name is required"))
                .andExpect(jsonPath("$.errors.githubOrganizationName").value("GitHub organization name is required"))
                .andExpect(jsonPath("$.errors.selectedRepositories").value("At least one repository must be selected"));

        verifyNoInteractions(companyService);
    }

    @Test
    void company_anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "DebtLens Labs",
                                  "githubOrganizationName": "debtlens-labs",
                                  "selectedRepositories": [
                                    {
                                      "githubRepositoryId": 1001,
                                      "repositoryName": "backend",
                                      "repositoryUrl": "https://github.com/debtlens-labs/backend"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(companyService);
    }

    @Test
    void invitation_authenticatedValidRequestReturnsCreatedInvitations() throws Exception {
        when(invitationService.sendInvitations(any())).thenReturn(List.of(invitationResponse(
                50L,
                InvitationStatus.PENDING
        )));

        mockMvc.perform(post("/api/invitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repositoryId": 100,
                                  "contributors": [
                                    {
                                      "githubUsername": "grace-hopper",
                                      "email": "grace@example.test"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].invitationId").value(50))
                .andExpect(jsonPath("$[0].email").value("grace@example.test"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void invitation_invalidNestedContributorReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/invitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repositoryId": null,
                                  "contributors": [
                                    {
                                      "githubUsername": "",
                                      "email": "not-an-email"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.repositoryId").value("Repository ID is required"))
                .andExpect(jsonPath("$.errors['contributors[0].githubUsername']")
                        .value("GitHub username is required"))
                .andExpect(jsonPath("$.errors['contributors[0].email']").value("Invalid email format"));

        verifyNoInteractions(invitationService);
    }

    @Test
    void invitation_authenticatedAcceptReturnsAcceptedInvitation() throws Exception {
        when(invitationService.acceptInvitation(50L)).thenReturn(invitationResponse(
                50L,
                InvitationStatus.ACCEPTED
        ));

        mockMvc.perform(post("/api/invitations/50/accept")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationId").value(50))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void analysis_authenticatedStartReturnsQueuedAnalysis() throws Exception {
        when(analysisService.startAnalysis(100L, "feature/api-tests"))
                .thenReturn(analysisResponse(75L, AnalysisJobStatus.QUEUED));

        mockMvc.perform(post("/api/repositories/100/analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN))
                        .param("branch", "feature/api-tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(75))
                .andExpect(jsonPath("$.repositoryId").value(100))
                .andExpect(jsonPath("$.branch").value("feature/api-tests"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void analysis_missingRepositoryReturnsNotFoundResponse() throws Exception {
        when(analysisService.startAnalysis(404L, null))
                .thenThrow(new ResourceNotFoundException("Repository with ID 404 not found"));

        mockMvc.perform(post("/api/repositories/404/analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Repository with ID 404 not found"));
    }

    @Test
    void analysis_authenticatedGetReturnsExistingAnalysis() throws Exception {
        when(analysisService.getAnalysisJob(75L))
                .thenReturn(analysisResponse(75L, AnalysisJobStatus.RUNNING));

        mockMvc.perform(get("/api/analysis/75")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(75))
                .andExpect(jsonPath("$.repositoryName").value("backend"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void analysis_unauthorizedRepositoryAccessReturnsForbidden() throws Exception {
        when(analysisService.getAnalysisJob(75L)).thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/api/analysis/75")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied"));
    }

    @Test
    void analysis_anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/analysis/75"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(analysisService);
    }

    @Test
    void report_unauthorizedAnalysisAccessReturnsForbidden() throws Exception {
        when(reportService.generateReport(75L)).thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/api/analysis/75/report")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied"));
    }

    @Test
    void report_anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/analysis/75/recommendations"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reportService);
    }

    @Test
    void admin_systemAdminRoleClaimCanAccessStats() throws Exception {
        when(adminDashboardService.getStats()).thenReturn(new AdminStatsResponseDTO(
                12L, 3L, 8L, 20L, 1L, 2L, 15L, 2L, 0L));

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(12))
                .andExpect(jsonPath("$.totalCompanies").value(3))
                .andExpect(jsonPath("$.totalRepositories").value(8))
                .andExpect(jsonPath("$.totalAnalysisJobs").value(20));
    }

    @Test
    void admin_authenticatedNonAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminDashboardService);
    }

    @Test
    void admin_anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminDashboardService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/companies",
            "/api/admin/companies/10",
            "/api/admin/companies/10/repositories",
            "/api/admin/companies/10/users",
            "/api/admin/companies/10/analysis-jobs",
            "/api/admin/users",
            "/api/admin/analysis-jobs",
            "/api/admin/analysis-jobs/75",
            "/api/admin/stats",
            "/api/admin/activity",
            "/api/admin/search?q=ac",
            "/api/admin/health"
    })
    void everyAdminEndpointRejectsNormalUsers(String path) throws Exception {
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/companies",
            "/api/admin/companies/10",
            "/api/admin/companies/10/repositories",
            "/api/admin/companies/10/users",
            "/api/admin/companies/10/analysis-jobs",
            "/api/admin/users",
            "/api/admin/analysis-jobs",
            "/api/admin/analysis-jobs/75",
            "/api/admin/stats",
            "/api/admin/activity",
            "/api/admin/search?q=ac",
            "/api/admin/health"
    })
    void everyAdminEndpointRejectsAnonymousRequests(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Jwt jwt(String token, String subject, List<String> roles) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim(ROLES_CLAIM, roles)
                .build();
    }

    private static CompanyResponseDTO companyResponse() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 27, 12, 0);
        return new CompanyResponseDTO(
                10L,
                "DebtLens Labs",
                "https://github.com/debtlens-labs",
                "debtlens-labs",
                1L,
                "Ada Lovelace",
                1,
                List.of(),
                now,
                now
        );
    }

    private static InvitationResponseDTO invitationResponse(Long id, InvitationStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 27, 12, 0);
        return new InvitationResponseDTO(
                id,
                "grace@example.test",
                "grace-hopper",
                100L,
                "backend",
                10L,
                "DebtLens Labs",
                status,
                "invitation-token",
                now.plusDays(7),
                now
        );
    }

    private static AnalysisResponseDTO analysisResponse(Long id, AnalysisJobStatus status) {
        return new AnalysisResponseDTO(
                id,
                100L,
                "backend",
                "https://github.com/debtlens-labs/backend",
                10L,
                "DebtLens Labs",
                "feature/api-tests",
                1L,
                "Ada Lovelace",
                status,
                LocalDateTime.of(2026, 9, 27, 12, 0),
                null,
                0
        );
    }
}
