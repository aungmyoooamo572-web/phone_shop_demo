package com.example.demo.catalog.service;

import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.entity.Phone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneService {

    private final PhoneRepository phoneRepository;

    public List<Phone> findAll() {
        return phoneRepository.findAll();
    }

    public Phone findById(UUID id) {
        return phoneRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Phone not found"));
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

    public void softDelete(UUID id) {
        Phone phone = findById(id);
        phone.setDeleted(true);
        phoneRepository.save(phone);
    }

}
