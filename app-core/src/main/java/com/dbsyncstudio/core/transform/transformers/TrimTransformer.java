package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

public class TrimTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        return value instanceof CharSequence ? value.toString().trim() : value;
    }
}
