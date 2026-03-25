package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.AddSkillRequest;
import com.connect.pairr.model.dto.SkillResponse;
import com.connect.pairr.model.entity.Category;
import com.connect.pairr.model.entity.Skill;

public class SkillMapper {

    public static SkillResponse toResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getCategory().getId(),
                skill.getCategory().getName()
        );
    }

    public static Skill toEntity(AddSkillRequest request, Category category) {
        return Skill.builder()
                .name(request.name())
                .category(category)
                .build();
    }
}
