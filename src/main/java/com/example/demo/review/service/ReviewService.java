package com.example.demo.review.service;

import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.entity.Phone;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.review.dao.ReviewRepository;
import com.example.demo.review.dto.CreateReviewDto;
import com.example.demo.review.dto.ReviewResponseDto;
import com.example.demo.review.dto.UpdateReviewDto;
import com.example.demo.review.entity.Review;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PhoneRepository phoneRepository;
    private final UserService userService;

    /** Reviews of one phone. Public, read-only. */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> findByPhone(UUID phoneId) {
        if (!phoneRepository.existsById(phoneId)) {
            throw new ResourceNotFoundException("Phone not found");
        }

        return reviewRepository.findByPhoneIdOrderByCreatedAtDesc(phoneId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** The caller's own reviews. */
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> findMine(String username) {
        User user = getUser(username);

        return reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * A customer may review a phone once. The check runs first for a clear 409;
     * the database uniqueness (uk_review_user_phone) backs it up so two
     * simultaneous submissions cannot both win.
     */
    public ReviewResponseDto create(String username, CreateReviewDto dto) {
        User user = getUser(username);

        Phone phone = phoneRepository.findById(dto.getPhoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));

        if (phone.isDeleted()) {
            throw new ResourceNotFoundException("Phone not found");
        }

        if (reviewRepository.existsByUserIdAndPhoneId(user.getId(), phone.getId())) {
            throw new ConflictException("You have already reviewed this phone");
        }

        Review review = new Review();
        review.setUser(user);
        review.setPhone(phone);
        review.setRating(dto.getRating());
        review.setComment(normaliseComment(dto.getComment()));

        try {
            return toDto(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("You have already reviewed this phone");
        }
    }

    public ReviewResponseDto update(String username, UUID reviewId, UpdateReviewDto dto) {
        Review review = findOwnedReview(username, reviewId);

        review.setRating(dto.getRating());
        review.setComment(normaliseComment(dto.getComment()));

        return toDto(reviewRepository.save(review));
    }

    public void delete(String username, UUID reviewId) {
        reviewRepository.delete(findOwnedReview(username, reviewId));
    }

    /** Loads a review and refuses to touch somebody else's. */
    private Review findOwnedReview(String username, UUID reviewId) {
        User user = getUser(username);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only manage your own reviews");
        }

        return review;
    }

    private User getUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normaliseComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReviewResponseDto toDto(Review review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getPhone().getId(),
                review.getPhone().getName(),
                review.getUser().getId(),
                review.getUser().getUsername(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
