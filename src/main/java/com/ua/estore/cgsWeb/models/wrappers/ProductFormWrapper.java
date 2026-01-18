package com.ua.estore.cgsWeb.models.wrappers;

import com.ua.estore.cgsWeb.models.Product;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ProductFormWrapper {
    private List<Product> products;
    private List<MultipartFile> productImages;
}
