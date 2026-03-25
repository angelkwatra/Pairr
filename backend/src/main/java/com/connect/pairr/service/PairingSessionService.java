package com.connect.pairr.service;

import com.connect.pairr.exception.PairingSessionNotFoundException;
import com.connect.pairr.exception.SkillNotFoundException;
import com.connect.pairr.exception.UserNotFoundException;
import com.connect.pairr.mapper.PairingSessionMapper;
import com.connect.pairr.model.dto.PairingSessionResponse;
import com.connect.pairr.model.dto.RequestPairingRequest;
import com.connect.pairr.model.entity.PairingSession;
import com.connect.pairr.model.entity.Skill;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.enums.PairingStatus;
import com.connect.pairr.repository.PairingSessionRepository;
import com.connect.pairr.repository.RatingRepository;
import com.connect.pairr.repository.SkillRepository;
import com.connect.pairr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PairingSessionService {

    private final PairingSessionRepository pairingSessionRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RatingRepository ratingRepository;

    private static final int MAX_PENDING_PER_TARGET = 2;
    private static final int MAX_PENDING_GLOBAL = 10;
    private static final int MAX_ACTIVE_SESSIONS = 10;

    @Transactional
    public PairingSessionResponse requestPairing(UUID requesterId, RequestPairingRequest request) {
        if (requesterId.equals(request.requesteeId())) {
            throw new RuntimeException("You cannot request a pairing session with yourself");
        }

        // Rule 1: Unique Pending per Skill
        if (pairingSessionRepository.existsByRequesterIdAndRequesteeIdAndSkillIdAndStatus(
                requesterId, request.requesteeId(), request.skillId(), PairingStatus.PENDING)) {
            throw new RuntimeException("A pending request for this skill already exists with this user");
        }

        // Rule 2: Target Person Limit
        long pendingToTarget = pairingSessionRepository.countByRequesterIdAndRequesteeIdAndStatus(
                requesterId, request.requesteeId(), PairingStatus.PENDING);
        if (pendingToTarget >= MAX_PENDING_PER_TARGET) {
            throw new RuntimeException("You cannot have more than " + MAX_PENDING_PER_TARGET + " pending requests to the same person");
        }

        // Rule 3: Global Outbound Limit
        long totalPending = pairingSessionRepository.countByRequesterIdAndStatus(requesterId, PairingStatus.PENDING);
        if (totalPending >= MAX_PENDING_GLOBAL) {
            throw new RuntimeException("You have reached the global limit of " + MAX_PENDING_GLOBAL + " pending requests");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException(requesterId));

        User requestee = userRepository.findById(request.requesteeId())
                .orElseThrow(() -> new UserNotFoundException(request.requesteeId()));

        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> new SkillNotFoundException(request.skillId()));

        PairingSession session = PairingSession.builder()
                .requester(requester)
                .requestee(requestee)
                .skill(skill)
                .status(PairingStatus.PENDING)
                .build();

        session = pairingSessionRepository.save(session);
        notifyParticipants(session);
        return PairingSessionMapper.toResponse(session, false); // New request never rated
    }

    @Transactional
    public PairingSessionResponse updateStatus(UUID userId, UUID sessionId, PairingStatus newStatus) {
        PairingSession session = pairingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PairingSessionNotFoundException(sessionId));

        // Authorization and state transition rules
        switch (newStatus) {
            case ACCEPTED -> {
                if (!session.getRequestee().getId().equals(userId)) {
                    throw new RuntimeException("Only the requestee can accept the session");
                }
                if (session.getStatus() != PairingStatus.PENDING) {
                    throw new RuntimeException("Can only accept pending sessions");
                }

                // Check active session limits for both users
                if (pairingSessionRepository.countActiveSessions(session.getRequester().getId()) >= MAX_ACTIVE_SESSIONS) {
                    throw new RuntimeException("The requester has reached the limit of " + MAX_ACTIVE_SESSIONS + " active sessions");
                }
                if (pairingSessionRepository.countActiveSessions(session.getRequestee().getId()) >= MAX_ACTIVE_SESSIONS) {
                    throw new RuntimeException("You have reached the limit of " + MAX_ACTIVE_SESSIONS + " active sessions");
                }

                session.setStatus(PairingStatus.ACCEPTED);
                session.setStartedAt(Instant.now());
            }
            case COMPLETED -> {
                if (!session.getRequester().getId().equals(userId) && !session.getRequestee().getId().equals(userId)) {
                    throw new RuntimeException("Only participants can complete the session");
                }
                if (session.getStatus() != PairingStatus.ACCEPTED) {
                    throw new RuntimeException("Can only complete accepted sessions");
                }
                session.setStatus(PairingStatus.COMPLETED);
                session.setCompletedAt(Instant.now());

                // Increment session counts
                User requester = session.getRequester();
                User requestee = session.getRequestee();
                requester.setCompletedSessionsCount(requester.getCompletedSessionsCount() + 1);
                requestee.setCompletedSessionsCount(requestee.getCompletedSessionsCount() + 1);
                userRepository.save(requester);
                userRepository.save(requestee);
            }
            case CANCELLED -> {
                if (!session.getRequester().getId().equals(userId) && !session.getRequestee().getId().equals(userId)) {
                    throw new RuntimeException("Only participants can cancel the session");
                }
                if (session.getStatus() == PairingStatus.COMPLETED) {
                    throw new RuntimeException("Cannot cancel a completed session");
                }
                session.setStatus(PairingStatus.CANCELLED);
                session.setCancelledAt(Instant.now());
            }
            default -> throw new RuntimeException("Unsupported status transition");
        }

        session = pairingSessionRepository.save(session);
        notifyParticipants(session);
        return PairingSessionMapper.toResponse(session, isRatedByCurrentUser(userId, session));
    }

    private void notifyParticipants(PairingSession session) {
        // Notify Requester
        messagingTemplate.convertAndSendToUser(
                session.getRequester().getId().toString(),
                "/queue/pairing",
                "SESSION_UPDATED"
        );

        // Notify Requestee
        messagingTemplate.convertAndSendToUser(
                session.getRequestee().getId().toString(),
                "/queue/pairing",
                "SESSION_UPDATED"
        );
    }

    @Transactional(readOnly = true)
    public Page<PairingSessionResponse> getSessions(UUID userId, Pageable pageable) {
        return pairingSessionRepository.findAllByUserId(userId, pageable)
                .map(session -> PairingSessionMapper.toResponse(session, isRatedByCurrentUser(userId, session)));
    }

    private boolean isRatedByCurrentUser(UUID userId, PairingSession session) {
        UUID otherId = session.getRequester().getId().equals(userId)
                ? session.getRequestee().getId()
                : session.getRequester().getId();

        return ratingRepository.existsByFromUserIdAndToUserIdAndSkillId(userId, otherId, session.getSkill().getId());
    }

    @Transactional(readOnly = true)
    public PairingSession getSessionEntity(UUID sessionId) {
        return pairingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PairingSessionNotFoundException(sessionId));
    }
}
