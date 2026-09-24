package com.example.demo.cart.service;

import com.example.demo.cart.dao.CartItemRepository;
import com.example.demo.cart.dao.CartRepository;
import com.example.demo.cart.dto.AddCartItemDto;
import com.example.demo.cart.dto.CartItemResponseDto;
import com.example.demo.cart.dto.CartResponseDto;
import com.example.demo.cart.dto.UpdateCartItemDto;
import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.catalog.dao.PhoneVariantRepository;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PhoneVariantRepository phoneVariantRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public CartResponseDto getCart(String username) {
        User user = getUser(username);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createCart(user));

        return toDto(cart);
    }

    public CartResponseDto addToCart(String username, AddCartItemDto dto) {
        User user = getUser(username);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createCart(user));

        PhoneVariant variant = phoneVariantRepository.findById(dto.getPhoneVariantId())
                .orElseThrow(() -> new RuntimeException("Phone variant not found"));

        if (!variant.isActive()) {
            throw new RuntimeException("Phone variant is not active");
        }

        if (variant.isDeleted()) {
            throw new RuntimeException("Phone variant is no longer available");
        }

        if (variant.getStock() <= 0) {
            throw new RuntimeException("Product is out of stock");
        }

        if (cartItemRepository.existsByCartIdAndPhoneVariantId(cart.getId(), variant.getId())) {
            throw new RuntimeException("Product is already in cart");
        }

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setPhoneVariant(variant);
        cartItem.setQuantity(1);

        cart.getItems().add(cartItem);
        cartRepository.save(cart);

        return toDto(cart);
    }

    public CartResponseDto updateQuantity(String username, UUID itemId, UpdateCartItemDto dto) {
        User user = getUser(username);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("This cart item does not belong to you");
        }

        PhoneVariant variant = cartItem.getPhoneVariant();

        if (!variant.isActive() || variant.isDeleted()) {
            throw new RuntimeException("Product is no longer available");
        }

        if (dto.getQuantity() > variant.getStock()) {
            throw new RuntimeException("Quantity exceeds available stock");
        }

        cartItem.setQuantity(dto.getQuantity());
        cartItemRepository.save(cartItem);

        return toDto(cart);
    }

    public CartResponseDto removeItem(String username, UUID itemId) {
        User user = getUser(username);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("This cart item does not belong to you");
        }

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        return toDto(cart);
    }

    public void clearCart(String username) {
        User user = getUser(username);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private User getUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public CartResponseDto toDto(Cart cart) {
        List<CartItemResponseDto> items = cart.getItems()
                .stream()
                .map(this::toCartItemDto)
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponseDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDto(
                cart.getId(),
                cart.getUser().getId(),
                items,
                total
        );
    }

    private CartItemResponseDto toCartItemDto(CartItem item) {
        PhoneVariant variant = item.getPhoneVariant();
        BigDecimal subtotal = variant.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponseDto(
                item.getId(),
                variant.getId(),
                variant.getPhone().getName(),
                variant.getRam(),
                variant.getStorage(),
                variant.getColor(),
                variant.getPrice(),
                item.getQuantity(),
                subtotal
        );
    }

}
