package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * LoginRecord
 * 用户登录记录实体类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Entity
@Table(name = "usr_login_record")
@Setter
@Getter
public class UserLoginRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;
    @Comment("登录IP")
    private String ip;
    @Temporal(TemporalType.TIMESTAMP)
    @Comment("登录时间")
    private LocalDateTime loginTime;
    @Comment("登录状态，1-成功，2-失败")
    private int state;
    @Comment("登录手机号")
    private String phone;
}
