package com.starrysky.aries.user.service;

import cn.hutool.core.lang.Snowflake;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.membership.event.UserRegisteredEvent;
import com.starrysky.aries.sms.service.SmsService;
import com.starrysky.aries.user.repository.UserRepository;
import com.starrysky.aries.user.schema.UserRequest;
import com.starrysky.aries.util.Constant;
import com.starrysky.aries.util.EncryptUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * UserService
 * 用户业务类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Service
@Transactional
@Slf4j
public class UserService {
    @Resource
    private UserRepository userRepository;
    @Resource
    private SmsService smsService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private Snowflake snowflake;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 注册和登录，用户手机短信注册和登录
     * 1. 根据手机号若检索不到，则创建新用户
     * 2. 若检索到，则判断用户是否已注销
     * 如已注销且未超过7天，则直接恢复，否则要求创建新用户
     * 3. 统一进行状态检查（如自动解锁）
     *
     * @param phone 手机号
     * @return 登录成功后的用户
     */
    public User registAndLogin(String phone) {
        return processLoginOrRegister(phone, false);
    }

    /**
     * 注册和登录，用户微信注册和登录
     * 1. 根据微信unionId若检索不到，则创建新用户
     * 2. 若检索到，则判断用户是否已注销
     * 如已注销且未超过7天，则直接恢复，否则要求创建新用户
     * 3. 统一进行状态检查（如自动解锁）
     *
     * @param unionId 微信unionId
     * @return 登录成功后的用户
     */
    public User registAndLoginByWeChat(String unionId) {
        return processLoginOrRegister(unionId, true);
    }

    /**
     * 处理登录或者注册的公共逻辑
     */
    private User processLoginOrRegister(String identifier, boolean isWeChat) {
        User user = isWeChat ? userRepository.findByUnionId(identifier) : userRepository.findByPhone(identifier);

        if (user == null) {
            // --- 场景 A：纯新用户 ---
            user = handleNewRegistration(identifier, isWeChat);
            return userRepository.save(user);
        }

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            // --- 场景 B：已注销用户（根据时间判断恢复还是重置） ---
            user = handleDeletedUser(user, identifier, isWeChat);
        } else {
            // --- 场景 C：正常活跃用户 ---
            log.debug("老用户正常登录 | 标识: {}", identifier);
        }

        // 统一进行状态检查（如自动解锁）
        processUserLockStatus(user);

