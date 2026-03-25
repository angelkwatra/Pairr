package com.connect.pairr.repository;

import com.connect.pairr.model.entity.Skill;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    @NonNull
    @EntityGraph(attributePaths = "category")
    List<Skill> findAll();
}
