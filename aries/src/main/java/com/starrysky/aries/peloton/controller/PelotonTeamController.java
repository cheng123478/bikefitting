package com.starrysky.aries.peloton.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.peloton.schema.PelotonPageResponse;
import com.starrysky.aries.peloton.schema.PelotonTeamMemberVO;
import com.starrysky.aries.peloton.schema.PelotonTeamQueryDTO;
import com.starrysky.aries.peloton.schema.PelotonTeamVO;
import com.starrysky.aries.peloton.service.PelotonTeamService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PelotonTeamController
 * 破风社-车队控制器
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@RestController
@RequestMapping("/team")
@Slf4j
public class PelotonTeamController {

    @Resource
    private PelotonTeamService pelotonTeamService;

    /**
     * 获取车队列表
     * <p>
     * 支持分类筛选（hot / new）、关键词搜索、分页。
     * 该接口无需登录即可访问。
     *
     * @param queryDTO 查询参数（page, size, keyword, category）
     * @return 分页车队列表
     */
    @GetMapping("/list")
    public ResponseResult<PelotonPageResponse<PelotonTeamVO>> getTeamList(PelotonTeamQueryDTO queryDTO) {
        return ResponseResult.success(pelotonTeamService.getTeamList(queryDTO));
    }

    /**
     * 获取我的车队（我创建的 + 我加入的）
     * <p>
     * 需要登录。
     *
     * @param user 当前登录用户
     * @return 我的车队列表
     */
    @GetMapping("/mine")
    public ResponseResult<List<PelotonTeamVO>> getMyTeams(@AuthenticationPrincipal User user) {
        return ResponseResult.success(pelotonTeamService.getMyTeams(user));
    }

    /**
     * 获取车队详情
     * <p>
     * 该接口无需登录即可访问。
     *
     * @param id 车队ID
     * @return 车队详情
     */
    @GetMapping("/{id}")
    public ResponseResult<PelotonTeamVO> getTeamDetail(@PathVariable("id") Long id) {
        return ResponseResult.success(pelotonTeamService.getTeamDetail(id));
    }

    /**
     * 获取车队成员列表
     * <p>
     * 该接口无需登录即可访问。
     *
     * @param teamId 车队ID
     * @return 成员列表
     */
    @GetMapping("/{teamId}/members")
    public ResponseResult<List<PelotonTeamMemberVO>> getTeamMembers(@PathVariable("teamId") Long teamId) {
        return ResponseResult.success(pelotonTeamService.getTeamMembers(teamId));
    }

    /**
     * 申请加入车队
     * <p>
     * 需要登录，同一车队不可重复加入。
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     * @return 操作结果
     */
    @PostMapping("/{teamId}/join")
    public ResponseResult<Void> joinTeam(@PathVariable("teamId") Long teamId,
                                         @AuthenticationPrincipal User user) {
        pelotonTeamService.joinTeam(teamId, user);
        return ResponseResult.success();
    }
}
