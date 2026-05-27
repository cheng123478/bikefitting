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
import com.starrysky.aries.membership.annotation.RequirePrivilege;
import com.starrysky.aries.membership.enums.PrivilegeTypeEnum;

import java.util.List;
import java.util.Map;

/**
 * 动态Fitting控制器
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
@RestController
@RequestMapping("/dynamic-fitting")
public class DynamicFittingController {

    @Resource
    private DynamicFittingService dynamicFittingService;

    /**
     * 保存动态采集会话数据
     * 包含原始轨迹、截图、及统计摘要
     *
     * @param user    当前登录用户
     * @param request 请求体
     * @return 会话ID
     */
    @RequirePrivilege(PrivilegeTypeEnum.DYNAMIC_FITTING)
    @PostMapping("/sessions")
    public ResponseResult<DynamicFittingSessionVO> saveSession(@AuthenticationPrincipal User user,
                                                               @RequestBody DynamicFittingRequest request) {
        DynamicFittingSessionVO sessionVO = dynamicFittingService.saveFittingSession(user, request);
        // 触发异步后台分析
        dynamicFittingService.asyncProcessFittingSession(sessionVO.getId(), request, user.getId(), sessionVO.getBodyId());
        return ResponseResult.success("采集数据上传中，后台正在分析...", sessionVO);
    }

    /**
     * 获取历史采集列表
     *
     * @param user   当前登录用户
     * @param bikeId 单车ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页数据
     */
    @GetMapping("/sessions")
    public ResponseResult<Page<DynamicFittingSessionVO>> getSessions(@AuthenticationPrincipal User user,
                                                                     @RequestParam(value = "bikeId", required = false) Long bikeId,
                                                                     @RequestParam(value = "page", defaultValue = "1") int page,
                                                                     @RequestParam(value = "size", defaultValue = "10") int size) {
        int pageIndex = page > 0 ? page - 1 : 0;
        PageRequest pageRequest = PageRequest.of(pageIndex, size, Sort.by("createTime").descending());
        return ResponseResult.success(dynamicFittingService.getSessionsByUser(user.getId(), bikeId, pageRequest));
    }

    /**
     * 获取采集会话详情
     *
     * @param id 会话ID
     * @return 会话详情
     */
    @GetMapping("/sessions/{id}")
    public ResponseResult<Map<String, Object>> getSessionDetail(@PathVariable("id") Long id) {
        return ResponseResult.success(dynamicFittingService.getSessionDetail(id));
    }

    /**
     * 获取诊断结果
     *
     * @param sessionId 会话ID
     * @return 诊断结果
     */
    @GetMapping("/results/{sessionId}")
    public ResponseResult<DynamicFittingReportVO> getResult(@PathVariable("sessionId") Long sessionId) {
        return ResponseResult.success(dynamicFittingService.getFittingReport(sessionId));
    }

    /**
     * 获取原始轨迹数据
     *
     * @param sessionId 会话ID
     * @param viewType  视角类型
     * @return 轨迹数据
     */
    @GetMapping("/motion-data/{sessionId}/{viewType}")
    public ResponseResult<DynamicFittingMotionDataVO> getMotionData(@PathVariable("sessionId") Long sessionId,
                                                                    @PathVariable("viewType") String viewType) {
        return ResponseResult.success(dynamicFittingService.getMotionData(sessionId, viewType));
    }

    /**
     * 获取原始轨迹数据
     *
     * @param sessionId 会话ID
     * @param viewType  视角类型
     * @return 轨迹数据
     */
    @GetMapping("/snapshots/{sessionId}/{viewType}")
    public ResponseResult<List<DynamicFittingSnapshotVO>> getSnapshot(@PathVariable("sessionId") Long sessionId,
                                                                      @PathVariable("viewType") String viewType) {
        return ResponseResult.success(dynamicFittingService.getSnapshots(sessionId, viewType));
    }

    /**
     * 删除采集记录
     *
     * @param id 会话ID
     * @return 删除结果
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseResult<Object> deleteSession(@PathVariable("id") Long id) {
        dynamicFittingService.deleteFittingSession(id);
        return ResponseResult.success("删除成功");
    }

    /**
     * 获取用户的动态Fitting报告总数
     *
     * @param user 当前登录用户
     * @return 报告总数
     */
    @GetMapping("/count")
    public ResponseResult<Long> getDynamicFittingCount(@AuthenticationPrincipal User user) {
        return ResponseResult.success(dynamicFittingService.countByUserId(user.getId()));
    }
}
