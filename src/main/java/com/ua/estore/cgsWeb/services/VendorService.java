package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.repositories.VendorRepository;
import com.ua.estore.cgsWeb.util.dataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public Page<Vendor> getAllVendors(int page) {
        Pageable pageable = PageRequest.of(page, 3);
        return vendorRepository.findAll(pageable);
    }

    public Optional<Vendor> getVendorById(String id) {
        try {
            String cleanId = dataUtil.parseToObjectId(id).toHexString();
            return vendorRepository.findById(cleanId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Map<String, String> getVendorNameMap() {
        return vendorRepository.findAll().stream()
                .collect(Collectors.toMap(Vendor::getId, Vendor::getName));
    }
}
