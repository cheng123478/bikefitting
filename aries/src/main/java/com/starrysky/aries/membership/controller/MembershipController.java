package com.starrysky.aries.membership.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.util.InviteCodeUtil;
import com.starrysky.aries.util.ResponseResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 会员特权控制器
 *
 * @author adi
 */
@RestController
@RequestMapping("/membership")
public class MembershipController {

    /**
     * 获取当前用户的专属邀请码
     * 
     * @param user 当前登录用户
     * @return 邀请码及短链参数
     */
    @GetMapping("/invite-code")
    public ResponseResult<Map<String, String>> getInviteCode(@AuthenticationPrincipal User user) {
        String code = InviteCodeUtil.encode(user.getId());
        Map<String, String> data = new HashMap<>();
        data.put("inviteCode", code);
        // 前端拼装示例: https://www.bikefitting.com/register?inviter=xxx
        data.put("queryParam", "inviter=" + code);
        return ResponseResult.success(data);
    }
}
