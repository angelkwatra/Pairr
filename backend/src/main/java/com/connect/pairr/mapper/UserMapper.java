package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.UserResponse;
import com.connect.pairr.model.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getOverallRating(),
                user.getCompletedSessionsCount(),
                user.getCreatedAt()
        );
    }
}
