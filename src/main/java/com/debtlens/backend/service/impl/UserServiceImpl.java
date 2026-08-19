package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.UserResponseDTO;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.auth0.Auth0RoleResponse;
import com.debtlens.backend.mapper.UserMapper;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final Auth0UserService auth0UserService;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            Auth0UserService auth0UserService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.auth0UserService = auth0UserService;
        this.userMapper = userMapper;
    }

    @Override
    public User getCurrentUser() {
        return auth0UserService.getAuthenticatedUser();
    }

    @Override
    public UserResponseDTO getCurrentUserProfile() {
        User user = auth0UserService.getAuthenticatedUser();
        List<String> roleNames = auth0UserService.getAuthenticatedUserRoles()
                .stream()
                .map(Auth0RoleResponse::name)
                .toList();

        return userMapper.toDTO(user, roleNames);
    }

    @Override
    public String getCurrentUserGithubUsername() {
        return auth0UserService.getAuthenticatedGithubUsername();
    }

    @Override
    public UserResponseDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        List<String> roleNames = auth0UserService.getUserRoles(user.getAuth0UserId())
                .stream()
                .map(Auth0RoleResponse::name)
                .toList();

        return userMapper.toDTO(user, roleNames);
    }

    @Override
    public UserResponseDTO getUserByAuth0Id(String auth0UserId) {
        User user = userRepository.findByAuth0UserId(auth0UserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Auth0 ID: " + auth0UserId));

        List<String> roleNames = auth0UserService.getUserRoles(auth0UserId)
                .stream()
                .map(Auth0RoleResponse::name)
                .toList();

        return userMapper.toDTO(user, roleNames);
    }
}