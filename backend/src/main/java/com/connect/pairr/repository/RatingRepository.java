package com.connect.pairr.repository;

import com.connect.pairr.model.entity.Rating;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    boolean existsByFromUserIdAndToUserIdAndSkillId(UUID fromUserId, UUID toUserId, UUID skillId);

    @EntityGraph(attributePaths = {"fromUser", "skill"})
    List<Rating> findAllByToUserId(UUID toUserId);

    @EntityGraph(attributePaths = {"fromUser", "skill"})
    List<Rating> findAllByToUserIdAndSkillId(UUID toUserId, UUID skillId);

    Optional<Rating> findByFromUserIdAndToUserIdAndSkillId(UUID fromUserId, UUID toUserId, UUID skillId);

    @Query("SELECT ROUND(AVG(r.rating), 1) FROM Rating r WHERE r.toUser.id = :toUserId AND r.skill.id = :skillId")
    BigDecimal averageRatingByToUserIdAndSkillId(@Param("toUserId") UUID toUserId, @Param("skillId") UUID skillId);

    @Query("SELECT ROUND(AVG(r.rating), 1) FROM Rating r WHERE r.toUser.id = :toUserId")
    BigDecimal averageRatingByToUserId(@Param("toUserId") UUID toUserId);
}
