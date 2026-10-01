package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    @Override
    @EntityGraph(attributePaths = "createdBy")
    Page<Company> findAll(Specification<Company> specification, Pageable pageable);

    List<Company> findByCreatedByUserId(Long userId);

    Optional<Company> findByGithubOrganizationUrl(String githubOrganizationUrl);

    boolean existsByGithubOrganizationUrl(String githubOrganizationUrl);

    boolean existsByCompanyName(String companyName);

    @Query("""
            SELECT c
            FROM Company c
            WHERE c.companyId = :companyId
              AND (
                EXISTS (
                    SELECT sa.superAdminId
                    FROM Super_Admin sa
                    WHERE sa.company = c AND sa.user.userId = :userId
                )
                OR EXISTS (
                    SELECT m.memberId
                    FROM Member m
                    WHERE m.company = c AND m.user.userId = :userId
                )
              )
            """)
    Optional<Company> findAccessibleByCompanyIdAndUserId(
            @Param("companyId") Long companyId,
            @Param("userId") Long userId
    );
}
