package com.example.demo.catalog.service;

import com.example.demo.catalog.dao.BrandRepository;
import com.example.demo.catalog.entity.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> findAll() {
        return brandRepository.findAll();
    }

    public Brand findById(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Brand not found"));
    }

    public boolean existsByName(String name) {
        return brandRepository.existsByName(name);
    }

    public Brand save(Brand brand) {
        return brandRepository.save(brand);
    }

    public void deleteById(UUID id) {
        brandRepository.deleteById(id);
    }

}
