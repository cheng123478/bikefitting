package com.starrysky.aries.user.controller;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.starrysky.aries.advice.LogAudit;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.redis.RedisConstant;
import com.starrysky.aries.redis.RedisUtil;
import com.starrysky.aries.security.jwt.JwtUtil;
import com.starrysky.aries.user.schema.UserRequest;
import com.starrysky.aries.user.schema.UserResponse;
import com.starrysky.aries.user.service.UserService;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.ResponseResultStatus;
import com.starrysky.aries.util.upload.CosUtil;
import com.starrysky.aries.util.upload.FileUploadResult;
import com.starrysky.aries.util.upload.FileUploadUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * UserController
 * 用户控制器
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private FileUploadUtil fileUploadUtil;
    @Resource
    private CosUtil cosUtil;
    @Resource
    private RedisUtil redisUtil;

    /**
     * 通过短信验证码添加或修改密码
     *
     * @param user        Security中的用户，主要使用id
     * @param passwordSet 验证码和密码
     * @return 添加成功或抛出异常
     */
    @LogAudit("添加密码")
    @PostMapping("/password/add/code")
    public ResponseResult<String> addPassword(@AuthenticationPrincipal User user,
            @RequestBody @Valid UserRequest.PasswordSet passwordSet) throws Exception {
        userService.addPassword(user, passwordSet.code(), passwordSet.password());
        return ResponseResult.success("密码设置成功");
    }

    /**
     * 修改密码
     *
     * @param user        Security中的用户，主要使用id
     * @param passwordSet 验证码和密码
     * @return 修改成功或抛出异常
     */
    @LogAudit("通过短信验证码修改密码")
    @PostMapping("/password/edit/code")
    public ResponseResult<String> editPasswordByCode(@AuthenticationPrincipal User user,
            @RequestBody @Valid UserRequest.PasswordSet passwordSet) throws Exception {
        userService.addPassword(user, passwordSet.code(), passwordSet.password());
        return ResponseResult.success("密码修改成功");
    }

    /**
     * 修改密码
     *
     * @param user           Security中的用户，主要使用id
     * @param passwordChange 旧密码和新密码
     * @return 修改成功或抛出异常
     */
    @LogAudit("通过密码修改密码")
    @PostMapping("/password/edit")
    public ResponseResult<String> editPassword(@AuthenticationPrincipal User user,
            @RequestBody @Valid UserRequest.PasswordChange passwordChange) throws Exception {
        userService.editPassword(user, passwordChange.oldPassword(), passwordChange.newPassword());
        return ResponseResult.success("密码修改成功");
    }

    /**
     * 修改手机号
     *
     * @param user        Security中的用户，主要使用id
     * @param phoneChange 手机号、密码和验证码
     * @return 修改成功或抛出异常
     */
    @LogAudit("修改手机号")
    @PostMapping("/phone/edit")
    public ResponseResult<String> editPhone(@AuthenticationPrincipal User user,
            @RequestBody @Valid UserRequest.PhoneChange phoneChange) throws Exception {
        if (!PhoneUtil.isPhone(phoneChange.newPhone())) {
            throw new BizException("手机号不正确");
        }
        userService.editPhone(user.getPhone(), phoneChange.newPhone(), phoneChange.code(), phoneChange.password());
        userService.updateRedisCache(user.getId());
        return ResponseResult.success("手机号修改成功");
    }

    /**
     * 修改用户信息
     *
     * @param user          当前用户
     * @param profileUpdate 用户信息
     * @return 修改成功或抛出异常
     */
    @LogAudit("修改用户信息")
    @PutMapping("/edit")
    public ResponseResult<Void> editUser(@AuthenticationPrincipal User user,
            @RequestBody @Valid UserRequest.ProfileUpdate profileUpdate) {
        userService.editUser(user.getId(), profileUpdate);
        userService.updateRedisCache(user.getId());
        return ResponseResult.success("用户信息修改成功", null);
    }

    /**
     * 注销账户
     * 不删除数据库记录，只标记为已注销
     * 注销后7天内登录，即取消注销账号的操作
     *
     * @param user 当前用户
     * @return 操作结果
     */
    @LogAudit("注销账户")
    @PostMapping("/delete")
    public ResponseResult<String> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        SecurityContextHolder.clearContext();
        redisUtil.getHashOperations().delete(RedisConstant.AUTHENTICATION + user.getId(), "principal");
        log.info("用户注销账户成功 | 用户phone: {}", user.getPhone());
        return ResponseResult.success("账户注销成功，若要使用请重新注册");
    }

    /**
     * 上传头像
     *
     * @param user 当前用户
     * @param file 头像文件
     * @return 头像地址
     */
    @LogAudit("上传头像")
    @PostMapping("/avatar")
    public ResponseResult<String> uploadAvatar(@AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        String key = String.format("/%d/user/%s.%s",
                user.getId(), UUID.randomUUID(), file.getContentType().split("/")[1]);

        try {
            String url = cosUtil.upload(file.getInputStream(), file.getSize(), key);
            userService.editAvatar(user.getId(), url);
            userService.updateRedisCache(user.getId());
            return ResponseResult.success(url);
        } catch (IOException e) {
            log.warn("上传头像失败", e);
            return ResponseResult.failure(500, "上传头像失败");
        }
//
//        FileUploadResult fileUploadResult;
//        try {
//            fileUploadResult = fileUploadUtil.uploadPic(user.getId() + "/avatar", file);
//        } catch (IOException e) {
//            log.warn("上传头像失败", e);
//            return ResponseResult.failure(500, "上传头像失败");
//        }
//        if (fileUploadResult.isResult()) {
//            userService.editAvatar(user.getId(), "/resource" + fileUploadResult.getUrl());
//            userService.updateRedisCache(user.getId());
//            return ResponseResult.success("/resource" + fileUploadResult.getUrl());
//        } else {
//            log.warn("上传头像,修改头像信息失败: {}", fileUploadResult.getMessage());
//            return ResponseResult.failure(500, fileUploadResult.getMessage());
//        }
    }

    /**
     * 刷新用户Token
     *
     * @param token 刷新令牌
     * @return 访问令牌
     */
    @PostMapping("/refresh")
    public ResponseResult<UserResponse.LoginResult> refresh(
            @RequestBody @Valid UserRequest.RefreshToken token,
            @RequestHeader("User-Agent") String userAgent) {
        String refreshToken = token.refreshToken();
        if (token.refreshToken().startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        String id;
        String tokenVersion;
        try {
            id = JwtUtil.getIdFromToken(refreshToken);
            tokenVersion = JwtUtil.getTokenVersionFromToken(refreshToken);
        } catch (InvalidJwtException e) {
            log.warn("Token解析失败: {}", e.getMessage());
            return ResponseResult.failure(ResponseResultStatus.TOKEN_INVALID.getCode(), "身份验证已过期，请重新登录");
        }
        Map<Object, Object> map = redisUtil.getHashOperations().entries(RedisConstant.AUTHENTICATION + id);
        if (CollectionUtils.isEmpty(map) || map.get("principal") == null) {
            return ResponseResult.failure(ResponseResultStatus.TOKEN_MISSING.getCode(), "登录已失效，请重新登录");
        }
        User user = (User) map.get("principal");
        String tokenVersionInRedis = (String) map.get("token_version");
        if (!tokenVersionInRedis.equals(tokenVersion)) {
            SecurityContextHolder.clearContext();
            redisUtil.getHashOperations().delete(RedisConstant.AUTHENTICATION + user.getId());
            log.warn("Token 刷新失败: TokenVersion不匹配");
            return ResponseResult.failure(ResponseResultStatus.TOKEN_INVALID.getCode(), "登录已失效，请重新登录");
        }
        String newTokenVersion = UUID.randomUUID().toString().replace("-", "");
        redisUtil.getHashOperations().put(RedisConstant.AUTHENTICATION + user.getId(), "token_version",
                newTokenVersion);
        redisUtil.expire(RedisConstant.AUTHENTICATION + user.getId(), RedisConstant.AUTHENTICATION_TIMEOUT);
        // 将用户信息、Token信息封装到JWT，发送给客户端
        String accessToken = "Bearer " + JwtUtil.createAccessToken(user, DigestUtil.sha256Hex(userAgent));
        refreshToken = "Bearer " + JwtUtil.createRefreshToken(user, newTokenVersion);

        UserResponse.Profile profile = new UserResponse.Profile(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getIntroduction(),
                user.getAvatar(),
                user.getBirthday(),
                user.getGender(),
                user.getLocation());

        return ResponseResult.success(new UserResponse.LoginResult(accessToken, refreshToken, profile));
    }

}
