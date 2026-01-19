package com.ua.estore.cgsWeb.repositories;

import com.ua.estore.cgsWeb.models.Vendor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends MongoRepository <Vendor, String> {
}
