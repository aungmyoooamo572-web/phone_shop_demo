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
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Business rules of the review module: one review per customer and phone, and a
 * customer may only edit or delete their own reviews.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewService reviewService;

    private UUID phoneId;

    private UUID reviewId;

    private User customer;

    private User otherCustomer;

    private Phone phone;

    private Review existing;

    @BeforeEach
    void setUp() {
        phoneId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        customer = newUser("customer-one");
        otherCustomer = newUser("customer-two");

        phone = new Phone();
        phone.setId(phoneId);
        phone.setName("Galaxy S25 Ultra");

        existing = new Review();
        existing.setId(reviewId);
        existing.setPhone(phone);
        existing.setUser(customer);
        existing.setRating(4);
        existing.setComment("Solid");
    }

    private User newUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setRole(Role.CUSTOMER);
        return user;
    }

    private CreateReviewDto createDto(int rating, String comment) {
        CreateReviewDto dto = new CreateReviewDto();
        dto.setPhoneId(phoneId);
        dto.setRating(rating);
        dto.setComment(comment);
        return dto;
    }

    private UpdateReviewDto updateDto(int rating, String comment) {
        UpdateReviewDto dto = new UpdateReviewDto();
        dto.setRating(rating);
        dto.setComment(comment);
        return dto;
    }

    @Test
    @DisplayName("A customer can create one review for a phone, comment trimmed")
    void customerCreatesOneReview() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.of(phone));
        when(reviewRepository.existsByUserIdAndPhoneId(customer.getId(), phoneId))
                .thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponseDto created = reviewService.create("customer-one", createDto(5, "  Great  "));

        assertThat(created.getRating()).isEqualTo(5);
        assertThat(created.getComment()).isEqualTo("Great");
        assertThat(created.getPhoneId()).isEqualTo(phoneId);
        assertThat(created.getPhoneName()).isEqualTo("Galaxy S25 Ultra");
        assertThat(created.getUsername()).isEqualTo("customer-one");
    }

    @Test
    @DisplayName("A blank comment is stored as null")
    void blankCommentBecomesNull() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.of(phone));
        when(reviewRepository.existsByUserIdAndPhoneId(customer.getId(), phoneId))
                .thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(reviewService.create("customer-one", createDto(3, "   ")).getComment())
                .isNull();
    }

    @Test
    @DisplayName("A second review for the same phone is a 409 (duplicate prevention)")
    void duplicateReviewIsConflict() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.of(phone));
        when(reviewRepository.existsByUserIdAndPhoneId(customer.getId(), phoneId))
                .thenReturn(true);

        assertThatThrownBy(() -> reviewService.create("customer-one", createDto(2, "again")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You have already reviewed this phone");

        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Reviewing an unknown phone is a 404")
    void unknownPhoneIsNotFound() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create("customer-one", createDto(5, "hi")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Phone not found");
    }

    @Test
    @DisplayName("A soft deleted phone cannot be reviewed")
    void deletedPhoneIsNotFound() {
        phone.setDeleted(true);
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.of(phone));

        assertThatThrownBy(() -> reviewService.create("customer-one", createDto(5, "hi")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Phone not found");
    }

    @Test
    @DisplayName("The owner can edit their own review")
    void ownerCanEditOwnReview() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing)).thenReturn(existing);

        ReviewResponseDto updated = reviewService.update("customer-one", reviewId, updateDto(1, "Changed my mind"));

        assertThat(updated.getRating()).isEqualTo(1);
        assertThat(updated.getComment()).isEqualTo("Changed my mind");
    }

    @Test
    @DisplayName("A customer cannot edit another customer's review (403)")
    void otherCustomerCannotEditReview() {
        when(userService.findByUsername("customer-two")).thenReturn(Optional.of(otherCustomer));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                reviewService.update("customer-two", reviewId, updateDto(1, "hacked")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only manage your own reviews");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("A customer cannot delete another customer's review (403)")
    void otherCustomerCannotDeleteReview() {
        when(userService.findByUsername("customer-two")).thenReturn(Optional.of(otherCustomer));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> reviewService.delete("customer-two", reviewId))
                .isInstanceOf(AccessDeniedException.class);

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("The owner can delete their own review")
    void ownerCanDeleteOwnReview() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existing));

        reviewService.delete("customer-one", reviewId);

        verify(reviewRepository).delete(existing);
    }

    @Test
    @DisplayName("A missing review is a 404")
    void missingReviewIsNotFound() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.delete("customer-one", reviewId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Review not found");
    }

    @Test
    @DisplayName("Listing the reviews of an unknown phone is a 404")
    void listingUnknownPhoneIsNotFound() {
        when(phoneRepository.existsById(phoneId)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.findByPhone(phoneId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Phone not found");
    }
}
