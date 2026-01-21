package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.Category;
import com.ua.estore.cgsWeb.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(String id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug).orElse(null);
    }

    /**
     * Creates a Map of ID -> Name for efficient lookups during DTO conversion.
     * This prevents multiple database calls when processing lists of products.
     */
    public Map<String, String> getCategoryNameMap() {
        return getAllCategories().stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        Category::getName,
                        (existing, replacement) -> existing // Handle duplicates if necessary
                ));
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }
}
