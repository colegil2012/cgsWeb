package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.wrappers.ProductFormWrapper;
import com.ua.estore.cgsWeb.repositories.ProductRepository;
import com.ua.estore.cgsWeb.util.dataUtil;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;


    /********************************************
     * Save Methods
     *******************************************/

    public String saveProduct(Product product, String vendorId) {
        ObjectId cleanVendorId = dataUtil.parseToObjectId(vendorId);
        product.setVendorId(cleanVendorId.toHexString());

        return productRepository.save(product).getId();
    }

    /********************************************
     * Get Methods
     *******************************************/

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    public Page<Product> getProductsByFilter(String search, String category, String vendor, boolean lowStock, int page) {
        String searchQuery = (search == null) ? "" : search;
        String categoryQuery = (category == null) ? "" : category;
        Object vendorQuery = (vendor == null || vendor.isEmpty()) ? "" : dataUtil.parseToObjectId(vendor);

        // Create page request (page is 0-indexed in Spring Data)
        var pageable = PageRequest.of(page, 15);

        return productRepository.findByFilter(categoryQuery, searchQuery, vendorQuery, lowStock, pageable);
    }

}
