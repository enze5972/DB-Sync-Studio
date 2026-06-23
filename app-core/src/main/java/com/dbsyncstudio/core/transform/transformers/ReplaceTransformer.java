package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

public class ReplaceTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        if (!(value instanceof CharSequence)) {
            return value;
        }
        String from = TransformConfigUtils.stringValue(context == null ? null : context.getRuleConfig(), "from");
        String to = TransformConfigUtils.stringValue(context == null ? null : context.getRuleConfig(), "to");
        if (from == null || from.length() == 0) {
            return value;
        }
        return value.toString().replace(from, to == null ? "" : to);
    }
}
