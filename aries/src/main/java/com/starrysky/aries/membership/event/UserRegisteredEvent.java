package com.starrysky.aries.membership.event;

import org.springframework.context.ApplicationEvent;

/**
 * 新用户注册成功事件
 * 
 * @author adi
 */
public class UserRegisteredEvent extends ApplicationEvent {

    private final Long newUserId;
    private final String inviterCode;

    public UserRegisteredEvent(Long newUserId, String inviterCode) {
        // Source should be the object that published the event, we can just pass the newUserId as source
        super(newUserId);
        this.newUserId = newUserId;
        this.inviterCode = inviterCode;
    }

    public Long getNewUserId() {
        return newUserId;
    }

    public String getInviterCode() {
        return inviterCode;
    }
}
