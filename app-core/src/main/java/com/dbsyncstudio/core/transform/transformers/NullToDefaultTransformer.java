package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import java.util.Map;

public class NullToDefaultTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        if (value != null) {
            return value;
        }
        Map<String, Object> config = context == null ? null : context.getRuleConfig();
        Object defaultValue = config != null && config.containsKey("defaultValue")
                ? config.get("defaultValue")
                : rule.getDefaultValue();
        return defaultValue;
    }
}
