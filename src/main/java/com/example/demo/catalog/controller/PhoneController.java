package com.example.demo.catalog.controller;

import com.example.demo.catalog.entity.Phone;
import com.example.demo.catalog.service.PhoneService;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/phones")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneService phoneService;

    @GetMapping
    public List<Phone> findAll() {
        return phoneService.findAllActive();
    }

    @GetMapping("/{id}")
    public Phone findById(@PathVariable UUID id) {
        return phoneService.findById(id);
    }

    @GetMapping("/brand/{brandId}")
    public List<Phone> findByBrandId(@PathVariable UUID brandId) {
        return phoneService.findByBrandId(brandId);
    }

    @GetMapping("/category/{categoryId}")
    public List<Phone> findByCategoryId(@PathVariable UUID categoryId) {
        return phoneService.findByCategoryId(categoryId);
    }

    @GetMapping("/search")
    public List<Phone> searchByName(
            @RequestParam String name
    ) {
        return phoneService.searchByName(name);
    }

}
