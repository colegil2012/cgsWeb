package com.ua.estore.cgsWeb.repositories.shop;

import com.ua.estore.cgsWeb.models.shop.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findAll();
    List<Product> findByCategoryId(String categoryId);
    Optional<Product> findBySlug(String slug);

    /* ========================================================================
     * STOREFRONT-FACING QUERIES
     *
     * These two now also require stock > 0. Round 1B decision: `active` stays
     * a manual on/off switch; stock is tracked separately; the storefront
     * hides anything out of stock REGARDLESS of the active flag. An admin can
     * still see 0-stock products via the admin product views (which use
     * findByVendorId / findByFilter, NOT these).
     *
     * If you have other storefront product queries elsewhere, they need the
     * same `stock > 0` treatment — see ROUND-1B-NOTES.md.
     * ====================================================================== */

    @Query("{ 'vendorId': { $in: ?0 }, '_id': { $nin: ?1 }, 'active': true, 'stock': { $gt: 0 } }")
    List<Product> findByVendorIdInAndIdNotInAndActiveTrue(
            Collection<String> vendorIds,
            Collection<String> excludeIds,
            Pageable pageable
    );

    @Query(value = "{ '_id': { $nin: ?0 }, 'active': true, 'stock': { $gt: 0 } }",
            sort = "{ 'createdAt': -1 }")
    List<Product> findByIdNotInAndActiveTrueOrderByCreatedAtDesc(
            Collection<String> excludeIds,
            Pageable pageable
    );

    /* ========================================================================
     * ADMIN / SHARED QUERIES — unchanged. Admin views must see 0-stock
     * products so the owner can restock them, so NO stock filter here.
     * ====================================================================== */

    @Query("{ 'vendorId': ?0 }")
    List<Product> findByVendorId(String vendorId);

    @Query("{ 'vendorId': ?0 }")
    Page<Product> findByVendorId(String vendorId, Pageable pageable);

    /** Count a vendor's products — used on the vendor detail page. */
    long countByVendorId(String vendorId);

    @Query("{ $and: [ " +
            "{ $or: [ { 'categoryId': ?0 }, { $expr: { $eq: ['?0', ''] } } ] }, " +
            "{ $or: [ { 'name': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }, " +
            "{ $or: [ { 'vendorId': ?2 }, { $expr: { $eq: ['?2', ''] } } ] }, " +
            "{ $or: [ { $expr: { $eq: [ { $literal: ?3 }, false ] } }, { $expr: { $lte: ['$stock', '$lowStockThreshold'] } } ] }" +
            "] }")
    Page<Product> findByFilter(Object categoryId, String search, Object vendorId, boolean lowStock, Pageable pageable);

}

