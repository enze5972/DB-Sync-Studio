package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

public class SuffixTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        if (!(value instanceof CharSequence)) {
            return value;
        }
        String suffix = TransformConfigUtils.stringValue(context == null ? null : context.getRuleConfig(), "suffix");
        if (suffix == null) {
            suffix = "";
        }
        return value.toString() + suffix;
    }
}
