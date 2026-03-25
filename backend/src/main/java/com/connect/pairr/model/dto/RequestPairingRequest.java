package com.connect.pairr.model.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequestPairingRequest(
        @NotNull UUID requesteeId,
        @NotNull UUID skillId
) {}
