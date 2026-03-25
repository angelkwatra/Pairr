package com.connect.pairr.exception;

import java.util.UUID;

public class DuplicateRatingException extends RuntimeException {
    public DuplicateRatingException(UUID toUserId, UUID skillId) {
        super("You have already rated user " + toUserId + " for skill " + skillId);
    }
}
