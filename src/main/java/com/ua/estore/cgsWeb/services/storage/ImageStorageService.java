package com.ua.estore.cgsWeb.services.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    void storeProductImage(String fileName, MultipartFile file);
}
