package com.htet.happystore.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path productUploadPath;
    private final Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "webp");
    private final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 5MB

    @PostConstruct
    public void init() throws IOException {
        productUploadPath = Paths.get(uploadDir, "products").toAbsolutePath().normalize();
        Files.createDirectories(productUploadPath);
    }

    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IOException("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new IOException("File size must be less than 5MB");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) throw new IOException("Invalid file name");

        originalFilename = StringUtils.cleanPath(originalFilename);
        if (originalFilename.contains("..")) throw new IOException("Invalid file name: path traversal detected");

        String extension = getFileExtension(originalFilename);
        if (!allowedExtensions.contains(extension.toLowerCase())) throw new IOException("Only JPG, PNG, WEBP images are allowed");

        String fileName = UUID.randomUUID() + "." + extension;
        Path filePath = productUploadPath.resolve(fileName).normalize();

        if (!filePath.startsWith(productUploadPath)) throw new IOException("Invalid file path");

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/products/" + fileName;
    }

    public void deleteImage(String imagePath) throws IOException {
        if (imagePath == null || imagePath.isBlank()) return;

        String relativePath = imagePath.replace("/uploads/", "");
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(relativePath).normalize();

        if (!filePath.startsWith(basePath)) throw new IOException("Invalid file path: path traversal detected");

        Files.deleteIfExists(filePath);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) return "";
        return filename.substring(dotIndex + 1);
    }
}