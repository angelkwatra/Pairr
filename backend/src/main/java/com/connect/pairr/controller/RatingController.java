package com.connect.pairr.controller;

import com.connect.pairr.model.dto.AddRatingRequest;
import com.connect.pairr.model.dto.RatingResponse;
import com.connect.pairr.service.RatingService;
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
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Tag(name = "Ratings", description = "Rate users on skills and view ratings")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @Operation(summary = "Rate a user on a skill", description = "Submit a 1-5 rating with optional feedback. Both users must have the skill. One rating per (rater, rated, skill) triple.")
    public ResponseEntity<RatingResponse> submitRating(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid AddRatingRequest request
    ) {
        return ResponseEntity.ok(ratingService.submitRating(userId, request));
    }

    @GetMapping
    @Operation(summary = "Get ratings for a user", description = "Returns all ratings received by a user. Optionally filter by skillId.")
    public ResponseEntity<List<RatingResponse>> getRatings(
            @RequestParam UUID userId,
            @RequestParam(required = false) UUID skillId
    ) {
        if (skillId != null) {
            return ResponseEntity.ok(ratingService.getRatingsForUserSkill(userId, skillId));
        }
        return ResponseEntity.ok(ratingService.getRatingsForUser(userId));
    }
}