        // 统一保存并返回
        return userRepository.saveAndFlush(user);
    }

    /**
     * 处理纯新用户注册逻辑
     *
     * @param identifier 手机号或unionId
     * @return 新用户
     */
    private User handleNewRegistration(String identifier, boolean isWeChat) {
        synchronized (identifier.intern()) {
            User exist = isWeChat ? userRepository.findByUnionId(identifier) : userRepository.findByPhone(identifier);
            if (exist != null) {
                return exist; // 并发情况下如果已被创建，则直接返回
            }
            User newUser = createNewUser(identifier, isWeChat);
            // 抛出注册事件，携带邀请码
            publishRegistrationEvent(newUser);
            return newUser;
        }
    }

    /**
     * 注册新用户时，如果是受邀请的，则处发起事件，引出后续处理，后续处理在MembershipEventListener里
     *
     * @param newUser 新用户
     */
    private void publishRegistrationEvent(User newUser) {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
                String inviterCode = request.getHeader("X-Inviter-Code");
                if (StringUtils.hasText(inviterCode)) {
                    eventPublisher.publishEvent(new UserRegisteredEvent(newUser.getId(), inviterCode));
                }
            }
        } catch (Exception e) {
            log.error("抛出注册事件失败", e);
        }
    }

    /**
     * 处理注销状态用户的逻辑
     *
     * @param user       旧用户
     * @param identifier 手机号或unionId
     * @return 新用户
     */
    private User handleDeletedUser(User user, String identifier, boolean isWeChat) {
        long days = ChronoUnit.DAYS.between(user.getDeleteTime(), LocalDateTime.now());

        if (days < Constant.EXPIRED_ACCOUNT_CLEANUP_DAYS) {
            // 7天内：直接恢复
            user.setIsDeleted(false);
            user.setDeleteTime(null);
            log.info("注销期内登录，账号自动恢复 | 标识: {}", identifier);
            return user;
        } else {
            // 超过7天：旧号解绑创建新账号，微信则释放unionId
            log.info("注销超期，旧号解绑并创建新账号 | 标识: {}", identifier);
            if (isWeChat) {
                user.setUnionId(null);
            }
            if (StringUtils.hasText(user.getPhone())) {
                user.setOldPhone(identifier);
                user.setPhone(null);
            }
            userRepository.saveAndFlush(user); // 必须先保存旧号的变更，释放唯一索引

            return createNewUser(identifier, isWeChat);
        }
    }

    /**
     * 处理用户锁定状态及自动解锁
     *
     * @param user 用户
     */
    private void processUserLockStatus(User user) {
        if (user.isAccountNonLocked()) {
            return;
        }
        // 自动解锁检查：如果是明天（及以后）或者是 24 小时后
        if (user.getLockTime() != null && LocalDate.now().isAfter(user.getLockTime().toLocalDate())) {
            user.setAccountNonLocked(true);
            user.setLoginFailureCount(0);
            user.setLockTime(null);
            log.info("用户账号满足时间条件，自动解锁 | 手机号: {}", user.getPhone());
        } else {
            throw new LockedException("您的账户已被锁定，请明天再试！");
        }
    }

    private User createNewUser(String identifier, boolean isWeChat) {
        User user = new User();
        user.setId(snowflake.nextId());

        String name = identifier.length() >= 4 ? identifier.substring(identifier.length() - 4) : identifier;
        if (isWeChat) {
            user.setUnionId(identifier);
            user.setName("微信用户" + name);
        } else {
            user.setPhone(identifier);
            user.setName("骑士" + name);
        }

        user.setRegistrationTime(LocalDateTime.now());
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        user.setLoginFailureCount(0);
        user.setIsDeleted(false);
        return user;
    }

    /**
     * 通过短信设置密码
     *
     * @param user     用户
     * @param password 密码
     */
    public void addPassword(User user, String code, String password) throws Exception {
        if (!smsService.validateSmsCode(user.getPhone(), code)) {
            throw new BizException("短信验证码错误");
        }
        User userDb = userRepository.findById(user.getId()).orElseThrow(() -> new BizException("未检索到用户"));
        String decrypt = EncryptUtil.decrypt(password);
        userDb.setPassword(passwordEncoder.encode(decrypt));
        userRepository.save(userDb);
    }

    /**
     * 通过密码修改密码
     *
     * @param user        Security框架中的用户
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void editPassword(User user, String oldPassword, String newPassword) throws Exception {
        User userDb = userRepository.findById(user.getId()).orElseThrow(() -> new BizException("未检索到用户"));
        if (passwordEncoder.matches(EncryptUtil.decrypt(oldPassword), userDb.getPassword())) {
            userDb.setPassword(passwordEncoder.encode(EncryptUtil.decrypt(newPassword)));
            userRepository.save(userDb);
            log.info("用户修改密码成功 | 用户: {}", user.getId());
        } else {
            log.warn("修改密码失败：旧密码错误 | 用户: {}", user.getId());
            throw new BizException(500, "旧密码错误");
        }
    }

    /**
     * 修改手机号
     *
     * @param oldPhone 旧手机号
     * @param newPhone 新手机号
     * @param password 密码
     * @param code     验证
     */
    public void editPhone(String oldPhone, String newPhone, String code, String password) throws Exception {
        // 1. 基础校验
        User newUser = userRepository.findByPhone(newPhone);
        if (newUser != null) {
            throw new BizException("该手机号已被使用");
        }

        // 2. 校验验证码
        // 验证码是发给newPhone的，不是发给oldPhone的
        if (!smsService.validateSmsCode(newPhone, code)) {
            throw new BizException("验证码错误");
        }

        // 3. 校验密码
        User user = userRepository.findByPhone(oldPhone);
        if (!passwordEncoder.matches(EncryptUtil.decrypt(password), user.getPassword())) {
            throw new BizException("登录密码错误");
        }

        // 4. 执行换绑
        // 如果你有 old_phone 逻辑，记得把旧号移过去
        user.setOldPhone(user.getPhone());
        user.setPhone(newPhone);
        userRepository.save(user);
    }

    /**
     * 修改用户信息
     *
     * @param user 用户
     */
    public void editUser(User user) {
        userRepository.save(user);
    }

    /**
     * 修改用户信息，只修改昵称、个人简介、城市等信息
     *
     * @param id            用户id
     * @param profileUpdate 用户信息
     */
    public void editUser(Long id, UserRequest.ProfileUpdate profileUpdate) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("未检索到用户"));
        if (StringUtils.hasText(profileUpdate.name())) {
            user.setName(profileUpdate.name());
        }
        if (profileUpdate.birthday() != null && profileUpdate.birthday().isBefore(LocalDate.now())) {
            user.setBirthday(profileUpdate.birthday());
        }
        if (profileUpdate.gender() != null) {
            user.setGender(profileUpdate.gender());
        }
        if (StringUtils.hasText(profileUpdate.location())) {
            user.setLocation(profileUpdate.location());
        }
        if (StringUtils.hasText(profileUpdate.introduction())) {
            user.setIntroduction(profileUpdate.introduction());
        }
        userRepository.save(user);
    }

    /**
     * 修改用户头像
     *
     * @param phone  手机号
     * @param avatar 头像
     */
    public void editAvatar(String phone, String avatar) {
        User user = userRepository.findByPhone(phone);
        user.setAvatar(avatar);
        userRepository.save(user);
    }

    /**
     * 根据手机号获取用户信息
     *
     * @param phone 手机号
     * @return 用户信息
     */
    @Transactional(readOnly = true)
    public User getOne(String phone) {
        return userRepository.findByPhone(phone);
    }

    /**
     * 注销账户
     * 不删除数据库记录，只标记为已注销
     *
     * @param user 当前用户
     */
    public void deleteAccount(User user) {
        User userDb = userRepository.findById(user.getId()).orElseThrow(() -> new BizException("未检索到用户"));
        // 标记为已注销
        userDb.setIsDeleted(true);
        userDb.setDeleteTime(LocalDateTime.now());
        userRepository.save(userDb);
    }

}
