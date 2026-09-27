package com.finance.modules.auth.controller;

import com.finance.common.result.Result;
import com.finance.modules.auth.dto.ChangePasswordRequest;
import com.finance.modules.auth.dto.LoginRequest;
import com.finance.modules.auth.dto.RegisterRequest;
import com.finance.modules.auth.service.AuthService;
import com.finance.modules.auth.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    //final 关键字保证线程安全、不可变，优于@Autowired
    private final AuthService authService;
    private final FileStorageService fileStorageService;
    //构造器注入
    public AuthController(AuthService authService, FileStorageService fileStorageService) {
        this.authService = authService;
        this.fileStorageService = fileStorageService;
    }


    /**
     * 注册
     * @param request
     * @return
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功", null);
    }

    /**
     * 登录
     * @param request
     * @return
     */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }


    /**
     * 修改密码
     * @param request
     * @return
     */
    @PutMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success("密码修改成功，请重新登录", null);
    }


    /**
     * 获取当前登录用户信息
     * @return
     */
    @GetMapping("/me")
    public Result<?> me() {
        return Result.success(authService.me());
    }


    /**
     * 更新个人信息
     * @param params
     * @return
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, Object> params) {
        return Result.success("个人信息更新成功", authService.updateProfile(params));
    }

    /**
     * 上传头像
     * 接收前端以 multipart/form-data 形式上传的图片文件，
     * 保存到服务器本地磁盘（src 同级的 uploads/avatars/ 目录），
     * 并将可渲染的访问路径更新到数据库的 avatar_url 字段
     *
     * @param file 前端上传的图片文件（表单字段名：file）
     * @return 更新后的用户信息
     */
    @PostMapping("/avatar")
    public Result<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 将文件保存到磁盘，返回前端可直接渲染的访问路径
        String avatarUrl = fileStorageService.saveAvatar(file);
        // 将头像路径更新到数据库
        return Result.success("头像上传成功", authService.updateProfile(Map.of("avatarUrl", avatarUrl)));
    }
}
