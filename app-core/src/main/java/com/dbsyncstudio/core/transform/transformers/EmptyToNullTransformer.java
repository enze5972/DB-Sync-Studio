package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

public class EmptyToNullTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        if (value instanceof CharSequence && value.toString().length() == 0) {
            return null;
        }
        return value;
    }
}
