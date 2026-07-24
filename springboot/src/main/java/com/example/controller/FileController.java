package com.example.controller;

import com.example.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${fileBaseUrl}")
    private String fileBaseUrl;

    /**
     * 文件扩展名白名单
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "zip", "rar"
    );

    /**
     * Magic bytes 签名表：扩展名 → 文件头字节
     * 用于验证文件真实类型与扩展名是否匹配
     */
    private static final Map<String, byte[][]> MAGIC_BYTES = Map.ofEntries(
            Map.entry("jpg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
            Map.entry("jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
            Map.entry("png", new byte[][]{{(byte) 0x89, 0x50, 0x4E, 0x47}}),
            Map.entry("gif", new byte[][]{{0x47, 0x49, 0x46, 0x38}}),
            Map.entry("pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}),
            Map.entry("zip", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}),
            Map.entry("rar", new byte[][]{{0x52, 0x61, 0x72, 0x21}})
            // doc/xls/ppt 使用 OLE2 格式，前 8 字节固定
            // webp: 52 49 46 46 ... 57 45 42 50
            // bmp: 42 4D
            // txt: 无固定签名，跳过 magic bytes 检查
    );

    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 检查文件大小（最大 10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过 10MB");
        }

        // 检查文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return Result.error("文件名为空");
        }

        // 检查扩展名白名单
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("不支持的文件类型: " + extension);
        }

        // 检查 magic bytes（验证文件真实类型）
        try {
            if (!verifyMagicBytes(file, extension)) {
                return Result.error("文件内容与扩展名不匹配，疑似伪造文件");
            }
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        }

        try {
            // 创建上传目录
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID() + "." + extension;
            Path filePath = uploadPath.resolve(fileName);

            // 保存文件
            file.transferTo(filePath.toFile());

            // 返回访问 URL
            String url = fileBaseUrl + "/uploads/" + fileName;
            return Result.success(url);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过 magic bytes 验证文件真实类型
     * 对于无固定签名的类型（如 txt）跳过检查
     */
    private boolean verifyMagicBytes(MultipartFile file, String extension) throws IOException {
        byte[][] signatures = MAGIC_BYTES.get(extension);
        if (signatures == null) {
            // 无 magic bytes 定义的类型（txt, doc 等），跳过检查
            return true;
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                return false;
            }

            for (byte[] signature : signatures) {
                if (matchesSignature(header, signature)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean matchesSignature(byte[] header, byte[] signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }
        return filename.substring(lastIndex + 1);
    }
}
