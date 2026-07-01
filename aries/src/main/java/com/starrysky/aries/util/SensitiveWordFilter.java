package com.starrysky.aries.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 敏感词/违禁词过滤器
 * <p>
 * 启动时从 classpath:sensitive-words.txt 加载词库，
 * 提供内容校验和过滤能力。
 * 词库按行分隔，# 开头的行为注释。
 * </p>
 *
 * @author adice
 * @since 2026-06-29
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    private static final String WORD_LIST_PATH = "sensitive-words.txt";

    /** 已加载的敏感词集合 */
    private Set<String> sensitiveWords = Collections.emptySet();

    /** 用于快速扫描的预编译 Pattern（OR 拼接） */
    private Pattern scanPattern;

    @PostConstruct
    public void init() {
        Set<String> words = new HashSet<>();
        try {
            ClassPathResource resource = new ClassPathResource(WORD_LIST_PATH);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // 跳过空行和注释行
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    words.add(line.toLowerCase());
                }
            }
            this.sensitiveWords = Collections.unmodifiableSet(words);
        } catch (Exception e) {
            log.error("敏感词库加载失败，将使用空词库", e);
            this.sensitiveWords = Collections.emptySet();
        }

        // 预编译扫描正则
        buildScanPattern();
    }

    /**
     * 检查文本是否包含敏感词
     *
     * @param text 待检查文本
     * @return true 如果包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty() || sensitiveWords.isEmpty()) {
            return false;
        }
        if (scanPattern == null) {
            buildScanPattern();
        }
        if (scanPattern != null) {
            return scanPattern.matcher(text.toLowerCase()).find();
        }
        return false;
    }

    /**
     * 查找文本中命中的敏感词
     *
     * @param text 待检查文本
     * @return 命中的第一个敏感词，未命中返回 null
     */
    public String findFirstHit(String text) {
        if (text == null || text.isEmpty() || sensitiveWords.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        for (String word : sensitiveWords) {
            if (lower.contains(word)) {
                return word;
            }
        }
        return null;
    }

    /**
     * 校验文本不包含敏感词，如命中则抛出业务异常
     *
     * @param text       待校验文本
     * @param fieldName  字段名称（用于错误提示）
     */
    public void assertClean(String text, String fieldName) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String hit = findFirstHit(text);
        if (hit != null) {
            throw new com.starrysky.aries.exception.BizException(
                    400, "文案内含敏感词，请修改后再进行发布！");
        }
    }

    // ============================ 私有方法 ============================

    private void buildScanPattern() {
        if (sensitiveWords.isEmpty()) {
            this.scanPattern = null;
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String word : sensitiveWords) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(Pattern.quote(word));
        }
        this.scanPattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
