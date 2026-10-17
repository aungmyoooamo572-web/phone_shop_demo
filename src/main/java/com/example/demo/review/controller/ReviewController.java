package com.example.demo.review.controller;

import com.example.demo.review.dto.CreateReviewDto;
import com.example.demo.review.dto.ReviewResponseDto;
import com.example.demo.review.dto.UpdateReviewDto;
import com.example.demo.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** Public: the reviews shown on a product page. */
    @GetMapping("/phone/{phoneId}")
    public List<ReviewResponseDto> findByPhone(@PathVariable UUID phoneId) {
        return reviewService.findByPhone(phoneId);
    }

    /** The signed-in customer's own reviews. */
    @GetMapping("/me")
    public List<ReviewResponseDto> findMine(Authentication authentication) {
        return reviewService.findMine(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponseDto create(
            Authentication authentication,
            @Valid @RequestBody CreateReviewDto dto
    ) {
        return reviewService.create(authentication.getName(), dto);
    }

    @PutMapping("/{reviewId}")
    public ReviewResponseDto update(
            Authentication authentication,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewDto dto
    ) {
        return reviewService.update(authentication.getName(), reviewId, dto);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable UUID reviewId
    ) {
        reviewService.delete(authentication.getName(), reviewId);
    }
}
