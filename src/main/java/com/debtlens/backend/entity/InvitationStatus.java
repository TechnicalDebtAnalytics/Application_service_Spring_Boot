package com.debtlens.backend.entity;

/**
 * Represents the lifecycle state of an invitation.
 */
public enum InvitationStatus {
    PENDING,  // The invitation has not yet been resolved.
    ACCEPTED, // The invitation was accepted.
    REJECTED, // The invitation was explicitly declined.
    EXPIRED   // The invitation is no longer valid.
}
