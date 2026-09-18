package com.back.global.exception;

public class GroupLimitExceededException extends RuntimeException {
    public GroupLimitExceededException(String message) {
        super(message);
    }
}