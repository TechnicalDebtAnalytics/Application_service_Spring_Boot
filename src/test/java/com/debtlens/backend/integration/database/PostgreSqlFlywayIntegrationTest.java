package com.debtlens.backend.integration.database;

import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlFlywayIntegrationTest {

    private static final Set<String> EXPECTED_APPLICATION_TABLES = Set.of(
            "users",
            "companies",
            "super_admins",
            "repositories",
            "members",
            "invitations",
            "repo_assignments",
            "analysis_jobs",
            "analysis_status_history",
            "reports",
            "class_metrics",
            "bug_predictions",
            "class_comments",
            "satd_detections",
            "debt_scores"
    );

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("debtlens_integration")
            .withUsername("debtlens_test")
            .withPassword("debtlens_test");

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void containerConnectionAndAllFlywayMigrationsCreateExpectedSchema() throws Exception {
        assertTrue(POSTGRES.isRunning());

        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(2));
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(POSTGRES.getDatabaseName(), connection.getCatalog());
            assertEquals(POSTGRES.getJdbcUrl(), connection.getMetaData().getURL());
        }

        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(17, flyway.info().applied().length);
        assertEquals(
                17,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                        Integer.class
                )
        );

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema()",
                String.class
        );
        assertTrue(new HashSet<>(tables).containsAll(EXPECTED_APPLICATION_TABLES));
    }

    @Test
    void userCanBePersistedFlushedClearedAndRetrievedThroughRepository() {
        User saved = userRepository.saveAndFlush(newUser(
                "auth0|integration-user",
                "integration.user@example.test",
                "integration-user"
        ));
        Long id = saved.getUserId();

        assertNotNull(id);
        entityManager.clear();

        User retrieved = userRepository.findById(id).orElseThrow();
        assertEquals("auth0|integration-user", retrieved.getAuth0UserId());
        assertEquals("integration.user@example.test", retrieved.getEmail());
        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
    }

    @Test
    void companyRelationshipAndUserEmailUniqueConstraintAreEnforced() {
        User creator = userRepository.saveAndFlush(newUser(
                "auth0|company-creator",
                "company.creator@example.test",
                "company-creator"
        ));

        Company company = new Company();
        company.setCompanyName("Integration Test Company");
        company.setGithubOrganizationUrl("https://github.com/debtlens-integration");
        company.setCreatedBy(creator);
        Long companyId = companyRepository.saveAndFlush(company).getCompanyId();

        entityManager.clear();

        Company retrieved = companyRepository.findById(companyId).orElseThrow();
        assertEquals(creator.getUserId(), retrieved.getCreatedBy().getUserId());

        User duplicateEmail = newUser(
                "auth0|different-user",
                "company.creator@example.test",
                "different-user"
        );
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicateEmail));
    }

    @Test
    void FR_05_databaseContainer_shouldReconnectAndRetainSchemaAndDataAfterPause() throws Exception {
        User retainedUser = userRepository.saveAndFlush(newUser(
                "auth0|recovery-user",
                "recovery.user@example.test",
                "recovery-user"
        ));
        Long retainedUserId = retainedUser.getUserId();
        entityManager.clear();

        String containerId = POSTGRES.getContainerId();

        POSTGRES.getDockerClient().pauseContainerCmd(containerId).exec();
        try {
            assertTrue(Boolean.TRUE.equals(
                    POSTGRES.getDockerClient().inspectContainerCmd(containerId).exec().getState().getPaused()
            ));
        } finally {
            POSTGRES.getDockerClient().unpauseContainerCmd(containerId).exec();
        }

        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(5));
            assertEquals(POSTGRES.getDatabaseName(), connection.getCatalog());
        }
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(17, flyway.info().applied().length);

        User recoveredUser = userRepository.findById(retainedUserId).orElseThrow();
        assertEquals("auth0|recovery-user", recoveredUser.getAuth0UserId());
        assertEquals("recovery.user@example.test", recoveredUser.getEmail());
    }

    private static User newUser(String auth0UserId, String email, String githubUsername) {
        User user = new User();
        user.setAuth0UserId(auth0UserId);
        user.setFirstName("Integration");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setGithubUsername(githubUsername);
        user.setEmailVerified(true);
        return user;
    }
}
