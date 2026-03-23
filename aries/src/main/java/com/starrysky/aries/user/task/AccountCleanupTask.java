package com.starrysky.aries.user.task;

import com.starrysky.aries.user.repository.UserRepository;
import com.starrysky.aries.util.Constant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务，定时清理已注销的用户
 * 设置phone为空
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/28 22:23
 */
@Slf4j
@Component
public class AccountCleanupTask {
    @Resource
    private UserRepository userRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredAccounts() {
        log.info("开始执行注销账号清理任务...");
        // 计算截止时间：当前时间 - 7天
        LocalDateTime deadline = LocalDateTime.now().minusDays(Constant.EXPIRED_ACCOUNT_CLEANUP_DAYS);
        // 执行数据库更新
        // 把所有注销时间早于 deadline 的用户的手机号设为 NULL
        int count = userRepository.anonymizeExpiredAccounts(deadline);
        if (count > 0) {
            log.info("清理完成，共抹除 {} 个过期注销账号的敏感信息", count);
        } else {
            log.info("清理完成，暂无过期账号需要处理");
        }
    }

    // 每次处理 1000 条，防止长事务
    private static final int BATCH_SIZE = 1000;

    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void batchCleanExpiredAccounts() {
        log.info(">>> 开始执行注销账号清理任务...");
        long startTime = System.currentTimeMillis();

        // 计算截止时间：当前时间 - 7天
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        int totalCount = 0;
        boolean hasMore = true;

        while (hasMore) {
            try {
                // 1. 查出一批 ID
                List<String> ids = userRepository.findIdsToClean(deadline, PageRequest.of(0, BATCH_SIZE));
                if (CollectionUtils.isEmpty(ids)) {
                    hasMore = false; // 没数据了，退出循环
                } else {
                    // 2. 根据 ID 批量更新
                    userRepository.cleanPhoneBatchByIds(ids);
                    int count = ids.size();
                    totalCount += count;
                    log.info("已清理批次，数量: {}", count);

                    // 3. 稍微休眠一下，释放数据库资源，避免 CPU 飙升
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                log.error("清理任务执行中发生异常，本轮终止", e);
                hasMore = false; // 遇到异常终止循环，避免死循环报错
            }
        }
        log.info("<<< 清理任务结束，共处理 {} 个账号，耗时 {} ms", totalCount, System.currentTimeMillis() - startTime);
    }
}

/**
 * 优化
 * #### 1. 必做：加联合索引 (索引优化)
 * 虽然 `phone` 有唯一索引，但我们的查询条件是 `is_deleted` 和 `update_time`。
 * 请务必在数据库添加一个**联合索引**：
 *
 * ```sql
 * -- MySQL 语法
 * ALTER TABLE `user` ADD INDEX `idx_deleted_time` (`is_deleted`, `update_time`);
 * ```
 * **效果**：有了这个索引，数据库不需要扫描全表，而是直接定位到 `is_deleted=1` 的那部分数据。因为注销的用户通常占极少数，扫描范围瞬间缩小 99%。
 *
 * #### 2. 必做：分批处理 (代码优化)
 * **永远不要在生产环境一次性更新几万条数据！**
 * 我们要采用 **“蚂蚁搬家”** 的策略：每次只查出 1000 个待清理的 ID，然后根据 ID 更新。
 *
 * **优化后的 Task 代码：**
 *
 * ```java
 * @Scheduled(cron = "0 0 3 * * ?")
 * public void cleanExpiredAccounts() {
 *     log.info("开始执行注销账号清理任务...");
 *     LocalDateTime deadline = LocalDateTime.now().minusDays(7);
 *
 *     int batchSize = 1000; // 每次处理 1000 条
 *     boolean hasMore = true;
 *
 *     while (hasMore) {
 *         // 第一步：只查 ID（利用覆盖索引，速度极快）
 *         // SELECT id FROM user WHERE is_deleted = 1 AND update_time < :deadline AND phone IS NOT NULL LIMIT 1000
 *         List<String> ids = userRepository.findExpiredUserIds(deadline, PageRequest.of(0, batchSize));
 *
 *         if (ids.isEmpty()) {
 *             hasMore = false;
 *         } else {
 *             // 第二步：根据 ID 批量更新
 *             // UPDATE user SET phone = NULL WHERE id IN (...)
 *             userRepository.clearPhoneByIds(ids);
 *
 *             log.info("已清理一批账号，数量: {}", ids.size());
 *
 *             // 休息一下，防止占用数据库 IO 太久
 *             try { Thread.sleep(100); } catch (InterruptedException e) {}
 *         }
 *     }
 *     log.info("清理任务结束");
 * }
 * ```
 *
 * **Repository 配合代码：**
 *
 * ```java
 * public interface UserRepository extends JpaRepository<User, String> {
 *
 *     // 1. 查 ID (利用 Pageable 做 Limit)
 *     @Query("SELECT u.id FROM User u WHERE u.isDeleted = true AND u.updateTime < :deadline AND u.phone IS NOT NULL")
 *     List<String> findExpiredUserIds(@Param("deadline") LocalDateTime deadline, Pageable pageable);
 *
 *     // 2. 根据 ID 更新
 *     @Modifying
 *     @Transactional
 *     @Query("UPDATE User u SET u.phone = NULL, u.oldPhone = NULL WHERE u.id IN :ids")
 *     void clearPhoneByIds(@Param("ids") List<String> ids);
 * }
 * ```
 *
 * **优点**：
 * *   每次只锁 1000 行，几乎不影响线上业务。
 * *   即使有一亿用户，因为有索引且分批处理，性能也极其平滑。
 *
 * #### 3. 进阶：冷热数据分离 (架构优化)
 * 如果你的用户量真的到了亿级，你会发现 `user` 表即使不查也很慢。
 * 这时候标准的做法是 **“物理转移”**：
 *
 * 当用户点击注销时（或者7天到期时）：
 * 1.  开启事务。
 * 2.  将该用户的数据 `INSERT` 到一张叫 `user_history` (历史归档表) 中。
 * 3.  从 `user` (主表) 中 `DELETE` 该记录。
 *
 * **结果**：
 * *   `user` 表里永远只有**活跃用户**，数据量小，查询飞快。
 * *   `user_history` 表里存放注销用户，哪怕有几亿条数据，平时也没人查它，慢就慢点。
 */

