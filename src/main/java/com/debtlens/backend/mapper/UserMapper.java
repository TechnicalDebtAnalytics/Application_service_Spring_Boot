package com.debtlens.backend.mapper;

import com.debtlens.backend.dto.response.UserResponseDTO;
import com.debtlens.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserMapper {

    public UserResponseDTO toDTO(User user) {
        return toDTO(user, Collections.emptyList());
    }

    public UserResponseDTO toDTO(User user, List<String> roles) {
        if (user == null) {
            return null;
        }

        return new UserResponseDTO(
                user.getUserId(),
                user.getAuth0UserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getGithubUsername(),
                user.getEmailVerified(),
                roles != null ? roles : Collections.emptyList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
