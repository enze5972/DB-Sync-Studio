package com.dbsyncstudio.core.transform.transformers;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ScriptJsResultNormalizer {

    private static final Source NORMALIZER_SOURCE = buildNormalizerSource();

    private ScriptJsResultNormalizer() {
    }

    static Object normalize(Context context, Value rawValue, String resultType, boolean allowNull, int maxOutputLength) {
        if (context == null) {
            throw new IllegalArgumentException("script_js normalization requires an active context");
        }
        try {
            Value normalizer = context.eval(NORMALIZER_SOURCE);
            Map<String, Object> options = new LinkedHashMap<String, Object>();
            options.put("resultType", resultType == null ? "auto" : resultType);
            options.put("allowNull", Boolean.valueOf(allowNull));
            options.put("maxOutputLength", Integer.valueOf(maxOutputLength));
            Value normalized = normalizer.execute(rawValue, ProxyObject.fromMap(Collections.unmodifiableMap(options)));
            if (normalized == null || normalized.isNull()) {
                return null;
            }
            if (normalized.isBoolean()) {
                return Boolean.valueOf(normalized.asBoolean());
            }
            if (normalized.isString()) {
                return normalized.asString();
            }
            if (normalized.isNumber()) {
                if (normalized.fitsInInt()) {
                    return Integer.valueOf(normalized.asInt());
                }
                if (normalized.fitsInLong()) {
                    return Long.valueOf(normalized.asLong());
                }
                if (normalized.fitsInDouble()) {
                    return Double.valueOf(normalized.asDouble());
                }
            }
            return normalized.as(Object.class);
        } catch (PolyglotException ex) {
            throw new IllegalArgumentException(buildErrorMessage(ex), ex);
        }
    }

    private static Source buildNormalizerSource() {
        String script =
                "(function(value, options) {\n"
                        + "  var resultType = options.resultType || 'auto';\n"
                        + "  var allowNull = options.allowNull !== false;\n"
                        + "  var maxOutputLength = typeof options.maxOutputLength === 'number' ? options.maxOutputLength : 10000;\n"
                        + "  function fail(message) {\n"
                        + "    throw new Error(message);\n"
                        + "  }\n"
                        + "  function stringify(value) {\n"
                        + "    try {\n"
                        + "      return JSON.stringify(value);\n"
                        + "    } catch (err) {\n"
                        + "      var message = err && err.message ? String(err.message) : String(err);\n"
                        + "      if (message.indexOf('BigInt') >= 0) {\n"
                        + "        fail('script_js result cannot be serialized because it contains BigInt');\n"
                        + "      }\n"
                        + "      if (message.toLowerCase().indexOf('circular') >= 0 || message.toLowerCase().indexOf('cyclic') >= 0) {\n"
                        + "        fail('script_js result cannot be serialized because it contains circular references');\n"
                        + "      }\n"
                        + "      fail('script_js result cannot be serialized: ' + message);\n"
                        + "    }\n"
                        + "  }\n"
                        + "  function checkLength(text) {\n"
                        + "    if (text != null && String(text).length > maxOutputLength) {\n"
                        + "      fail('script_js output exceeds maxOutputLength=' + maxOutputLength);\n"
                        + "    }\n"
                        + "  }\n"
                        + "  if (value === null || value === undefined) {\n"
                        + "    if (!allowNull) {\n"
                        + "      fail('script_js returned null or undefined but allowNull=false');\n"
                        + "    }\n"
                        + "    if (resultType === 'json') {\n"
                        + "      return 'null';\n"
                        + "    }\n"
                        + "    return null;\n"
                        + "  }\n"
                        + "  var kind = typeof value;\n"
                        + "  var normalized;\n"
                        + "  if (resultType === 'json') {\n"
                        + "    if (kind === 'string') {\n"
                        + "      try {\n"
                        + "        JSON.parse(value);\n"
                        + "      } catch (err) {\n"
                        + "        fail('resultType=json requires a valid JSON string');\n"
                        + "      }\n"
                        + "      normalized = value;\n"
                        + "    } else if (kind === 'number' || kind === 'boolean') {\n"
                        + "      normalized = JSON.stringify(value);\n"
                        + "    } else if (kind === 'object') {\n"
                        + "      normalized = stringify(value);\n"
                        + "    } else {\n"
                        + "      fail('resultType=json does not support return type: ' + kind);\n"
                        + "    }\n"
                        + "    checkLength(normalized);\n"
                        + "    return normalized;\n"
                        + "  }\n"
                        + "  if (resultType === 'text') {\n"
                        + "    if (kind === 'object') {\n"
                        + "      normalized = stringify(value);\n"
                        + "    } else {\n"
                        + "      normalized = String(value);\n"
                        + "    }\n"
                        + "    checkLength(normalized);\n"
                        + "    return normalized;\n"
                        + "  }\n"
                        + "  if (kind === 'object') {\n"
                        + "    normalized = stringify(value);\n"
                        + "    checkLength(normalized);\n"
                        + "    return normalized;\n"
                        + "  }\n"
                        + "  if (kind === 'bigint') {\n"
                        + "    fail('script_js result cannot be a BigInt');\n"
                        + "  }\n"
                        + "  if (kind === 'symbol' || kind === 'function') {\n"
                        + "    fail('script_js result type is not supported: ' + kind);\n"
                        + "  }\n"
                        + "  if (kind === 'number' || kind === 'boolean') {\n"
                        + "    normalized = value;\n"
                        + "  } else {\n"
                        + "    normalized = String(value);\n"
                        + "  }\n"
                        + "  checkLength(normalized);\n"
                        + "  return normalized;\n"
                        + "})";
        try {
            return Source.newBuilder("js", script, "script_js_result_normalizer").cached(true).buildLiteral();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build script_js result normalizer", ex);
        }
    }

    private static String buildErrorMessage(PolyglotException ex) {
        String message = ex == null ? null : ex.getMessage();
        if (message == null || message.trim().length() == 0) {
            return "script_js normalization failed";
        }
        return "script_js normalization failed: " + sanitizeMessage(message);
    }

    private static String sanitizeMessage(String message) {
        String normalized = message.replace('\n', ' ').replace('\r', ' ');
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }
}
