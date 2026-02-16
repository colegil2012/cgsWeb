package com.ua.estore.cgsWeb.models.wrappers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PackageWrapper {
    private String vendorId;
    private String vendorName;
    private List<ProductDTO> items = new ArrayList<>();

    private double totalLength = 0;
    private double totalWidth = 0;
    private double totalHeight = 0;
    private double totalWeight = 0;

    public PackageWrapper(String vendorId, String vendorName) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;
    }

    public void addItem(ProductDTO dto, Product originalProduct) {
        items.add(dto);
        Product.ProductAttributes attr = originalProduct.getAttributes();

        if (attr != null) {
            int qty = dto.getQuantity();

            // Logic: Weight is always additive
            try {
                double unitWeight = Double.parseDouble(attr.getWeight().replaceAll("[^0-9.]", ""));
                this.totalWeight += (unitWeight * qty);
            } catch (Exception e) {
                this.totalWeight += qty; // Fallback to 1lb per item
            }

            // Logic: For simplicity, we stack items vertically.
            // Length and Width take the maximum dimension, Height is additive.
            this.totalLength = Math.max(this.totalLength, attr.getLength() != null ? attr.getLength() : 1.0);
            this.totalWidth = Math.max(this.totalWidth, attr.getWidth() != null ? attr.getWidth() : 1.0);
            this.totalHeight += (attr.getHeight() != null ? attr.getHeight() : 1.0) * qty;
        }
    }
}
