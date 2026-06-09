package com.starrysky.aries.staticfitting.service;

import com.starrysky.aries.util.RidingPainType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将 List<RidingPainType> 转换为逗号分隔的字符串存入单表字段
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/14 21:16
 */
@Converter
public class PainTypeListConverter implements AttributeConverter<List<RidingPainType>, String> {

    @Override
    public String convertToDatabaseColumn(List<RidingPainType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        // 转成逗号分隔的字符串存库
        return attribute.stream()
                .map(RidingPainType::getCode)
                .collect(Collectors.joining(","));
    }

    @Override
    public List<RidingPainType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        // 从逗号分隔的字符串恢复为 List<Enum>
        return Arrays.stream(dbData.split(","))
                .map(RidingPainType::fromCode)
                .collect(Collectors.toList());
    }
}