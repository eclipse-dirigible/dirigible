package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import com.google.gson.reflect.TypeToken;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;

class VariableValueSerializer {

    static Object serializeValue(Object value) {
        if (null == value || isPrimitiveWrapperOrString(value)) {
            return value;
        }
        return JsonHelper.toJson(value);
    }

    private static boolean isPrimitiveWrapperOrString(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    static <T> T deserializeValue(Object raw, Class<T> type) throws InvalidVariableException {
        if (null == raw) {
            return null;
        }

        if (type.isInstance(raw)) {
            return type.cast(raw);
        }

        if (raw instanceof String stringValue) {
            if (type.equals(String.class)) {
                return type.cast(stringValue);
            }

            return JsonHelper.fromJson(stringValue, type);
        }

        throw new InvalidVariableException(
                "Invalid variable value [" + raw + "]. " + "Expected type [" + type + "], got [" + raw.getClass() + "]");
    }

    static <T> T deserializeValue(Object raw, TypeToken<T> typeToken) throws InvalidVariableException {
        if (null == raw) {
            return null;
        }

        Class<?> rawType = typeToken.getRawType();

        if (rawType.isInstance(raw)) {
            return (T) raw;
        }

        if (raw instanceof String stringValue) {
            if (rawType.equals(String.class)) {
                return (T) stringValue;
            }
            return JsonHelper.fromJson(stringValue, typeToken);
        }

        throw new InvalidVariableException(
                "Invalid variable value [" + raw + "]. Expected type [" + typeToken + "], got [" + raw.getClass() + "]");
    }

}
