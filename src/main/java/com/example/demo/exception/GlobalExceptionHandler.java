package com.example.demo.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.dto.ApiResponse;


@ControllerAdvice
public class GlobalExceptionHandler {
    
    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = Exception.class) //Unhandled exceptions
    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception){
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setCode(9999);
        apiResponse.setMessage(exception.getMessage());
        exception.printStackTrace();
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = AppException.class) //Handled exceptions
    ResponseEntity<ApiResponse> handlingAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = MethodArgumentNotValidException.class) //Validation DTO
    ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception){
        @SuppressWarnings("null")
        String enumkey = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.valueOf(enumkey);

        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage()); 

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(AccessDeniedException.class) //SpringSecurity
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: " + ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class) //SpringSecurity
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: " + ex.getMessage());
    }


}
