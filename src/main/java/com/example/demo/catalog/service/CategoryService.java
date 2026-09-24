package com.example.demo.catalog.service;


import com.example.demo.catalog.dao.CategoryRepository;
import com.example.demo.catalog.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));
    }

    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteById(UUID id) {
        categoryRepository.deleteById(id);
    }
}