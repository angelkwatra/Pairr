package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.AddRatingRequest;
import com.connect.pairr.model.dto.RatingResponse;
import com.connect.pairr.model.entity.Rating;
import com.connect.pairr.model.entity.Skill;
import com.connect.pairr.model.entity.User;

public class RatingMapper {

    public static RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .fromUserId(rating.getFromUser().getId())
                .fromUserDisplayName(rating.getFromUser().getDisplayName())
                .toUserId(rating.getToUser().getId())
                .skillId(rating.getSkill().getId())
                .skillName(rating.getSkill().getName())
                .rating(rating.getRating())
                .feedback(rating.getFeedback())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    public static Rating toEntity(AddRatingRequest request, User fromUser, User toUser, Skill skill) {
        return Rating.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .skill(skill)
                .rating(request.rating())
                .feedback(request.feedback())
                .build();
    }
}
