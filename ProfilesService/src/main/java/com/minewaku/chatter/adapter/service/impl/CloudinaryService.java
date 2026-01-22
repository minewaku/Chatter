package com.minewaku.chatter.adapter.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.minewaku.chatter.domain.port.out.service.FileStorage;
import com.minewaku.chatter.domain.response.FileStorageResponse;
import com.minewaku.chatter.domain.value.file.StorableFile;
import com.minewaku.chatter.domain.value.id.StorageKey;

@Service
public class CloudinaryService implements FileStorage {
    
    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public FileStorageResponse upload(StorableFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            StorageKey key = file.getStorageKey();
            String path = file.getPath();

            Map<String, Object> options = Map.of(
                "folder", path,
                "public_id", key.getValue(),
                "resource_type", "auto"
            );

            final Map result = cloudinary.uploader().upload(
                inputStream, 
                options);

            final String secureUrl = (String) result.get("secure_url");
            final String publicId = (String) result.get("public_id");
            final String format = (String) result.get("format");
            final long sizeInBytes = ((Number) result.get("bytes")).longValue();

            FileStorageResponse response = new FileStorageResponse(
                new StorageKey(publicId),
                URI.create(secureUrl),
                format,
                sizeInBytes
            );
            
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    @Override
    public void deleteByKey(StorageKey key) {
        try {
            Map<String, Object> options = Map.of(
                "resource_type", "auto"
            );
            cloudinary.uploader().destroy(key.getValue(), options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from Cloudinary", e);
        }
    }
}
