package com.debtlens.backend.service;

public interface Auth0ManagementService {

    String createUser(
            String email,
            String password,
            String firstName,
            String lastName
    );

    void assignRole(
            String auth0UserId
    );
}