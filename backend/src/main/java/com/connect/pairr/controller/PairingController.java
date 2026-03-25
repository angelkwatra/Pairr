package com.connect.pairr.controller;

import com.connect.pairr.model.dto.PairingSessionResponse;
import com.connect.pairr.model.dto.RequestPairingRequest;
import com.connect.pairr.model.enums.PairingStatus;
import com.connect.pairr.service.PairingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pairing")
@RequiredArgsConstructor
@Tag(name = "Pairing Sessions", description = "Manage the lifecycle of pairing collaborations")
public class PairingController {

    private final PairingSessionService pairingSessionService;

    @PostMapping("/request")
    @Operation(summary = "Request a pairing session", description = "Initiate a collaboration request with another user for a specific skill.")
    public ResponseEntity<PairingSessionResponse> requestPairing(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid RequestPairingRequest request
    ) {
        return ResponseEntity.ok(pairingSessionService.requestPairing(userId, request));
    }

    @PatchMapping("/{sessionId}/status")
    @Operation(summary = "Update session status", description = "Accept, complete, or cancel a pairing session.")
    public ResponseEntity<PairingSessionResponse> updateStatus(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId,
            @RequestParam PairingStatus status
    ) {
        return ResponseEntity.ok(pairingSessionService.updateStatus(userId, sessionId, status));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List your pairing sessions", description = "Returns all sessions where you are either the requester or requestee.")
    public ResponseEntity<Page<PairingSessionResponse>> getSessions(
            @AuthenticationPrincipal UUID userId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(pairingSessionService.getSessions(userId, pageable));
    }
}
