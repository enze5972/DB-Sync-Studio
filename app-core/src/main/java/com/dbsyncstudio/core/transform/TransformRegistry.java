package com.dbsyncstudio.core.transform;

import com.dbsyncstudio.core.transform.transformers.ConstantTransformer;
import com.dbsyncstudio.core.transform.transformers.DateFormatTransformer;
import com.dbsyncstudio.core.transform.transformers.DictMapTransformer;
import com.dbsyncstudio.core.transform.transformers.EmptyToNullTransformer;
import com.dbsyncstudio.core.transform.transformers.LowercaseTransformer;
import com.dbsyncstudio.core.transform.transformers.NullToDefaultTransformer;
import com.dbsyncstudio.core.transform.transformers.NumberScaleTransformer;
import com.dbsyncstudio.core.transform.transformers.PrefixTransformer;
import com.dbsyncstudio.core.transform.transformers.ReplaceTransformer;
import com.dbsyncstudio.core.transform.transformers.ScriptJsTransformer;
import com.dbsyncstudio.core.transform.transformers.SuffixTransformer;
import com.dbsyncstudio.core.transform.transformers.TrimTransformer;
import com.dbsyncstudio.core.transform.transformers.UppercaseTransformer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TransformRegistry {

    private final Map<String, ValueTransformer> transformers = new HashMap<String, ValueTransformer>();

    public TransformRegistry() {
        register("null_to_default", new NullToDefaultTransformer());
        register("empty_to_null", new EmptyToNullTransformer());
        register("trim", new TrimTransformer());
        register("uppercase", new UppercaseTransformer());
        register("lowercase", new LowercaseTransformer());
        register("prefix", new PrefixTransformer());
        register("suffix", new SuffixTransformer());
        register("replace", new ReplaceTransformer());
        register("date_format", new DateFormatTransformer());
        register("number_scale", new NumberScaleTransformer());
        register("dict_map", new DictMapTransformer());
        register("constant", new ConstantTransformer());
        register("script_js", new ScriptJsTransformer());
    }

    public void register(String transformType, ValueTransformer transformer) {
        if (transformType == null || transformer == null) {
            throw new IllegalArgumentException("Transform type and transformer must not be null");
        }
        transformers.put(normalize(transformType), transformer);
    }

    public ValueTransformer getRequiredTransformer(String transformType) {
        ValueTransformer transformer = transformers.get(normalize(transformType));
        if (transformer == null) {
            throw new IllegalArgumentException("Unknown transform_type: " + transformType);
        }
        return transformer;
    }

    private String normalize(String transformType) {
        return transformType == null ? null : transformType.trim().toLowerCase(Locale.ROOT);
    }
}
