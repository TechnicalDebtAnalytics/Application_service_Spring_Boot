package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByAuth0OrganizationId(String auth0OrganizationId);

    boolean existsByAuth0OrganizationId(String auth0OrganizationId);

    Optional<Company> findByCompanyName(String companyName);
}