package com.starrysky.aries.bikerecommend.controller;

import com.starrysky.aries.bikerecommend.schema.BikeRecommendRequest;
import com.starrysky.aries.bikerecommend.schema.BikeRecommendVO;
import com.starrysky.aries.bikerecommend.service.BikeRecommendService;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 单车智能推荐控制器
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/28
 */
@RestController
@RequestMapping("/bike-model")
public class BikeRecommendController {

    @Resource
    private BikeRecommendService bikeRecommendService;

    /**
     * 智能推荐单车
     * 根据用户身高、跨高、骑行场景和预算进行多维度评分推荐
     *
     * @param request 推荐请求参数
     * @param currentUser 当前登录用户(可选)
     * @return 推荐单车列表(按综合评分降序)
     */
    @PostMapping("/recommend")
    public ResponseResult<List<BikeRecommendVO>> recommendBikes(
            @RequestBody BikeRecommendRequest request,
            @AuthenticationPrincipal User currentUser) {
        // 参数校验
        if (request.getHeight() == null) {
            return ResponseResult.failure(400, "身高为必填项");
        }
        if (request.getScenes() == null || request.getScenes().isEmpty()) {
            return ResponseResult.failure(400, "请至少选择一个骑行场景");
        }

        return ResponseResult.success(bikeRecommendService.recommend(request));
    }
}
