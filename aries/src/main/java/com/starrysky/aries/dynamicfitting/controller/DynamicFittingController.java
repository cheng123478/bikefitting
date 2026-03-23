package com.starrysky.aries.dynamicfitting.controller;

import com.starrysky.aries.dynamicfitting.schema.*;
import com.starrysky.aries.dynamicfitting.service.DynamicFittingService;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动态 Fitting 控制器
 */
@RestController
@RequestMapping("/dynamic-fitting")
public class DynamicFittingController {

    @Resource
    private DynamicFittingService dynamicFittingService;

    /**
     * 保存动态采集会话数据
     * 包含原始轨迹、截图、及统计摘要
     */
    @PostMapping("/sessions")
    public ResponseResult<DynamicFittingSessionVO> saveSession(@AuthenticationPrincipal User user,
            @RequestBody DynamicFittingRequest request) {
        DynamicFittingSessionVO sessionVO = dynamicFittingService.saveFittingSession(user, request);
        // 触发异步后台分析
        dynamicFittingService.asyncProcessFittingSession(sessionVO.getId(), request, user, sessionVO.getBodyId());
        return ResponseResult.success("采集数据上传中，后台正在分析...", sessionVO);
    }

    /**
     * 获取历史采集列表 (分页)
     */
    @GetMapping("/sessions")
    public ResponseResult<Page<DynamicFittingSessionVO>> getSessions(@AuthenticationPrincipal User user,
            @RequestParam(required = false) Long bikeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        return ResponseResult.success(dynamicFittingService.getSessionsByUser(user.getId(), bikeId, pageRequest));
    }

    /**
     * 获取采集会话详情
     */
    @GetMapping("/sessions/{id}")
    public ResponseResult<Map<String, Object>> getSessionDetail(@PathVariable Long id) {
        return ResponseResult.success(dynamicFittingService.getSessionDetail(id));
    }

    /**
     * 获取诊断结果
     */
    @GetMapping("/results/{sessionId}")
    public ResponseResult<DynamicFittingReportVO> getResult(@PathVariable Long sessionId) {
        return ResponseResult.success(dynamicFittingService.getFittingReport(sessionId));
    }

    /**
     * 获取原始轨迹数据
     */
    @GetMapping("/motion-data/{sessionId}/{viewType}")
    public ResponseResult<DynamicFittingMotionDataVO> getMotionData(@PathVariable Long sessionId,
            @PathVariable String viewType) {
        return ResponseResult.success(dynamicFittingService.getMotionData(sessionId, viewType));
    }

    /**
     * 获取原始轨迹数据
     */
    @GetMapping("/snapshots/{sessionId}/{viewType}")
    public ResponseResult<List<DynamicFittingSnapshotVO>> getSnapshot(@PathVariable Long sessionId,
            @PathVariable String viewType) {
        return ResponseResult.success(dynamicFittingService.getSnapshots(sessionId, viewType));
    }

    /**
     * 删除采集记录
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseResult<Object> deleteSession(@PathVariable Long id) {
        dynamicFittingService.deleteFittingSession(id);
        return ResponseResult.success("删除成功");
    }
}
