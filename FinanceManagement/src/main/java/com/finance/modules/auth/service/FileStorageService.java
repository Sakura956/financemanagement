package com.finance.modules.auth.service;

import com.finance.common.exception.BusinessException;
import com.finance.modules.auth.entity.User;
import com.finance.modules.auth.mapper.UserMapper;
import com.finance.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务
 * 负责将上传的文件保存到服务器本地磁盘，并返回前端可直接渲染的访问路径
 */
@Service
public class FileStorageService {
    private final UserMapper userMapper;
    // 注入 UserMapper 用于获取旧头像
    public FileStorageService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    // 允许上传的图片类型
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    ));

    // 最大文件大小（5MB）
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 保存头像文件到本地磁盘
     *
     * @param file 前端上传的文件
     * @return 头像的访问路径（如 /uploads/avatars/xxx.jpg），前端可直接拼接域名进行渲染
     */
    public String saveAvatar(MultipartFile file) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "头像文件大小不能超过 5MB");
        }

        // 3. 校验文件类型（MIME 类型必须是 image）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "只允许上传图片文件");
        }

        // 4. 获取并校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "不支持的图片格式，仅支持 jpg、png、gif、bmp、webp");
        }

        deleteOldAvatar();

        // 6. 确保存储目录存在
        // 取 项目根目录（绝对路径）
        String projectPath = System.getProperty("user.dir");
        File uploadDir = new File(projectPath, "uploads/avatars/");
        // 创建目录
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 新文件名
        String fileName = UUID.randomUUID() + extension;
        File destFile = new File(uploadDir, fileName);

        // 【不用 transferTo】改用流复制 → 彻底避开 Tomcat 坑
        try (InputStream in = file.getInputStream();
             FileOutputStream out = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(500, "头像保存失败：" + e.getMessage());
        }
        // ===================== 数据库只存相对路径，端口变更也不影响访问 =====================
        // 以前存完整URL（如 http://localhost:8082/uploads/avatars/xxx.jpg），
        // 一旦后端端口变了，数据库中所有旧URL全部404。
        // 现在只存相对路径 /uploads/avatars/xxx.jpg，
        // 前端读取时由 buildAccessUrl() 动态拼接当前请求的域名端口。
        return "/uploads/avatars/" + fileName;
    }

    // ===================== 动态拼接完整访问URL（解决端口变更导致404的问题） =====================
    /**
     * 将数据库中存储的头像路径转换为当前环境可完整访问的 URL
     * 背景：数据库只存相对路径 /uploads/avatars/xxx.jpg，
     *      读取时基于当前请求的域名端口动态拼接，端口怎么变都能正常访问。
     *      同时兼容旧数据中存储的全 URL（如 http://localhost:8082/uploads/...）。
     *
     * @param storedPath 数据库中的头像路径（相对路径 或 旧的全URL 或 网络图片URL）
     * @return 当前环境可访问的完整头像URL
     */
    public static String buildAccessUrl(String storedPath) {
        // 空值直接返回
        if (storedPath == null || storedPath.isEmpty()) {
            return null;
        }

        // 1. 提取相对路径部分（兼容旧数据中的全URL）
        String relativePath;
        if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
            // 旧数据是全URL格式 → 提取 /uploads/... 路径部分
            int idx = storedPath.indexOf("/uploads/");
            if (idx >= 0) {
                relativePath = storedPath.substring(idx);
            } else {
                // 网络图片（如微信头像），不是本地文件，原样返回
                return storedPath;
            }
        } else {
            // 已经是相对路径，直接使用
            relativePath = storedPath;
        }

        // 2. 基于当前请求动态拼接完整URL（端口变更也不怕）
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getScheme() + "://"   // http 或 https
                        + request.getServerName()    // localhost 或域名
                        + ":" + request.getServerPort() // 当前后端端口
                        + relativePath;              // /uploads/avatars/xxx.jpg
            }
        } catch (Exception ignored) {
            // 非请求上下文（如定时任务），返回相对路径兜底
        }

        // 3. 没有请求上下文时的兜底，返回相对路径
        return relativePath;
    }

    // ===================== 【自动删除旧头像】 =====================
    private void deleteOldAvatar() {
        try {
            // 1. 获取当前登录用户ID（你自己的获取方式）
            Long userId = SecurityUtil.getCurrentUserId(); // 你项目里的工具类

            // 2. 查询用户旧头像
            User user = userMapper.selectById(userId);
            if (user == null || user.getAvatarUrl() == null) {
                return;
            }

            String oldAvatar = user.getAvatarUrl();
            // 只删除本地上传的图片（http 开头的是网络图片，不删）
            if (oldAvatar.contains("/uploads/avatars/")) {
                // 截取相对路径
                String relativePath = oldAvatar.substring(oldAvatar.indexOf("/uploads/avatars/"));
                String projectPath = System.getProperty("user.dir");
                File oldFile = new File(projectPath, relativePath);

                // 删除文件
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
