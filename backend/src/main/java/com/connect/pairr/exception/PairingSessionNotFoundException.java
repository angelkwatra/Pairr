package com.connect.pairr.exception;

import java.util.UUID;

public class PairingSessionNotFoundException extends RuntimeException {
    public PairingSessionNotFoundException(UUID id) {
        super("Pairing session not found: " + id);
    }
}
