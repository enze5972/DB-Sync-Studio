package com.dbsyncstudio.core.transform;

import com.dbsyncstudio.model.sync.TransformErrorStrategy;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;
import com.dbsyncstudio.model.sync.vo.TransformStepResultVO;
import com.dbsyncstudio.model.sync.vo.TransformTestResultVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransformEngine {

    private static final String SINGLE_VALUE_KEY = "__value__";

    private final TransformRegistry registry;

    public TransformEngine() {
        this(new TransformRegistry());
    }

    public TransformEngine(TransformRegistry registry) {
        this.registry = registry;
    }

    public TransformPlan compile(Map<String, List<TransformRuleDO>> rulesByField) {
        return TransformPlan.fromRules(normalizeRulesByField(rulesByField), registry);
    }

    public Object transform(Object value, List<TransformRuleDO> rules, TransformContext context) {
        TransformContext safeContext = normalizeSingleValueContext(context, value);
        TransformPlan plan = compile(singleFieldRules(rules));
        return plan.transform(SINGLE_VALUE_KEY, value, safeContext);
    }

    public TransformTestResultVO test(Object value, List<TransformRuleDO> rules, TransformContext context) {
        List<TransformRuleDO> safeRules = normalizeRules(rules);
        TransformContext safeContext = normalizeSingleValueContext(context, value);
        if (safeRules.isEmpty()) {
            return TransformTestResultVO.builder()
                    .success(true)
                    .originalValue(value)
                    .resultValue(value)
                    .steps(new ArrayList<TransformStepResultVO>())
                    .build();
        }

        Object currentValue = value;
        List<TransformStepResultVO> steps = new ArrayList<TransformStepResultVO>();
        boolean hasFailure = false;
        for (TransformRuleDO rule : safeRules) {
            Object before = currentValue;
            TransformContext ruleContext = safeContext == null ? TransformContext.builder().build() : safeContext;
            ruleContext = ruleContext.toBuilder()
                    .currentValue(before)
                    .sourceField(resolveFieldName(safeContext))
                    .targetField(resolveFieldName(safeContext))
                    .transformType(rule.getTransformType())
                    .ruleConfig(TransformConfigUtils.parseConfig(rule.getTransformConfig()))
                    .build();
            try {
                currentValue = registry.getRequiredTransformer(rule.getTransformType()).transform(before, rule, ruleContext);
                steps.add(TransformStepResultVO.builder()
                        .transformType(rule.getTransformType())
                        .before(before)
                        .after(currentValue)
                        .success(true)
                        .build());
            } catch (RuntimeException ex) {
                TransformErrorStrategy strategy = rule.getOnError() == null ? TransformErrorStrategy.FAIL : rule.getOnError();
                if (strategy == TransformErrorStrategy.FAIL) {
                    steps.add(TransformStepResultVO.builder()
                            .transformType(rule.getTransformType())
                            .before(before)
                            .after(before)
                            .success(false)
                            .errorMessage(ex.getMessage())
                            .build());
                    return TransformTestResultVO.builder()
                            .success(false)
                            .originalValue(value)
                            .resultValue(before)
                            .steps(steps)
                            .errorMessage(ex.getMessage())
                            .build();
                }
                Object resolvedValue = resolveOnError(before, rule, ex, ruleContext);
                steps.add(TransformStepResultVO.builder()
                        .transformType(rule.getTransformType())
                        .before(before)
                        .after(resolvedValue)
                        .success(false)
                        .errorMessage(ex.getMessage())
                        .build());
                currentValue = resolvedValue;
                hasFailure = true;
            }
        }

        return TransformTestResultVO.builder()
                .success(!hasFailure)
                .originalValue(value)
                .resultValue(currentValue)
                .steps(steps)
                .build();
    }

    public Map<String, Object> transformRow(Map<String, Object> sourceRow, Map<String, List<TransformRuleDO>> rulesByField, TransformContext context) {
        TransformPlan plan = compile(rulesByField);
        return plan.transformRow(sourceRow, context);
    }

    private Object applyRule(Object value, TransformRuleDO rule, TransformContext context) {
        if (rule == null) {
            return value;
        }
        if (rule.getEnabled() != null && !rule.getEnabled().booleanValue()) {
            return value;
        }
        TransformContext ruleContext = normalizeSingleValueContext(context, value);
        ruleContext = ruleContext.toBuilder()
                .currentValue(value)
                .sourceField(resolveFieldName(ruleContext))
                .targetField(resolveFieldName(ruleContext))
                .transformType(rule.getTransformType())
                .ruleConfig(TransformConfigUtils.parseConfig(rule.getTransformConfig()))
                .build();
        return registry.getRequiredTransformer(rule.getTransformType()).transform(value, rule, ruleContext);
    }

    private Map<String, List<TransformRuleDO>> singleFieldRules(List<TransformRuleDO> rules) {
        Map<String, List<TransformRuleDO>> result = new HashMap<String, List<TransformRuleDO>>();
        result.put(SINGLE_VALUE_KEY, normalizeRules(rules));
        return result;
    }

    private Map<String, List<TransformRuleDO>> normalizeRulesByField(Map<String, List<TransformRuleDO>> rulesByField) {
        Map<String, List<TransformRuleDO>> result = new HashMap<String, List<TransformRuleDO>>();
        if (rulesByField == null) {
            return result;
        }
        for (Map.Entry<String, List<TransformRuleDO>> entry : rulesByField.entrySet()) {
            result.put(normalizeFieldName(entry.getKey()), normalizeRules(entry.getValue()));
        }
        return result;
    }

    private List<TransformRuleDO> normalizeRules(List<TransformRuleDO> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransformRuleDO> result = new ArrayList<TransformRuleDO>();
        for (TransformRuleDO rule : rules) {
            if (rule != null && !Boolean.FALSE.equals(rule.getEnabled())) {
                result.add(rule);
            }
        }
        Collections.sort(result, new Comparator<TransformRuleDO>() {
            @Override
            public int compare(TransformRuleDO left, TransformRuleDO right) {
                int leftOrder = left.getTransformOrder() == null ? 0 : left.getTransformOrder().intValue();
                int rightOrder = right.getTransformOrder() == null ? 0 : right.getTransformOrder().intValue();
                if (leftOrder != rightOrder) {
                    return leftOrder - rightOrder;
                }
                long leftId = left.getId() == null ? 0L : left.getId().longValue();
                long rightId = right.getId() == null ? 0L : right.getId().longValue();
                return leftId < rightId ? -1 : (leftId == rightId ? 0 : 1);
            }
        });
        return result;
    }

    private String resolveFieldName(TransformContext context) {
        if (context == null) {
            return null;
        }
        if (context.getTargetField() != null && context.getTargetField().trim().length() > 0) {
            return normalizeFieldName(context.getTargetField());
        }
        if (context.getSourceField() != null && context.getSourceField().trim().length() > 0) {
            return normalizeFieldName(context.getSourceField());
        }
        return null;
    }

    private String normalizeFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().length() == 0) {
            return null;
        }
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }

    private TransformContext normalizeSingleValueContext(TransformContext context, Object value) {
        TransformContext safeContext = context == null ? TransformContext.builder().build() : context;
        if (safeContext.getSourceRow() == null || safeContext.getSourceRow().isEmpty()) {
            Map<String, Object> sourceRow = new LinkedHashMap<String, Object>();
            String sourceField = safeContext.getSourceField();
            if (sourceField == null || sourceField.trim().length() == 0) {
                sourceField = safeContext.getTargetField();
            }
            if (sourceField == null || sourceField.trim().length() == 0) {
                sourceField = SINGLE_VALUE_KEY;
            }
            sourceRow.put(sourceField, value);
            safeContext = safeContext.toBuilder().sourceRow(sourceRow).build();
        }
        if (safeContext.getTargetRow() == null) {
            safeContext = safeContext.toBuilder().targetRow(new LinkedHashMap<String, Object>()).build();
        }
        return safeContext;
    }

    private Object resolveOnError(Object currentValue, TransformRuleDO rule, RuntimeException ex, TransformContext context) {
        TransformErrorStrategy strategy = rule == null ? TransformErrorStrategy.FAIL : rule.getOnError();
        if (strategy == null) {
            strategy = TransformErrorStrategy.FAIL;
        }
        if (strategy == TransformErrorStrategy.USE_ORIGINAL) {
            return currentValue;
        }
        if (strategy == TransformErrorStrategy.USE_DEFAULT) {
            return resolveDefaultValue(rule, context);
        }
        if (strategy == TransformErrorStrategy.SET_NULL) {
            return null;
        }
        throw new IllegalArgumentException(buildErrorMessage(rule, ex), ex);
    }

    private Object resolveDefaultValue(TransformRuleDO rule, TransformContext context) {
        if (rule != null && rule.getDefaultValue() != null) {
            return rule.getDefaultValue();
        }
        Map<String, Object> config = context == null ? null : context.getRuleConfig();
        if (config != null && config.containsKey("defaultValue")) {
            return config.get("defaultValue");
        }
        return null;
    }

    private String buildErrorMessage(TransformRuleDO rule, RuntimeException ex) {
        String transformType = rule == null ? "unknown" : rule.getTransformType();
        return "Transform failed for " + transformType + ": " + ex.getMessage();
    }
}
