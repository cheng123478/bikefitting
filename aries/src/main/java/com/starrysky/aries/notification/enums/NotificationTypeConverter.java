package com.starrysky.aries.notification.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/**
 * NotificationTypeConverter
 * JPA 属性转换器：NotificationType 枚举 <-> 数据库 code 字符串
 * <p>
 * 使用 code 值（如 "new-activity"）存储到数据库，与历史数据完全兼容。
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/17
 */
@Converter(autoApply = false)
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Arrays.stream(NotificationType.values())
                .filter(t -> t.getCode().equals(dbData))
                .findFirst()
                .orElse(null);
    }
}
