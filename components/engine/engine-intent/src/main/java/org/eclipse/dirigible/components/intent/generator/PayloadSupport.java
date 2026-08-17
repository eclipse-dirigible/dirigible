/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * The declared <b>payload</b> of an outward-facing entry - the message an application sends to
 * another system, spelled out instead of being "the whole record, as stored".
 *
 * <p>
 * Forwarding the entity JSON makes every column a public contract, so adding a field silently
 * changes what the outside world receives; and a real integration contract is an <em>envelope</em>
 * (a type, a version, a minted idempotency key, a timestamp, a tenant, a couple of record fields),
 * which no arrangement of entity columns can produce. A {@code payload:} block declares that
 * envelope in the model:
 *
 * <pre>
 * payload:
 *   type: "user.assignment.requested"     # literal
 *   version: 1
 *   messageId: "{uuid}"                   # minted per message
 *   tenantId: "{tenant}"                  # execution context
 *   appId: "&#64;config:APP_ID"                # configuration, as elsewhere
 *   email: email                          # a field of the record
 *   role: role.name                       # one hop off a to-one relation
 *   requestedAt: "{now}"
 * </pre>
 *
 * <p>
 * <b>The value forms are borrowed from {@code notify}, not invented</b> - a value is a literal, a
 * direct field, or a one-hop {@code relation.field} of a to-one relation, resolved by the very same
 * {@link NotificationSupport.Resolver} (so the generated listener loads a related record once by FK
 * id, exactly as a notification does). Multi-hop is rejected. {@code @config:KEY} resolves
 * server-side, as in the integration's own {@code url:}. The context tokens are a <b>closed set</b>
 * - uuid, now, tenant, user - and an unknown one is a parse error, never an empty string in a
 * shipped message.
 *
 * <p>
 * That cap is the point: three value forms and four tokens express a contract without the construct
 * growing into a transformation language. A payload that needs more than this is an algorithm and
 * belongs in a hand-written handler.
 *
 * <p>
 * A bare word that is not a field or a to-one relation of the record is a <b>literal</b> - the only
 * way a payload can carry a one-word constant, since YAML quoting does not survive the parse. Brace
 * it ({@code "{email}"}) to have the reference checked.
 */
public final class PayloadSupport {

    /** A per-message idempotency key. */
    static final String TOKEN_UUID = "uuid";

    /** The send time, as an ISO-8601 instant. */
    static final String TOKEN_NOW = "now";

    /** The id of the tenant the send runs for. */
    static final String TOKEN_TENANT = "tenant";

    /** The name of the user behind the write that raised the event. */
    static final String TOKEN_USER = "user";

    /** The closed set of context tokens, in the order the "valid: ..." message lists them. */
    private static final Map<String, String> CONTEXT_TOKENS = contextTokens();

    /** A dotted path of identifiers - one segment is a field, two a one-hop relation walk. */
    private static final Pattern DOTTED_PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*");

    /** An explicitly braced reference: a context token, a field, or a one-hop relation walk. */
    private static final Pattern BRACED = Pattern.compile("\\{([^{}]*)\\}");

    private static final String CONFIG_PREFIX = "@config:";

    private PayloadSupport() {}

    /**
     * One resolved payload entry: the JSON key and the Java expression that produces its value.
     *
     * @param key the payload key, as authored
     * @param expression the Java expression evaluated against the loaded record
     */
    public record Entry(String key, String expression) {
    }

    /**
     * A resolved payload: the entries in declaration order plus the one-hop relation loads the
     * generated listener must perform before building them.
     *
     * @param entries the payload entries, in declaration order
     * @param loads the one-hop relation loads the entries read from
     */
    public record Plan(List<Entry> entries, List<NotificationSupport.RelationLoad> loads) {
    }

    /**
     * Validate a declared payload against the record it is built from, collecting every problem rather
     * than stopping at the first. Cross-model {@code relation.field} paths are accepted here and
     * resolved at generation time (the owner's model is not readable from the parser), which is the
     * same division {@code notify} makes.
     *
     * @param payload the authored payload, may be empty
     * @param entity the entity the event carries, or {@code null} when it could not be resolved
     * @param byName all LOCAL entities by name
     * @param subject how to name the owning entry in a message, e.g. {@code integration [pushOrder]}
     * @param issues the collecting issue list
     */
    public static void validate(Map<String, Object> payload, EntityIntent entity, Map<String, EntityIntent> byName, String subject,
            List<String> issues) {
        if (payload == null || payload.isEmpty() || entity == null) {
            return;
        }
        for (Map.Entry<String, Object> declared : payload.entrySet()) {
            String key = declared.getKey();
            Object value = declared.getValue();
            if (value instanceof Map || value instanceof Iterable) {
                issues.add(subject + " payload [" + key
                        + "] must be a scalar - a nested object or list is not a payload value form (literal, field, relation.field,"
                        + " @config:KEY or a context token)");
                continue;
            }
            if (!(value instanceof String text)) {
                continue; // a number / boolean / null literal needs no resolution
            }
            validateString(text.trim(), key, entity, byName, subject, issues);
        }
    }

