package com.example.demo.catalog.controller;

import com.example.demo.catalog.dao.BrandRepository;
import com.example.demo.catalog.dao.CategoryRepository;
import com.example.demo.catalog.dto.PhoneDto;
import com.example.demo.catalog.entity.Phone;
import com.example.demo.catalog.entity.PhoneImage;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.catalog.service.PhoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/phones")
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneService phoneService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

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

    @PostMapping
    public ResponseEntity<Phone> create(@RequestBody PhoneDto dto) {
        return ResponseEntity.ok(phoneService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Phone> update(@PathVariable UUID id, @RequestBody PhoneDto dto) {
        return ResponseEntity.ok(phoneService.update(id, dto));
    }

    @PostMapping("/{phoneId}/variants")
    public ResponseEntity<Phone> addVariant(@PathVariable UUID phoneId,
                                             @RequestBody PhoneVariant variant) {
        Phone phone = phoneService.findById(phoneId);
        variant.setPhone(phone);
        phone.getVariants().add(variant);
        return ResponseEntity.ok(phoneService.save(phone));
    }

    @PutMapping("/variants/{variantId}")
    public ResponseEntity<PhoneVariant> updateVariant(@PathVariable UUID variantId,
                                                      @RequestBody PhoneVariant variant) {
        return ResponseEntity.ok(phoneService.updateVariant(variantId, variant));
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(@PathVariable UUID variantId) {
        phoneService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{phoneId}/images")
    public ResponseEntity<Phone> addImage(@PathVariable UUID phoneId,
                                           @RequestBody PhoneImage image) {
        Phone phone = phoneService.findById(phoneId);
        image.setPhone(phone);
        phone.getImages().add(image);
        return ResponseEntity.ok(phoneService.save(phone));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        phoneService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
