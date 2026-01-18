package com.ua.estore.cgsWeb.util;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.Vendor;

import java.util.List;

public class searchUtil {

     public static List<String> getCategories(List<Product> products) {
        return products.stream()
                .map(Product::getCategory)
                .distinct()
                .toList();
    }
}
