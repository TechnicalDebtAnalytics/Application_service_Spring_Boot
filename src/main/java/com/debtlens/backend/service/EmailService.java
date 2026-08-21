package com.debtlens.backend.service;

import java.time.LocalDateTime;

public interface EmailService {

    /**
     * Sends a formatted invitation email to a prospective contributor.
     *
     * @param toEmail         Recipient's email address.
     * @param githubUsername  GitHub username of the contributor being invited.
     * @param companyName     Name of the company/organization issuing the invite.
     * @param repositoryName  Name of the repository being granted access to.
     * @param invitationToken Unique token for the invitation link.
     * @param expiresAt       Expiration datetime of the invitation.
     */
    void sendInvitationEmail(
            String toEmail,
            String githubUsername,
            String companyName,
            String repositoryName,
            String invitationToken,
            LocalDateTime expiresAt
    );
}
