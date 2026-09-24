package com.example.demo.wishlist.dao;

import com.example.demo.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    List<Wishlist> findByUserId(UUID userId);

    Optional<Wishlist> findByUserIdAndPhoneId(
            UUID userId,
            UUID phoneId
    );

}
