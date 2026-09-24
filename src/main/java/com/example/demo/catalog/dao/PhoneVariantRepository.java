package com.example.demo.catalog.dao;

import com.example.demo.catalog.entity.PhoneVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhoneVariantRepository extends JpaRepository<PhoneVariant, UUID> {

    Optional<PhoneVariant> findBySku(String sku);

    List<PhoneVariant> findByPhoneId(UUID phoneId);

    List<PhoneVariant> findByPhoneIdAndIsDeletedFalse(UUID phoneId);

}