    /**
     * Translate a declared payload into the entries and relation loads the generated listener renders.
     *
     * @param payload the authored payload
     * @param entity the entity the event carries
     * @param byName all LOCAL entities by name
     * @param compositionParents composition-parent map (to resolve a load target's perspective)
     * @param crossModel resolver for a cross-model relation's owner facts, or {@code null}
     * @return the plan, or {@code null} when the payload is empty
     * @throws IllegalArgumentException when a value cannot be resolved - the caller reports the drop
     *         with the precise reason instead of sending a message with a hole in it
     */
    public static Plan plan(Map<String, Object> payload, EntityIntent entity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel) {
        if (payload == null || payload.isEmpty() || entity == null) {
            return null;
        }
        NotificationSupport.Resolver resolver = NotificationSupport.resolver(entity, byName, compositionParents, crossModel);
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, Object> declared : payload.entrySet()) {
            entries.add(new Entry(declared.getKey(), expression(declared.getKey(), declared.getValue(), entity, resolver)));
        }
        return new Plan(entries, resolver.loads());
    }

    /**
     * The glue keys a payload contributes, always present so the template can branch on them - an
     * undefined Velocity variable renders as its own literal name, so a template must never depend on a
     * key being absent.
     *
     * @param plan the resolved payload, or {@code null} when the entry declares none
     * @return the {@code hasPayload} / {@code payloadFields} keys
     */
    public static Map<String, Object> payloadFields(Plan plan) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("hasPayload", plan != null);
        List<Map<String, Object>> rendered = new ArrayList<>();
        if (plan != null) {
            for (Entry entry : plan.entries()) {
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("key", entry.key());
                // The key is rendered pre-quoted: it goes into a Java string literal, and the template
                // must not be the place that decides how a key is escaped.
                field.put("keyLiteral", NotificationSupport.quote(entry.key()));
                field.put("expression", entry.expression());
                rendered.add(field);
            }
        }
        fields.put("payloadFields", rendered);
        return fields;
    }

    private static void validateString(String value, String key, EntityIntent entity, Map<String, EntityIntent> byName, String subject,
            List<String> issues) {
        if (value.startsWith(CONFIG_PREFIX)) {
            if (value.substring(CONFIG_PREFIX.length())
                     .isBlank()) {
                issues.add(subject + " payload [" + key + "] has an empty @config: key");
            }
            return;
        }
        var braced = BRACED.matcher(value);
        if (braced.matches()) {
            String inner = braced.group(1)
                                 .trim();
            if (CONTEXT_TOKENS.containsKey(inner)) {
                return;
            }
            if (DOTTED_PATH.matcher(inner)
                           .matches()
                    && inner.indexOf('.') >= 0) {
                validatePath(inner, key, entity, byName, subject, issues, true);
                return;
            }
            // A braced single word the author expected to be substituted: most likely a mistyped token,
            // possibly a field that does not exist. The message offers both readings rather than guessing.
            if (fieldOf(entity, inner) == null && toOneRelation(entity, inner) == null) {
                issues.add(subject + " payload [" + key + "] references [" + inner + "], which is neither a context token (valid: "
                        + tokenList() + ") nor a field or a to-one relation of [" + entity.getName() + "]");
            }
            return;
        }
        if (value.indexOf('{') >= 0 || value.indexOf('}') >= 0) {
            issues.add(subject + " payload [" + key + "] value [" + value
                    + "] mixes braces into text - a payload value is one whole value (a literal, a field, a one-hop relation.field,"
                    + " @config:KEY or a context token), never an interpolated string");
            return;
        }
        if (value.indexOf('.') >= 0 && DOTTED_PATH.matcher(value)
                                                  .matches()) {
            validatePath(value, key, entity, byName, subject, issues, false);
        }
        // Anything else is a literal - a URL, a one-word constant, a sentence.
    }

    /**
     * Check a dotted path the author may have meant as a relation walk. When it was NOT braced, a head
     * naming no to-one relation is simply a literal (a message type reads exactly like a path), so
     * nothing is reported; once the head does name one, the author clearly meant a walk and a bad one
     * is an error.
     */
    private static void validatePath(String path, String key, EntityIntent entity, Map<String, EntityIntent> byName, String subject,
            List<String> issues, boolean braced) {
        String[] segments = path.split("\\.");
        String head = segments[0];
        RelationIntent relation = toOneRelation(entity, head);
        if (relation == null) {
            if (braced) {
                issues.add(subject + " payload [" + key + "] references [" + path + "], whose first segment [" + head
                        + "] is not a to-one relation of [" + entity.getName() + "]");
            }
            return; // an unbraced dotted literal, e.g. a message type
        }
        if (segments.length > 2) {
            issues.add(subject + " payload [" + key + "] references [" + path
                    + "], which walks more than one relation - a payload value resolves at most one hop (relation.field)");
            return;
        }
        if (relation.getModel() != null && !relation.getModel()
                                                    .isBlank()) {
            return; // cross-model: the owner's fields are resolved at generation time
        }
        EntityIntent target = byName.get(relation.getTo());
        if (target != null && fieldOf(target, segments[1]) == null) {
            issues.add(subject + " payload [" + key + "] references [" + path + "], but [" + segments[1] + "] is not a field of ["
                    + relation.getTo() + "]");
        }
    }

    private static String expression(String key, Object value, EntityIntent entity, NotificationSupport.Resolver resolver) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean flag) {
            return flag.toString();
        }
        if (value instanceof Number number) {
            return numberLiteral(number);
        }
        String text = value.toString()
                           .trim();
        if (text.startsWith(CONFIG_PREFIX)) {
            return "Configurations.get(" + NotificationSupport.quote(text.substring(CONFIG_PREFIX.length())
                                                                         .trim())
                    + ")";
        }
        var braced = BRACED.matcher(text);
        if (braced.matches()) {
            String inner = braced.group(1)
                                 .trim();
            String token = CONTEXT_TOKENS.get(inner);
            return token != null ? token : resolved(key, inner, entity, resolver);
        }
        if (isReference(text, entity)) {
            return resolved(key, text, entity, resolver);
        }
        return NotificationSupport.quote(text);
    }

    /** An unbraced value is a reference only when its head actually names something on the record. */
    private static boolean isReference(String text, EntityIntent entity) {
        if (!DOTTED_PATH.matcher(text)
                        .matches()) {
            return false;
        }
        String head = text.split("\\.")[0];
        return text.indexOf('.') < 0 ? fieldOf(entity, head) != null || toOneRelation(entity, head) != null
                : toOneRelation(entity, head) != null;
    }

    private static String resolved(String key, String path, EntityIntent entity, NotificationSupport.Resolver resolver) {
        String access = resolver.access(path, false);
        if (access == null) {
            throw new IllegalArgumentException(
                    "payload [" + key + "] references [" + path + "], which cannot be resolved on [" + entity.getName() + "]");
        }
        return access;
    }

    /**
     * An integral number becomes an {@code int} literal (a {@code long} one when it does not fit), a
     * fractional one a {@code BigDecimal} so the authored precision survives into the JSON.
     */
    private static String numberLiteral(Number number) {
        BigDecimal decimal = new BigDecimal(number.toString());
        if (decimal.stripTrailingZeros()
                   .scale() > 0) {
            return "new java.math.BigDecimal(\"" + decimal.toPlainString() + "\")";
        }
        long value = decimal.longValue();
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? Long.toString(value) : value + "L";
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private static RelationIntent toOneRelation(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static String tokenList() {
        Set<String> names = new LinkedHashSet<>(CONTEXT_TOKENS.keySet());
        return "{" + String.join("}, {", names) + "}";
    }

    private static Map<String, String> contextTokens() {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put(TOKEN_UUID, "java.util.UUID.randomUUID().toString()");
        tokens.put(TOKEN_NOW, "java.time.Instant.now().toString()");
        tokens.put(TOKEN_TENANT, "org.eclipse.dirigible.sdk.core.Tenant.getId()");
        tokens.put(TOKEN_USER, "org.eclipse.dirigible.sdk.security.User.getName()");
        return Collections.unmodifiableMap(tokens);
    }
}
