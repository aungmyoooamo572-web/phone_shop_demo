package com.example.demo.catalog.dao;

import com.example.demo.catalog.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    boolean existsByName(String name);

}
