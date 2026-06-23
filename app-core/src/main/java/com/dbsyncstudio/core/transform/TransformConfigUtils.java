package com.dbsyncstudio.core.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TransformConfigUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TransformConfigUtils() {
    }

    public static Map<String, Object> parseConfig(String json) {
        if (json == null || json.trim().length() == 0) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> config = OBJECT_MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
            return config == null ? Collections.<String, Object>emptyMap() : config;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse transform_config JSON", ex);
        }
    }

    public static String stringValue(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static Integer intValue(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid integer for config key: " + key, ex);
        }
    }

    public static Long longValue(Map<String, Object> config, String key) {
        Object value = config == null ? null : config.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid long for config key: " + key, ex);
        }
    }

    public static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config == null ? null : config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean for config key: " + key);
    }

    public static RoundingMode roundingMode(Map<String, Object> config, String key, RoundingMode defaultValue) {
        String value = stringValue(config, key);
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        try {
            return RoundingMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid roundingMode: " + value, ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapping(Map<String, Object> config) {
        Object value = config == null ? null : config.get("mapping");
        if (value == null) {
            return Collections.emptyMap();
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new IllegalArgumentException("dict_map configuration requires object mapping");
    }
}
