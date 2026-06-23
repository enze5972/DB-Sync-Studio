package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import java.util.Locale;

public class UppercaseTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        return value instanceof CharSequence ? value.toString().toUpperCase(Locale.ROOT) : value;
    }
}
