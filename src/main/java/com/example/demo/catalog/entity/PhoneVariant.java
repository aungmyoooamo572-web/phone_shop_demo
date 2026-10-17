package com.example.demo.catalog.entity;

import com.example.demo.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    /*
     * The Java fields are named "active"/"deleted" (not "isActive"/"isDeleted")
     * on purpose, exactly like PhoneImage.primary:
     *
     * - Lombok still generates isActive()/setActive() and isDeleted()/setDeleted()
     *   for boolean fields, so every existing caller keeps compiling.
     * - Jackson merges the field, the getter and the setter into ONE logical
     *   property (the getter's implicit name is "active"/"deleted"), and
     *   @JsonProperty then renames that single property to "isActive"/"isDeleted",
     *   which is what the React client reads.
     *
     * Naming the fields "isActive"/"isDeleted" made Jackson emit two properties
     * per flag: "active" (getter) plus "isActive" (annotated field). The database
     * columns are unchanged (is_active / is_deleted).
     */
    @Column(nullable = false, name = "is_active")
    @JsonProperty("isActive")
    private boolean active = true;

    @Column(nullable = false, name = "is_deleted")
    @JsonProperty("isDeleted")
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id", nullable = false)
    @JsonIgnore
    private Phone phone;

}
