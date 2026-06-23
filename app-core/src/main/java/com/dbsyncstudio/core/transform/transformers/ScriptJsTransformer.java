package com.dbsyncstudio.core.transform.transformers;

import com.dbsyncstudio.core.transform.TransformConfigUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.ValueTransformer;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.Proxy;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyIterator;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ScriptJsTransformer implements ValueTransformer {

    private static final String TRANSFORM_TYPE = "script_js";
    private static final int DEFAULT_TIMEOUT_MS = 1000;
    private static final int MAX_TIMEOUT_MS = 1000;
    private static final int DEFAULT_MAX_OUTPUT_LENGTH = 10000;
    private static final int MAX_SCRIPT_LENGTH = 20000;
    private static final String DEFAULT_RESULT_TYPE = "auto";
    private static final Engine ENGINE = Engine.newBuilder().build();
    private static final HostAccess HOST_ACCESS = HostAccess.newBuilder(HostAccess.NONE)
            .allowArrayAccess(true)
            .allowListAccess(true)
            .build();
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    private final ConcurrentMap<String, Source> sourceCache = new ConcurrentHashMap<String, Source>();

    @Override
    public Object transform(Object value, TransformRuleDO rule, TransformContext context) {
        ScriptJsSettings settings = ScriptJsSettings.from(rule, context);
        Context polyglotContext = createContext();
        ScheduledFuture<?> timeoutFuture = null;
        long startedAt = System.currentTimeMillis();
        try (Context closeableContext = polyglotContext) {
            timeoutFuture = scheduleTimeout(closeableContext, settings.timeoutMs);
            Source scriptSource = sourceCache.computeIfAbsent(settings.cacheKey, new java.util.function.Function<String, Source>() {
                @Override
                public Source apply(String key) {
                    return buildScriptSource(settings, key);
                }
            });
            Value scriptFunction = closeableContext.eval(scriptSource);
            TransformContext safeContext = normalizeContext(context, value);
            Value rawResult = scriptFunction.execute(
                    toGuestValue(value),
                    toGuestValue(safeContext.getSourceRow()),
                    toGuestValue(safeContext.getTargetRow()),
                    toGuestValue(buildContextMap(safeContext, settings)));
            return ScriptJsResultNormalizer.normalize(closeableContext, rawResult, settings.resultType, settings.allowNull, settings.maxOutputLength);
        } catch (PolyglotException ex) {
            if (isTimeout(timeoutFuture, startedAt, settings.timeoutMs)) {
                throw new IllegalArgumentException("script_js execution timeout after " + settings.timeoutMs + " ms", ex);
            }
            throw new IllegalArgumentException(buildErrorMessage(ex, settings), ex);
        } finally {
            if (timeoutFuture != null) {
                timeoutFuture.cancel(true);
            }
        }
    }

    private Context createContext() {
        return Context.newBuilder("js")
                .engine(ENGINE)
                .allowAllAccess(false)
                .allowHostAccess(HOST_ACCESS)
                .allowHostClassLookup(new java.util.function.Predicate<String>() {
                    @Override
                    public boolean test(String className) {
                        return false;
                    }
                })
                .allowHostClassLoading(false)
                .allowCreateThread(false)
                .allowCreateProcess(false)
                .allowNativeAccess(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .allowIO(IOAccess.newBuilder().allowHostFileAccess(false).allowHostSocketAccess(false).build())
                .build();
    }

    private ScheduledFuture<?> scheduleTimeout(final Context context, int timeoutMs) {
        return TIMEOUT_EXECUTOR.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    context.close(true);
                } catch (Exception ignored) {
                    // Best-effort timeout cancellation.
                }
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }

    private Source buildScriptSource(ScriptJsSettings settings, String cacheKey) {
        String script = "(function(value, source, target, context) {\n"
                + "  'use strict';\n"
                + "  function cloneValue(input) {\n"
                + "    if (input === null || input === undefined) {\n"
                + "      return input;\n"
                + "    }\n"
                + "    if (Array.isArray(input)) {\n"
                + "      var array = [];\n"
                + "      for (var i = 0; i < input.length; i += 1) {\n"
                + "        array.push(cloneValue(input[i]));\n"
                + "      }\n"
                + "      return Object.freeze(array);\n"
                + "    }\n"
                + "    if (typeof input === 'object') {\n"
                + "      var object = {};\n"
                + "      Object.keys(input).forEach(function (key) {\n"
                + "        object[key] = cloneValue(input[key]);\n"
                + "      });\n"
                + "      return Object.freeze(object);\n"
                + "    }\n"
                + "    return input;\n"
                + "  }\n"
                + "  value = cloneValue(value);\n"
                + "  source = cloneValue(source);\n"
                + "  target = cloneValue(target);\n"
                + "  context = cloneValue(context);\n"
                + settings.script
                + "\n})";
        try {
            return Source.newBuilder("js", script, "script_js_" + cacheKey).cached(true).buildLiteral();
        } catch (Exception ex) {
            throw new IllegalArgumentException("script_js failed to compile: " + safeMessage(ex), ex);
        }
    }

    private Map<String, Object> buildContextMap(TransformContext context, ScriptJsSettings settings) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (context == null) {
            return result;
        }
        result.put("taskId", context.getTaskId());
        result.put("tableTaskId", context.getTableTaskId());
        result.put("fieldMappingId", context.getFieldMappingId());
        result.put("runId", context.getRunId());
        result.put("sourceField", context.getSourceField());
        result.put("targetField", context.getTargetField());
        result.put("transformType", settings == null ? context.getTransformType() : settings.transformType);
        result.put("currentValue", context.getCurrentValue());
        return result;
    }

    private TransformContext normalizeContext(TransformContext context, Object value) {
        TransformContext safeContext = context == null ? TransformContext.builder().build() : context;
        if (safeContext.getSourceRow() == null || safeContext.getSourceRow().isEmpty()) {
            Map<String, Object> sourceRow = new LinkedHashMap<String, Object>();
            String fieldName = safeContext.getSourceField();
            if (fieldName == null || fieldName.trim().length() == 0) {
                fieldName = safeContext.getTargetField();
            }
            if (fieldName == null || fieldName.trim().length() == 0) {
                fieldName = "__value__";
            }
            sourceRow.put(fieldName, value);
            safeContext = safeContext.toBuilder().sourceRow(sourceRow).build();
        }
        if (safeContext.getTargetRow() == null) {
            safeContext = safeContext.toBuilder().targetRow(new LinkedHashMap<String, Object>()).build();
        }
        return safeContext;
    }

    private Object toGuestValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ProxyObject || value instanceof ProxyArray) {
            return value;
        }
        if (value instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) value);
        }
        if (value instanceof Map) {
            return toGuestMap((Map<?, ?>) value);
        }
        if (value instanceof Collection) {
            return toGuestList((Collection<?>) value);
        }
        if (value.getClass().isArray()) {
            return toGuestArray(value);
        }
        if (value instanceof Date) {
            return String.valueOf(value);
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Character) {
            return String.valueOf(value);
        }
        return value;
    }

    private ProxyObject toGuestMap(Map<?, ?> value) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
            if (key == null) {
                continue;
            }
            result.put(key, toGuestValue(entry.getValue()));
        }
        return new ReadOnlyProxyObject(Collections.unmodifiableMap(result));
    }

    private ProxyArray toGuestList(Collection<?> value) {
        List<Object> result = new ArrayList<Object>();
        for (Object item : value) {
            result.add(toGuestValue(item));
        }
        return new ReadOnlyProxyArray(Collections.unmodifiableList(result));
    }

    private ProxyArray toGuestArray(Object array) {
        int length = Array.getLength(array);
        List<Object> result = new ArrayList<Object>(length);
        for (int i = 0; i < length; i++) {
            result.add(toGuestValue(Array.get(array, i)));
        }
        return new ReadOnlyProxyArray(Collections.unmodifiableList(result));
    }

    private String buildErrorMessage(PolyglotException ex, ScriptJsSettings settings) {
        String message = safeMessage(ex).toLowerCase(Locale.ROOT);
        if (ex != null && ex.isCancelled()) {
            return "script_js execution timeout after " + settings.timeoutMs + " ms";
        }
        if (isReadOnlyWriteError(ex, message)) {
            return "script_js attempted to modify a read-only input object";
        }
        if (message.contains("unsupportedoperationexception")
                || message.contains("read-only")
                || message.contains("readonly")
                || message.contains("unsupported operation")
                || message.contains("not writable")
                || message.contains("not writeable")
                || message.contains("cannot assign")
                || message.contains("cannot set")
                || message.contains("not modifiable")
                || message.contains("immutable")
                || message.contains("cancel")
                || message.contains("interrupt")
                || message.contains("termination")) {
            if (message.contains("cancel") || message.contains("interrupt") || message.contains("termination")) {
                return "script_js execution timeout after " + settings.timeoutMs + " ms";
            }
            return "script_js attempted to modify a read-only input object";
        }
        return "script_js execution failed: " + message;
    }

    private boolean isReadOnlyWriteError(Throwable ex, String message) {
        if (message != null) {
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("read-only")
                    || normalized.contains("readonly")
                    || normalized.contains("unsupportedoperationexception")
                    || normalized.contains("unsupported operation")
                    || normalized.contains("not writable")
                    || normalized.contains("not writeable")
                    || normalized.contains("cannot assign")
                    || normalized.contains("cannot set")
                    || normalized.contains("not modifiable")
                    || normalized.contains("immutable")) {
                return true;
            }
        }
        Throwable current = ex;
        while (current != null) {
            if (current instanceof UnsupportedOperationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String safeMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().trim().length() == 0) {
            return "unknown error";
        }
        String message = ex.getMessage().replace('\n', ' ').replace('\r', ' ');
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private boolean isTimeout(ScheduledFuture<?> timeoutFuture, long startedAt, int timeoutMs) {
        if (timeoutFuture != null && timeoutFuture.isDone()) {
            return true;
        }
        return System.currentTimeMillis() - startedAt >= timeoutMs;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static final class ScriptJsSettings {

        private final String script;
        private final String resultType;
        private final boolean allowNull;
        private final int timeoutMs;
        private final int maxOutputLength;
        private final String transformType;
        private final String cacheKey;

        private ScriptJsSettings(String script, String resultType, boolean allowNull, int timeoutMs, int maxOutputLength) {
            this.script = script;
            this.resultType = resultType;
            this.allowNull = allowNull;
            this.timeoutMs = timeoutMs;
            this.maxOutputLength = maxOutputLength;
            this.transformType = TRANSFORM_TYPE;
            this.cacheKey = buildCacheKey(script, resultType, allowNull, timeoutMs, maxOutputLength);
        }

        private static ScriptJsSettings from(TransformRuleDO rule, TransformContext context) {
            Map<String, Object> config = context == null ? null : context.getRuleConfig();
            if (config == null) {
                config = TransformConfigUtils.parseConfig(rule == null ? null : rule.getTransformConfig());
            }
            String script = TransformConfigUtils.stringValue(config, "script");
            if (script == null || script.trim().length() == 0) {
                throw new IllegalArgumentException("script_js requires script");
            }
            if (script.length() > MAX_SCRIPT_LENGTH) {
                throw new IllegalArgumentException("script_js script exceeds max length of " + MAX_SCRIPT_LENGTH);
            }
            String resultType = TransformConfigUtils.stringValue(config, "resultType");
            if (resultType == null || resultType.trim().length() == 0) {
                resultType = DEFAULT_RESULT_TYPE;
            } else {
                resultType = resultType.trim().toLowerCase(Locale.ROOT);
            }
            if (!"auto".equals(resultType) && !"text".equals(resultType) && !"json".equals(resultType)) {
                throw new IllegalArgumentException("script_js resultType must be auto, text, or json");
            }
            boolean allowNull = TransformConfigUtils.booleanValue(config, "allowNull", true);
            Long timeoutValue = TransformConfigUtils.longValue(config, "timeoutMs");
            int timeoutMs = timeoutValue == null ? DEFAULT_TIMEOUT_MS : timeoutValue.intValue();
            if (timeoutMs <= 0 || timeoutMs > MAX_TIMEOUT_MS) {
                throw new IllegalArgumentException("script_js timeoutMs must be between 1 and " + MAX_TIMEOUT_MS);
            }
            Long maxOutputLengthValue = TransformConfigUtils.longValue(config, "maxOutputLength");
            int maxOutputLength = maxOutputLengthValue == null ? DEFAULT_MAX_OUTPUT_LENGTH : maxOutputLengthValue.intValue();
            if (maxOutputLength <= 0) {
                throw new IllegalArgumentException("script_js maxOutputLength must be greater than 0");
            }
            return new ScriptJsSettings(script, resultType, allowNull, timeoutMs, maxOutputLength);
        }

        private static String buildCacheKey(String script, String resultType, boolean allowNull, int timeoutMs, int maxOutputLength) {
            StringBuilder builder = new StringBuilder();
            builder.append(script == null ? "" : script).append('\n')
                    .append(resultType == null ? "" : resultType).append('\n')
                    .append(allowNull).append('\n')
                    .append(timeoutMs).append('\n')
                    .append(maxOutputLength);
            return ScriptJsTransformer.sha256(builder.toString());
        }
    }

    private static final class ReadOnlyProxyObject implements ProxyObject {

        private final Map<String, Object> members;

        private ReadOnlyProxyObject(Map<String, Object> members) {
            this.members = members == null ? Collections.<String, Object>emptyMap() : members;
        }

        @Override
        public Object getMember(String key) {
            return members.get(key);
        }

        @Override
        public Object getMemberKeys() {
            return new ArrayList<String>(members.keySet());
        }

        @Override
        public boolean hasMember(String key) {
            return members.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("read-only object");
        }

        @Override
        public boolean removeMember(String key) {
            throw new UnsupportedOperationException("read-only object");
        }
    }

    private static final class ReadOnlyProxyArray implements ProxyArray {

        private final List<Object> values;

        private ReadOnlyProxyArray(List<Object> values) {
            this.values = values == null ? Collections.emptyList() : values;
        }

        @Override
        public Object get(long index) {
            return values.get((int) index);
        }

        @Override
        public void set(long index, Value value) {
            throw new UnsupportedOperationException("read-only array");
        }

        @Override
        public boolean remove(long index) {
            throw new UnsupportedOperationException("read-only array");
        }

        @Override
        public long getSize() {
            return values.size();
        }

        @Override
        public Object getIterator() {
            return ProxyIterator.from(values.iterator());
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {

        private int index = 0;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "script-js-timeout-" + (++index));
            thread.setDaemon(true);
            return thread;
        }
    }
}
