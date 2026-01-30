package com.ua.estore.cgsWeb.services.storage.impl;

import com.ua.estore.cgsWeb.services.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Profile("!prod")
@RequiredArgsConstructor
public class LocalImageStorageService implements ImageStorageService {

    @Value("${app.upload.path}")
    private String uploadPath;

    public void storeProductImage(String fileName, MultipartFile file) {
        try {
            Path path = Paths.get(uploadPath, "products", fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store image locally: " + e.getMessage(), e);
        }
    }

    public void storeVendorLogo(String fileName, MultipartFile file) {
        try {
            Path path = Paths.get(uploadPath, "vendors", fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store image locally: " + e.getMessage(), e);
        }
    }
}
