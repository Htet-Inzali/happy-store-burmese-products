package com.htet.happystore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final Cloudinary cloudinary;

    private static final String FOLDER = "happystore/products";
    private final Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "webp");
    private final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * ပုံကို Cloudinary သို့ upload လုပ်ပြီး secure URL (https://res.cloudinary.com/...) ကို ပြန်ပေးသည်။
     * Render restart ဖြစ်လျှင်လည်း ပုံများ မပျောက်တော့ပါ (Cloudinary သည် permanent storage ဖြစ်သည်)။
     */
    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IOException("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new IOException("File size must be less than 10MB");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) throw new IOException("Invalid file name");

        String extension = getFileExtension(originalFilename);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IOException("Only JPG, PNG, WEBP images are allowed");
        }

        return uploadBytes(file.getBytes());
    }

    /**
     * Excel ထဲမှ ထုတ်ယူသော embed ပုံ (byte array) ကို တိုက်ရိုက် Cloudinary သို့ upload လုပ်သည်။
     */
    public String saveImageBytes(byte[] data) throws IOException {
        if (data == null || data.length == 0) throw new IOException("Image data is empty");
        if (data.length > MAX_FILE_SIZE) throw new IOException("Image size must be less than 10MB");
        return uploadBytes(data);
    }

    private String uploadBytes(byte[] data) throws IOException {
        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", FOLDER,
                "public_id", publicId,
                "resource_type", "image",
                "overwrite", true,
                "transformation", new com.cloudinary.Transformation()
                        .quality("auto")
                        .fetchFormat("auto")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(data, options);
        String secureUrl = (String) result.get("secure_url");
        if (secureUrl == null) {
            secureUrl = (String) result.get("url");
        }
        log.info("Image uploaded to Cloudinary: {}", secureUrl);
        return secureUrl;
    }

    /**
     * Cloudinary URL မှ public_id ကို ထုတ်ယူ၍ ပုံကို ဖျက်သည်။ ဖျက်၍ မရလျှင်လည်း error မတက်စေဘဲ log သာ ထုတ်သည်။
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        // Cloudinary URL မဟုတ်ပါက (ဥပမာ ဟောင်းသော /uploads/... path) ကျော်သွားသည်
        if (!imageUrl.contains("res.cloudinary.com")) {
            return;
        }

        try {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Image deleted from Cloudinary: {}", publicId);
            }
        } catch (Exception e) {
            // ပုံဖျက်ခြင်း မအောင်မြင်လျှင်လည်း ပစ္စည်းဖျက်ခြင်းကို ဆက်လုပ်နိုင်စေရန် error မ throw ပါ
            log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
        }
    }

    /**
     * https://res.cloudinary.com/<cloud>/image/upload/v123/happystore/products/<id>.jpg
     * မှ "happystore/products/<id>" (extension မပါ) ကို ထုတ်ယူသည်။
     */
    private String extractPublicId(String url) {
        int uploadIdx = url.indexOf("/upload/");
        if (uploadIdx == -1) return null;

        String path = url.substring(uploadIdx + "/upload/".length());
        // version segment (v123456/) ကို ဖယ်ရှားသည်
        if (path.matches("^v\\d+/.*")) {
            path = path.substring(path.indexOf('/') + 1);
        }
        // extension ကို ဖယ်ရှားသည်
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx != -1) {
            path = path.substring(0, dotIdx);
        }
        return path;
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) return "";
        return filename.substring(dotIndex + 1);
    }
}
