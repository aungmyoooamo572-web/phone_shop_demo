package com.example.demo.wishlist.entity;

import com.example.demo.common.entity.BaseEntity;
import com.example.demo.catalog.entity.Phone;
import com.example.demo.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "wishlists",
        uniqueConstraints = {
                @UniqueConstraint(

                        name = "uk_wishlist_user_phone",
                        columnNames = {"user_id", "phone_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Wishlist extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id", nullable = false)
    private Phone phone;
}
