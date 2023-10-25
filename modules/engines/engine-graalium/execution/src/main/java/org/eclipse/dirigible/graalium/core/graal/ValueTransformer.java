package org.eclipse.dirigible.graalium.core.graal;

import org.graalvm.polyglot.Value;

public class ValueTransformer {
    private ValueTransformer() {}

    /**
     * Transform value.
     *
     * @param value the value
     * @return the object
     */
    public static  Object transformValue(Value value) {
        if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isDate()) {
            return value.asDate();
        } else if (value.isDuration()) {
            return value.asDuration();
        } else if (value.isNull()) {
            return null;
        } else if (value.isNumber()) {
            if (value.fitsInDouble()) {
                return value.asDouble();
            } else if (value.fitsInFloat()) {
                return value.asFloat();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInShort()) {
                return value.asShort();
            } else if (value.fitsInByte()) {
                return value.asByte();
            }
        } else if (value.isString()) {
            return value.asString();
        } else if (value.isTime()) {
            return value.asTime();
        } else if (value.isTimeZone()) {
            return value.asTimeZone();
        }
        return null;
    }
}
