package com.example.demo.catalog.entity;

import com.example.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "phone_variants")
@Getter
@Setter
@NoArgsConstructor
public class PhoneVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String ram;

    @Column(nullable = false, length = 20)
    private String storage;

    @Column(nullable = false, length = 50)
    private String color;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 500, name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, name = "is_deleted")
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id", nullable = false)
    private Phone phone;

}
