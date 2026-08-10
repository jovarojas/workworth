package com.workworth.common.exception;

import com.workworth.salary.exception.SalaryConfigurationIncompleteException;
import com.workworth.salary.exception.SalaryProfileConflictException;
import com.workworth.salary.exception.SalaryProfileNotFoundException;
import com.workworth.salary.exception.SalaryRateUnavailableException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setTitle("Validation error");
        problem.setProperty("code", ApiErrorCode.VALIDATION_ERROR.name());
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(SalaryProfileNotFoundException.class)
    ProblemDetail handleNotFound(SalaryProfileNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(SalaryProfileConflictException.class)
    ProblemDetail handleConflict(SalaryProfileConflictException exception) {
        return problem(HttpStatus.CONFLICT, ApiErrorCode.SALARY_PROFILE_CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(SalaryRateUnavailableException.class)
    ProblemDetail handleUnavailableRate(SalaryRateUnavailableException exception) {
        return problem(HttpStatus.CONFLICT, ApiErrorCode.SALARY_RATE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(SalaryConfigurationIncompleteException.class)
    ProblemDetail handleIncompleteConfiguration(SalaryConfigurationIncompleteException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ApiErrorCode.SALARY_CONFIGURATION_INCOMPLETE,
                exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, ApiErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code.name());
        return problem;
    }
}
