package com.connect.pairr.controller;

import com.connect.pairr.mapper.UserMapper;
import com.connect.pairr.model.dto.UserResponse;
import com.connect.pairr.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Retrieve profile information")
public class UserController {

    private final UserService userService;

    @GetMapping("/user/me")
    @Operation(summary = "Get current user profile", description = "Returns the details of the currently authenticated user.")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UUID userId) {
        UserResponse response = UserMapper.toResponse(userService.getUserById(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get a user's public profile", description = "Returns the public details of a specific user.")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable UUID userId) {
        UserResponse response = UserMapper.toResponse(userService.getUserById(userId));
        return ResponseEntity.ok(response);
    }
}
