package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {

    List<Repository> findByCompanyCompanyId(Long companyId);

    Optional<Repository> findByGithubRepositoryId(String githubRepositoryId);

    boolean existsByGithubRepositoryId(String githubRepositoryId);
}
