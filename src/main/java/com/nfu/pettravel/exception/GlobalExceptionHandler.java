package com.nfu.pettravel.exception;

import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null
                        ? ErrorCode.INVALID_ARGUMENT.getMessage() : error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_ARGUMENT.getMessage());
        return badRequest(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst().map(violation -> violation.getMessage())
                .orElse(ErrorCode.INVALID_ARGUMENT.getMessage());
        return badRequest(message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MultipartException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            BindException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return badRequest(ErrorCode.INVALID_ARGUMENT.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateKeyException exception) {
        return ResponseEntity.status(ErrorCode.DUPLICATE_BOOKING.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.DUPLICATE_BOOKING));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIoFailure(
            IOException exception, HttpServletResponse response) {
        log.error("Response I/O failure", exception);
        if (response.isCommitted()) {
            return ResponseEntity.status(response.getStatus()).build();
        }
        return ResponseEntity.status(ErrorCode.EXPORT_FAILED.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.EXPORT_FAILED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled request failure", exception);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return ResponseEntity.status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, message));
    }
}
