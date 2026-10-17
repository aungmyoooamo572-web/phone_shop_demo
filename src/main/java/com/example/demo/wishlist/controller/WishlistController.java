package com.example.demo.wishlist.controller;

import com.example.demo.wishlist.dto.WishlistItemResponseDto;
import com.example.demo.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public List<WishlistItemResponseDto> getWishlist(Authentication authentication) {
        return wishlistService.getWishlist(authentication.getName());
    }

    @PostMapping("/{phoneId}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<WishlistItemResponseDto> add(
            Authentication authentication,
            @PathVariable UUID phoneId
    ) {
        return wishlistService.add(authentication.getName(), phoneId);
    }

    @DeleteMapping("/{phoneId}")
    public List<WishlistItemResponseDto> remove(
            Authentication authentication,
            @PathVariable UUID phoneId
    ) {
        return wishlistService.remove(authentication.getName(), phoneId);
    }
}
