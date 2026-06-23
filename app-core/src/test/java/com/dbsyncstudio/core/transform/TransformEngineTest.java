package com.dbsyncstudio.core.transform;

import com.dbsyncstudio.model.sync.TransformErrorStrategy;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;
import com.dbsyncstudio.model.sync.vo.TransformStepResultVO;
import com.dbsyncstudio.model.sync.vo.TransformTestResultVO;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformEngineTest {

    private final TransformEngine engine = new TransformEngine();

    @Test
    public void shouldApplyTrimAndNullToDefaultInOrder() {
        TransformTestResultVO result = engine.test("  abc  ", Arrays.asList(
                rule("trim", 1, true, TransformErrorStrategy.FAIL, null, null),
                rule("null_to_default", 2, true, TransformErrorStrategy.FAIL, null, "{\"defaultValue\":\"x\"}")
        ), context());

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals("abc", result.getResultValue());
        Assert.assertEquals(2, result.getSteps().size());
        Assert.assertEquals("trim", result.getSteps().get(0).getTransformType());
    }

    @Test
    public void shouldHandleEmptyToNull() {
        Assert.assertNull(engine.transform("", Collections.singletonList(rule("empty_to_null", 1, true, TransformErrorStrategy.FAIL, null, null)), context()));
        Assert.assertEquals("x", engine.transform("x", Collections.singletonList(rule("empty_to_null", 1, true, TransformErrorStrategy.FAIL, null, null)), context()));
    }

    @Test
    public void shouldConvertUppercaseAndLowercase() {
        Assert.assertEquals("ABC", engine.transform("abc", Collections.singletonList(rule("uppercase", 1, true, TransformErrorStrategy.FAIL, null, null)), context()));
        Assert.assertEquals("abc", engine.transform("ABC", Collections.singletonList(rule("lowercase", 1, true, TransformErrorStrategy.FAIL, null, null)), context()));
    }

    @Test
    public void shouldApplyPrefixSuffixAndReplace() {
        Assert.assertEquals("USER_abc_SYNC", engine.transform("abc", Arrays.asList(
                rule("prefix", 1, true, TransformErrorStrategy.FAIL, null, "{\"prefix\":\"USER_\"}"),
                rule("suffix", 2, true, TransformErrorStrategy.FAIL, null, "{\"suffix\":\"_SYNC\"}")
        ), context()));
        Assert.assertEquals("abc123", engine.transform("abc-123", Collections.singletonList(rule("replace", 1, true, TransformErrorStrategy.FAIL, null, "{\"from\":\"-\",\"to\":\"\"}")), context()));
    }

    @Test
    public void shouldConvertDatesAndNumbers() {
        Assert.assertEquals("2026-06-23 12:30:45", engine.transform("2026/06/23 12:30:45", Collections.singletonList(
                rule("date_format", 1, true, TransformErrorStrategy.FAIL, null, "{\"fromPattern\":\"yyyy/MM/dd HH:mm:ss\",\"toPattern\":\"yyyy-MM-dd HH:mm:ss\"}")
        ), context()));

        Object scaled = engine.transform("12.345", Collections.singletonList(
                rule("number_scale", 1, true, TransformErrorStrategy.FAIL, null, "{\"scale\":2,\"roundingMode\":\"HALF_UP\"}")
        ), context());
        Assert.assertTrue(scaled instanceof BigDecimal);
        Assert.assertEquals("12.35", ((BigDecimal) scaled).toPlainString());
    }

    @Test
    public void shouldApplyDictMapAndConstant() {
        Assert.assertEquals("启用", engine.transform("Y", Collections.singletonList(
                rule("dict_map", 1, true, TransformErrorStrategy.FAIL, null, "{\"mapping\":{\"Y\":\"启用\",\"N\":\"禁用\"},\"defaultValue\":\"未知\"}")
        ), context()));
        Assert.assertEquals("DB_SYNC_STUDIO", engine.transform("anything", Collections.singletonList(
                rule("constant", 1, true, TransformErrorStrategy.FAIL, null, "{\"value\":\"DB_SYNC_STUDIO\"}")
        ), context()));
    }

    @Test
    public void shouldSkipDisabledRules() {
        Assert.assertEquals("abc", engine.transform("abc", Collections.singletonList(
                rule("prefix", 1, false, TransformErrorStrategy.FAIL, null, "{\"prefix\":\"USER_\"}")
        ), context()));
    }

    @Test
    public void shouldStopOnFailStrategyForInvalidDate() {
        try {
            engine.transform("not-a-date", Collections.singletonList(
                    rule("date_format", 1, true, TransformErrorStrategy.FAIL, null, "{\"fromPattern\":\"yyyy/MM/dd HH:mm:ss\",\"toPattern\":\"yyyy-MM-dd HH:mm:ss\"}")
            ), context());
            Assert.fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("date_format"));
        }
    }

    @Test
    public void shouldFallbackToOriginalValueOnError() {
        Object result = engine.transform("not-a-date", Collections.singletonList(
                rule("date_format", 1, true, TransformErrorStrategy.USE_ORIGINAL, null, "{\"fromPattern\":\"yyyy/MM/dd HH:mm:ss\",\"toPattern\":\"yyyy-MM-dd HH:mm:ss\"}")
        ), context());
        Assert.assertEquals("not-a-date", result);
    }

    @Test
    public void shouldFallbackToDefaultValueOnError() {
        Object result = engine.transform("not-a-number", Collections.singletonList(
                rule("number_scale", 1, true, TransformErrorStrategy.USE_DEFAULT, "7.50", "{\"scale\":2,\"roundingMode\":\"HALF_UP\"}")
        ), context());
        Assert.assertEquals("7.50", String.valueOf(result));
    }

    @Test
    public void shouldContinuePreviewWhenOnErrorUsesFallback() {
        TransformTestResultVO result = engine.test("not-a-number", Arrays.asList(
                rule("number_scale", 1, true, TransformErrorStrategy.USE_DEFAULT, "7.50", "{\"scale\":2,\"roundingMode\":\"HALF_UP\"}"),
                rule("uppercase", 2, true, TransformErrorStrategy.FAIL, null, null)
        ), context());

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals("7.50", String.valueOf(result.getResultValue()));
        Assert.assertEquals(2, result.getSteps().size());
        Assert.assertFalse(result.getSteps().get(0).isSuccess());
        Assert.assertEquals("7.50", String.valueOf(result.getSteps().get(0).getAfter()));
    }

    @Test
    public void shouldFallbackToNullOnError() {
        Object result = engine.transform("not-a-number", Collections.singletonList(
                rule("number_scale", 1, true, TransformErrorStrategy.SET_NULL, null, "{\"scale\":2,\"roundingMode\":\"HALF_UP\"}")
        ), context());
        Assert.assertNull(result);
    }

    @Test
    public void shouldExposePerStepResults() {
        TransformTestResultVO result = engine.test("  abc-123  ", Arrays.asList(
                rule("trim", 1, true, TransformErrorStrategy.FAIL, null, null),
                rule("replace", 2, true, TransformErrorStrategy.FAIL, null, "{\"from\":\"-\",\"to\":\"\"}"),
                rule("uppercase", 3, true, TransformErrorStrategy.FAIL, null, null)
        ), context());

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals("ABC123", result.getResultValue());
        Assert.assertEquals(3, result.getSteps().size());
        Assert.assertEquals("abc-123", result.getSteps().get(0).getAfter());
        Assert.assertEquals("ABC123", result.getSteps().get(2).getAfter());
    }

    @Test
    public void shouldSupportUnknownTransformTypeAsError() {
        try {
            engine.transform("x", Collections.singletonList(
                    rule("missing_rule", 1, true, TransformErrorStrategy.FAIL, null, null)
            ), context());
            Assert.fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue(ex.getMessage().contains("missing_rule"));
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

    private TransformContext context() {
        Map<String, Object> sourceRow = new HashMap<String, Object>();
        sourceRow.put("sourceField", "value");
        return TransformContext.builder()
                .taskId(Long.valueOf(1L))
                .runId("run-1")
                .tableTaskId(Long.valueOf(2L))
                .sourceField("sourceField")
                .targetField("targetField")
                .sourceRow(sourceRow)
                .currentValue("value")
                .build();
    }
}
