package com.starrysky.aries.staticfitting.service;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用的 List<String> 与 逗号分隔字符串 转换器
 * 用于转换用户标签
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/14 21:30
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        // ["大长腿", "核心王者"] -> "大长腿,核心王者"
        return String.join(",", attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // "大长腿,核心王者" -> ["大长腿", "核心王者"]
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
