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
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/** What a provider read off one document. */
public final class CaptureReading {

    private final String provider;
    private final String model;
    private final int inputTokens;
    private final int outputTokens;
    private final String answer;
    private final CaptureValues fields;
    private final List<CaptureValues> items;

    private CaptureReading(String provider, CaptureAnswer answer, CaptureValues fields, List<CaptureValues> items) {
        this.provider = provider;
        this.model = answer.model();
        this.inputTokens = answer.inputTokens();
        this.outputTokens = answer.outputTokens();
        this.answer = answer.json();
        this.fields = fields;
        this.items = items;
    }

    static CaptureReading parse(String provider, CaptureAnswer answer) {
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(answer.json() == null ? "" : answer.json());
            if (!parsed.isJsonObject()) {
                throw new CaptureException(CaptureException.Kind.UPSTREAM, "the capture provider's answer is not a JSON object");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new CaptureException(CaptureException.Kind.UPSTREAM, "the capture provider's answer is not valid JSON", e);
        }
        JsonElement fieldsElement = root.get("fields");
        if (fieldsElement == null || !fieldsElement.isJsonObject()) {
            throw new CaptureException(CaptureException.Kind.UPSTREAM, "the capture provider's answer has no [fields] object");
        }
        List<CaptureValues> items = new ArrayList<>();
        JsonElement itemsElement = root.get("items");
        if (itemsElement != null && itemsElement.isJsonArray()) {
            for (JsonElement item : itemsElement.getAsJsonArray()) {
                if (item.isJsonObject()) {
                    items.add(new CaptureValues(item.getAsJsonObject(), false));
                }
            }
        }
        return new CaptureReading(provider, answer, new CaptureValues(fieldsElement.getAsJsonObject(), true), List.copyOf(items));
    }

    /**
     * The provider that read the document.
     *
     * @return the provider name
     */
    public String getProvider() {
        return provider;
    }

    /**
     * The model that read the document.
     *
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * Tokens read.
     *
     * @return the input token count
     */
    public int getInputTokens() {
        return inputTokens;
    }

    /**
     * Tokens written.
     *
     * @return the output token count
     */
    public int getOutputTokens() {
        return outputTokens;
    }

    /**
     * The raw JSON the provider returned.
     *
     * @return the answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * The header values.
     *
     * @return the values
     */
    public CaptureValues fields() {
        return fields;
    }

    /**
     * The line items, one per line.
     *
     * @return the items, empty when the schema has none
     */
    public List<CaptureValues> items() {
        return items;
    }
}
