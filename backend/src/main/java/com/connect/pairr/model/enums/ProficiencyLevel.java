package com.connect.pairr.model.enums;

import lombok.Getter;

@Getter
public enum ProficiencyLevel {
    BEGINNER (1),
    AMATEUR (2),
    INTERMEDIATE (3),
    EXPERT (4);

    private final int level;

    ProficiencyLevel(int level) {
        this.level = level;
    }

    public double normalized() {
        return level / 4.0;
    }
}
