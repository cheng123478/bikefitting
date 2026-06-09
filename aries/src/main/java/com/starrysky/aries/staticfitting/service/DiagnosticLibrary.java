package com.starrysky.aries.staticfitting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 文案库加载器
 * 使用Jackson解析fitting_diagnostics.json里的内容
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/5 8:57
 */
@Component
@Slf4j
public class DiagnosticLibrary {

    /**
     * JSON文件中：
     * module_0：专注数据校验。
     * module_1-3：核心生物力学诊断（比例、下肢、上肢）。
     * module_4-5：高级补偿与安全建议（痛点处理）。
     * module_6-7：器材与总结。
     */
    private JsonNode library;
    @Resource
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("fitting_diagnostics.json");
            library = objectMapper.readTree(resource.getInputStream());
        } catch (IOException e) {
            log.error("Failed to load fitting_diagnostics.json", e);
            throw new RuntimeException("Static Fitting Diagnostic Library load failed", e);
        }
    }

    /**
     * 获取模版并替换占位符
     *
     * @param module 模块名称
     * @param key    模版key
     * @param values 占位符值
     */
    public String getTemplate(String module, String key, Map<String, Object> values) {
        if (library == null) return "Template Library Not Loaded";
        String template = library.path(module).path(key).asText("");
        if (template.isEmpty()) return "";

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                template = template.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
        }
        return template;
    }
}
