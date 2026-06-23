package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class NumberScaleTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        if (value == null) {
            return null;
        }
        Map<String, Object> config = context == null ? null : context.getRuleConfig();
        Integer scale = TransformConfigUtils.intValue(config, "scale");
        if (scale == null) {
            throw new IllegalArgumentException("number_scale requires scale");
        }
        RoundingMode roundingMode = TransformConfigUtils.roundingMode(config, "roundingMode", RoundingMode.HALF_UP);
        try {
            BigDecimal decimal = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value).trim());
            return decimal.setScale(scale.intValue(), roundingMode);
        } catch (Exception ex) {
            throw new IllegalArgumentException("number_scale transform failed: " + ex.getMessage(), ex);
        }
    }
}
