package com.debtlens.backend.mapper;

import com.debtlens.backend.dto.response.InvitationResponseDTO;
import com.debtlens.backend.entity.Invitation;
import org.springframework.stereotype.Component;

@Component
public class InvitationMapper {

    public InvitationResponseDTO toDTO(Invitation invitation) {
        if (invitation == null) {
            return null;
        }

        Long repoId = (invitation.getRepository() != null) ? invitation.getRepository().getRepositoryId() : null;
        String repoName = (invitation.getRepository() != null) ? invitation.getRepository().getRepositoryName() : null;

        Long companyId = (invitation.getRepository() != null && invitation.getRepository().getCompany() != null)
                ? invitation.getRepository().getCompany().getCompanyId()
                : (invitation.getSuperAdmin() != null && invitation.getSuperAdmin().getCompany() != null)
                ? invitation.getSuperAdmin().getCompany().getCompanyId()
                : null;

        String companyName = (invitation.getRepository() != null && invitation.getRepository().getCompany() != null)
                ? invitation.getRepository().getCompany().getCompanyName()
                : (invitation.getSuperAdmin() != null && invitation.getSuperAdmin().getCompany() != null)
                ? invitation.getSuperAdmin().getCompany().getCompanyName()
                : null;

        return new InvitationResponseDTO(
                invitation.getInvitationId(),
                invitation.getEmail(),
                invitation.getGithubUsername(),
                repoId,
                repoName,
                companyId,
                companyName,
                invitation.getStatus(),
                invitation.getToken(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
