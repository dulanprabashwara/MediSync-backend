package com.medisync.user.exception;

public class InvalidAuthenticatedUserException extends RuntimeException {
    public InvalidAuthenticatedUserException(String message) {
        super(message);
    }
}
