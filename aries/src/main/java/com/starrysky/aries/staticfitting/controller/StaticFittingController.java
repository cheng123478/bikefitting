package com.starrysky.aries.staticfitting.controller;

import com.starrysky.aries.entity.StaticFittingResult;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.staticfitting.schema.StaticFittingReportVO;
import com.starrysky.aries.staticfitting.schema.StaticFittingResultVO;
import com.starrysky.aries.staticfitting.service.StaticFittingService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.starrysky.aries.staticfitting.schema.StaticFittingRequest;

/**
 * 静态Fitting控制器
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/5 14:13
 */
@RestController
@RequestMapping("/static-fitting")
public class StaticFittingController {
    @Resource
    private StaticFittingService staticFittingService;

    /**
     * 计算静态Fitting结果
     *
     * @param request    静态Fitting请求参数
     * @return 静态Fitting结果
     */
    @PostMapping("/calculate")
    public StaticFittingResult calculateStaticFitting(@RequestBody StaticFittingRequest request) {
        return staticFittingService.calculateStaticFitting(request);
    }

    /**
     * 获取静态Fitting报告
     *
     * @param bikeId 单车ID
     * @param user   当前登录用户
     * @return 静态Fitting报告
     */
    @GetMapping("/report/{bikeId}")
    public StaticFittingReportVO getReport(@PathVariable Long bikeId, @AuthenticationPrincipal User user) {
        return staticFittingService.diagnoseBikeSetup(user.getId(), bikeId);
    }

    /**
     * 获取静态Fitting历史
     *
     * @param user 当前登录用户
     * @return 静态Fitting历史
     */
    @GetMapping("/history")
    public Page<StaticFittingResultVO> getHistory(@AuthenticationPrincipal User user,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return staticFittingService.getHistoryByUserId(user.getId(), page, size);
    }
}
