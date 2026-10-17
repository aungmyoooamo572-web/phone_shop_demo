package com.example.demo.exception;

import com.example.demo.user.dto.UserCreateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the centralised exception -> HTTP translation.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static MethodParameter dummyParameter() {
        Method method;
        try {
            method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void dummy(String value) {
        // Only used to build a MethodParameter.
    }

    private ApiError bodyOf(ResponseEntity<ApiError> response) {
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404 with the exception message")
    void notFoundBecomes404() {
        ResponseEntity<ApiError> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Order not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bodyOf(response).getStatus()).isEqualTo(404);
        assertThat(bodyOf(response).getMessage()).isEqualTo("Order not found");
        assertThat(bodyOf(response).getError()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("DuplicateResourceException -> 409")
    void duplicateBecomes409() {
        ResponseEntity<ApiError> response = handler.handleDuplicateResource(
                new DuplicateResourceException("Username is already taken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(bodyOf(response).getMessage()).isEqualTo("Username is already taken");
    }

    @Test
    @DisplayName("ConflictException -> 409 with the exception message")
    void conflictBecomes409() {
        ResponseEntity<ApiError> response = handler.handleConflict(
                new ConflictException("Not enough stock for: SKU-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(bodyOf(response).getStatus()).isEqualTo(409);
        assertThat(bodyOf(response).getError()).isEqualTo("Conflict");
        assertThat(bodyOf(response).getMessage()).isEqualTo("Not enough stock for: SKU-1");
    }

    @Test
    @DisplayName("BadRequestException -> 400 with the exception message")
    void badRequestBecomes400() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new BadRequestException("Cart is empty"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bodyOf(response).getStatus()).isEqualTo(400);
        assertThat(bodyOf(response).getError()).isEqualTo("Bad Request");
        assertThat(bodyOf(response).getMessage()).isEqualTo("Cart is empty");
    }

    @Test
    @DisplayName("Bean validation failure -> 400 listing the offending fields")
    void validationBecomes400() {
        BeanPropertyBindingResult binding =
                new BeanPropertyBindingResult(new UserCreateDto(), "userCreateDto");
        binding.rejectValue("username", "NotBlank", "Username is required");
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(dummyParameter(), binding);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bodyOf(response).getMessage())
                .contains("username")
                .contains("Username is required");
    }

    @Test
    @DisplayName("Unparseable request body -> 400")
    void unreadableBodyBecomes400() {
        ResponseEntity<ApiError> response = handler.handleUnreadableBody(
                new HttpMessageNotReadableException("malformed", (HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Wrong path variable type -> 400 naming the argument")
    void typeMismatchBecomes400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "not-a-uuid", UUID.class, "orderId", dummyParameter(), null);

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bodyOf(response).getMessage()).contains("orderId");
    }

    @Test
    @DisplayName("Bad credentials -> 401 and never leak whether the user exists")
    void badCredentialsBecomes401() {
        ResponseEntity<ApiError> response = handler.handleAuthentication(
                new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(bodyOf(response).getMessage()).isEqualTo("Invalid username or password");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403")
    void accessDeniedBecomes403() {
        ResponseEntity<ApiError> response = handler.handleAccessDenied(
                new AccessDeniedException("You are not allowed to access this order"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(bodyOf(response).getMessage())
                .isEqualTo("You are not allowed to access this order");
    }

    @Test
    @DisplayName("An unmapped path keeps its 404 instead of becoming a 500")
    void unmappedPathKeeps404() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new NoResourceFoundException(HttpMethod.GET, "/api/does-not-exist", "/api/does-not-exist"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("An unexpected failure -> safe 500 with no internal details")
    void unexpectedBecomesSafe500() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new IllegalStateException("jdbc:mysql://localhost:3306/phone_shop_demo?user=root&password=admin"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(bodyOf(response).getMessage())
                .isEqualTo("An unexpected error occurred. Please try again later.")
                .doesNotContain("jdbc")
                .doesNotContain("password");
    }
}
