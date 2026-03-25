package com.connect.pairr.service;

import com.connect.pairr.exception.SkillNotFoundException;
import com.connect.pairr.exception.UserNotFoundException;
import com.connect.pairr.model.dto.AddUserSkillRequest;
import com.connect.pairr.model.entity.*;
import com.connect.pairr.model.enums.ProficiencyLevel;
import com.connect.pairr.model.enums.Role;
import com.connect.pairr.repository.SkillRepository;
import com.connect.pairr.repository.UserRepository;
import com.connect.pairr.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSkillServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private SkillRepository skillRepository;

    @InjectMocks
    private UserSkillService userSkillService;

    private UUID userId;
    private User user;
    private UUID skillId1;
    private UUID skillId2;
    private Skill skill1;
    private Skill skill2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("u@test.com")
                .displayName("U").password("p").role(Role.USER).createdAt(Instant.now()).build();

        Category category = Category.builder().id(UUID.randomUUID()).name("Tech").build();
        skillId1 = UUID.randomUUID();
        skillId2 = UUID.randomUUID();
        skill1 = Skill.builder().id(skillId1).name("Java").category(category).build();
        skill2 = Skill.builder().id(skillId2).name("Python").category(category).build();
    }

    @Test
    void addSkills_happyPath() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(skillRepository.findAllById(anyList())).thenReturn(List.of(skill1, skill2));

        List<AddUserSkillRequest> requests = List.of(
                new AddUserSkillRequest(skillId1, ProficiencyLevel.BEGINNER),
                new AddUserSkillRequest(skillId2, ProficiencyLevel.EXPERT));

        userSkillService.addSkills(userId, requests);

        verify(userSkillRepository).deleteAllByUserId(userId);
        verify(userSkillRepository).saveAll(argThat(list -> {
            List<UserSkill> saved = (List<UserSkill>) list;
            return saved.size() == 2;
        }));
    }

    @Test
    void addSkills_userNotFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> userSkillService.addSkills(userId, List.of(
                        new AddUserSkillRequest(skillId1, ProficiencyLevel.BEGINNER))));
    }

    @Test
    void addSkills_overwritesExisting() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(skillRepository.findAllById(anyList())).thenReturn(List.of(skill1));

        List<AddUserSkillRequest> requests = List.of(
                new AddUserSkillRequest(skillId1, ProficiencyLevel.INTERMEDIATE));

        userSkillService.addSkills(userId, requests);

        verify(userSkillRepository).deleteAllByUserId(userId);
        verify(userSkillRepository).saveAll(anyList());
    }

    @Test
    void addSkills_skillNotInDb_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(skillRepository.findAllById(anyList())).thenReturn(Collections.emptyList()); // nothing found

        assertThrows(SkillNotFoundException.class,
                () -> userSkillService.addSkills(userId, List.of(
                        new AddUserSkillRequest(skillId1, ProficiencyLevel.BEGINNER))));
    }
}
