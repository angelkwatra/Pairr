package com.connect.pairr.service;

import com.connect.pairr.mapper.CategoryMapper;
import com.connect.pairr.mapper.SkillMapper;
import com.connect.pairr.mapper.UserMapper;
import com.connect.pairr.model.dto.AddCategoryRequest;
import com.connect.pairr.model.dto.AddSkillRequest;
import com.connect.pairr.model.dto.CategoryResponse;
import com.connect.pairr.model.dto.SkillResponse;
import com.connect.pairr.model.dto.UserResponse;
import com.connect.pairr.model.entity.Category;
import com.connect.pairr.model.entity.Skill;
import com.connect.pairr.repository.CategoryRepository;
import com.connect.pairr.repository.SkillRepository;
import com.connect.pairr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SkillRepository skillRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public SkillResponse addSkill(AddSkillRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NoSuchElementException("No category present with provided id"));

        Skill skill = SkillMapper.toEntity(request, category);
        skill = skillRepository.save(skill);
        return SkillMapper.toResponse(skill);
    }

    public CategoryResponse addCategory(AddCategoryRequest request) {
        Category category = CategoryMapper.toEntity(request);
        category = categoryRepository.save(category);
        return CategoryMapper.toResponse(category);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
                .toList();
    }
}
