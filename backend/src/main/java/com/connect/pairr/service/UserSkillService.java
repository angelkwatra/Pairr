package com.connect.pairr.service;

import com.connect.pairr.mapper.UserSkillMapper;
import com.connect.pairr.model.dto.AddUserSkillRequest;
import com.connect.pairr.model.entity.Skill;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.entity.UserSkill;
import com.connect.pairr.exception.SkillNotFoundException;
import com.connect.pairr.exception.UserNotFoundException;
import com.connect.pairr.repository.SkillRepository;
import com.connect.pairr.repository.UserRepository;
import com.connect.pairr.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSkillService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;

    public List<UserSkill> getUserSkills(UUID userId) {
        return userSkillRepository.findAllByUserId(userId);
    }

    @Transactional
    public void addSkills(UUID userId, List<AddUserSkillRequest> requests) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Delete all existing skills and replace with the new set
        userSkillRepository.deleteAllByUserId(userId);
        userSkillRepository.flush();

        if (requests.isEmpty()) return;

        // Fetch all requested skills in ONE query
        List<UUID> requestedSkillIds = requests.stream()
                .map(AddUserSkillRequest::skillId)
                .toList();

        List<Skill> skills = skillRepository.findAllById(requestedSkillIds);

        Map<UUID, Skill> skillMap = skills.stream()
                .collect(Collectors.toMap(Skill::getId, s -> s));

        // Build new entities
        List<UserSkill> toSave = new ArrayList<>();

        for (AddUserSkillRequest request : requests) {
            Skill skill = skillMap.get(request.skillId());

            if (skill == null) {
                throw new SkillNotFoundException(request.skillId());
            }

            toSave.add(UserSkillMapper.toEntity(user, skill, request.proficiency()));
        }

        // Bulk save
        userSkillRepository.saveAll(toSave);
    }
}

