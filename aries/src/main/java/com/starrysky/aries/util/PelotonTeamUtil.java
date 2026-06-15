package com.starrysky.aries.util;

import com.starrysky.aries.entity.User;
import com.starrysky.aries.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PelotonTeamUtil
 * 破风社模块通用工具方法
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
public final class PelotonTeamUtil {

    private PelotonTeamUtil() {
        // 工具类禁止实例化
    }

    /**
     * 批量关联用户信息：从实体列表中提取 userId 集合并批量查询 User，返回 userId -> User 映射。
     *
     * @param entities        实体列表
     * @param userIdExtractor 从实体中提取 userId 的函数
     * @param userRepository  用户数据仓库
     * @param <T>             实体类型
     * @return userId -> User 映射（实体列表为空时返回空 Map）
     */
    public static <T> Map<Long, User> buildUserMap(List<T> entities,
                                                    Function<T, Long> userIdExtractor,
                                                    UserRepository userRepository) {
        Set<Long> userIds = entities.stream()
                .map(userIdExtractor)
                .collect(Collectors.toSet());
        return userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
    }
}
