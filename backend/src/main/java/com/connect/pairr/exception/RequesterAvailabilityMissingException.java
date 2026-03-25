package com.connect.pairr.exception;

public class RequesterAvailabilityMissingException extends RuntimeException {
    public RequesterAvailabilityMissingException() {
        super("Requester has not added any availability");
    }
}
