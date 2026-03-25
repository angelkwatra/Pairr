package com.connect.pairr.repository;

import com.connect.pairr.model.entity.PairingSession;
import com.connect.pairr.model.enums.PairingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PairingSessionRepository extends JpaRepository<PairingSession, UUID> {

    @Query("""
        SELECT ps FROM PairingSession ps
        WHERE ps.requester.id = :userId OR ps.requestee.id = :userId
        ORDER BY ps.requestedAt DESC
    """)
    Page<PairingSession> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByRequesterIdAndRequesteeIdAndSkillIdAndStatus(UUID requesterId, UUID requesteeId, UUID skillId, PairingStatus status);

    long countByRequesterIdAndStatus(UUID requesterId, PairingStatus status);

    long countByRequesterIdAndRequesteeIdAndStatus(UUID requesterId, UUID requesteeId, PairingStatus status);

    @Query("""
        SELECT COUNT(ps) FROM PairingSession ps
        WHERE (ps.requester.id = :userId OR ps.requestee.id = :userId)
        AND ps.status = 'ACCEPTED'
    """)
    long countActiveSessions(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(ps) > 0 FROM PairingSession ps
        WHERE ((ps.requester.id = :user1 AND ps.requestee.id = :user2) OR (ps.requester.id = :user2 AND ps.requestee.id = :user1))
        AND ps.skill.id = :skillId
        AND ps.status IN :statuses
    """)
    boolean existsByParticipantsAndSkillAndStatusIn(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2,
            @Param("skillId") UUID skillId,
            @Param("statuses") java.util.Collection<PairingStatus> statuses
    );
}
