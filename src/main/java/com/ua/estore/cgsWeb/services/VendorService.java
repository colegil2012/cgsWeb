package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.repositories.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public Optional<Vendor> getVendorById(ObjectId id) {
        return vendorRepository.findById(id);
    }

    public Map<String, String> getVendorNameMap() {
        return vendorRepository.findAll().stream()
                .collect(Collectors.toMap(Vendor::getId, Vendor::getName));
    }
}
