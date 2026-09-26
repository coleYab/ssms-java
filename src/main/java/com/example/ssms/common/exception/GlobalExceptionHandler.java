package com.example.ssms.common.exception;

import com.example.ssms.common.constant.ErrorCode;
import com.example.ssms.common.dto.ApiErrorDetail;
import com.example.ssms.common.dto.ApiErrorResponse;
import com.example.ssms.common.dto.FieldErrorDetail;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DOC_BASE_URL = "https://docs.school.example.com/errors/";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildErrorResponse(
            ex.getErrorCode().name(),
            ex.getMessage(),
            ex.getHttpStatus(),
            ex.getDetails().isEmpty() ? null : ex.getDetails(),
            ex.getContext().isEmpty() ? null : ex.getContext(),
            request,
            response
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request, HttpServletResponse response) {
        List<FieldErrorDetail> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            String code = mapValidationCode(fieldError.getCode());
            String message = fieldError.getDefaultMessage();
            Object rejected = sanitizeRejectedValue(field, fieldError.getRejectedValue());
            details.add(new FieldErrorDetail(field, code, message != null ? message : "Invalid value.", "body", rejected));
        }

        details.sort(Comparator.comparing(FieldErrorDetail::field));
        if (details.size() > 50) {
            details = details.subList(0, 50);
        }

        return buildErrorResponse(
            ErrorCode.VALIDATION_FAILED.name(),
            ErrorCode.VALIDATION_FAILED.getDefaultMessageTemplate(),
            HttpStatus.UNPROCESSABLE_ENTITY,
            details,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request, HttpServletResponse response) {
        Throwable cause = ex.getCause();
        if (cause instanceof UnrecognizedPropertyException upe) {
            String field = upe.getPropertyName();
            String message = "Unknown field " + field + ".";
            return buildErrorResponse(
                ErrorCode.VALIDATION_UNKNOWN_FIELD.name(),
                message,
                HttpStatus.UNPROCESSABLE_ENTITY,
                List.of(FieldErrorDetail.body(field, "UNKNOWN_FIELD", message)),
                Map.of("field", field),
                request,
                response
            );
        }

        return buildErrorResponse(
            ErrorCode.REQUEST_MALFORMED_JSON.name(),
            ErrorCode.REQUEST_MALFORMED_JSON.getDefaultMessageTemplate(),
            HttpStatus.BAD_REQUEST,
            null,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request, HttpServletResponse response) {
        if ("id".equalsIgnoreCase(ex.getName()) || (ex.getRequiredType() != null && ex.getRequiredType().equals(UUID.class))) {
            return buildErrorResponse(
                ErrorCode.REQUEST_INVALID_ID.name(),
                ErrorCode.REQUEST_INVALID_ID.getDefaultMessageTemplate(),
                HttpStatus.BAD_REQUEST,
                null,
                null,
                request,
                response
            );
        }

        return buildErrorResponse(
            ErrorCode.REQUEST_INVALID_QUERY.name(),
            "Invalid parameter: " + ex.getName() + ".",
            HttpStatus.BAD_REQUEST,
            null,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildErrorResponse(
            ErrorCode.REQUEST_METHOD_NOT_ALLOWED.name(),
            ErrorCode.REQUEST_METHOD_NOT_ALLOWED.getDefaultMessageTemplate(),
            HttpStatus.METHOD_NOT_ALLOWED,
            null,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildErrorResponse(
            ErrorCode.REQUEST_UNSUPPORTED_MEDIA_TYPE.name(),
            ErrorCode.REQUEST_UNSUPPORTED_MEDIA_TYPE.getDefaultMessageTemplate(),
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            null,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildErrorResponse(
            ErrorCode.REQUEST_NOT_ACCEPTABLE.name(),
            ErrorCode.REQUEST_NOT_ACCEPTABLE.getDefaultMessageTemplate(),
            HttpStatus.NOT_ACCEPTABLE,
            null,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildErrorResponse(
            ErrorCode.AUTHZ_FORBIDDEN_ROLE.name(),
            ErrorCode.AUTHZ_FORBIDDEN_ROLE.getDefaultMessageTemplate(),
            HttpStatus.FORBIDDEN,
            null,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request, HttpServletResponse response) {
        return buildErrorResponse(
            ErrorCode.AUTH_TOKEN_INVALID.name(),
            ErrorCode.AUTH_TOKEN_INVALID.getDefaultMessageTemplate(),
            HttpStatus.UNAUTHORIZED,
            null,
            null,
            request,
            response
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("Unhandled server error: ", ex);
        return buildErrorResponse(
            ErrorCode.SERVER_INTERNAL_ERROR.name(),
            ErrorCode.SERVER_INTERNAL_ERROR.getDefaultMessageTemplate(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            null,
            null,
            request,
            response
        );
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
        String code,
        String message,
        HttpStatus status,
        List<FieldErrorDetail> details,
        Map<String, Object> context,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String requestId = (String) request.getAttribute("X-Request-ID");
        if (requestId == null) {
            requestId = request.getHeader("X-Request-ID");
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        response.setHeader("X-Request-ID", requestId);

        ApiErrorDetail detail = new ApiErrorDetail(
            code,
            message,
            status.value(),
            details,
            context,
            requestId,
            Instant.now(),
            request.getRequestURI(),
            request.getMethod(),
            DOC_BASE_URL + code
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", requestId);
        return new ResponseEntity<>(ApiErrorResponse.of(detail), headers, status);
    }

    private String mapValidationCode(String springCode) {
        if (springCode == null) return "INVALID_FORMAT";
        return switch (springCode) {
            case "NotNull", "NotEmpty", "NotBlank" -> "REQUIRED";
            case "Size", "Length" -> "OUT_OF_RANGE";
            case "Min" -> "TOO_SHORT";
            case "Max" -> "TOO_LONG";
            case "Pattern" -> "INVALID_FORMAT";
            case "Email" -> "INVALID_FORMAT";
            case "Future" -> "DATE_IN_FUTURE";
            case "Past" -> "DATE_IN_PAST";
            default -> "INVALID_FORMAT";
        };
    }

    private Object sanitizeRejectedValue(String field, Object value) {
        if (field == null) return value;
        String lower = field.toLowerCase();
        if (lower.contains("password") || lower.contains("token") || lower.contains("secret")) {
            return null;
        }
        return value;
    }
}
