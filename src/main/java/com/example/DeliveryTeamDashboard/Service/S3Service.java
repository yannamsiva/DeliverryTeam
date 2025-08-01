package com.example.DeliveryTeamDashboard.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class S3Service {

     private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public S3Service(S3Client s3Client,S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner=s3Presigner;
    }
    
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/pdf",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        );


    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String fileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        logger.info("Uploading file: {} to S3 bucket: {}, folder: {}", file.getOriginalFilename(), bucketName, folder);
        try {
            String contentType = file.getContentType();
            boolean isProfilePicture = "profile-pictures".equals(folder);
            if (isProfilePicture) {
                if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
                    throw new IllegalArgumentException("Profile picture must be a JPEG (.jpg, .jpeg) or PNG (.png) file");
                }
            } else {
                if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                    throw new IllegalArgumentException("File must be an Excel (.xls, .xlsx), Word (.doc, .docx), PDF (.pdf), or PowerPoint (.ppt, .pptx) file");
                }
            }
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            logger.info("File uploaded successfully: {}", fileName);
            return fileName;
        } catch (Exception e) {
            logger.error("Failed to upload file: {} to S3: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    public byte[] downloadFile(String s3Key) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
        return s3Object.readAllBytes();
    }

    public void deleteFile(String s3Key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }
    
    public List<String> uploadMultipleFiles(MultipartFile[] files, String folder) throws IOException {
        List<String> s3Keys = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String contentType = file.getContentType();
                if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                    throw new IllegalArgumentException("File must be an Excel (.xls, .xlsx), Word (.doc, .docx), PDF (.pdf), or PowerPoint (.ppt, .pptx) file");
                }
                String s3Key = uploadFile(file, folder);
                s3Keys.add(s3Key);
            }
        }
        return s3Keys;
    }

    public String generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(builder -> builder.bucket(bucketName).key(s3Key))
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }
}
