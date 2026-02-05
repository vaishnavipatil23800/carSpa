package com.carspa.chatservice.config;

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

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Chat service error";

        // Distinguish API key errors from rate limits from general errors
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (msg.contains("rate limit"))    status = HttpStatus.TOO_MANY_REQUESTS;
        if (msg.contains("Invalid OpenAI"))status = HttpStatus.UNAUTHORIZED;

        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle("Chat Error");
        pd.setDetail(msg);
        return pd;
    }
}
