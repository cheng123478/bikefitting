package com.starrysky.aries.peloton.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.peloton.schema.*;
import com.starrysky.aries.peloton.service.PelotonTeamService;
import com.starrysky.aries.peloton.service.TeamApplicationService;
import com.starrysky.aries.peloton.service.TeamGarageService;
import com.starrysky.aries.peloton.service.TeamMemberService;
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
 * <h3>认证边界说明</h3>
 * <ul>
 *   <li><b>无需登录</b>：GET /list, GET /{teamId}, GET /{teamId}/members, GET /{teamId}/garage</li>
 *   <li><b>需要登录</b>：POST /, PUT /{teamId}, DELETE /{teamId}, POST /upload,
 *       GET /mine, POST /{teamId}/join, DELETE /{teamId}/members, 以及所有管理类接口</li>
 * </ul>
 * <p>
 * 依赖 {@link AuthenticationPrincipal} 注入当前用户，需确保 Spring Security 配置正确拦截
 * 需要登录的路径，否则未登录时 user 参数为 null 会触发 NPE。
 *
 * <h3>缓存建议</h3>
 * TODO(perf): GET /list、GET /{teamId}、GET /{teamId}/members 等高频读取接口可引入
 * Spring Cache（@Cacheable + @CacheEvict），TTL 建议 30~60s，在写操作时主动失效。
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
    private TeamMemberService teamMemberService;

    @Resource
    private TeamApplicationService teamApplicationService;

    @Resource
    private TeamGarageService teamGarageService;

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
     * @param teamId 车队ID
     * @param dto  编辑内容（与创建时相同结构）
     * @param user 当前登录用户
     * @return 更新后的车队信息
     */
    @PutMapping("/{teamId}")
    public ResponseResult<PelotonTeamVO> updateTeam(@PathVariable("teamId") Long teamId,
                                                     @Valid @RequestBody CreateTeamDTO dto,
                                                     @AuthenticationPrincipal User user) {
        return ResponseResult.success(pelotonTeamService.updateTeam(teamId, dto, user));
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
            // #13 fix: 不暴露异常详情给前端
            return ResponseResult.failure(500, "上传图片失败，请稍后重试");
        }
    }

    /**
     * 获取车队详情
     * <p>
     * 该接口无需登录即可访问。
     *
     * @param teamId 车队ID
     * @return 车队详情
     */
    @GetMapping("/{teamId}")
    public ResponseResult<PelotonTeamVO> getTeamDetail(@PathVariable("teamId") Long teamId) {
        return ResponseResult.success(pelotonTeamService.getTeamDetail(teamId));
    }

    /**
     * 获取车队成员列表（支持分页 + 关键词搜索）
     * <p>
     * 该接口无需登录即可访问。关键词模糊匹配成员昵称或骑行标签。
     *
     * @param teamId   车队ID
     * @param queryDTO 查询参数（page, size, keyword）
     * @return 成员分页列表
     */
    @GetMapping("/{teamId}/members")
    public ResponseResult<PelotonPageResponse<PelotonTeamMemberVO>> getTeamMembers(
            @PathVariable("teamId") Long teamId,
            TeamMemberQueryDTO queryDTO) {
        return ResponseResult.success(pelotonTeamService.getTeamMembers(teamId, queryDTO));
    }

    /**
     * 获取车队车库
     * <p>
     * 该接口无需登录即可访问。聚合车主信息 + 单车几何信息返回。
     *
     * @param teamId 车队ID
     * @return 车库列表（含 records 和 total）
     */
    @GetMapping("/{teamId}/garage")
    public ResponseResult<PelotonPageResponse<TeamGarageVO>> getTeamGarage(@PathVariable("teamId") Long teamId) {
        return ResponseResult.success(teamGarageService.getTeamGarage(teamId));
    }

    /**
     * 将个人车库中的单车同步至车队车库
     * <p>
     * 需要登录，且当前用户必须是车队成员。
     *
     * @param teamId 车队ID
     * @param body   包含 bikeId 字段
     * @param user   当前登录用户
     * @return 新增的车库记录
     */
    @PostMapping("/{teamId}/garage")
    public ResponseResult<TeamGarageVO> addBikeToGarage(
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody AddBikeToGarageDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamGarageService.addBikeToGarage(teamId, dto.getBikeId(), user));
    }

    /**
     * 从车队车库中移除单车
     * <p>
     * 车主本人、队长或管理员可操作。
     *
     * @param teamId 车队ID
     * @param bikeId 单车ID
     * @param user   当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/garage/{bikeId}")
    public ResponseResult<Void> removeBikeFromGarage(
            @PathVariable("teamId") Long teamId,
            @PathVariable("bikeId") Long bikeId,
            @AuthenticationPrincipal User user) {
        teamGarageService.removeBikeFromGarage(teamId, bikeId, user);
        return ResponseResult.success();
    }

    /**
     * 申请加入车队
     * <p>
     * 需要登录，提交申请理由后等待队长/管理员审核。
     *
     * @param teamId 车队ID
     * @param body   包含 reason 字段
     * @param user   当前登录用户
     * @return 操作结果
     */
    @PostMapping("/{teamId}/join")
    public ResponseResult<Void> applyJoin(@PathVariable("teamId") Long teamId,
                                           @Valid @RequestBody ApplyJoinDTO dto,
                                           @AuthenticationPrincipal User user) {
        teamApplicationService.applyJoinTeam(teamId, dto.getReason(), user);
        return ResponseResult.success();
    }

    /**
     * 获取车队待审核申请列表
     * <p>
     * 仅队长/管理员可查看。需要登录。
     *
     * @param teamId 车队ID
     * @param user   当前登录用户
     * @return 申请列表
     */
    @GetMapping("/{teamId}/applications")
    public ResponseResult<List<PelotonTeamMemberVO>> getPendingApplications(
            @PathVariable("teamId") Long teamId,
            @AuthenticationPrincipal User user) {
        return ResponseResult.success(teamApplicationService.getPendingApplications(teamId, user));
    }

    /**
     * 同意加入申请
     * <p>
     * 仅队长/管理员可操作。
     *
     * @param id   申请记录ID
     * @param user 当前登录用户
     * @return 操作结果
     */
    @PostMapping("/applications/{id}/approve")
    public ResponseResult<Void> approveJoin(@PathVariable("id") Long id,
                                             @AuthenticationPrincipal User user) {
        teamApplicationService.approveJoin(id, user);
        return ResponseResult.success();
    }

    /**
     * 拒绝加入申请
     * <p>
     * 仅队长/管理员可操作。
     *
     * @param id   申请记录ID
     * @param user 当前登录用户
     * @return 操作结果
     */
    @PostMapping("/applications/{id}/reject")
    public ResponseResult<Void> rejectJoin(@PathVariable("id") Long id,
                                            @AuthenticationPrincipal User user) {
        teamApplicationService.rejectJoin(id, user);
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
        teamMemberService.leaveTeam(teamId, user);
        return ResponseResult.success();
    }

    /**
     * 解散车队
     * <p>
     * 仅车队创建者可操作。删除所有成员记录并移除车队。
     *
     * @param teamId 车队ID
     * @param user 当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}")
    public ResponseResult<Void> disbandTeam(@PathVariable("teamId") Long teamId,
                                            @AuthenticationPrincipal User user) {
        pelotonTeamService.disbandTeam(teamId, user);
        return ResponseResult.success();
    }

    /**
     * 队长退出车队（需指定新队长）
     * <p>
     * 仅车队创建者可操作。将队长身份转让给指定成员后，原队长退出车队。
     *
     * @param teamId          车队ID
     * @param body            包含 newCaptainUserId 字段
     * @param user            当前登录用户（原队长）
     * @return 操作结果
     */
    @PutMapping("/{teamId}/captain/leave")
    public ResponseResult<Void> captainLeaveTeam(@PathVariable("teamId") Long teamId,
                                                  @Valid @RequestBody CaptainLeaveDTO dto,
                                                  @AuthenticationPrincipal User user) {
        teamMemberService.captainLeaveTeam(teamId, dto.getNewCaptainUserId(), user);
        return ResponseResult.success();
    }

    /**
     * 设为管理员
     *
     * @param teamId     车队ID
     * @param targetUserId 目标用户ID
     * @param admin      当前登录用户
     * @return 操作结果
     */
    @PutMapping("/{teamId}/members/{targetUserId}/admin")
    public ResponseResult<Void> setAdmin(@PathVariable("teamId") Long teamId,
                                          @PathVariable("targetUserId") Long targetUserId,
                                          @AuthenticationPrincipal User admin) {
        teamMemberService.setAdmin(teamId, targetUserId, admin);
        return ResponseResult.success();
    }

    /**
     * 撤销管理员
     *
     * @param teamId     车队ID
     * @param targetUserId 目标用户ID
     * @param admin      当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/members/{targetUserId}/admin")
    public ResponseResult<Void> revokeAdmin(@PathVariable("teamId") Long teamId,
                                             @PathVariable("targetUserId") Long targetUserId,
                                             @AuthenticationPrincipal User admin) {
        teamMemberService.revokeAdmin(teamId, targetUserId, admin);
        return ResponseResult.success();
    }

    /**
     * 请离车队
     *
     * @param teamId     车队ID
     * @param targetUserId 目标用户ID
     * @param admin      当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{teamId}/members/{targetUserId}/kick")
    public ResponseResult<Void> kickMember(@PathVariable("teamId") Long teamId,
                                            @PathVariable("targetUserId") Long targetUserId,
                                            @AuthenticationPrincipal User admin) {
        teamMemberService.kickMember(teamId, targetUserId, admin);
        return ResponseResult.success();
    }

    /**
     * 修改成员骑行标签
     *
     * @param teamId       车队ID
     * @param targetUserId 目标用户ID
     * @param body         包含 tag 字段
     * @param admin        当前登录用户
     * @return 操作结果
     */
    @PutMapping("/{teamId}/members/{targetUserId}/tag")
    public ResponseResult<Void> updateMemberTag(@PathVariable("teamId") Long teamId,
                                                 @PathVariable("targetUserId") Long targetUserId,
                                                 @Valid @RequestBody UpdateMemberTagDTO dto,
                                                 @AuthenticationPrincipal User admin) {
        teamMemberService.updateMemberTag(teamId, targetUserId, dto.getTag(), admin);
        return ResponseResult.success();
    }
}
