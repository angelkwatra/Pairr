package com.connect.pairr.model.dto;

import com.connect.pairr.model.enums.ProficiencyLevel;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record UserSkillAvailabilityData(
        UUID userId,
        String displayName,
        ProficiencyLevel proficiency,
        BigDecimal rating,
        BigDecimal overallRating,
        Long completedSessionsCount,
        LocalTime startTime,
        LocalTime endTime
) {}
