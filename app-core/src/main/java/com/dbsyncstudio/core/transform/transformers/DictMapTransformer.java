package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import java.util.Map;

public class DictMapTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        Map<String, Object> config = context == null ? null : context.getRuleConfig();
        Map<String, Object> mapping = TransformConfigUtils.mapping(config);
        Object mapped = mapping.get(value == null ? null : String.valueOf(value));
        if (mapped != null) {
            return mapped;
        }
        if (config != null && config.containsKey("defaultValue")) {
            return config.get("defaultValue");
        }
        throw new IllegalArgumentException("dict_map has no mapping for value: " + value);
    }
}
