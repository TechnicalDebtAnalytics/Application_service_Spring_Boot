package com.debtlens.backend.security;

public class Auth0UserService {
}
/*This is related to your Auth0 integration.

Its purpose can be to map the authenticated Auth0 identity to your local application user.

For example:

Auth0 Subject
     ↓
auth0|123456
     ↓
UserService
     ↓
UserRepository
     ↓
Local User

This is not responsible for generating JWTs.

Auth0 generates the token.

Spring Security validates it.

Your application uses the authenticated identity.*/