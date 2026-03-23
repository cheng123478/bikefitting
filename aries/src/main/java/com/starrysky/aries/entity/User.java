package com.starrysky.aries.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * User
 * 用户实体类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Entity
@Table(name = "usr_user")
@Comment("用户表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1821407981899427631L;
    @Id
    @Column(columnDefinition = "BIGINT", length = 20, nullable = false)
    @JsonSerialize(using = ToStringSerializer.class)
    @Comment("主键，账号")
    private Long id;
    @Column(length = 20, unique = true)
    @Comment("电话")
    private String phone;
    @Column(length = 100)
    @Comment("密码")
    private String password;
    @Column(length = 50)
    @Comment("昵称")
    private String name;
    @Column(length = 50)
    @Comment("介绍")
    private String introduction;
    @Column(length = 500)
    @Comment("个性头像")
    private String avatar;
    @Temporal(TemporalType.DATE)
    @Comment("生日")
    private LocalDate birthday;
    @Column(length = 64, unique = true)
    @Comment("微信unionId")
    private String unionId;
    @Column(length = 100, unique = true)
    @Comment("邮箱")
    private String email;
    @Column(length = 10)
    @Comment("性别")
    private Byte gender;
    @Column(length = 100)
    @Comment("地址")
    private String location;
    @Temporal(TemporalType.TIMESTAMP)
    @Comment("注册时间")
    private LocalDateTime registrationTime;
    @Comment("1天内登陆失败次数")
    private int loginFailureCount;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("注销时间")
    private LocalDateTime deleteTime;
    @Comment("是否注销：false-未注销，true-已注销")
    private Boolean isDeleted = false;
    @Column(length = 20)
    @Comment("注销前的手机号，用于15天内限制重新注册")
    private String oldPhone;

    @Override
    public String getUsername() {
        return this.id.toString();
    }
    @Comment("账号是否过期")
    private boolean accountNonExpired;
    @Comment("账号是否锁定")
    private boolean accountNonLocked;
    @Comment("凭证是否过期")
    private boolean credentialsNonExpired;
    @Comment("是否可用")
    private boolean enabled;
    @Temporal(TemporalType.TIMESTAMP)
    @Comment("锁定时间")
    private LocalDateTime lockTime;


    @Transient
    private Collection<? extends GrantedAuthority> authorities;

}

