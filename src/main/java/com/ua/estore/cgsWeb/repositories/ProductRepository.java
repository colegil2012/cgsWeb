package com.ua.estore.cgsWeb.repositories;

import com.ua.estore.cgsWeb.models.Product;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findAll();

    List<Product> findByCategoryId(String categoryId);

    Optional<Product> findBySlug(String slug);

    @Query("{ $and: [ " +
            "{ $or: [ { 'categoryId': ?0 }, { $expr: { $eq: ['?0', ''] } } ] }, " +
            "{ $or: [ { 'name': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }, " +
            "{ $or: [ { 'vendorId': ?2 }, { $expr: { $eq: ['?2', ''] } } ] }, " +
            "{ $or: [ { $expr: { $eq: [ { $literal: ?3 }, false ] } }, { $expr: { $lte: ['$stock', '$lowStockThreshold'] } } ] }" +
            "] }")
    Page<Product> findByFilter(Object categoryId, String search, Object vendorId, boolean lowStock, Pageable pageable);

    @Query("{ 'vendorId': ?0 }")
    Page<Product> findByVendorId(String vendorId, Pageable pageable);

    Page<Product> getProductsByVendorId(ObjectId vendorId, Pageable pageable);

}

