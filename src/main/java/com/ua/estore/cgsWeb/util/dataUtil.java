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


    /**
     * Parses a given string into an {@link ObjectId} instance. The method removes any wrapper text
     * around the ObjectId string if present (e.g., "ObjectId('...')"), and validates the string
     * as a valid 24-character hexadecimal value before creating the {@link ObjectId}.
     *
     * @param idString the string to be parsed into an {@link ObjectId}. It must be a valid
     *                 24-character hexadecimal string, optionally wrapped as "ObjectId('...')".
     * @return the parsed {@link ObjectId} instance.
     * @throws IllegalArgumentException if the input string is null, empty, or not a valid ObjectId.
     */

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

    /**
     * Converts a list of {@link Product} objects into a list of {@link ProductDTO} objects.
     * The method maps each product to its corresponding DTO representation, including
     * resolving the vendor name from a vendor ID using the provided {@link VendorService}.
     *
     * @param products the list of {@link Product} objects to be converted
     * @param vendorService an instance of {@link VendorService} used to retrieve vendor information
     * @return a list of {@link ProductDTO} objects containing the mapped data
     */

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
                    p.getPrice(), p.getCategory(), p.getImageUrl(),
                    vName, p.getStock(), 0
            );
        }).toList();
    }

}
