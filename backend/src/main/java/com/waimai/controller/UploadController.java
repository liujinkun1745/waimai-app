package com.waimai.controller;

import com.waimai.dto.response.Result;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "商品图片上传至 MinIO")
public class UploadController {

    private final MinioClient minioClient;

    @Value("${minio.bucket:waimai-products}")
    private String bucket;

    @PostMapping("/image")
    @Operation(summary = "上传商品图片")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 确保 bucket 存在
            boolean exists = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder().bucket(bucket).build());
            }

            // 生成文件名
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectName = "product/" + UUID.randomUUID().toString() + extension;

            // 上传
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            // 返回访问 URL
            String url = "/api/upload/file/" + objectName;
            log.info("图片上传成功: {}", url);

            return Result.success(Map.of("url", url, "objectName", objectName));
        } catch (Exception e) {
            log.error("上传失败", e);
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/file/{objectName}")
    @Operation(summary = "获取图片（代理 MinIO）")
    public void getFile(@PathVariable String objectName, jakarta.servlet.http.HttpServletResponse response) {
        try {
            var stream = minioClient.getObject(
                    io.minio.GetObjectArgs.builder().bucket(bucket).object(objectName).build());
            response.setContentType("image/" + getExtension(objectName));
            org.springframework.util.StreamUtils.copy(stream, response.getOutputStream());
            stream.close();
        } catch (Exception e) {
            log.error("获取文件失败: {}", objectName, e);
            response.setStatus(404);
        }
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) return "png";
        return name.substring(name.lastIndexOf(".") + 1);
    }
}
