package com.example.demo.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One wishlist entry with just enough product information to render the card.
 * No other customer's data is exposed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponseDto {

    private UUID id;
    private UUID phoneId;
    private String name;
    private String model;
    private String brandName;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime addedAt;

}
