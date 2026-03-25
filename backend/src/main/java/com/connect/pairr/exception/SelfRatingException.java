package com.connect.pairr.exception;

public class SelfRatingException extends RuntimeException {
    public SelfRatingException() {
        super("You cannot rate yourself");
    }
}
