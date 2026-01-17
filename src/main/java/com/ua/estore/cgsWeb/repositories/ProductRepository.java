package com.ua.estore.cgsWeb.repositories;

import com.ua.estore.cgsWeb.models.Product;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findAll();

    @Query("{ $and: [ " +
            "{ $or: [ { 'category': ?0 }, { $expr: { $eq: ['?0', ''] } } ] }, " +
            "{ $or: [ { 'name': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }, " +
            "{ $or: [ { $expr: { $eq: [ { $literal: ?2 }, false ] } }, { 'stock': { $lte: 25 } } ] }" +
            "] }")
    List<Product> findByFilter(String category, String search, boolean lowStock);
}
