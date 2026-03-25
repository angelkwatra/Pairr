package com.connect.pairr.model.dto;

public record CreateUserDto(
        String displayName,
        String email,
        String password
) {}
