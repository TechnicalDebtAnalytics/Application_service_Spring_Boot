package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByCreatedByUserId(Long userId);

    Optional<Company> findByGithubOrganizationUrl(String githubOrganizationUrl);

    boolean existsByGithubOrganizationUrl(String githubOrganizationUrl);

    boolean existsByCompanyName(String companyName);
}
