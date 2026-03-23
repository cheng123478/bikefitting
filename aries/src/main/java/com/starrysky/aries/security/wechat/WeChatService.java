package com.starrysky.aries.security.wechat;

import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.user.schema.WeChatSessionResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WeChatService
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/20 12:44
 */
@Slf4j
@Service
public class WeChatService {

    private static final String JSCODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String OAUTH2_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

    @Value("${wechat.mini-program.app-id:}")
    private String miniAppId;
    @Value("${wechat.mini-program.app-secret:}")
    private String miniAppSecret;

    @Value("${wechat.web.app-id:}")
    private String webAppId;
    @Value("${wechat.web.app-secret:}")
    private String webAppSecret;
    @Resource
    private RestTemplate restTemplate;

    /**
     * 获取微信小程序用户的唯一标识
     */
    public String getUnionIdFromMiniProgram(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BizException("无效的微信授权码");
        }
        if (!StringUtils.hasText(miniAppId) || !StringUtils.hasText(miniAppSecret)) {
            throw new BizException("后端未配置微信小程序AppID与Secret");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(JSCODE2SESSION_URL)
                .queryParam("appid", miniAppId)
                .queryParam("secret", miniAppSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code");

        log.debug("发起微信小程序登录请求，code: {}", code);
        
        WeChatSessionResponseVO response;
        try {
            response = restTemplate.getForObject(builder.toUriString(), WeChatSessionResponseVO.class);
        } catch (Exception e) {
            log.error("调用微信接口失败", e);
            throw new BizException("调用微信接口异常，请稍后重试");
        }

        if (response == null) {
            log.error("微信接口返回空响应");
            throw new BizException("微信登录异常：无响应");
        }

        // 微信API成功时可能不返回errcode或者返回0
        if (response.getErrCode() != null && response.getErrCode() != 0) {
            log.warn("微信登录失败: errCode: {}, errMsg: {}", response.getErrCode(), response.getErrMsg());
            throw new BizException("微信登录失败: " + response.getErrMsg());
        }

        // 提取标识并返回，优先使用unionId，如果没有则降级使用openId
        String identifier = StringUtils.hasText(response.getUnionid()) ? response.getUnionid() : response.getOpenid();
        
        if (!StringUtils.hasText(identifier)) {
            log.error("微信登录未返回有效的标识符，响应: openid={}, unionid={}", response.getOpenid(), response.getUnionid());
            throw new BizException("微信登录异常：无法获取用户标识");
        }

        return identifier;
    }

    /**
     * 获取微信网页（扫码/公众号授权）用户的唯一标识
     */
    public String getUnionIdFromWeb(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BizException("无效的微信授权码");
        }
        if (!StringUtils.hasText(webAppId) || !StringUtils.hasText(webAppSecret)) {
            throw new BizException("后端未配置微信网页端AppID与Secret");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(OAUTH2_ACCESS_TOKEN_URL)
                .queryParam("appid", webAppId)
                .queryParam("secret", webAppSecret)
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code");

        log.debug("发起微信网页授权接口请求，code: {}", code);
        
        WeChatSessionResponseVO response;
        try {
            response = restTemplate.getForObject(builder.toUriString(), WeChatSessionResponseVO.class);
        } catch (Exception e) {
            log.error("调用微信接口失败", e);
            throw new BizException("调用微信网页授权接口异常，请稍后重试");
        }

        if (response == null) {
            log.error("微信接口返回空响应");
            throw new BizException("微信网页授权登录异常：无响应");
        }

        if (response.getErrCode() != null && response.getErrCode() != 0) {
            log.warn("微信网页授权登录失败: errCode: {}, errMsg: {}", response.getErrCode(), response.getErrMsg());
            throw new BizException("微信登录失败: " + response.getErrMsg());
        }

        String identifier = StringUtils.hasText(response.getUnionid()) ? response.getUnionid() : response.getOpenid();
        
        if (!StringUtils.hasText(identifier)) {
            log.error("微信网页授权未返回有效的标识符，响应: openid={}, unionid={}", response.getOpenid(), response.getUnionid());
            throw new BizException("微信登录异常：无法获取用户标识");
        }

        return identifier;
    }
}
