package com.workworth.goals.exception;

public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException(String message) {
        super(message);
    }
}
