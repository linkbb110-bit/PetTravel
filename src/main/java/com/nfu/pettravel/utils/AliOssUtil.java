package com.nfu.pettravel.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class AliOssUtil {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucketName;
    private final String publicBaseUrl;
    private final long maxFileSize;

    public AliOssUtil(
            @Value("${aliyun.oss.endpoint}") String endpoint,
            @Value("${aliyun.oss.access-key-id}") String accessKeyId,
            @Value("${aliyun.oss.access-key-secret}") String accessKeySecret,
            @Value("${aliyun.oss.bucket-name}") String bucketName,
            @Value("${aliyun.oss.public-base-url:}") String publicBaseUrl,
            @Value("${aliyun.oss.max-file-size:10485760}") long maxFileSize) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
        this.maxFileSize = maxFileSize;
    }

    public String upload(MultipartFile file) {
        byte[] content = validateAndRead(file);
        String extension = extensionOf(file.getOriginalFilename());
        LocalDate today = LocalDate.now();
        String objectName = "uploads/%d/%02d/%s%s".formatted(
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), extension);
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
            return buildPublicUrl(objectName);
        } catch (RuntimeException uploadFailure) {
            throw new BusinessException(ErrorCode.UPLOAD_FAILED);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        String extension = extensionOf(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (!ALLOWED_EXTENSIONS.contains(extension)
                || contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        try {
            byte[] content = file.getBytes();
            if (!hasMatchingImageSignature(extension, content)) {
                throw new BusinessException(ErrorCode.FILE_INVALID);
            }
            return content;
        } catch (IOException readFailure) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_INVALID);
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private boolean hasMatchingImageSignature(String extension, byte[] content) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> content.length >= 3
                    && unsigned(content[0]) == 0xff && unsigned(content[1]) == 0xd8
                    && unsigned(content[2]) == 0xff;
            case ".png" -> content.length >= 8
                    && unsigned(content[0]) == 0x89 && content[1] == 'P' && content[2] == 'N'
                    && content[3] == 'G';
            case ".webp" -> content.length >= 12
                    && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                    && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private String buildPublicUrl(String objectName) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return stripTrailingSlash(publicBaseUrl) + "/" + objectName;
        }
        String cleanEndpoint = endpoint.replaceFirst("^https?://", "");
        return "https://" + bucketName + "." + stripTrailingSlash(cleanEndpoint) + "/" + objectName;
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
