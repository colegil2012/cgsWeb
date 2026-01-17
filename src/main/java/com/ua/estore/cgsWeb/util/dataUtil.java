package com.ua.estore.cgsWeb.util;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.services.VendorService;
import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

@UtilityClass
public class dataUtil {

    public ObjectId parseToObjectId(String idString) {
        if (idString == null || idString.isEmpty()) {
            throw new IllegalArgumentException("ID string cannot be null or empty");
        }

        // Strip the wrapper if it exists: "ObjectId('696a...')" -> "696a..."
        String hexCandidate = idString.contains("'") ?
                idString.split("'")[1] : idString;

        // The ObjectId library will validate if this is a valid 24-character hex string
        return new ObjectId(hexCandidate);
    }

    //Override method for mapping products to DTO with vendor service
    public List<ProductDTO> convertToProductDto(List<Product> products, VendorService vendorService) {
        // Fetch all vendors and create a Map for quick lookup: ID -> Name
        Map<String, String> vendorMap = vendorService.getVendorNameMap();

        return products.stream().map(p -> {
            // Clean the vendorId from the product using your utility
            String cleanVendorId = p.getVendorId() != null ?
                    dataUtil.parseToObjectId(p.getVendorId()).toHexString() : null;

            String vName = vendorMap.getOrDefault(cleanVendorId, "Unknown Vendor");

            return new ProductDTO(
                    p.getId(), p.getName(), p.getDescription(),
                    p.getPrice(), p.getCategory(), p.getImageUrl(), vName, 0
            );
        }).toList();
    }

}
