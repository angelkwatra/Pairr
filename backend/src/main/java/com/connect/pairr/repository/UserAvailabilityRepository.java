package com.connect.pairr.repository;

import com.connect.pairr.model.entity.UserAvailability;
import com.connect.pairr.model.enums.DayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAvailabilityRepository extends JpaRepository<UserAvailability, UUID> {

    /**
     * Fetch all availabilities for a user on a specific day type **for a specific skill**.
     * Skill is fetched via UserSkill mapping table.
     */
    List<UserAvailability> findByUserIdAndDayType(@Param("userId") UUID userId,
                                                  @Param("dayType") DayType dayType);

    List<UserAvailability> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}

