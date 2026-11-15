/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;

/** The values the model read: the header fields, or one line item. */
public final class CaptureValues {

    private final JsonObject source;
    private final boolean header;

    CaptureValues(JsonObject source, boolean header) {
        this.source = source;
        this.header = header;
    }

    /**
     * The value as text. Numbers come as plain decimal text, booleans as {@code true}/{@code false},
     * dates as the ISO string the model returned.
     *
     * @param name the field name
     * @return the text, or null when absent, JSON null or blank
     */
    public String text(String name) {
        JsonElement element = source.get(name);
        if (element != null && header) {
            element = element.isJsonObject() ? element.getAsJsonObject()
                                                      .get("value")
                    : null;
        }
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                try {
                    return new BigDecimal(primitive.getAsString()).toPlainString();
                } catch (NumberFormatException e) {
                    return primitive.getAsString();
                }
            }
            String text = primitive.getAsString();
            return text.isBlank() ? null : text.trim();
        }
        return element.toString();
    }

    /**
     * The model's confidence in a value.
     *
     * @param name the field name; ignored for a line item, whose confidence is the line's
     * @return the confidence, {@link CaptureConfidence#LOW} when unknown
     */
    public CaptureConfidence confidence(String name) {
        JsonElement holder = header ? source.get(name) : source;
        if (holder == null || !holder.isJsonObject()) {
            return CaptureConfidence.LOW;
        }
        JsonElement confidence = holder.getAsJsonObject()
                                       .get("confidence");
        if (confidence == null || !confidence.isJsonPrimitive()) {
            return CaptureConfidence.LOW;
        }
        return CaptureConfidence.parse(confidence.getAsString());
    }
}
