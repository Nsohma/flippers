package com.example.demo.controller;

import com.example.demo.controller.dto.ErrorResponse;
import com.example.demo.service.exception.InvalidPosExcelException;
import com.example.demo.service.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(
                HttpStatus.NOT_FOUND,
                "not_found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            InvalidPosExcelException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            MultipartException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "bad_request",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_server_error",
                "unexpected server error",
                request.getRequestURI()
        );
    }

    private static ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {
        ErrorResponse body = new ErrorResponse(
                error,
                message == null ? "" : message,
                path,
                OffsetDateTime.now().toString()
        );
        return ResponseEntity.status(status).body(body);
    }
}
