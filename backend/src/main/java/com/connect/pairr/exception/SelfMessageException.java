package com.connect.pairr.exception;

public class SelfMessageException extends RuntimeException {
    public SelfMessageException() {
        super("You cannot send a message to yourself");
    }
}
