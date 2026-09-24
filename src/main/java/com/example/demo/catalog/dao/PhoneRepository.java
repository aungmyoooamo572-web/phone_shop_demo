package com.example.demo.catalog.dao;

import com.example.demo.catalog.entity.Phone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhoneRepository extends JpaRepository<Phone, UUID> {

    List<Phone> findByBrandId(UUID brandId);

    List<Phone> findByCategoryId(UUID categoryId);

    List<Phone> findByNameContainingIgnoreCase(String name);

    List<Phone> findByIsDeletedFalse();

}
