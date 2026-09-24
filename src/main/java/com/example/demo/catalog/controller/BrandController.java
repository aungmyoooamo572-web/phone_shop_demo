package com.example.demo.catalog.controller;

import com.example.demo.catalog.entity.Brand;
import com.example.demo.catalog.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public List<Brand> findAll() {
        return brandService.findAll();
    }

    @GetMapping("/{id}")
    public Brand findById(@PathVariable UUID id) {
        return brandService.findById(id);
    }

    @PostMapping
    public Brand save(@RequestBody Brand brand) {
        return brandService.save(brand);
    }

}
