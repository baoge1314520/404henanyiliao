package com.neuedu.controller;

import com.neuedu.config.UploadStorage;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

/**
 * 图片上传（医生头像等）。
 * 接收 multipart 文件，校验类型与大小后落盘，返回可直接被 img 加载的完整 URL。
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    /** 单张图片最大 5MB */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    @Resource
    private UploadStorage uploadStorage;

    @PostMapping("/image")
    public Result image(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            return Result.fail("只能上传图片文件");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.fail("图片大小不能超过 5MB");
        }
        try {
            String name = uploadStorage.store(file);
            // 基于当前请求的 scheme://host:port 拼出完整地址，前端跨域 img 可直接加载
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(name)
                    .toUriString();
            return Result.success(url, "头像上传成功");
        } catch (IOException e) {
            return Result.fail("图片保存失败：" + e.getMessage());
        }
    }
}