package com.ua.estore.cgsWeb.services.storage.impl;

import com.ua.estore.cgsWeb.services.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class SpacesImageStorageService implements ImageStorageService {

    private final S3Client s3Client;

    @Value("${app.images.bucket}")
    private String bucket;

    @Override
    public void storeProductImage(String fileName, MultipartFile file) {
        try {
            String key = "products/" + fileName;

            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ) // assumes you want public images
                    .build();

            s3Client.putObject(req, RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image to Spaces: " + e.getMessage(), e);
        }
    }
}
