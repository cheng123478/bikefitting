package com.starrysky.aries.peloton.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.peloton.schema.CreateActivityDTO;
import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.peloton.schema.TeamActivityVO;
import com.starrysky.aries.peloton.service.TeamActivityService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * TeamActivityController
 * 车队活动接口
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@RestController
@RequestMapping("/team")
public class TeamActivityController {

    @Resource
    private TeamActivityService teamActivityService;

    /**
     * 发布活动
     * <p>
     * 需要登录。仅队长或管理员可发布。
     *
     * @param teamId 车队ID
     * @param dto    发布参数
     * @param user   当前登录用户
     * @return 发布的活动信息
     */
    @PostMapping("/{teamId}/activities")
    public ResponseResult<TeamActivityVO> publishActivity(
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody CreateActivityDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamActivityService.publishActivity(teamId, dto, user));
    }

    /**
     * 编辑活动
     * <p>
     * 需要登录。活动发布者本人、队长或管理员可编辑。
     *
     * @param teamId     车队ID
     * @param activityId 活动ID
     * @param dto        编辑参数
     * @param user       当前登录用户
     * @return 更新后的活动信息
     */
    @PutMapping("/{teamId}/activities/{activityId}")
    public ResponseResult<TeamActivityVO> updateActivity(
            @PathVariable("teamId") Long teamId,
            @PathVariable("activityId") Long activityId,
            @Valid @RequestBody CreateActivityDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamActivityService.updateActivity(teamId, activityId, dto, user));
    }

    /**
     * 删除活动（软删除）
     * <p>
     * 需要登录。活动发布者本人、队长或管理员可删除。
     *
     * @param teamId     车队ID
     * @param activityId 活动ID
     * @param user       当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/activities/{activityId}")
    public ResponseResult<Void> deleteActivity(
            @PathVariable("teamId") Long teamId,
            @PathVariable("activityId") Long activityId,
            @AuthenticationPrincipal User user) {
        teamActivityService.deleteActivity(teamId, activityId, user);
        return ResponseResult.success();
    }

    /**
     * 获取活动列表（分页查询车队的正常活动，按活动日期倒序）
     * <p>
     * 公开接口，无需登录即可查看。
     *
     * @param teamId 车队ID
     * @param page   页码（从1开始，默认1）
     * @param size   每页条数（默认10，最大50）
     * @return 活动分页列表
     */
    @GetMapping("/{teamId}/activities")
    public ResponseResult<PageResponse<TeamActivityVO>> getActivities(
            @PathVariable("teamId") Long teamId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ResponseResult.success(teamActivityService.getActivities(teamId, page, size));
    }
}
