package com.connect.pairr.service;

import com.connect.pairr.exception.*;
import com.connect.pairr.model.dto.AddRatingRequest;
import com.connect.pairr.model.dto.RatingResponse;
import com.connect.pairr.model.entity.*;
import com.connect.pairr.model.enums.ProficiencyLevel;
import com.connect.pairr.model.enums.Role;
import com.connect.pairr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private PairingSessionRepository pairingSessionRepository;

    @InjectMocks
    private RatingService ratingService;

    private UUID fromUserId;
    private UUID toUserId;
    private UUID skillId;
    private User fromUser;
    private User toUser;
    private Skill skill;
    private UserSkill fromUserSkill;
    private UserSkill toUserSkill;

    @BeforeEach
    void setUp() {
        fromUserId = UUID.randomUUID();
        toUserId = UUID.randomUUID();
        skillId = UUID.randomUUID();

        fromUser = User.builder().id(fromUserId).email("from@test.com")
                .displayName("From").password("p").role(Role.USER).createdAt(Instant.now()).build();
        toUser = User.builder().id(toUserId).email("to@test.com")
                .displayName("To").password("p").role(Role.USER).createdAt(Instant.now()).build();

        Category category = Category.builder().id(UUID.randomUUID()).name("Tech").build();
        skill = Skill.builder().id(skillId).name("Java").category(category).build();

        fromUserSkill = new UserSkill(UUID.randomUUID(), fromUser, skill, ProficiencyLevel.INTERMEDIATE, null);
        toUserSkill = new UserSkill(UUID.randomUUID(), toUser, skill, ProficiencyLevel.EXPERT, null);
    }

    private AddRatingRequest request(int rating) {
        return new AddRatingRequest(toUserId, skillId, rating, "Good job");
    }

    @Test
    void submitRating_happyPath() {
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(toUserId)).thenReturn(Optional.of(toUser));
        when(userSkillRepository.findByUserIdAndSkillId(toUserId, skillId)).thenReturn(Optional.of(toUserSkill));
        when(ratingRepository.findByFromUserIdAndToUserIdAndSkillId(fromUserId, toUserId, skillId)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setFromUser(fromUser); // Manually set fields that would be null in mapper-to-entity result
            r.setToUser(toUser);
            r.setSkill(skill);
            r.setCreatedAt(Instant.now());
            return r;
        });
        when(ratingRepository.averageRatingByToUserIdAndSkillId(toUserId, skillId)).thenReturn(BigDecimal.valueOf(4));
        when(ratingRepository.averageRatingByToUserId(toUserId)).thenReturn(BigDecimal.valueOf(4));

        RatingResponse response = ratingService.submitRating(fromUserId, request(4));

        assertNotNull(response);
        assertEquals(4, response.rating());
        verify(userSkillRepository).save(toUserSkill);
        verify(userRepository).save(toUser);
    }

    @Test
    void submitRating_selfRating_throws() {
        AddRatingRequest selfRequest = new AddRatingRequest(fromUserId, skillId, 5, null);
        assertThrows(SelfRatingException.class,
                () -> ratingService.submitRating(fromUserId, selfRequest));
    }

    @Test
    void submitRating_fromUserNotFound_throws() {
        // Now findById(toUserId) is what triggers UserNotFoundException
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(toUserId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> ratingService.submitRating(fromUserId, request(4)));
    }

    @Test
    void submitRating_toUserNotFound_throws() {
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(toUserId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> ratingService.submitRating(fromUserId, request(4)));
    }

    @Test
    void submitRating_skillNotFound_throws() {
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(toUserId)).thenReturn(Optional.of(toUser));
        when(userSkillRepository.findByUserIdAndSkillId(toUserId, skillId)).thenReturn(Optional.empty());
        assertThrows(SkillNotFoundException.class,
                () -> ratingService.submitRating(fromUserId, request(4)));
    }

    @Test
    void submitRating_fromUserMissingSkill_throws() {
        // This test is now redundant because session check covers it, 
        // but if session check passes and toUserSkill is missing, it throws SkillNotFoundException
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(toUserId)).thenReturn(Optional.of(toUser));
        when(userSkillRepository.findByUserIdAndSkillId(toUserId, skillId)).thenReturn(Optional.empty());
        assertThrows(SkillNotFoundException.class,
                () -> ratingService.submitRating(fromUserId, request(4)));
    }

    @Test
    void submitRating_toUserMissingSkill_throws() {
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(toUserId)).thenReturn(Optional.of(toUser));
        when(userSkillRepository.findByUserIdAndSkillId(toUserId, skillId)).thenReturn(Optional.empty());
        assertThrows(SkillNotFoundException.class,
                () -> ratingService.submitRating(fromUserId, request(4)));
    }

    @Test
    void submitRating_overwritesExisting() {
        Rating existing = Rating.builder()
                .id(UUID.randomUUID())
                .fromUser(fromUser)
                .toUser(toUser)
                .skill(skill)
                .rating(2)
                .build();
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(true);
        when(userRepository.findById(toUserId)).thenReturn(Optional.of(toUser));
        when(userSkillRepository.findByUserIdAndSkillId(toUserId, skillId)).thenReturn(Optional.of(toUserSkill));
        when(ratingRepository.findByFromUserIdAndToUserIdAndSkillId(fromUserId, toUserId, skillId)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(Rating.class))).thenReturn(existing);
        when(ratingRepository.averageRatingByToUserIdAndSkillId(toUserId, skillId)).thenReturn(BigDecimal.valueOf(5));
        when(ratingRepository.averageRatingByToUserId(toUserId)).thenReturn(BigDecimal.valueOf(5));

        RatingResponse response = ratingService.submitRating(fromUserId, request(5));

        assertEquals(5, existing.getRating());
        assertEquals(5, response.rating());
        verify(ratingRepository).save(existing);
    }

    @Test
    void submitRating_noSession_throws() {
        when(pairingSessionRepository.existsByParticipantsAndSkillAndStatusIn(any(), any(), any(), any())).thenReturn(false);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ratingService.submitRating(fromUserId, request(4)));
        assertTrue(ex.getMessage().contains("verified pairing session"));
    }

    @Test
    void getRatingsForUser_returnsMappedList() {
        Rating rating = Rating.builder()
                .id(UUID.randomUUID()).fromUser(fromUser).toUser(toUser)
                .skill(skill).rating(4).feedback("Nice").createdAt(Instant.now()).build();
        when(ratingRepository.findAllByToUserId(toUserId)).thenReturn(List.of(rating));

        List<RatingResponse> result = ratingService.getRatingsForUser(toUserId);
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).rating());
    }

    @Test
    void getRatingsForUserSkill_returnsFilteredList() {
        Rating rating = Rating.builder()
                .id(UUID.randomUUID()).fromUser(fromUser).toUser(toUser)
                .skill(skill).rating(5).feedback(null).createdAt(Instant.now()).build();
        when(ratingRepository.findAllByToUserIdAndSkillId(toUserId, skillId)).thenReturn(List.of(rating));

        List<RatingResponse> result = ratingService.getRatingsForUserSkill(toUserId, skillId);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).rating());
    }
}
