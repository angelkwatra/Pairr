package com.connect.pairr.controller;

import com.connect.pairr.mapper.UserSkillMapper;
import com.connect.pairr.model.dto.AddUserSkillRequest;
import com.connect.pairr.model.dto.UserSkillResponse;
import com.connect.pairr.service.UserSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "User Skills", description = "Manage your skill profile")
public class UserSkillController {

    private final UserSkillService userSkillService;

    @PostMapping("/api/user/skills")
    @Operation(summary = "Add skills to your profile", description = "Bulk add skills with proficiency levels. This will overwrite any existing skills on your profile.")
    public void addUserSkills(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid List<AddUserSkillRequest> requests
    ) {
        userSkillService.addSkills(userId, requests);
    }

    @GetMapping("/api/user/skills")
    @Operation(summary = "Get your skills", description = "Returns all skills on your profile with proficiency levels and ratings")
    public ResponseEntity<List<UserSkillResponse>> getUserSkills(@AuthenticationPrincipal UUID userId) {

        List<UserSkillResponse> response = userSkillService.getUserSkills(userId).stream()
                .map(UserSkillMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/users/{userId}/skills")
    @Operation(summary = "Get a user's skills", description = "Returns all public skills for a specific user.")
    public ResponseEntity<List<UserSkillResponse>> getUserSkillsById(@PathVariable UUID userId) {

        List<UserSkillResponse> response = userSkillService.getUserSkills(userId).stream()
                .map(UserSkillMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}
