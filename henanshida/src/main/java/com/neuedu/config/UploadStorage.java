package com.neuedu.config;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 头像等图片文件的本地存储。
 * 保存到可配置目录（默认相对后端工作目录的 uploads），用随机文件名避免重名与路径穿越。
 * 控制器与静态资源映射共用同一根目录，保证存进去的文件能被 /uploads/** 访问到。
 */
@Component
public class UploadStorage {

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    private final Path root;

    public UploadStorage(@Value("${his.upload.dir:uploads}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(root);
    }

    public Path getRoot() {
        return root;
    }

    /**
     * 保存上传文件，返回存储文件名（不含目录）。
     */
    public String store(MultipartFile file) throws IOException {
        String original = StringUtils.defaultIfBlank(file.getOriginalFilename(), "image");
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            ext = ".png";
        }
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("非法的存储路径");
        }
        file.transferTo(target.toFile());
        return name;
    }
}