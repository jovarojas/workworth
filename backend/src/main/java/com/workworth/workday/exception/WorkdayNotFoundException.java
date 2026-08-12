package com.workworth.workday.exception;

public class WorkdayNotFoundException extends RuntimeException {
    public WorkdayNotFoundException(String message) {
        super(message);
    }
}
