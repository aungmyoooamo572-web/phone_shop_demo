package com.example.demo.exception;

import lombok.Getter;

/**
 * Uniform error payload returned for every handled failure.
 *
 * The React client reads {@code response.data.message}, so every error response
 * contains a {@code message} field. No stack traces, SQL or credentials are ever
 * put into this object.
 */
@Getter
public class ApiError {

    private final int status;

    private final String error;

    private final String message;

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }
}
