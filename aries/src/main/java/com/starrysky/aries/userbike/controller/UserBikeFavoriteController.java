package com.starrysky.aries.userbike.controller;

import com.starrysky.aries.advice.LogAudit;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.userbike.schema.FavoriteDTO;
import com.starrysky.aries.userbike.schema.FavoriteVO;
import com.starrysky.aries.userbike.schema.PageResponse;
import com.starrysky.aries.userbike.service.UserBikeFavoriteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户收藏控制器
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/25
 */
@RestController
@RequestMapping("/favorite")
@Slf4j
public class UserBikeFavoriteController {
    
    @Resource
    private UserBikeFavoriteService favoriteService;
    
    /**
     * 收藏车型
     * 如果已收藏则直接返回
     *
     * @param dto 收藏请求(包含车型ID)
     * @param user 当前用户
     * @return 收藏VO
     */
    @LogAudit("收藏车型")
    @PostMapping("/bike-model")
    public FavoriteVO favoriteBikeModel(@RequestBody FavoriteDTO dto, 
                                         @AuthenticationPrincipal User user) {
        log.info("========== 收藏接口被调用 ==========");
        log.info("用户ID: {}", user.getId());
        log.info("车型ID: {}", dto.getBikeModelId());
        log.info("用户名: {}", user.getName());
        
        try {
            FavoriteVO result = favoriteService.favoriteBikeModel(dto.getBikeModelId(), user);
            log.info("收藏成功，返回结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("收藏失败，详细异常:", e);
            throw e;
        }
    }
    
    /**
     * 取消收藏(通过收藏记录ID)
     *
     * @param favoriteId 收藏记录ID
     * @param user 当前用户
     */
    @LogAudit("取消收藏")
    @DeleteMapping("/{favoriteId}")
    public void unfavorite(@PathVariable Long favoriteId, 
                           @AuthenticationPrincipal User user) {
        log.info("用户 {} 取消收藏记录 {}", user.getId(), favoriteId);
        favoriteService.unfavorite(favoriteId, user);
    }
    
    /**
     * 取消收藏(通过车型ID)
     *
     * @param bikeModelId 车型ID
     * @param user 当前用户
     */
    @LogAudit("取消收藏")
    @DeleteMapping("/bike-model/{bikeModelId}")
    public void unfavoriteByBikeModelId(@PathVariable Long bikeModelId,
                                         @AuthenticationPrincipal User user) {
        log.info("用户 {} 取消收藏车型 {}", user.getId(), bikeModelId);
        favoriteService.unfavoriteByBikeModelId(bikeModelId, user);
    }
    
    /**
     * 获取用户收藏列表(分页)
     *
     * @param page 页码(从1开始)
     * @param size 每页数量
     * @param user 当前用户
     * @return 分页收藏列表
     */
    @GetMapping("/list")
    public PageResponse<FavoriteVO> getFavoriteList(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     @AuthenticationPrincipal User user) {
        log.info("用户 {} 获取收藏列表, 页码 {}, 每页 {}", user.getId(), page, size);
        return favoriteService.getFavoriteList(user, page, size);
    }
    
    /**
     * 检查是否已收藏
     *
     * @param bikeModelId 车型ID
     * @param user 当前用户
     * @return 是否已收藏
     */
    @GetMapping("/bike-model/{bikeModelId}/status")
    public Boolean checkFavoriteStatus(@PathVariable Long bikeModelId,
                                        @AuthenticationPrincipal User user) {
        return favoriteService.isFavorited(bikeModelId, user);
    }
    
    /**
     * 统计用户收藏数量
     *
     * @param user 当前用户
     * @return 收藏数量
     */
    @GetMapping("/count")
    public Long countFavorites(@AuthenticationPrincipal User user) {
        return favoriteService.countFavorites(user);
    }
}
