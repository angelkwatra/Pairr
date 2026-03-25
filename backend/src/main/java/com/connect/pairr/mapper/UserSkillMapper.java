package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.UserSkillResponse;
import com.connect.pairr.model.entity.Skill;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.entity.UserSkill;
import com.connect.pairr.model.enums.ProficiencyLevel;

public class UserSkillMapper {

    public static UserSkillResponse toResponse(UserSkill userSkill) {
        return UserSkillResponse.builder()
                .skill(SkillMapper.toResponse(userSkill.getSkill()))
                .rating(userSkill.getRating())
                .proficiencyLevel(userSkill.getProficiency())
                .build();
    }

    public static UserSkill toEntity(User user, Skill skill, ProficiencyLevel proficiency) {
        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setProficiency(proficiency);
        return userSkill;
    }
}
