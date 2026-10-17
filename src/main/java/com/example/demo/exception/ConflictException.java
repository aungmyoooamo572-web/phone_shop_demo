package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A request that is understood but cannot be carried out because it conflicts
 * with the current state of the resource (out of stock, already paid, already
 * rejected, ...). Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
