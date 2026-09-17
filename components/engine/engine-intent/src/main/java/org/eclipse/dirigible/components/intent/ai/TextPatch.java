/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Applies a model's anchored edits to a document as plain text splices.
 *
 * <p>
 * This is the apply half of the patch-shaped proposal contract (dirigible #6958). An assistant
 * editing an existing document proposes the ranges that change instead of retyping the file, so its
 * output cost tracks the size of the change rather than the size of the application.
 *
 * <p>
 * Two properties make that safe enough to be the default, and both are structural rather than
 * best-effort:
 *
 * <ul>
 * <li><b>It is a splice, never a parse-serialize round-trip.</b> Each edit locates a byte range by
 * an exact-match anchor and replaces that range; everything outside the spliced ranges is copied
 * verbatim, so comments, key order, indentation and formatting survive <em>by construction</em> -
 * the property a YAML load-and-dump cannot offer, and the one the DSL's diff-stability discipline
 * depends on. This is also why the anchor is text rather than a YAML path: a path implies resolving
 * a node through a parser, and the only parse this platform trusts for an intent is the one that
 * validates the finished document.</li>
 * <li><b>It is all-or-nothing.</b> Every anchor is resolved and every range checked for overlap
 * <em>before</em> anything is written. An anchor that matches nothing or matches twice is refused
 * with a message naming it - a half-applied patch is a corrupted document, and an ambiguous anchor
 * silently picking the first match is worse than a refusal the model can correct.</li>
 * </ul>
 *
 * <p>
 * Anchors are matched with line endings normalized, because the document travels through a browser
 * editor buffer that may carry {@code \r\n} while a model reproducing a line writes {@code \n}. The
 * splices themselves are still computed against the original text, and inserted content adopts the
 * document's own line ending, so a CRLF document stays CRLF outside the edited ranges and inside
 * them too.
 */
public final class TextPatch {

    /** The operations an edit may perform, and the wire values the model uses to name them. */
    private enum Op {

        REPLACE("replace"), INSERT_BEFORE("insertBefore"), INSERT_AFTER("insertAfter"), DELETE("delete");

        private final String wire;

        Op(String wire) {
            this.wire = wire;
        }

        static Op of(String wire) {
            return Arrays.stream(values())
                         .filter(op -> op.wire.equals(wire))
                         .findFirst()
                         .orElse(null);
        }

        static List<String> wireValues() {
            return Arrays.stream(values())
                         .map(op -> op.wire)
                         .toList();
        }
    }

    /**
     * The outcome of applying a patch.
     *
     * @param document the patched document, or {@code null} when the patch was refused
     * @param issues why the patch was refused, empty when it applied
     */
    public record Result(String document, List<String> issues) {
    }

    /** One resolved edit: the range of the original document it rewrites, and what it writes there. */
    private record Splice(int start, int end, String content, int ordinal) {
    }

    private TextPatch() {}

    /**
     * The JSON Schema of the {@code edits} argument, so every assistant that offers patch-shaped
     * proposals declares the same contract.
     *
     * @param documentName how the edited document is named to the model, e.g. {@code app.intent}
     * @return the schema as nested maps
     */
    public static Map<String, Object> editsSchema(String documentName) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("op", Map.of("type", "string", "enum", Op.wireValues(), "description",
                "What to do at the anchor: replace it, insert before or after it, or delete it."));
        properties.put("anchor",
                Map.of("type", "string", "description",
                        "Text copied EXACTLY from the current " + documentName + " - whole lines, including their indentation. "
                                + "It must occur exactly once in the document; when it does not, extend it with surrounding lines "
                                + "until it is unique. Leading/trailing whitespace is significant."));
        properties.put("content",
                Map.of("type", "string", "description",
                        "The replacement (op=replace) or the text to insert (op=insertBefore/insertAfter), with the same indentation "
                                + "style as its surroundings and no trailing newline. Omitted for op=delete."));
        return Map.of("type", "array", "description",
                "The edits to apply to the current " + documentName + ", each anchored on text copied exactly from it. "
                        + "Anchors are resolved against the document as given, never against another edit's output, "
                        + "and the edits may not overlap.",
                "items", Map.of("type", "object", "properties", properties, "required", List.of("op", "anchor")));
    }

    /**
     * Apply anchored edits to a document.
     *
     * @param document the document to patch
     * @param edits the model's {@code edits} argument
     * @return the patched document, or the reasons it was refused
     */
    public static Result apply(String document, JsonArray edits) {
        if (StringUtils.isEmpty(document)) {
            return refused(List.of("there is no current document to patch - propose the complete document instead of edits"));
        }
        if (edits == null || edits.isEmpty()) {
            return refused(List.of("the proposal carried no edits"));
        }
        Anchors anchors = Anchors.of(document);
        List<String> issues = new ArrayList<>();
        List<Splice> splices = new ArrayList<>();
        for (int i = 0; i < edits.size(); i++) {
            resolve(anchors, edits.get(i), i + 1, issues).ifPresent(splices::add);
        }
        issues.addAll(overlaps(splices));
        return issues.isEmpty() ? new Result(splice(document, splices), List.of()) : refused(issues);
    }

    /** Resolve one edit against the document, or record why it cannot be resolved. */
    private static Optional<Splice> resolve(Anchors anchors, JsonElement element, int ordinal, List<String> issues) {
        if (!element.isJsonObject()) {
            issues.add("edit #" + ordinal + " is not an object");
            return Optional.empty();
        }
        JsonObject edit = element.getAsJsonObject();
        Op op = Op.of(string(edit, "op"));
        if (op == null) {
            issues.add("edit #" + ordinal + " names an unknown op [" + string(edit, "op") + "]; use one of " + Op.wireValues());
            return Optional.empty();
        }
        String anchor = string(edit, "anchor");
        if (StringUtils.isEmpty(anchor)) {
            issues.add("edit #" + ordinal + " (" + op.wire + ") carries no anchor");
            return Optional.empty();
        }
        String content = StringUtils.defaultString(string(edit, "content"));
        if (op != Op.DELETE && content.isEmpty()) {
            issues.add("edit #" + ordinal + " (" + op.wire + ") carries no content; use op=delete to remove the anchor");
            return Optional.empty();
        }
        int[] range = anchors.locate(anchor);
        if (range == null) {
            issues.add("edit #" + ordinal + " (" + op.wire + "): the anchor was not found in the current document - " + preview(anchor)
                    + ". Copy the anchor exactly as it appears, including its indentation.");
            return Optional.empty();
        }
        if (range.length == 0) {
            issues.add("edit #" + ordinal + " (" + op.wire + "): the anchor is not unique - " + preview(anchor)
                    + ". Extend it with the surrounding lines until it matches exactly once.");
            return Optional.empty();
        }
        return Optional.of(switch (op) {
            case REPLACE -> new Splice(range[0], range[1], anchors.adopt(content), ordinal);
            case DELETE -> new Splice(range[0], range[1], "", ordinal);
            case INSERT_BEFORE -> new Splice(range[0], range[0], anchors.adopt(content), ordinal);
            case INSERT_AFTER -> new Splice(range[1], range[1], anchors.adopt(content), ordinal);
        });
    }

    /**
     * Overlapping edits, reported rather than applied: two edits rewriting the same text have no
     * defined outcome, and the model asked for something it did not mean.
     */
    private static List<String> overlaps(List<Splice> splices) {
        List<Splice> ordered = new ArrayList<>(splices);
        ordered.sort(Comparator.comparingInt(Splice::start)
                               .thenComparingInt(Splice::ordinal));
        List<String> issues = new ArrayList<>();
        for (int i = 1; i < ordered.size(); i++) {
            Splice previous = ordered.get(i - 1);
            Splice current = ordered.get(i);
            if (current.start() < previous.end()) {
                issues.add("edits #" + previous.ordinal() + " and #" + current.ordinal()
                        + " overlap; anchor each edit on a distinct part of the document");
            }
        }
        return issues;
    }

    /**
     * Write the resolved splices into the document, in document order. Everything between two spliced
     * ranges is copied verbatim - that copy is the whole preservation guarantee.
     */
    private static String splice(String document, List<Splice> splices) {
        List<Splice> ordered = new ArrayList<>(splices);
        ordered.sort(Comparator.comparingInt(Splice::start)
                               .thenComparingInt(Splice::ordinal));
        StringBuilder patched = new StringBuilder(document.length());
        int cursor = 0;
        for (Splice splice : ordered) {
            patched.append(document, cursor, splice.start())
                   .append(splice.content());
            cursor = splice.end();
        }
        return patched.append(document, cursor, document.length())
                      .toString();
    }

    private static Result refused(List<String> issues) {
        return new Result(null, List.copyOf(issues));
    }

    /** The anchor's first line, shortened - enough for the model to recognize which edit is meant. */
    private static String preview(String anchor) {
        String line = anchor.strip()
                            .lines()
                            .findFirst()
                            .orElse("");
        return "[" + StringUtils.abbreviate(line, 80) + "]";
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || !value.isJsonPrimitive() ? null : value.getAsString();
    }

    /**
     * The document as anchors are matched against it: line endings normalized to {@code \n}, with the
     * index map back to the original text. A model reproducing a line of a CRLF buffer writes
     * {@code \n}, and an anchor that cannot match is a feature that never applies.
     *
     * @param haystack the document with {@code \r\n} collapsed to {@code \n}
     * @param origin haystack index to original index, or {@code null} when the document has no
     *        {@code \r} and the two are the same text
     */
    private record Anchors(String haystack, int[] origin) {

        static Anchors of(String document) {
            if (document.indexOf('\r') < 0) {
                return new Anchors(document, null);
            }
            StringBuilder normalized = new StringBuilder(document.length());
            int[] origin = new int[document.length() + 1];
            for (int i = 0; i < document.length(); i++) {
                char character = document.charAt(i);
                int start = i;
                if (character == '\r' && i + 1 < document.length() && document.charAt(i + 1) == '\n') {
                    character = '\n';
                    i++;
                }
                // The whole CRLF pair maps to the position of its CR, so a splice at that boundary
                // lands before the pair rather than between its two characters.
                origin[normalized.length()] = start;
                normalized.append(character);
            }
            origin[normalized.length()] = document.length();
            return new Anchors(normalized.toString(), origin);
        }

        /**
         * Locate an anchor in the original document.
         *
         * @param anchor the anchor text
         * @return the {@code {start, end}} range in the original document, an empty array when the anchor
         *         is not unique, or {@code null} when it is absent
         */
        int[] locate(String anchor) {
            String needle = origin == null ? anchor : anchor.replace("\r\n", "\n");
            int first = haystack.indexOf(needle);
            if (first < 0) {
                return null;
            }
            if (haystack.indexOf(needle, first + 1) >= 0) {
                return new int[0];
            }
            return new int[] {index(first), index(first + needle.length())};
        }

        /** Inserted content adopts the document's own line ending. */
        String adopt(String content) {
            return origin == null ? content
                    : content.replace("\r\n", "\n")
                             .replace("\n", "\r\n");
        }

        private int index(int normalized) {
            return origin == null ? normalized : origin[normalized];
        }
    }
}
