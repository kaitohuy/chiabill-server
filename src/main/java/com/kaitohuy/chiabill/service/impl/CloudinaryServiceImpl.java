package com.kaitohuy.chiabill.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File cannot be empty");
        }

        try {
            // Đặt tên ngẫu nhiên cho file trên Cloudinary để tránh trùng lặp
            String publicId = UUID.randomUUID().toString();
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "chiabill_uploads"
                    ));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("IO Error uploading file to Cloudinary: ", e);
            throw new BusinessException("Lỗi đọc dữ liệu hình ảnh. Vui lòng thử lại.");
        } catch (Exception e) {
            log.error("Cloudinary Library Error: ", e);
            throw new BusinessException("Lỗi tải ảnh lên hệ thống lưu trữ. Vui lòng kiểm tra lại cấu hình Cloudinary.");
        }
    }

    @Override
    public String uploadImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.contains("cloudinary.com")) {
            return imageUrl;
        }

        try {
            byte[] imageBytes = downloadImageBytes(imageUrl);
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(imageBytes,
                    ObjectUtils.asMap(
                            "folder", "chiabill_uploads_places"
                    ));
            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            log.warn("Failed to download/upload image bytes for URL {}, falling back to direct URL upload: {}", imageUrl, e.getMessage());
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> uploadResult = cloudinary.uploader().upload(imageUrl,
                        ObjectUtils.asMap(
                                "folder", "chiabill_uploads_places"
                        ));
                return uploadResult.get("secure_url").toString();
            } catch (Exception ex) {
                log.error("Cloudinary direct URL upload fallback also failed for URL {}: ", imageUrl, ex);
                return imageUrl;
            }
        }
    }

    private byte[] downloadImageBytes(String imageUrl) throws Exception {
        // Create a trust manager that does not validate certificate chains
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
            new javax.net.ssl.X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        };

        javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                .sslContext(sslContext)
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(imageUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .build();
        java.net.http.HttpResponse<byte[]> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to download image, status code: " + response.statusCode());
        }
        return response.body();
    }

    @org.springframework.scheduling.annotation.Async
    @Override
    public void deleteImage(String secureUrl) {
        if (secureUrl == null || secureUrl.trim().isEmpty() || !secureUrl.contains("cloudinary.com")) {
            return;
        }

        try {
            // Lấy ra phần Tên thư mục và Tên file để xoá (VD: "chiabill_uploads/df12s-g234fd")
            String[] parts = secureUrl.split("/");
            String filename = parts[parts.length - 1];
            String folder = parts[parts.length - 2];
            String publicId = folder + "/" + filename.substring(0, filename.lastIndexOf('.'));

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted old image from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.error("Could not delete image from Cloudinary: ", e);
        }
    }
}
