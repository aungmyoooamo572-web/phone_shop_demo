package com.example.demo.cart.controller;

import com.example.demo.cart.dto.AddCartItemDto;
import com.example.demo.cart.dto.CartResponseDto;
import com.example.demo.cart.dto.UpdateCartItemDto;
import com.example.demo.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponseDto getCart(Authentication authentication) {
        return cartService.getCart(authentication.getName());
    }

    @PostMapping("/items")
    public CartResponseDto addToCart(
            Authentication authentication,
            @Valid @RequestBody AddCartItemDto dto
    ) {
        return cartService.addToCart(authentication.getName(), dto);
    }

    @PutMapping("/items/{itemId}")
    public CartResponseDto updateQuantity(
            Authentication authentication,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemDto dto
    ) {
        return cartService.updateQuantity(authentication.getName(), itemId, dto);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponseDto removeItem(
            Authentication authentication,
            @PathVariable UUID itemId
    ) {
        return cartService.removeItem(authentication.getName(), itemId);
    }

    @DeleteMapping
    public void clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
    }

}
