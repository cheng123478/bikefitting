package com.starrysky.aries.lab.controller;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.lab.schema.LabDashboardVO;
import com.starrysky.aries.lab.service.LabService;
import com.starrysky.aries.util.ResponseResult;
import com.starrysky.aries.util.RidingPainType;
import jakarta.annotation.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实验室控制器
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/18 20:20
 */
@RestController
@RequestMapping("/lab")
public class LabController {
    @Resource
    private LabService labService;

    @GetMapping("/dashboard")
    public ResponseResult<LabDashboardVO> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseResult.success(labService.getDashboard(user.getId()));
    }

    /**
     * 获取骑行痛点选项
     *
     * @return 痛点选项列表
     */
    @GetMapping("/pain-options")
    public ResponseResult<List<Map<String, String>>> getPainOptions() {
        return ResponseResult.success(Arrays.stream(RidingPainType.values())
                .map(type -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("code", type.getCode());
                    map.put("description", type.getDescription());
                    return map;
                })
                .collect(Collectors.toList()));
    }
}
