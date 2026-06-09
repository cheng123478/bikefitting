package com.starrysky.aries.security.jwt;

import com.starrysky.aries.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JwtUtil
 * JWT工具类，用于生成密钥，生成token，校验token
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Slf4j
@Component
public class JwtUtil {
    @Value("${aries.security.jwt.key-id}")
    private String keyId;
    @Value("${aries.security.jwt.public-key}")
    private String publicKey;
    @Value("${aries.security.jwt.private-key}")
    private String privateKey;

    private static final float ACCESS_TOKEN_EXPIRATION_MINUTES = 60 * 2;
    private static final float REFRESH_TOKEN_EXPIRATION_MINUTES = 60 * 24 * 7;

    private static String cachedKeyId;
    private static PrivateKey cachedPrivateKey;
    private static PublicKey cachedPublicKey;

    @PostConstruct
    public void init() {
        try {
            cachedKeyId = keyId;
            if (StringUtils.hasText(privateKey) && StringUtils.hasText(publicKey)) {
                cachedPrivateKey = new RsaJsonWebKey(JsonUtil.parseJson(privateKey)).getPrivateKey();
                cachedPublicKey = new RsaJsonWebKey(JsonUtil.parseJson(publicKey)).getPublicKey();
            } else {
                log.error("JWT公钥或私钥配置为空！请检查 application.yml 和环境变量。");
            }
        } catch (Exception e) {
            log.error("初始化JWT密钥失败", e);
        }
    }

    /**
     * 创建Access Token (短效，包含详细信息)
     *
     * @param user 用户信息
     */
    public static String createAccessToken(User user, String userAgent) {
        // isRefreshToken = false
        return buildToken(user, ACCESS_TOKEN_EXPIRATION_MINUTES, false, userAgent, null);
    }

    /**
     * 创建Refresh Token (长效，仅包含 ID)
     *
     * @param user 用户信息
     */
    public static String createRefreshToken(User user, String tokenVersion) {
        // isRefreshToken = true
        return buildToken(user, REFRESH_TOKEN_EXPIRATION_MINUTES, true, null, tokenVersion);
    }

    /**
     * 从Token中获取用户名(ID)
     * 适用AccessToken和RefreshToken
     *
     * @param token 令牌
     */
    public static String getIdFromToken(String token) throws InvalidJwtException {
        JwtClaims claims = parseToken(token);
        return claims.getClaimValue("id").toString();
    }

    /**
     * 从Token中获取Token版本
     * 适用RefreshToken
     *
     * @param token 令牌
     */
    public static String getTokenVersionFromToken(String token) throws InvalidJwtException {
        JwtClaims claims = parseToken(token);
        return claims.getClaimValue("token_version").toString();
    }

    /**
     * 从Token中获取UserAgent
     * 适用AccessToken
     *
     * @param token 令牌
     */
    public static String getUserAgentFromToken(String token) throws InvalidJwtException {
        JwtClaims claims = parseToken(token);
        return claims.getClaimValue("user_agent").toString();
    }

    /**
     * 通用Token构建逻辑
     *
     * @param user           用户信息
     * @param minutes        过期时间(分钟)
     * @param isRefreshToken 是否为刷新令牌 (如果是，载荷尽量小)
     */
    private static String buildToken(User user, float minutes, boolean isRefreshToken, String userAgent, String tokenVersion) {
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setSubject("aries_token");
        jwtClaims.setIssuer("com.starrysky");
        jwtClaims.setIssuedAtToNow();
        jwtClaims.setAudience("com.starrysky.*");
        jwtClaims.setGeneratedJwtId();
        jwtClaims.setExpirationTimeMinutesInTheFuture(minutes); // 过期时间

        // 核心身份标识：username
        jwtClaims.setClaim("id", user.getId().toString());
        // 如果是Access Token，可以多放点业务信息，避免频繁查库
        if (isRefreshToken) {
            jwtClaims.setClaim("token_version", tokenVersion);
        } else {
            jwtClaims.setClaim("userinfo", user.getId().toString() + "," + LocalDateTime.now());
            jwtClaims.setClaim("user_agent", userAgent);
            // 还可以放入 role 等权限信息
        }

        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.setKeyIdHeaderValue(cachedKeyId);
        jws.setPayload(jwtClaims.toJson());
        jws.setKey(cachedPrivateKey);

        try {
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            log.error("Token 创建失败", e);
            return null;
        }
    }

    /**
     * 通用解析方法，获取 Claims
     *
     * @param token 令牌
     */
    public static JwtClaims parseToken(String token) throws InvalidJwtException {
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setMaxFutureValidityInMinutes(60 * 24 * 30)
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer("com.starrysky")
                .setExpectedAudience("com.starrysky.*")
                .setVerificationKey(cachedPublicKey) // 使用缓存的公钥
                .build();
        return jwtConsumer.processToClaims(token);
    }

    public static void main(String[] args) {
        cachedKeyId = UUID.randomUUID().toString().replaceAll("-", "");
        try {
            RsaJsonWebKey jwk = RsaJwkGenerator.generateJwk(2048);
            jwk.setKeyId(cachedKeyId);
            jwk.setAlgorithm(AlgorithmIdentifiers.RSA_USING_SHA256);
            String publicKeyJson = jwk.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
            String privateKeyJson = jwk.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
            System.out.println("keyId: " + cachedKeyId);
            System.out.println("publicKey: " + publicKeyJson);
            System.out.println("privateKey: " + privateKeyJson);
        } catch (JoseException e) {
            log.debug("Key 创建失败", e);
        }
    }
}
