package com.example.demo.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Requests rejected inside the filter chain never reach the
                // @RestControllerAdvice, so answer them here as well, using the same
                // JSON shape ({status, error, message}) the frontend reads everywhere.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "Authentication is required to access this resource"
                                ))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "You do not have permission to access this resource"
                                ))
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()

                        // Public catalog browsing (read only)
                        .requestMatchers(HttpMethod.GET, "/api/phones", "/api/phones/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/brands", "/api/brands/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()

                        // Reading the reviews of a phone is public, like the catalog it
                        // belongs to. "/api/reviews/me" and every write stays behind
                        // authentication / ownership checks in ReviewService.
                        .requestMatchers(HttpMethod.GET, "/api/reviews/phone/**").permitAll()

                        // Phone write operations (POST / PUT / PATCH / DELETE, including a
                        // phone's variants and images) require the ADMIN role.
                        // This rule MUST stay after the GET permitAll rules above: request
                        // matchers are evaluated in order and the first match wins, so a
                        // broad "/api/phones/**" permitAll elsewhere would bypass it.
                        .requestMatchers("/api/phones", "/api/phones/**").hasRole("ADMIN")

                        // Brand and category write operations (POST / PUT / PATCH / DELETE)
                        // require the ADMIN role. These rules MUST stay after the GET
                        // permitAll rules above for the same reason as the phone rules:
                        // matchers are evaluated in order and the first match wins.
                        .requestMatchers("/api/brands", "/api/brands/**").hasRole("ADMIN")
                        .requestMatchers("/api/categories", "/api/categories/**").hasRole("ADMIN")

                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/payments/*/verify")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/payments/*/reject")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/payments/pending", "/api/payments/all")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/orders/all")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/orders/*/status")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/orders/*/tracking")
                        .hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /*
     * Minimal JSON writer for filter-chain errors (same shape as
     * com.example.demo.exception.ApiError):
     *
     *   {"status":403,"error":"Forbidden","message":"..."}
     *
     * Only fixed, quote-free literal messages are passed in, so no escaping is needed.
     */
    private static void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":" + status.value()
                        + ",\"error\":\"" + status.getReasonPhrase()
                        + "\",\"message\":\"" + message + "\"}"
        );
    }
}