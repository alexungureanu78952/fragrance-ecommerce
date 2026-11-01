package com.fragrance.ecommerce.configuration;

import com.fragrance.ecommerce.exception.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var detail = problem(HttpStatus.BAD_REQUEST, "Validation failed",
            "One or more fields are invalid.", request);
        detail.setProperty("fieldErrors", exception.getBindingResult().getFieldErrors().stream()
            .collect(java.util.stream.Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                (first, ignored) -> first)));
        return detail;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail,
                                  HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://fragrance.example/problems/" + status.value()));
        problem.setProperty("requestId", request.getHeader("X-Request-ID"));
        return problem;
    }
}
