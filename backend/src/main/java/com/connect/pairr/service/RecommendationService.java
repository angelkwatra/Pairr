package com.connect.pairr.service;

import com.connect.pairr.model.dto.Recommendation;
import com.connect.pairr.core.recommendation.RecommendationEngine;
import com.connect.pairr.model.dto.UserSkillAvailabilityData;
import com.connect.pairr.model.entity.UserAvailability;
import com.connect.pairr.model.entity.UserSkill;
import com.connect.pairr.model.enums.DayType;
import com.connect.pairr.model.enums.ProficiencyLevel;
import com.connect.pairr.exception.RequesterAvailabilityMissingException;
import com.connect.pairr.exception.RequesterSkillMissingException;
import com.connect.pairr.repository.UserSkillRepository;
import com.connect.pairr.repository.UserAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final UserSkillRepository userSkillRepository;
    private final UserAvailabilityRepository userAvailabilityRepository;
    private final RecommendationEngine recommendationEngine;

    public List<Recommendation> getRecommendations(
            UUID requesterUserId,
            UUID skillId,
            DayType dayType,
            int numberOfRecommendations
    ) {

        // Fetch requester availabilities for this skill
        // TODO: make UserAvailability a proxy record
        List<UserAvailability> requesterAvailabilities =
                userAvailabilityRepository.findByUserIdAndDayType(requesterUserId, dayType);

        if (requesterAvailabilities.isEmpty()) {
            throw new RequesterAvailabilityMissingException();
        }

        Optional<UserSkill> requesterUserSkillOptional = userSkillRepository
                .findByUserIdAndSkillId(requesterUserId, skillId);

        if (requesterUserSkillOptional.isEmpty()) {
            throw new RequesterSkillMissingException(skillId);
        }

        ProficiencyLevel requesterProficiency = requesterUserSkillOptional.get().getProficiency();
        UserSkill requesterUserSkill = requesterUserSkillOptional.get();

        List<UserSkillAvailabilityData> candidates =
                userSkillRepository.getRecommendationCandidates(skillId, dayType, requesterUserId);

        // Group by user
        Map<UUID, List<UserSkillAvailabilityData>> groupedCandidates =
                candidates.stream()
                        .collect(Collectors.groupingBy(UserSkillAvailabilityData::userId));

        // current user -> List.of(UA) [(uid1, name1, [start1,end1]), (uid1, name1, [start2,end2])]
        // recommendation views -> List.of(UA) [(uid2, name2, [start3,end3]), (uid3, name3, [start4,end4]), (uid3, name3, [start5,end5])]
        // now we need to find a list of ranked user ids with an overlap
        // availability times

        // Potential future improvements if dataset grows large (10k+ candidates):
        // Push filtering to DB (pre-filter by rating threshold)
        // Add pagination
        return recommendationEngine.recommend(
                requesterAvailabilities,
                requesterUserSkill,
                groupedCandidates,
                numberOfRecommendations);

    }
}

