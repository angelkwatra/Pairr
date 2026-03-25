package com.connect.pairr.model.dto;

import com.connect.pairr.model.enums.ProficiencyLevel;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UserSkillResponse(
        SkillResponse skill,
        BigDecimal rating,
        ProficiencyLevel proficiencyLevel
) {}

