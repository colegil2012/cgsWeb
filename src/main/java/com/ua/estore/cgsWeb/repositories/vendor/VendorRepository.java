package com.ua.estore.cgsWeb.repositories.vendor;

import com.ua.estore.cgsWeb.models.vendor.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends MongoRepository <Vendor, String> {

    Page<Vendor> findAll(Pageable pageable);
    Optional<Vendor> findBySlug(String slug);
    boolean existsBySlug(String slug);

}
