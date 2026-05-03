package com.ua.estore.cgsWeb.repositories.shop;

import com.ua.estore.cgsWeb.models.shop.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findBySlug(String slug);
}
