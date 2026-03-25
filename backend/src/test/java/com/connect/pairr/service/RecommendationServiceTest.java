package com.connect.pairr.service;

import com.connect.pairr.core.recommendation.RecommendationEngine;
import com.connect.pairr.exception.RequesterAvailabilityMissingException;
import com.connect.pairr.exception.RequesterSkillMissingException;
import com.connect.pairr.model.dto.Recommendation;
import com.connect.pairr.model.dto.UserSkillAvailabilityData;
import com.connect.pairr.model.entity.*;
import com.connect.pairr.model.enums.DayType;
import com.connect.pairr.model.enums.ProficiencyLevel;
import com.connect.pairr.model.enums.Role;
import com.connect.pairr.repository.UserAvailabilityRepository;
import com.connect.pairr.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private UserSkillRepository userSkillRepository;
    @Mock private UserAvailabilityRepository userAvailabilityRepository;
    @Mock private RecommendationEngine recommendationEngine;

    @InjectMocks
    private RecommendationService recommendationService;

    private UUID userId;
    private UUID skillId;
    private UserAvailability availability;
    private UserSkill userSkill;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();

        User user = User.builder().id(userId).email("u@test.com")
                .displayName("U").password("p").role(Role.USER).createdAt(Instant.now()).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("Tech").build();
        Skill skill = Skill.builder().id(skillId).name("Java").category(category).build();

        availability = new UserAvailability();
        availability.setId(UUID.randomUUID());
        availability.setUser(user);
        availability.setDayType(DayType.WEEKDAY);
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(17, 0));

        userSkill = new UserSkill(UUID.randomUUID(), user, skill, ProficiencyLevel.INTERMEDIATE, BigDecimal.valueOf(4));
    }

    @Test
    void getRecommendations_happyPath() {
        when(userAvailabilityRepository.findByUserIdAndDayType(userId, DayType.WEEKDAY))
                .thenReturn(List.of(availability));
        when(userSkillRepository.findByUserIdAndSkillId(userId, skillId))
                .thenReturn(Optional.of(userSkill));

        UUID candidateId = UUID.randomUUID();
        UserSkillAvailabilityData candidateData = new UserSkillAvailabilityData(
                candidateId, "Candidate", ProficiencyLevel.EXPERT,
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(4.2), 0L,
                LocalTime.of(10, 0), LocalTime.of(15, 0));
        when(userSkillRepository.getRecommendationCandidates(skillId, DayType.WEEKDAY, userId))
                .thenReturn(List.of(candidateData));

        Recommendation rec = Recommendation.builder()
                .userId(candidateId).displayName("Candidate").score(0.85).build();
        when(recommendationEngine.recommend(anyList(), eq(userSkill), anyMap(), eq(5)))
                .thenReturn(List.of(rec));

        List<Recommendation> result = recommendationService.getRecommendations(userId, skillId, DayType.WEEKDAY, 5);

        assertEquals(1, result.size());
        assertEquals(0.85, result.get(0).score());
    }

    @Test
    void getRecommendations_noAvailability_throws() {
        when(userAvailabilityRepository.findByUserIdAndDayType(userId, DayType.WEEKDAY))
                .thenReturn(Collections.emptyList());

        assertThrows(RequesterAvailabilityMissingException.class,
                () -> recommendationService.getRecommendations(userId, skillId, DayType.WEEKDAY, 5));
    }

    @Test
    void getRecommendations_missingSkill_throws() {
        when(userAvailabilityRepository.findByUserIdAndDayType(userId, DayType.WEEKDAY))
                .thenReturn(List.of(availability));
        when(userSkillRepository.findByUserIdAndSkillId(userId, skillId))
                .thenReturn(Optional.empty());

        assertThrows(RequesterSkillMissingException.class,
                () -> recommendationService.getRecommendations(userId, skillId, DayType.WEEKDAY, 5));
    }
}
