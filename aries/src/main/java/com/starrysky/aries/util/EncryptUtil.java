package com.starrysky.aries.util;

import com.starrysky.aries.exception.BizException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 用于数据加密、解密，比如登录、注册、修改密码
 *
 * @author adi
 * @version 1.0
 * @since 2026/1/24 21:17
 */
@Slf4j
@Component
public class EncryptUtil {
    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    @Value("${aries.security.rsa.private-key}")
    private String configPrivateKey;
    private static String PRIVATE_KEY;

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(configPrivateKey)) {
            PRIVATE_KEY = configPrivateKey;
        } else {
            log.error("RSA解密私钥未配置！");
        }
    }

    /**
     * 私钥解密
     *
     * @param encryptedBase64Str 前端传来的加密串
     * @return 明文
     */
    public static String decrypt(String encryptedBase64Str) throws Exception {
        if (!StringUtils.hasText(encryptedBase64Str)) {
            throw new BizException("密码不能为空");
        }
        // 1. 解析私钥
        byte[] keyBytes = Base64.getDecoder().decode(PRIVATE_KEY);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        // 2. 解密
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64Str);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        // 1. 初始化生成器
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048); // 建议使用 2048 位，1024 位已不再安全

        // 2. 生成密钥对
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        // 3. 转换为 Base64 字符串
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        // 4. 打印结果
        System.out.println("====== 请复制下面的公钥 (给前端 Vue 用) ======");
        System.out.println(publicKeyString);
        System.out.println("\n");
        System.out.println("====== 请复制下面的私钥 (后端 SpringBoot 保密) ======");
        System.out.println(privateKeyString);
    }

}
