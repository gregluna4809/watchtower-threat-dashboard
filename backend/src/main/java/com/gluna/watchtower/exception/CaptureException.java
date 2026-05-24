package com.gluna.watchtower.exception;

public class CaptureException extends RuntimeException {

    public CaptureException() {
        super();
    }

    public CaptureException(String message) {
        super(message);
    }

    public CaptureException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaptureException(Throwable cause) {
        super(cause);
    }
}

