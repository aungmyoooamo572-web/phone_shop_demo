package com.example.demo.catalog.entity;

import com.example.demo.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "phone_images")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhoneImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    /*
     * The Java field is named "primary" (not "isPrimary") on purpose.
     * Lombok still generates isPrimary()/setPrimary() because the field type is
     * boolean, so every existing caller keeps compiling, but Jackson now sees the
     * field, the getter and the setter under the SAME logical property name
     * ("primary") and merges them into one property. @JsonProperty then renames
     * that single property to "isPrimary", which is what the React client reads.
     *
     * Naming the field "isPrimary" would instead make Jackson emit two properties
     * ("primary" from the getter plus "isPrimary" from the annotated field).
     * The database column mapping is unchanged (is_primary).
     */
    @Column(name = "is_primary", nullable = false)
    @JsonProperty("isPrimary")
    private boolean primary = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phone_id", nullable = false)
    @JsonIgnore
    private Phone phone;
}
