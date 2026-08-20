package com.medisync.exception;

import com.medisync.user.exception.DuplicateOnboardingException;
import com.medisync.user.exception.InvalidAuthenticatedUserException;
import com.medisync.user.exception.InvalidOnboardingRoleException;
import com.medisync.user.exception.OnboardingRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Invalid request",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> unreadable(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request body is missing or invalid", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> invalidPathValue(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A path value is invalid", request);
    }

    @ExceptionHandler(InvalidOnboardingRoleException.class)
    ResponseEntity<ApiErrorResponse> invalidRole(InvalidOnboardingRoleException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ROLE", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ApiErrorResponse> invalidRequest(InvalidRequestException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ApiErrorResponse> conflict(ResourceConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> forbidden(HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource", request);
    }

    @ExceptionHandler(DuplicateOnboardingException.class)
    ResponseEntity<ApiErrorResponse> duplicate(DuplicateOnboardingException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "ONBOARDING_ALREADY_COMPLETED", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> constraintConflict(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "RESOURCE_CONFLICT",
                "The requested operation conflicts with an existing record", request);
    }

    @ExceptionHandler(OnboardingRequiredException.class)
    ResponseEntity<ApiErrorResponse> onboardingRequired(OnboardingRequiredException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "ONBOARDING_REQUIRED", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidAuthenticatedUserException.class)
    ResponseEntity<ApiErrorResponse> invalidIdentity(InvalidAuthenticatedUserException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler({DataAccessException.class, CannotCreateTransactionException.class})
    ResponseEntity<ApiErrorResponse> databaseUnavailable(Exception exception, HttpServletRequest request) {
        log.error("Database operation failed", exception);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE", "MediSync is temporarily unavailable", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected API error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), code, message, request.getRequestURI()));
    }
}
