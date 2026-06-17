package com.starrysky.aries.peloton.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.peloton.schema.CreateNoticeDTO;
import com.starrysky.aries.common.schema.PageResponse;
import com.starrysky.aries.peloton.schema.TeamNoticeVO;
import com.starrysky.aries.peloton.service.TeamNoticeService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * TeamNoticeController
 * 破风社-车队公告控制器
 *
 * <h3>认证边界说明</h3>
 * <ul>
 *   <li><b>需要登录（仅队内成员可见）</b>：GET /{teamId}/notices, POST /{teamId}/notices,
 *       PUT /{teamId}/notices/{noticeId}, DELETE /{teamId}/notices/{noticeId}</li>
 * </ul>
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@RestController
@RequestMapping("/team")
public class TeamNoticeController {

    @Resource
    private TeamNoticeService teamNoticeService;

    /**
     * 发布公告
     * <p>
     * 需要登录。仅队长或管理员可发布。
     *
     * @param teamId 车队ID
     * @param dto    发布参数（title, content）
     * @param user   当前登录用户
     * @return 发布的公告信息
     */
    @PostMapping("/{teamId}/notices")
    public ResponseResult<TeamNoticeVO> publishNotice(
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody CreateNoticeDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamNoticeService.publishNotice(teamId, dto, user));
    }

    /**
     * 编辑公告
     * <p>
     * 需要登录。公告发布者本人、队长或管理员可编辑。
     *
     * @param teamId   车队ID
     * @param noticeId 公告ID
     * @param dto      编辑参数（title, content）
     * @param user     当前登录用户
     * @return 更新后的公告信息
     */
    @PutMapping("/{teamId}/notices/{noticeId}")
    public ResponseResult<TeamNoticeVO> updateNotice(
            @PathVariable("teamId") Long teamId,
            @PathVariable("noticeId") Long noticeId,
            @Valid @RequestBody CreateNoticeDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamNoticeService.updateNotice(teamId, noticeId, dto, user));
    }

    /**
     * 删除公告
     * <p>
     * 需要登录。公告发布者本人、队长或管理员可删除（软删除）。
     *
     * @param teamId   车队ID
     * @param noticeId 公告ID
     * @param user     当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/notices/{noticeId}")
    public ResponseResult<Void> deleteNotice(
            @PathVariable("teamId") Long teamId,
            @PathVariable("noticeId") Long noticeId,
            @AuthenticationPrincipal User user) {
        teamNoticeService.deleteNotice(teamId, noticeId, user);
        return ResponseResult.success();
    }

    /**
     * 获取公告栏（分页查询车队的正常公告，按发布时间倒序）
     * <p>
     * 需要登录，仅队内成员可访问。
     *
     * @param teamId 车队ID
     * @param page   页码（从1开始，默认1）
     * @param size   每页条数（默认10，最大50）
     * @param user   当前登录用户
     * @return 公告分页列表
     */
    @GetMapping("/{teamId}/notices")
    public ResponseResult<PageResponse<TeamNoticeVO>> getNoticeBoard(
            @PathVariable("teamId") Long teamId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamNoticeService.getNoticeBoard(teamId, page, size, user));
    }
}
