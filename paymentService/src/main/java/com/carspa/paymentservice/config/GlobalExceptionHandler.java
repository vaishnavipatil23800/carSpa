package com.carspa.paymentservice.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleAccessDenied(SecurityException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Access Denied");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Invalid Request");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntime(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("not found")) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            pd.setTitle("Not Found");
            pd.setDetail(ex.getMessage());
            return pd;
        }
        if (ex.getMessage() != null && ex.getMessage().contains("signature")) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.PAYMENT_REQUIRED);
            pd.setTitle("Payment Verification Failed");
            pd.setDetail(ex.getMessage());
            return pd;
        }
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Error");
        pd.setDetail(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        return pd;
    }
}
