package com.connect.pairr.repository;

import com.connect.pairr.model.dto.UserSkillAvailabilityData;
import com.connect.pairr.model.entity.Skill;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.entity.UserSkill;
import com.connect.pairr.model.enums.DayType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {

    @Query("""
        SELECT new com.connect.pairr.model.dto.UserSkillAvailabilityData(
            u.id,
            u.displayName,
            us.proficiency,
            us.rating,
            u.overallRating,
            u.completedSessionsCount,
            ua.startTime,
            ua.endTime
        )
        FROM User u
        JOIN UserSkill us ON us.user = u
        JOIN UserAvailability ua ON ua.user = u
        WHERE us.skill.id = :skillId
          AND ua.dayType = :dayType
          AND u.id <> :requesterUserId
    """)
    List<UserSkillAvailabilityData> getRecommendationCandidates(
            @Param("skillId") UUID skillId,
            @Param("dayType") DayType dayType,
            @Param("requesterUserId") UUID requesterUserId
    );

    boolean existsByUserAndSkill(User user, Skill skill);

    @EntityGraph(attributePaths = {"skill", "skill.category"})
    List<UserSkill> findAllByUserId(UUID userId);

    Optional<UserSkill> findByUserIdAndSkillId(UUID userId, UUID skillId);

    void deleteAllByUserId(UUID userId);

}

