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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.sdk.component.Beans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads an uploaded document with the configured vision-model provider.
 *
 * <p>
 * Every configuration value is read per call, never cached, so tenant overrides and tests take
 * effect immediately.
 */
public final class Captures {

    private static final Logger LOGGER = LoggerFactory.getLogger(Captures.class);

    private static final String BUILT_IN_PROVIDER = "anthropic";
    private static final long DEFAULT_MAX_FILE_SIZE = 10_485_760L;
    private static final Set<String> CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");

    private static final String PREAMBLE = "You are reading a scanned or photographed business document, a receipt or an invoice. "
            + "It may be in any language and script; do not translate or transliterate what it prints. "
            + "Return each requested field exactly as printed on the document. "
            + "Return dates as ISO yyyy-MM-dd. Return amounts as plain numbers with a dot as the decimal separator, "
            + "with no thousands separator and no currency sign. "
            + "Return null for a field that is not on the document rather than guessing. "
            + "Set each field's confidence to high only when the value is clearly legible and unambiguous, "
            + "to medium when it is legible but its meaning is uncertain, " + "and to low when it is partly illegible or inferred. "
            + "List every line item in document order.";

    private Captures() {}

    /**
     * Whether a capture can run: the selected provider is the built-in one with an API key, or a
     * registered custom provider.
     *
     * @return true when a call would reach a provider
     */
    public static boolean isConfigured() {
        String name = providerName();
        if (BUILT_IN_PROVIDER.equals(name)) {
            String key = DirigibleConfig.CAPTURE_API_KEY.getStringValue();
            return key != null && !key.isBlank();
        }
        return findProvider(name) != null;
    }

    /**
     * Send the document to the configured provider and return the reading.
     *
     * @param schema what to read
     * @param instructions the caller's extra instructions, may be null
     * @param fileName the file's name, may be null
     * @param contentType the file's media type, may be null
     * @param content the file's bytes
     * @param hints values already decoded from a machine-readable code on the document, may be null
     * @return the reading
     * @throws CaptureException when the document cannot be read
     */
    public static CaptureReading read(CaptureSchema schema, String instructions, String fileName, String contentType, byte[] content,
            Map<String, String> hints) {
        if (content == null || content.length == 0) {
            throw new CaptureException(CaptureException.Kind.UNREADABLE, "the document is empty");
        }
        long max = maxFileSize();
        if (content.length > max) {
            throw new CaptureException(CaptureException.Kind.UNREADABLE,
                    "the document is " + content.length + " bytes, more than the allowed " + max);
        }
        String type = resolveContentType(fileName, contentType);
        if (!CONTENT_TYPES.contains(type)) {
            throw new CaptureException(CaptureException.Kind.UNREADABLE,
                    "the document type [" + type + "] is not supported; use JPEG, PNG, GIF, WebP or PDF");
        }
        CaptureProvider provider = provider();
        CaptureRequest request = new CaptureRequest(buildInstructions(instructions, hints), schema.toJsonSchema(), fileName, type, content);
        CaptureAnswer answer = provider.extract(request);
        return CaptureReading.parse(provider.name(), answer);
    }

    static String buildInstructions(String instructions, Map<String, String> hints) {
        StringBuilder text = new StringBuilder(PREAMBLE);
        if (instructions != null && !instructions.isBlank()) {
            text.append("\n\n")
                .append(instructions.trim());
        }
        if (hints != null && !hints.isEmpty()) {
            text.append("\n\nValues decoded from a machine-readable code on the document (exact, prefer them over what you read):");
            hints.forEach((key, value) -> text.append('\n')
                                              .append(key)
                                              .append(": ")
                                              .append(value));
        }
        return text.toString();
    }

    static String resolveContentType(String fileName, String contentType) {
        String type = contentType == null ? ""
                : contentType.split(";")[0].trim()
                                           .toLowerCase(Locale.ROOT);
        if (!type.isEmpty() && !"application/octet-stream".equals(type)) {
            return "image/jpg".equals(type) ? "image/jpeg" : type;
        }
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".webp")) {
            return "image/webp";
        } else if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        return type.isEmpty() ? "unknown" : type;
    }

    private static long maxFileSize() {
        String configured = DirigibleConfig.CAPTURE_MAX_FILE_SIZE.getStringValue();
        try {
            return Long.parseLong(configured.trim());
        } catch (RuntimeException e) {
            return DEFAULT_MAX_FILE_SIZE;
        }
    }

    private static String providerName() {
        String name = DirigibleConfig.CAPTURE_PROVIDER.getStringValue();
        return name == null || name.isBlank() ? BUILT_IN_PROVIDER : name.trim();
    }

    private static CaptureProvider provider() {
        String name = providerName();
        if (BUILT_IN_PROVIDER.equals(name)) {
            return new AnthropicCaptureProvider();
        }
        CaptureProvider found = findProvider(name);
        if (found == null) {
            throw new CaptureException(CaptureException.Kind.NOT_CONFIGURED, "no capture provider named [" + name + "] is registered");
        }
        return found;
    }

    private static CaptureProvider findProvider(String name) {
        try {
            List<CaptureProvider> beans = Beans.getAll(CaptureProvider.class);
            for (CaptureProvider bean : beans) {
                if (name.equals(bean.name())) {
                    return bean;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Cannot list the capture providers", e);
        }
        return null;
    }
}
