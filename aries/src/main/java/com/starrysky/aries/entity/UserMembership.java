package com.starrysky.aries.entity;

import com.starrysky.aries.membership.enums.MemberTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户会员特权与资产账户
 * 统管用户的会员状态(PRO)以及虚拟资产(如令牌)
 *
 * @author adi
 */
@Entity
@Table(name = "usr_membership")
@Comment("用户会员特权与账户")
@Setter
@Getter
@EntityListeners(AuditingEntityListener.class)
public class UserMembership {

    @Id
    @Comment("用户ID (1对1关联usr_user)")
    private Long userId;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    @Comment("会员类型 (0:免费, 1:PRO)")
    private MemberTypeEnum memberType = MemberTypeEnum.FREE;

    @Comment("PRO会员过期时间")
    private LocalDateTime proExpireTime;

    @Comment("累计成功邀请人数 (用于7-7裂变规则)")
    @Column(nullable = false)
    private Integer totalInvites = 0;

    @Comment("当前永久令牌余额 (如邀请获得的令牌)")
    @Column(nullable = false)
    private Integer tokenBalance = 0;

    @Comment("最后一次使用系统免费令牌的日期 (用于判定每2天发放逻辑)")
    private LocalDate lastTokenRefreshDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Comment("账户创建时间")
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column
    @Comment("账户最后更新时间")
    private LocalDateTime updateTime;

    /**
     * 判断当前是否为有效的 PRO 会员
     */
    public boolean isProActive() {
        return memberType == MemberTypeEnum.PRO && proExpireTime != null && proExpireTime.isAfter(LocalDateTime.now());
    }
}
