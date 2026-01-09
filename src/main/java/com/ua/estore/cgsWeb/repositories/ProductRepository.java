package com.ua.estore.cgsWeb.repositories;

import com.ua.estore.cgsWeb.models.Product;
import org.jspecify.annotations.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    @NonNull List<Product> findAll();

    List<Product> findByCategory(String category);

    List<Product> findByStock(Integer stock);
}
