package com.dbsyncstudio.core.transform;

import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

public interface ValueTransformer {

    Object transform(Object value, TransformRuleDO rule, TransformContext context);
}
