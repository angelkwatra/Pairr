package com.connect.pairr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull @Schema(description = "ID of the message recipient") UUID recipientId,
        @NotBlank @Size(max = 2000) @Schema(description = "Message text (max 2000 characters)") String content
) {}
