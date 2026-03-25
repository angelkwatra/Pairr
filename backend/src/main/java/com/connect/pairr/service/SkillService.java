package com.connect.pairr.service;

import com.connect.pairr.mapper.SkillMapper;
import com.connect.pairr.model.dto.SkillResponse;
import com.connect.pairr.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(SkillMapper::toResponse)
                .toList();
    }
}
