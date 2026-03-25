package com.connect.pairr.exception;

import java.util.UUID;

public class RequesterSkillMissingException extends RuntimeException {
    public RequesterSkillMissingException(UUID skillId) {
        super("Requester has not registered the skill: " + skillId);
    }
}