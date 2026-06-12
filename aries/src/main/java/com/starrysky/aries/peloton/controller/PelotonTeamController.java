package com.starrysky.aries.peloton.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.peloton.schema.CreateTeamDTO;
import com.starrysky.aries.peloton.schema.PelotonPageResponse;
import com.starrysky.aries.peloton.schema.PelotonTeamMemberVO;
import com.starrysky.aries.peloton.schema.PelotonTeamQueryDTO;
import com.starrysky.aries.peloton.schema.PelotonTeamVO;
import com.starrysky.aries.peloton.service.PelotonTeamService;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.upload.CosUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

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

    @Resource
    private CosUtil cosUtil;

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
     * 创建车队
     * <p>
     * 需要登录。创建成功后自动将创建者添加为队长。
     *
     * @param dto  创建车队参数
     * @param user 当前登录用户
     * @return 创建的车队信息
     */
    @PostMapping
    public ResponseResult<PelotonTeamVO> createTeam(@Valid @RequestBody CreateTeamDTO dto,
                                                     @AuthenticationPrincipal User user) {
        return ResponseResult.success(pelotonTeamService.createTeam(dto, user));
    }

    /**
     * 编辑车队信息
     * <p>
     * 仅车队创建者可操作。
     *
     * @param id   车队ID
     * @param dto  编辑内容（与创建时相同结构）
     * @param user 当前登录用户
     * @return 更新后的车队信息
     */
    @PutMapping("/{id}")
    public ResponseResult<PelotonTeamVO> updateTeam(@PathVariable("id") Long id,
                                                     @Valid @RequestBody CreateTeamDTO dto,
                                                     @AuthenticationPrincipal User user) {
        return ResponseResult.success(pelotonTeamService.updateTeam(id, dto, user));
    }

    /**
     * 上传车队图片（头像或封面）
     * <p>
     * 需要登录。上传至 COS 后返回图片访问 URL。
     *
     * @param file 图片文件
     * @param user 当前登录用户
     * @return 图片访问 URL
     */
    @PostMapping("/upload")
    public ResponseResult<String> uploadTeamImage(@RequestParam("file") MultipartFile file,
                                                   @AuthenticationPrincipal User user) {
        String contentType = file.getContentType();
        String ext = "jpg";
        if (contentType != null && contentType.contains("/")) {
            ext = contentType.split("/")[1];
        }
        String key = String.format("/%d/team/%s.%s",
                user.getId(), UUID.randomUUID(), ext);
        try {
            String url = cosUtil.upload(file.getInputStream(), file.getSize(), key, contentType);
            return ResponseResult.success(url);
        } catch (Exception e) {
            log.warn("上传车队图片失败", e);
            return ResponseResult.failure(500, "上传图片失败: " + e.getMessage());
        }
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

    /**
     * 退出车队
     * <p>
     * 需要登录。队长不能退出，需先转让队长身份。
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/members")
    public ResponseResult<Void> leaveTeam(@PathVariable("teamId") Long teamId,
                                          @AuthenticationPrincipal User user) {
        pelotonTeamService.leaveTeam(teamId, user);
        return ResponseResult.success();
    }

    /**
     * 解散车队
     * <p>
     * 仅车队创建者可操作。删除所有成员记录并移除车队。
     *
     * @param id   车队ID
     * @param user 当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseResult<Void> disbandTeam(@PathVariable("id") Long id,
                                            @AuthenticationPrincipal User user) {
        pelotonTeamService.disbandTeam(id, user);
        return ResponseResult.success();
    }
}
