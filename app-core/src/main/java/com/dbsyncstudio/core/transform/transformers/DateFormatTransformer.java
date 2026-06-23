package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

public class DateFormatTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        if (value == null) {
            return null;
        }
        Map<String, Object> config = context == null ? null : context.getRuleConfig();
        String fromPattern = TransformConfigUtils.stringValue(config, "fromPattern");
        String toPattern = TransformConfigUtils.stringValue(config, "toPattern");
        if (toPattern == null || toPattern.length() == 0) {
            throw new IllegalArgumentException("date_format requires toPattern");
        }
        DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(toPattern);
        try {
            if (value instanceof CharSequence) {
                if (fromPattern == null || fromPattern.length() == 0) {
                    throw new IllegalArgumentException("date_format requires fromPattern for string values");
                }
                DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(fromPattern);
                LocalDateTime parsed = parseDateTime(value.toString(), sourceFormatter);
                return targetFormatter.format(parsed);
            }
            if (value instanceof Timestamp) {
                return targetFormatter.format(LocalDateTime.ofInstant(((Timestamp) value).toInstant(), ZoneId.systemDefault()));
            }
            if (value instanceof Date) {
                return targetFormatter.format(LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault()));
            }
            throw new IllegalArgumentException("date_format only supports string/date values");
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("date_format transform failed: " + ex.getMessage(), ex);
        }
    }

    private LocalDateTime parseDateTime(String value, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(value, formatter);
        } catch (Exception ex) {
            try {
                return LocalDate.parse(value, formatter).atStartOfDay();
            } catch (Exception ignored) {
                throw new IllegalArgumentException("Unable to parse date value: " + value);
            }
        }
    }
}
