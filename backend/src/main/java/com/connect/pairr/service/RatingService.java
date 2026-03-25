package com.connect.pairr.service;

import com.connect.pairr.exception.*;
import com.connect.pairr.mapper.RatingMapper;
import com.connect.pairr.model.dto.AddRatingRequest;
import com.connect.pairr.model.dto.RatingResponse;
import com.connect.pairr.model.entity.*;
import com.connect.pairr.model.enums.PairingStatus;
import com.connect.pairr.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final PairingSessionRepository pairingSessionRepository;

    @Transactional
    public RatingResponse submitRating(UUID fromUserId, AddRatingRequest request) {

        if (fromUserId.equals(request.toUserId())) {
            throw new SelfRatingException();
        }

        // 1. Guard: Verify a verified session exists (Source of Truth)
        boolean hasValidSession = pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(
                fromUserId, request.toUserId(), request.skillId(), List.of(PairingStatus.ACCEPTED, PairingStatus.COMPLETED));

        if (!hasValidSession) {
            throw new RuntimeException("Can only rate users during an active or completed verified pairing session for this skill");
        }

        // 2. Fetch necessary entities for the update
        User fromUser = userRepository.getReferenceById(fromUserId);
        User toUser = userRepository.findById(request.toUserId())
                .orElseThrow(() -> new UserNotFoundException(request.toUserId()));
        Skill skill = skillRepository.getReferenceById(request.skillId());

        UserSkill toUserSkill = userSkillRepository.findByUserIdAndSkillId(request.toUserId(), request.skillId())
                .orElseThrow(() -> new SkillNotFoundException(request.skillId()));

        // 3. Upsert Logic: Check if a rating already exists for this (fromUser, toUser, skill)
        Rating rating = ratingRepository.findByFromUserIdAndToUserIdAndSkillId(fromUserId, request.toUserId(), request.skillId())
                .map(existing -> {
                    existing.setRating(request.rating());
                    existing.setFeedback(request.feedback());
                    return existing;
                })
                .orElseGet(() -> RatingMapper.toEntity(request, fromUser, toUser, skill));

        rating = ratingRepository.save(rating);

        // Recalculate per-skill rating for the rated user
        toUserSkill.setRating(ratingRepository.averageRatingByToUserIdAndSkillId(request.toUserId(), request.skillId()));
        userSkillRepository.save(toUserSkill);

        // Recalculate overall rating for the rated user
        toUser.setOverallRating(ratingRepository.averageRatingByToUserId(request.toUserId()));
        userRepository.save(toUser);

        return RatingMapper.toResponse(rating);
    }

    public List<RatingResponse> getRatingsForUser(UUID userId) {
        return ratingRepository.findAllByToUserId(userId).stream()
                .map(RatingMapper::toResponse)
                .toList();
    }

    public List<RatingResponse> getRatingsForUserSkill(UUID userId, UUID skillId) {
        return ratingRepository.findAllByToUserIdAndSkillId(userId, skillId).stream()
                .map(RatingMapper::toResponse)
                .toList();
    }
}
