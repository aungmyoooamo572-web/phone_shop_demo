package com.example.demo.catalog.service;

import com.example.demo.catalog.dao.BrandRepository;
import com.example.demo.catalog.dao.CategoryRepository;
import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.dao.PhoneVariantRepository;
import com.example.demo.catalog.dto.PhoneDto;
import com.example.demo.catalog.entity.Phone;
import com.example.demo.catalog.entity.PhoneImage;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneRepository phoneRepository;
    private final PhoneVariantRepository phoneVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public List<Phone> findAll() {
        return phoneRepository.findAll();
    }

    public Phone findById(UUID id) {
        return phoneRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Phone not found"));
    }

    public List<Phone> findByBrandId(UUID brandId) {
        return phoneRepository.findByBrandId(brandId);
    }

    public List<Phone> findByCategoryId(UUID categoryId) {
        return phoneRepository.findByCategoryId(categoryId);
    }

    public List<Phone> searchByName(String name) {
        return phoneRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Phone> findAllActive() {
        return phoneRepository.findByIsDeletedFalse();
    }

    public Phone save(Phone phone) {
        return phoneRepository.save(phone);
    }

    public Phone create(PhoneDto dto) {
        Phone phone = new Phone();
        phone.setName(dto.getName());
        phone.setModel(dto.getModel());
        phone.setDescription(dto.getDescription());
        phone.setProcessor(dto.getProcessor());
        phone.setScreenSize(dto.getScreenSize());
        phone.setBattery(dto.getBattery());
        phone.setCamera(dto.getCamera());
        phone.setOperatingSystem(dto.getOperatingSystem());
        phone.setReleaseDate(dto.getReleaseDate());
        if (dto.getBrandId() != null) {
            phone.setBrand(brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found")));
        }
        if (dto.getCategoryId() != null) {
            phone.setCategory(categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
        }
        if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
            PhoneImage img = new PhoneImage();
            img.setImageUrl(dto.getImageUrl().trim());
            img.setPrimary(true);
            img.setPhone(phone);
            phone.getImages().add(img);
        }
        phone.setDeleted(false);
        return phoneRepository.save(phone);
    }

    public Phone update(UUID id, PhoneDto dto) {
        Phone phone = findById(id);
        if (dto.getName() != null) phone.setName(dto.getName());
        if (dto.getModel() != null) phone.setModel(dto.getModel());
        if (dto.getDescription() != null) phone.setDescription(dto.getDescription());
        if (dto.getProcessor() != null) phone.setProcessor(dto.getProcessor());
        if (dto.getScreenSize() != null) phone.setScreenSize(dto.getScreenSize());
        if (dto.getBattery() != null) phone.setBattery(dto.getBattery());
        if (dto.getCamera() != null) phone.setCamera(dto.getCamera());
        if (dto.getOperatingSystem() != null) phone.setOperatingSystem(dto.getOperatingSystem());
        if (dto.getReleaseDate() != null) phone.setReleaseDate(dto.getReleaseDate());
        if (dto.getBrandId() != null) {
            phone.setBrand(brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found")));
        }
        if (dto.getCategoryId() != null) {
            phone.setCategory(categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found")));
        }
        if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
            String newImg = dto.getImageUrl().trim();
            PhoneImage primary = phone.getImages().stream()
                    .filter(PhoneImage::isPrimary)
                    .findFirst()
                    .orElse(null);
            if (primary != null) {
                primary.setImageUrl(newImg);
            } else {
                PhoneImage img = new PhoneImage();
                img.setImageUrl(newImg);
                img.setPrimary(true);
                img.setPhone(phone);
                phone.getImages().add(img);
            }
        }
        return phoneRepository.save(phone);
    }

    public PhoneVariant updateVariant(UUID variantId, PhoneVariant updated) {
        PhoneVariant variant = phoneVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (updated.getRam() != null) variant.setRam(updated.getRam());
        if (updated.getStorage() != null) variant.setStorage(updated.getStorage());
        if (updated.getColor() != null) variant.setColor(updated.getColor());
        if (updated.getPrice() != null) variant.setPrice(updated.getPrice());
        if (updated.getStock() != null) variant.setStock(updated.getStock());
        if (updated.getSku() != null) variant.setSku(updated.getSku());
        if (updated.getImageUrl() != null) variant.setImageUrl(updated.getImageUrl());
        variant.setActive(updated.isActive());
        return phoneVariantRepository.save(variant);
    }

    public void deleteVariant(UUID variantId) {
        PhoneVariant variant = phoneVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        variant.setDeleted(true);
        variant.setActive(false);
        phoneVariantRepository.save(variant);
    }

    public void softDelete(UUID id) {
        Phone phone = findById(id);
        phone.setDeleted(true);
        phoneRepository.save(phone);
    }

}

