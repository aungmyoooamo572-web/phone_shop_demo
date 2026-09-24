package com.example.demo.catalog.dao;

import com.example.demo.catalog.entity.PhoneImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhoneImageRepository extends JpaRepository<PhoneImage, UUID> {

    List<PhoneImage> findByPhoneId(UUID phoneId);

    List<PhoneImage> findByPhoneIdAndIsPrimaryTrue(UUID phoneId);

}
