package com.dbsyncstudio.core.transform;

import com.dbsyncstudio.model.sync.TransformErrorStrategy;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptJsTransformerTest {

    private final TransformEngine engine = new TransformEngine();

    @Test
    public void shouldSerializeObjectResultAsJsonStringInAutoMode() {
        Map<String, Object> sourceRow = new HashMap<String, Object>();
        sourceRow.put("first_name", "Ada");

        Object result = engine.transform("  abc  ", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.FAIL, null,
                        "return { raw: value, normalized: String(value).trim(), firstName: source.first_name };",
                        "auto", 1000, true, 10000)
        ), context(sourceRow));

        Assert.assertEquals("{\"raw\":\"  abc  \",\"normalized\":\"abc\",\"firstName\":\"Ada\"}", result);
    }

    @Test
    public void shouldSerializeArrayResultAsJsonStringInTextMode() {
        Object result = engine.transform("abc", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.FAIL, null,
                        "return [value, String(value).trim()];",
                        "text", 1000, true, 10000)
        ), context(null));

        Assert.assertEquals("[\"abc\",\"abc\"]", result);
    }

    @Test
    public void shouldAcceptValidJsonStringInJsonMode() {
        Object result = engine.transform("ignored", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.FAIL, null,
                        "return '{\"a\":1,\"b\":[true,null]}';",
                        "json", 1000, true, 10000)
        ), context(null));

        Assert.assertEquals("{\"a\":1,\"b\":[true,null]}", result);
    }

    @Test
    public void shouldRejectInvalidJsonStringInJsonMode() {
        try {
            engine.transform("ignored", Collections.singletonList(
                    scriptRule(1, TransformErrorStrategy.FAIL, null,
                            "return 'not-json';",
                            "json", 1000, true, 10000)
            ), context(null));
            Assert.fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("resultType=json"));
        }
    }

    @Test
    public void shouldReturnPrimitiveScalarsInAutoMode() {
        Object numberResult = engine.transform("ignored", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.FAIL, null,
                        "return 123;",
                        "auto", 1000, true, 10000)
        ), context(null));
        Object booleanResult = engine.transform("ignored", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.FAIL, null,
                        "return true;",
                        "auto", 1000, true, 10000)
        ), context(null));
        Object nullResult = engine.transform("ignored", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.FAIL, null,
                        "return null;",
                        "auto", 1000, true, 10000)
        ), context(null));

        Assert.assertEquals("123", String.valueOf(numberResult));
        Assert.assertEquals("true", String.valueOf(booleanResult));
        Assert.assertNull(nullResult);
    }

    @Test
    public void shouldRejectOutputsLongerThanMaxOutputLength() {
        try {
            engine.transform("ignored", Collections.singletonList(
                    scriptRule(1, TransformErrorStrategy.FAIL, null,
                            "return '123456';",
                            "text", 1000, true, 5)
            ), context(null));
            Assert.fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("maxOutputLength"));
        }
    }

    @Test
    public void shouldRejectCyclicObjectResults() {
        try {
            engine.transform("ignored", Collections.singletonList(
                    scriptRule(1, TransformErrorStrategy.FAIL, null,
                            "var a = {}; a.self = a; return a;",
                            "auto", 1000, true, 10000)
            ), context(null));
            Assert.fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().toLowerCase().contains("serial"));
        }
    }

    @Test(timeout = 5000)
    public void shouldTimeoutLongRunningScript() {
        try {
            engine.transform("ignored", Collections.singletonList(
                    scriptRule(1, TransformErrorStrategy.FAIL, null,
                            "while (true) {}",
                            "auto", 10, true, 10000)
            ), context(null));
            Assert.fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().toLowerCase().contains("timeout"));
        }
    }

    @Test
    public void shouldApplyOnErrorStrategyForScriptFailures() {
        Object originalResult = engine.transform("abc", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.USE_ORIGINAL, null,
                        "throw new Error('boom');",
                        "auto", 1000, true, 10000)
        ), context(null));
        Object nullResult = engine.transform("abc", Collections.singletonList(
                scriptRule(1, TransformErrorStrategy.SET_NULL, null,
                        "throw new Error('boom');",
                        "auto", 1000, true, 10000)
        ), context(null));

        Assert.assertEquals("abc", originalResult);
        Assert.assertNull(nullResult);
    }

    @Test
    public void shouldRejectWritesToSourceObject() {
        try {
            engine.transform("abc", Collections.singletonList(
                    scriptRule(1, TransformErrorStrategy.FAIL, null,
                            "source.first_name = 'Bob'; return source.first_name;",
                            "auto", 1000, true, 10000)
            ), context(Collections.singletonMap("first_name", (Object) "Ada")));
            Assert.fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().toLowerCase().contains("read"));
        }
    }

    private TransformRuleDO rule(String type, int order, boolean enabled, TransformErrorStrategy onError, String defaultValue, String configJson) {
        return TransformRuleDO.builder()
                .transformType(type)
                .transformOrder(Integer.valueOf(order))
                .enabled(Boolean.valueOf(enabled))
                .onError(onError)
                .defaultValue(defaultValue)
                .transformConfig(configJson)
                .build();
    }

    private TransformRuleDO scriptRule(int order, TransformErrorStrategy onError, String defaultValue,
                                     String script, String resultType, int timeoutMs, boolean allowNull, int maxOutputLength) {
        return rule("script_js", order, true, onError, defaultValue,
                buildScriptConfig(script, resultType, timeoutMs, allowNull, maxOutputLength));
    }

    private String buildScriptConfig(String script, String resultType, int timeoutMs, boolean allowNull, int maxOutputLength) {
        return "{"
                + "\"script\":\"" + escapeJson(script) + "\","
                + "\"resultType\":\"" + escapeJson(resultType) + "\","
                + "\"timeoutMs\":" + timeoutMs + ","
                + "\"allowNull\":" + allowNull + ","
                + "\"maxOutputLength\":" + maxOutputLength
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '"') {
                builder.append('\\');
            } else if (ch == '\n') {
                builder.append("\\n");
                continue;
            } else if (ch == '\r') {
                builder.append("\\r");
                continue;
            } else if (ch == '\t') {
                builder.append("\\t");
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private TransformContext context(Map<String, Object> sourceRow) {
        Map<String, Object> row = sourceRow == null ? new HashMap<String, Object>() : new HashMap<String, Object>(sourceRow);
        return TransformContext.builder()
                .taskId(Long.valueOf(1L))
                .runId("run-1")
                .tableTaskId(Long.valueOf(2L))
                .fieldMappingId(Long.valueOf(3L))
                .sourceField("sourceField")
                .targetField("targetField")
                .sourceRow(row)
                .currentValue("value")
                .build();
    }
}
