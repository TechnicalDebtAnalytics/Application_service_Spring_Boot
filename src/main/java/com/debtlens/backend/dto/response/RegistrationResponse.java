package com.debtlens.backend.dto.response;

public class RegistrationResponse {

    private String message;
    private String auth0UserId;
    private String email;
    private String role;

    public RegistrationResponse(
            String message,
            String auth0UserId,
            String email,
            String role
    ) {
        this.message = message;
        this.auth0UserId = auth0UserId;
        this.email = email;
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public String getAuth0UserId() {
        return auth0UserId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAuth0UserId(String auth0UserId) {
        this.auth0UserId = auth0UserId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
