package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

public class ConstantTransformer implements ValueTransformer {

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        Object constant = TransformConfigUtils.stringValue(context == null ? null : context.getRuleConfig(), "value");
        if (constant != null) {
            return constant;
        }
        return rule.getDefaultValue();
    }
}
