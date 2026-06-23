package com.dbsyncstudio.core.transform;

import com.dbsyncstudio.model.sync.TransformErrorStrategy;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;
import com.dbsyncstudio.model.sync.vo.TransformStepResultVO;
import com.dbsyncstudio.model.sync.vo.TransformTestResultVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransformPlan {

    private final TransformRegistry registry;
    private final Map<String, List<PreparedRule>> rulesByField;

    TransformPlan(TransformRegistry registry, Map<String, List<PreparedRule>> rulesByField) {
        this.registry = registry;
        this.rulesByField = rulesByField == null ? Collections.<String, List<PreparedRule>>emptyMap() : rulesByField;
    }

    public Object transform(String fieldName, Object value, TransformContext context) {
        List<PreparedRule> rules = findRules(fieldName);
        if (rules.isEmpty()) {
            return value;
        }
        Object currentValue = value;
        for (PreparedRule preparedRule : rules) {
            currentValue = applyRule(currentValue, preparedRule, context);
        }
        return currentValue;
    }

    public Map<String, Object> transformRow(Map<String, Object> sourceRow, TransformContext context) {
        if (sourceRow == null || sourceRow.isEmpty()) {
            return sourceRow == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(sourceRow);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>(sourceRow);
        for (Map.Entry<String, List<PreparedRule>> entry : rulesByField.entrySet()) {
            String fieldName = entry.getKey();
            String actualKey = findActualKey(result, fieldName);
            if (actualKey == null) {
                continue;
            }
            Object currentValue = result.get(actualKey);
            Object transformed = transform(fieldName, currentValue, contextFor(context,
                    actualKey,
                    actualKey,
                    currentValue,
                    sourceRow,
                    new LinkedHashMap<String, Object>(result),
                    null,
                    null));
            result.put(actualKey, transformed);
        }
        return result;
    }

    public boolean isEmpty() {
        if (rulesByField.isEmpty()) {
            return true;
        }
        for (List<PreparedRule> rules : rulesByField.values()) {
            if (rules != null && !rules.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    TransformTestResultVO test(Object value, TransformContext context) {
        if (rulesByField.isEmpty()) {
            return TransformTestResultVO.builder()
                    .success(true)
                    .originalValue(value)
                    .resultValue(value)
                    .steps(new ArrayList<TransformStepResultVO>())
                    .build();
        }
        List<PreparedRule> rules = rulesByField.values().iterator().next();
        Object currentValue = value;
        List<TransformStepResultVO> steps = new ArrayList<TransformStepResultVO>();
        boolean hasFailure = false;
        for (PreparedRule preparedRule : rules) {
            Object before = currentValue;
            TransformContext ruleContext = contextFor(context,
                    preparedRule.rule.getSourceField(),
                    preparedRule.rule.getTargetField(),
                    currentValue,
                    context == null ? null : context.getSourceRow(),
                    context == null ? null : context.getTargetRow(),
                    preparedRule.rule.getTransformType(),
                    preparedRule.config);
            try {
                currentValue = preparedRule.transformer.transform(before, preparedRule.rule, ruleContext);
                steps.add(TransformStepResultVO.builder()
                        .transformType(preparedRule.rule.getTransformType())
                        .before(before)
                        .after(currentValue)
                        .success(true)
                        .build());
            } catch (RuntimeException ex) {
                TransformErrorStrategy strategy = preparedRule.rule.getOnError() == null ? TransformErrorStrategy.FAIL : preparedRule.rule.getOnError();
                if (strategy == TransformErrorStrategy.FAIL) {
                    steps.add(TransformStepResultVO.builder()
                            .transformType(preparedRule.rule.getTransformType())
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
                Object resolvedValue = resolveOnError(before, preparedRule.rule, ex, ruleContext);
                steps.add(TransformStepResultVO.builder()
                        .transformType(preparedRule.rule.getTransformType())
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

    private List<PreparedRule> findRules(String fieldName) {
        String key = normalizeFieldName(fieldName);
        if (key == null) {
            return Collections.emptyList();
        }
        List<PreparedRule> rules = rulesByField.get(key);
        return rules == null ? Collections.<PreparedRule>emptyList() : rules;
    }

    private Object applyRule(Object currentValue, PreparedRule preparedRule, TransformContext context) {
        if (preparedRule == null || preparedRule.rule == null) {
            return currentValue;
        }
        if (preparedRule.rule.getEnabled() != null && !preparedRule.rule.getEnabled().booleanValue()) {
            return currentValue;
        }
        TransformContext ruleContext = contextFor(context,
                preparedRule.rule.getSourceField(),
                preparedRule.rule.getTargetField(),
                currentValue,
                context == null ? null : context.getSourceRow(),
                context == null ? null : context.getTargetRow(),
                preparedRule.rule.getTransformType(),
                preparedRule.config);
        try {
            return preparedRule.transformer.transform(currentValue, preparedRule.rule, ruleContext);
        } catch (RuntimeException ex) {
            return resolveOnError(currentValue, preparedRule.rule, ex, ruleContext);
        }
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

    private TransformContext contextFor(TransformContext context, String sourceField, String targetField,
                                        Object currentValue, Map<String, Object> sourceRow, Map<String, Object> targetRow,
                                        String transformType, Map<String, Object> ruleConfig) {
        TransformContext base = context == null ? TransformContext.builder().build() : context;
        return base.toBuilder()
                .sourceField(sourceField)
                .targetField(targetField)
                .transformType(transformType)
                .currentValue(currentValue)
                .sourceRow(sourceRow)
                .targetRow(targetRow)
                .ruleConfig(ruleConfig)
                .build();
    }

    private String normalizeFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().length() == 0) {
            return null;
        }
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }

    private String findActualKey(Map<String, Object> row, String fieldName) {
        if (row == null || fieldName == null) {
            return null;
        }
        if (row.containsKey(fieldName)) {
            return fieldName;
        }
        for (String key : row.keySet()) {
            if (key != null && key.equalsIgnoreCase(fieldName)) {
                return key;
            }
        }
        return null;
    }

    static final class PreparedRule {

        private final TransformRuleDO rule;
        private final Map<String, Object> config;
        private final ValueTransformer transformer;

        private PreparedRule(TransformRuleDO rule, Map<String, Object> config, ValueTransformer transformer) {
            this.rule = rule;
            this.config = config;
            this.transformer = transformer;
        }
    }

    static TransformPlan fromRules(Map<String, List<TransformRuleDO>> rulesByField, TransformRegistry registry) {
        Map<String, List<PreparedRule>> prepared = new LinkedHashMap<String, List<PreparedRule>>();
        if (rulesByField != null) {
            for (Map.Entry<String, List<TransformRuleDO>> entry : rulesByField.entrySet()) {
                String fieldName = entry.getKey();
                List<TransformRuleDO> rules = entry.getValue();
                if (fieldName == null || rules == null || rules.isEmpty()) {
                    continue;
                }
                List<TransformRuleDO> sortedRules = new ArrayList<TransformRuleDO>(rules);
                Collections.sort(sortedRules, new Comparator<TransformRuleDO>() {
                    @Override
                    public int compare(TransformRuleDO left, TransformRuleDO right) {
                        int leftOrder = left == null || left.getTransformOrder() == null ? 0 : left.getTransformOrder().intValue();
                        int rightOrder = right == null || right.getTransformOrder() == null ? 0 : right.getTransformOrder().intValue();
                        if (leftOrder != rightOrder) {
                            return leftOrder - rightOrder;
                        }
                        long leftId = left == null || left.getId() == null ? 0L : left.getId().longValue();
                        long rightId = right == null || right.getId() == null ? 0L : right.getId().longValue();
                        return leftId < rightId ? -1 : (leftId == rightId ? 0 : 1);
                    }
                });
                List<PreparedRule> preparedRules = new ArrayList<PreparedRule>();
                for (TransformRuleDO rule : sortedRules) {
                    if (rule == null || Boolean.FALSE.equals(rule.getEnabled())) {
                        continue;
                    }
                    preparedRules.add(new PreparedRule(rule, TransformConfigUtils.parseConfig(rule.getTransformConfig()), registry.getRequiredTransformer(rule.getTransformType())));
                }
                prepared.put(fieldName.trim().toLowerCase(Locale.ROOT), preparedRules);
            }
        }
        return new TransformPlan(registry, prepared);
    }
}
