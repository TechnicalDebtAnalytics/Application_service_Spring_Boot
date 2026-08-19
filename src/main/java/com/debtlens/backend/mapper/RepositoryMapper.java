package com.debtlens.backend.mapper;

import com.debtlens.backend.dto.response.RepositoryResponseDTO;
import com.debtlens.backend.entity.Repository;
import org.springframework.stereotype.Component;

@Component
public class RepositoryMapper {

    public RepositoryResponseDTO toDTO(Repository repo) {
        if (repo == null) {
            return null;
        }

        return new RepositoryResponseDTO(
                repo.getRepositoryId(),
                repo.getGithubRepositoryId(),
                repo.getRepositoryName(),
                repo.getRepositoryUrl(),
                repo.getDefaultBranch(),
                repo.getCreatedAt()
        );
    }
}
