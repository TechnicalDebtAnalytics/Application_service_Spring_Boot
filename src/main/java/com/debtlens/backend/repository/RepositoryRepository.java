package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long>, JpaSpecificationExecutor<Repository> {

    List<Repository> findByCompanyCompanyId(Long companyId);

    List<Repository> findByCompanyCompanyIdIn(List<Long> companyIds);

    Optional<Repository> findByGithubRepositoryId(String githubRepositoryId);

    boolean existsByGithubRepositoryId(String githubRepositoryId);
}
