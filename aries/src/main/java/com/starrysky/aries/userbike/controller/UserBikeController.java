package com.starrysky.aries.userbike.controller;

import com.starrysky.aries.advice.LogAudit;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.userbike.schema.UserBikeDTO;
import com.starrysky.aries.userbike.schema.UserBikeVO;
import com.starrysky.aries.userbike.service.UserBikeService;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.upload.FileUploadResult;
import com.starrysky.aries.util.upload.FileUploadUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 用户单车档案控制器
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/10 22:41
 */
@RestController
@RequestMapping("/user-bike")
@Slf4j
public class UserBikeController {
    @Resource
    private UserBikeService userBikeService;
    @Resource
    private FileUploadUtil fileUploadUtil;

    /**
     * 【单车入库/校准】
     * <p>
     * 业务逻辑：
     * 如果 DTO 中 id 为空，则为新增车辆。
     * 如果 DTO 中 id 有值，则为“校准”现有车辆设定。
     *
     * @param dto  车辆参数载体
     * @param user 当前登录用户
     */
    @PostMapping("/save")
    public UserBikeVO save(@RequestBody UserBikeDTO dto, @AuthenticationPrincipal User user) {
        return userBikeService.saveUserBikeInfo(dto, user);
    }

    /**
     * 【我的车库列表】
     * <p>
     * 获取用户下属的所有单车。前端卡片会展示每辆车的品牌、型号、当前座高
     * 以及由 Fittor 引擎预计算出的 STR (激进指数)。
     *
     * @param user 当前登录用户
     */
    @GetMapping("/list/actived")
    public List<UserBikeVO> getMyGarage(@AuthenticationPrincipal User user) {
        return userBikeService.getMyGarage(user.getId());
    }

    /**
     * 退役单车列表
     * 获取用户下属的所有退役车辆。
     *
     * @param user 当前登录用户
     */
    @GetMapping("/list/retired")
    public List<UserBikeVO> getRetiredBikeList(@AuthenticationPrincipal User user) {
        return userBikeService.getRetiredBikeList(user.getId());
    }

    /**
     * 【战车详情/规格查询】
     * <p>
     * 获取某一特定单车的完整几何数据及实时设定参数。
     */
    @GetMapping("/{id}")
    public UserBikeVO getDetail(@PathVariable("id") Long id) {
        return userBikeService.getBikeDetail(id);
    }

    /**
     * 单车退役
     * 从我的车库中退役某辆单车。
     *
     * @param user 当前登录用户
     * @param id   单车ID
     */
    @DeleteMapping("/{id}")
    public void retire(@AuthenticationPrincipal User user, @PathVariable Long id) {
        userBikeService.retireBike(user, id);
    }

    /**
     * 恢复已退役单车
     * 从我的车库中恢复某退役单车。
     *
     * @param user 当前登录用户
     * @param id   单车ID
     */
    @PutMapping("/{id}")
    public void restore(@AuthenticationPrincipal User user, @PathVariable Long id) {
        userBikeService.restoreBike(user, id);
    }

    /**
     * 删除单车
     * 从我的车库中彻底删除某辆单车（在役或退役）。
     *
     * @param user 当前登录用户
     * @param id   单车ID
     */
    @LogAudit("删除单车")
    @DeleteMapping("/delete/{id}")
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        userBikeService.deleteBike(user, id);
    }

    /**
     * 上传单车图片
     *
     * @param user 当前登录用户
     * @param file 图片文件
     * @return 图片地址
     */
    @LogAudit("上传单车图片")
    @PostMapping("/upload/pic")
    public ResponseResult<String> uploadBikePic(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) {
        FileUploadResult fileUploadResult;
        try {
            fileUploadResult = fileUploadUtil.uploadPic(user.getId() + "/bike", file);
        } catch (IOException e) {
            log.warn("上传单车图片失败", e);
            return ResponseResult.failure(500, "上传单车图片失败");
        }
        if (fileUploadResult.isResult()) {
            return ResponseResult.success("/resource" + fileUploadResult.getUrl());
        } else {
            log.warn("上传单车图片失败: {}", fileUploadResult.getMessage());
            return ResponseResult.failure(500, fileUploadResult.getMessage());
        }
    }

    /**
     * 获取用户的在役车辆数量
     *
     * @param user 当前登录用户
     * @return 在役车辆数量
     */
    @GetMapping("/count/active")
    public ResponseResult<Long> getActiveBikeCount(@AuthenticationPrincipal User user) {
        return ResponseResult.success(userBikeService.getActiveBikeCount(user.getId()));
    }
}
