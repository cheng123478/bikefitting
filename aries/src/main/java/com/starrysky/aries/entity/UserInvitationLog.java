package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户邀请注册记录
 *
 * @author adi
 */
@Entity
@Table(name = "usr_invitation_log", indexes = {
        @Index(name = "idx_inviter_id", columnList = "inviterId"),
        @Index(name = "idx_invitee_id", columnList = "inviteeId", unique = true)
})
@Comment("用户邀请注册记录")
@Setter
@Getter
@EntityListeners(AuditingEntityListener.class)
public class UserInvitationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("邀请人(老用户)ID")
    @Column(nullable = false)
    private Long inviterId;

    @Comment("被邀请人(新注册用户)ID")
    @Column(nullable = false, unique = true)
    private Long inviteeId;

    @Comment("是否已发放邀请奖励 (0:否, 1:是)")
    @Column(nullable = false)
    private Boolean rewardGranted = false;

    @Column(nullable = false, updatable = false)
    @Comment("记录创建时间")
    private LocalDateTime createTime;
}
