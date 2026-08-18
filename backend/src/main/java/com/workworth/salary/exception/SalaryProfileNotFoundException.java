package com.workworth.salary.exception;

public class SalaryProfileNotFoundException extends RuntimeException {

    public SalaryProfileNotFoundException(String message) {
        super(message);
    }
}
