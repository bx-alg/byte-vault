package com.bytevault.app.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucketName}")
    private String bucketName;
    
    @Value("${minio.userFilesBucket}")
    private String userFilesBucket;

    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            
            // 检查avatar bucket是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            
            if (!bucketExists) {
                // 如果不存在，则创建bucket
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Bucket {} 创建成功", bucketName);
            } else {
                log.info("Bucket {} 已存在", bucketName);
            }
            
            // 检查user-files bucket是否存在
            boolean userFilesBucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(userFilesBucket)
                    .build());
            
            if (!userFilesBucketExists) {
                // 如果不存在，则创建bucket
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(userFilesBucket)
                        .build());
                log.info("Bucket {} 创建成功", userFilesBucket);
            } else {
                log.info("Bucket {} 已存在", userFilesBucket);
            }
            
            return minioClient;
        } catch (Exception e) {
            log.error("初始化MinIO客户端失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化MinIO客户端失败", e);
        }
    }
}