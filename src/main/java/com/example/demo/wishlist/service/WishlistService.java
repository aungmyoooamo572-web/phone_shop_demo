package com.example.demo.wishlist.service;

import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.entity.Phone;
import com.example.demo.catalog.entity.PhoneImage;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import com.example.demo.wishlist.dao.WishlistRepository;
import com.example.demo.wishlist.dto.WishlistItemResponseDto;
import com.example.demo.wishlist.entity.Wishlist;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The wishlist is always scoped to the authenticated customer: every query is
 * keyed by the caller's id, so one customer can never read, add to, or remove
 * from somebody else's list.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final PhoneRepository phoneRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<WishlistItemResponseDto> getWishlist(String username) {
        return toDtos(getUser(username));
    }

    public List<WishlistItemResponseDto> add(String username, UUID phoneId) {
        User user = getUser(username);

        Phone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));

        if (phone.isDeleted()) {
            throw new ResourceNotFoundException("Phone not found");
        }

        if (wishlistRepository.findByUserIdAndPhoneId(user.getId(), phone.getId()).isPresent()) {
            throw new ConflictException("Phone is already in your wishlist");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setPhone(phone);

        try {
            wishlistRepository.saveAndFlush(wishlist);
        } catch (DataIntegrityViolationException ex) {
            // uk_wishlist_user_phone catches two simultaneous adds.
            throw new ConflictException("Phone is already in your wishlist");
        }

        return toDtos(user);
    }

    public List<WishlistItemResponseDto> remove(String username, UUID phoneId) {
        User user = getUser(username);

        Wishlist wishlist = wishlistRepository.findByUserIdAndPhoneId(user.getId(), phoneId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Phone is not in your wishlist"));

        wishlistRepository.delete(wishlist);

        return toDtos(user);
    }

    private List<WishlistItemResponseDto> toDtos(User user) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    private WishlistItemResponseDto toDto(Wishlist wishlist) {
        Phone phone = wishlist.getPhone();

        return new WishlistItemResponseDto(
                wishlist.getId(),
                phone.getId(),
                phone.getName(),
                phone.getModel(),
                phone.getBrand() != null ? phone.getBrand().getName() : null,
                cheapestPrice(phone),
                primaryImage(phone),
                wishlist.getCreatedAt()
        );
    }

    /** The cheapest sellable variant, or null when nothing is in stock. */
    private BigDecimal cheapestPrice(Phone phone) {
        return phone.getVariants().stream()
                .filter(PhoneVariant::isActive)
                .map(PhoneVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private String primaryImage(Phone phone) {
        return phone.getImages().stream()
                .filter(PhoneImage::isPrimary)
                .map(PhoneImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> phone.getImages().stream()
                        .map(PhoneImage::getImageUrl)
                        .findFirst()
                        .orElse(null));
    }

    private User getUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
