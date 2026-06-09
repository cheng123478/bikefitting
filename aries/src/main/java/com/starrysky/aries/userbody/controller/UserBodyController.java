package com.starrysky.aries.userbody.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.userbody.schema.UserBodyComparisonVO;
import com.starrysky.aries.userbody.schema.UserBodyDTO;
import com.starrysky.aries.userbody.schema.UserBodyVO;
import com.starrysky.aries.userbody.service.UserBodyService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户身体档案控制器
 * <p>
 * 职责：
 * 1. 录入并存储用户的原始解剖尺寸与柔韧性数据。
 * 2. 返回基于生物力学分析的“骑士画像”与“素质评估”。
 * 3. 维护身体档案的历史版本。
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/6 17:02
 */
@RestController
@RequestMapping("/user-body")
public class UserBodyController {
    @Resource
    private UserBodyService userBodyService;

    /**
     * 【核心入口】录入新的身体测量数据
     * <p>
     * 业务逻辑：
     * 每次录入都会生成一份全新的快照记录（Versioned Storage）。
     * 后端会自动计算：猿臂比、股胫比、柔韧性得分、并生成专家建议。
     *
     * @param user 当前登录用户
     * @param dto  测量数据载体
     * @return 包含画像分析结果的 VO
     */
    @PostMapping("/save")
    public UserBodyVO save(@AuthenticationPrincipal User user, @Validated @RequestBody UserBodyDTO dto) {
        return userBodyService.saveUserBody(dto, user);
    }

    /**
     * 获取用户最新的身体档案（画像报告）
     * <p>
     * 用于在用户个人中心展示当前的身体素质评分、柔韧性等级及画像标签。
     *
     * @return 最新的画像数据
     */
    @GetMapping("/latest")
    public UserBodyVO getLatest(@AuthenticationPrincipal User user) {
        return userBodyService.getLatestUserBody(user.getId());
    }

    /**
     * 查询身体数据进化史
     * <p>
     * 用于前端展示用户身体状态的演变趋势（例如通过拉伸训练，胸椎灵活性分数的提升）。
     *
     * @return 历史档案列表（按时间倒序）
     */
    @GetMapping("/history")
    public Page<UserBodyVO> getHistory(@AuthenticationPrincipal User user,
                                       @PageableDefault(sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return userBodyService.getUserBodyHistory(user.getId(), pageable);
    }

    /**
     * 深度对比两条身体档案记录
     *
     * @param id1 记录ID 1
     * @param id2 记录ID 2
     * @return 包含雷达图数据和差异矩阵的对比报告
     */
    @GetMapping("/compare")
    public UserBodyComparisonVO compare(
            @RequestParam Long id1,
            @RequestParam Long id2) {
        return userBodyService.compareRecords(id1, id2);
    }
}
