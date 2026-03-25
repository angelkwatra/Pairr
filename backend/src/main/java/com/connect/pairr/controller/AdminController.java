package com.connect.pairr.controller;

import com.connect.pairr.model.dto.AddCategoryRequest;
import com.connect.pairr.model.dto.AddSkillRequest;
import com.connect.pairr.model.dto.CategoryResponse;
import com.connect.pairr.model.dto.SkillResponse;
import com.connect.pairr.model.dto.UserResponse;
import com.connect.pairr.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only operations (requires ADMIN role)")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard")
    public ResponseEntity<String> dashboard() {
        return ResponseEntity.ok("Welcome Admin!");
    }

    @PostMapping("/skills")
    @Operation(summary = "Create a skill", description = "Creates a new skill under an existing category")
    public ResponseEntity<SkillResponse> addSkill(@RequestBody @Valid AddSkillRequest request) {
        return ResponseEntity.ok(adminService.addSkill(request));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a skill category")
    public ResponseEntity<CategoryResponse> addCategory(@RequestBody @Valid AddCategoryRequest request) {
        return ResponseEntity.ok(adminService.addCategory(request));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
}
