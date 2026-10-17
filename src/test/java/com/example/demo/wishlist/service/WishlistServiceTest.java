package com.example.demo.wishlist.service;

import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.entity.Brand;
import com.example.demo.catalog.entity.Phone;
import com.example.demo.catalog.entity.PhoneImage;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import com.example.demo.wishlist.dao.WishlistRepository;
import com.example.demo.wishlist.dto.WishlistItemResponseDto;
import com.example.demo.wishlist.entity.Wishlist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The wishlist is always scoped to the signed-in customer and never stores a
 * phone twice.
 */
@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private WishlistService wishlistService;

    private UUID phoneId;

    private User customer;

    private Phone phone;

    @BeforeEach
    void setUp() {
        phoneId = UUID.randomUUID();

        customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setUsername("customer-one");
        customer.setRole(Role.CUSTOMER);

        Brand brand = new Brand();
        brand.setName("Samsung");

        PhoneVariant variant = new PhoneVariant();
        variant.setPrice(new BigDecimal("1500000"));
        variant.setActive(true);

        PhoneImage image = new PhoneImage();
        image.setImageUrl("https://cdn.example/galaxy.png");
        image.setPrimary(true);

        phone = new Phone();
        phone.setId(phoneId);
        phone.setName("Galaxy S25 Ultra");
        phone.setModel("SM-S938");
        phone.setBrand(brand);
        phone.getVariants().add(variant);
        phone.getImages().add(image);
    }

    private Wishlist storedItem() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(UUID.randomUUID());
        wishlist.setUser(customer);
        wishlist.setPhone(phone);
        return wishlist;
    }

    @Test
    @DisplayName("The wishlist returns the customer's own entries with product details")
    void wishlistReturnsOwnEntries() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(customer.getId()))
                .thenReturn(List.of(storedItem()));

        List<WishlistItemResponseDto> items = wishlistService.getWishlist("customer-one");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getPhoneId()).isEqualTo(phoneId);
        assertThat(items.get(0).getName()).isEqualTo("Galaxy S25 Ultra");
        assertThat(items.get(0).getBrandName()).isEqualTo("Samsung");
        assertThat(items.get(0).getPrice()).isEqualByComparingTo("1500000");
        assertThat(items.get(0).getImageUrl()).isEqualTo("https://cdn.example/galaxy.png");
    }

    @Test
    @DisplayName("Adding a phone stores it against the signed-in customer")
    void addStoresAgainstTheCaller() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.of(phone));
        when(wishlistRepository.findByUserIdAndPhoneId(customer.getId(), phoneId))
                .thenReturn(Optional.empty());
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(customer.getId()))
                .thenReturn(List.of(storedItem()));

        List<WishlistItemResponseDto> items = wishlistService.add("customer-one", phoneId);

        assertThat(items).hasSize(1);
        verify(wishlistRepository).saveAndFlush(any(Wishlist.class));
    }

    @Test
    @DisplayName("Adding the same phone twice is a 409 (duplicate prevention)")
    void duplicateAddIsConflict() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.of(phone));
        when(wishlistRepository.findByUserIdAndPhoneId(customer.getId(), phoneId))
                .thenReturn(Optional.of(storedItem()));

        assertThatThrownBy(() -> wishlistService.add("customer-one", phoneId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Phone is already in your wishlist");

        verify(wishlistRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Adding an unknown phone is a 404")
    void addingUnknownPhoneIsNotFound() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.add("customer-one", phoneId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Phone not found");
    }

    @Test
    @DisplayName("Adding a soft deleted phone is a 404")
    void addingDeletedPhoneIsNotFound() {
        phone.setDeleted(true);
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(phoneRepository.findById(phoneId)).thenReturn(Optional.of(phone));

        assertThatThrownBy(() -> wishlistService.add("customer-one", phoneId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Phone not found");
    }

    @Test
    @DisplayName("Removing a phone that is not in the caller's list is a 404")
    void removingMissingEntryIsNotFound() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(wishlistRepository.findByUserIdAndPhoneId(customer.getId(), phoneId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.remove("customer-one", phoneId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Phone is not in your wishlist");
    }

    @Test
    @DisplayName("Removing the caller's own entry deletes exactly that row")
    void removingOwnEntryDeletesIt() {
        Wishlist item = storedItem();

        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(wishlistRepository.findByUserIdAndPhoneId(customer.getId(), phoneId))
                .thenReturn(Optional.of(item));
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(customer.getId()))
                .thenReturn(List.of());

        assertThat(wishlistService.remove("customer-one", phoneId)).isEmpty();

        verify(wishlistRepository).delete(item);
    }
}
