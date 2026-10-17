package com.example.demo.catalog.dao;

import com.example.demo.catalog.entity.PhoneImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhoneImageRepository extends JpaRepository<PhoneImage, UUID> {

    List<PhoneImage> findByPhoneId(UUID phoneId);

    /*
     * Spring Data resolves this from the Java property name, so it must match the
     * entity field ("primary" in PhoneImage, mapped to the is_primary column).
     * The JSON name exposed by the API is "isPrimary" and does not matter here.
     */
    List<PhoneImage> findByPhoneIdAndPrimaryTrue(UUID phoneId);

}
