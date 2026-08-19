package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.UserResponseDTO;
import com.debtlens.backend.entity.User;

public interface UserService {

    User getCurrentUser();

    UserResponseDTO getCurrentUserProfile();

    String getCurrentUserGithubUsername();

    UserResponseDTO getUserById(Long userId);

    UserResponseDTO getUserByAuth0Id(String auth0UserId);
}