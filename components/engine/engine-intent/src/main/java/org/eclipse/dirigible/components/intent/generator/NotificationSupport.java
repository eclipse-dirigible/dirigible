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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * Translates a {@link NotificationIntent}'s author-facing fields into the Java the generated
 * {@code @Listener} pastes in. Kept free of Spring/IO so the tricky bits are unit-tested directly.
 *
 * <p>
 * A value or {@code {placeholder}} is one of: a <b>reserved link token</b> ({@code appUrl} - the
 * application's external base URL, see {@link #APP_URL_TOKEN}; {@code recordUrl} / {@code inboxUrl}
 * - the ready-made deep links, see {@link #RECORD_URL_TOKEN}), a <b>direct field</b> of the event
 * entity (rendered {@code entity.<PascalField>}), or a one-hop <b>{@code relation.field}</b> of a
 * to-one relation (rendered against a related entity the listener loads once by FK id - the same
 * one-hop mechanism the decision resolvers use, see {@link ProcessResolverSupport}). Multi-hop
 * paths are not supported. The {@code when} guard supports a single {@code field ==|!= literal}
 * comparison on a direct field.
 *
 * <p>
 * Inside a <b>fan-out</b> the entity every bare path resolves against is the ROW; a placeholder
 * (never the recipient) reaches the record the rows hang off through the explicit
 * {@link NotifySupport#RECORD_SCOPE} prefix - {@code {record.<field>}}.
 */
public final class NotificationSupport {

    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)\\}");
    private static final Pattern SIMPLE_COMPARISON = Pattern.compile("\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=)\\s*(.+?)\\s*");

    /**
     * The reserved {@code {appUrl}} placeholder name - a config-backed token, not an entity field or
     * relation. Resolves to the application's external base URL ({@link DirigibleConfig#APP_BASE_URL},
     * tenant-overridable), so a body can compose a deep link by hand, e.g. {@code "Review it here:
     * {appUrl}/orders/{id}"}. Deliberately supplies only the origin - the concrete per-entity route is
     * the template layer's knowledge, not the intent layer's (see the engine-intent guide's
     * path-agnostic rule), so the author appends the rest of the path as plain text and other
     * placeholders.
     * <p>
     * Resolved per dispatch inside the sending tenant's own configuration scope - the generated
     * listener (via {@code ListenerClassConsumer}) loads that tenant's overrides before invoking the
     * handler, the same way {@code TenantConfigurationInitFilter} does for HTTP requests, so a tenant
     * override of {@code DIRIGIBLE_APP_BASE_URL} takes effect here, falling back to the global
     * environment/default value when the tenant has not set one.
     */
    static final String APP_URL_TOKEN = "appUrl";

    /** The Java expression {@link #APP_URL_TOKEN} resolves to. */
    private static final String APP_URL_EXPRESSION =
            "org.eclipse.dirigible.sdk.core.Configurations.get(\"" + DirigibleConfig.APP_BASE_URL.getKey() + "\", \"\")";

    /**
     * The reserved {@code {recordUrl}} placeholder name - the ready-made deep link to the record the
     * message is about ("you have an approval waiting" is useless without one), where
     * {@link #APP_URL_TOKEN} supplies only the origin and leaves the author to type the route by hand.
     * <p>
     * It resolves to a bare Java identifier, not to an expression: the local is <b>declared by the
     * events template</b>, which is the layer that knows the generated application's routes. The intent
     * layer contributes only model facts - the entity and its key property, carried in the glue as
     * {@code recordUrlEntity} / {@code recordUrlKeyProperty} - so the path-agnostic rule holds (see the
     * engine-intent guide) and a change to the generated app's URL layout is a template change alone.
     * <p>
     * Inside a fan-out it links the <b>row</b>, like every other bare path: the row is what that
     * message is about. The anchor record is reachable for VALUES through {@code {record.<field>}}, but
     * not as a link - a fan-out that wants to point at its anchor should say so with {@code {appUrl}}.
     */
    static final String RECORD_URL_TOKEN = "recordUrl";

    /**
     * The reserved {@code {inboxUrl}} placeholder name - the deep link to the recipient's process
     * Inbox, the other half of "a notification cannot carry a link to the record or task". Declared by
     * the events template exactly like {@link #RECORD_URL_TOKEN}, and needing no model facts at all.
     */
    static final String INBOX_URL_TOKEN = "inboxUrl";

    private NotificationSupport() {}

    /**
     * A one-hop to-one relation the listener must load before building the message. A cross-model
     * relation ({@code crossModel} true) points at an entity owned by another model, so the generated
     * listener imports the OWNER's {@code targetModel}/{@code targetProject} generated
     * Entity/Repository (the same registry-wide-compile mechanism the relation links / personal
     * assignee use), not this project's.
     */
    public record RelationLoad(String local, String targetEntity, String targetPerspective, String fkProperty, boolean crossModel,
            String targetModel, String targetProject) {
    }

    /**
     * The glue projection of a set of relation loads - the shape every consuming template iterates
     * ({@code local} / {@code targetEntity} / {@code targetPerspective} / {@code fkProperty} plus the
     * cross-model coordinates the generation pipeline turns into the OWNER-package import). Kept beside
     * the record so the two cannot drift.
     *
     * @param resolved the loads, in first-use order
     * @return one map per load
     */
    public static List<Map<String, Object>> loadFields(List<RelationLoad> resolved) {
        List<Map<String, Object>> loads = new ArrayList<>();
        for (RelationLoad load : resolved) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("local", load.local());
            entry.put("targetEntity", load.targetEntity());
            entry.put("targetPerspective", load.targetPerspective());
            entry.put("fkProperty", load.fkProperty());
            // Cross-model recipient/placeholder: the owner's model alias + project drive the OWNER-package
            // import in the generated listener/job (the generation pipeline picks the gen folder from these).
            entry.put("crossModel", load.crossModel());
            entry.put("targetModel", load.targetModel());
            entry.put("targetProject", load.targetProject());
            loads.add(entry);
        }
        return loads;
    }

    /**
     * Resolves a cross-model to-one relation's target facts so a {@code relation.field} recipient or
     * placeholder can reference an entity owned by another model. The lookup performs the IO (reads the
     * owner's {@code .model} via {@code CrossModelSupport}); {@code NotificationSupport} stays free of
     * Spring/IO so its path logic remains directly unit-testable. Returns {@code null} when the
     * relation is not a resolvable cross-model target.
     */
    @FunctionalInterface
    public interface CrossModelLookup {
        CrossModelTarget resolve(RelationIntent relation);
    }

    /**
     * The facts about a cross-model relation target a notification needs: the owner perspective and
     * project/alias (to import the owner's generated Entity/Repository) plus its property names
     * (PascalCase) to validate the referenced field. A {@code null} {@code propertyNames} means "not
     * validated" (a naming-convention fallback) - the caller then trusts the authored field.
     */
    public record CrossModelTarget(String perspectiveName, String project, String modelAlias, java.util.Set<String> propertyNames) {
    }

    /**
     * The translated, ready-to-render shape of a notification. The two {@code uses*} flags report which
     * template-declared deep-link locals the expressions reference, so a generated handler declares
     * only the links its message actually uses.
     */
    public record Plan(List<RelationLoad> loads, String guardExpression, String toExpression, String subjectExpression,
            String bodyExpression, boolean usesRecordUrl, boolean usesInboxUrl) {
    }

    /**
     * The <b>lifecycle</b> half of the event axis only - a notification bound to a process step
     * ({@link StepEventSupport}) has no lifecycle kind and yields {@code null} here.
     *
     * @param notification the notification
     * @return the lifecycle event kind it binds to, or {@code null}
     */
    public static String eventKind(NotificationIntent notification) {
        return EventBinding.kind(notification.getEvent());
    }

    /**
     * The <b>lifecycle</b> half of the event axis only - use
     * {@link StepEventSupport#eventEntity(org.eclipse.dirigible.components.intent.model.IntentModel, Map)}
     * to resolve a binding of either axis (a step event is about the process's trigger entity).
     *
     * @param notification the notification
     * @return the entity named by the bound lifecycle event, or {@code null}
     */
    public static String eventEntity(NotificationIntent notification) {
        return EventBinding.entity(notification.getEvent());
    }

    /**
     * The per-operation topic suffix the Java DAO publishes to: create uses the unsuffixed base topic,
     * update/delete use {@code -updated}/{@code -deleted}.
     *
     * @param eventKind the lifecycle event kind
     * @return the topic suffix, possibly empty
     */
    public static String topicSuffix(String eventKind) {
        return EventBinding.topicSuffix(eventKind);
    }

    /**
     * Build the full translation plan for a notification against its event entity.
     *
     * @param notification the notification
     * @param eventEntity the entity whose event fires it
     * @param byName all entities by name (to resolve relation targets)
     * @param compositionParents composition-parent map (to resolve a target's perspective)
     * @return the plan, or {@code null} if a {@code relation.field} in {@code to} cannot be resolved (a
     *         bad recipient is fatal; the caller skips the notification)
     */
    public static Plan plan(NotificationIntent notification, EntityIntent eventEntity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents) {
        return plan(notification, eventEntity, byName, compositionParents, null);
    }

    /**
     * Build the full translation plan, resolving cross-model {@code relation.field} paths through the
     * given {@code crossModel} lookup (pass {@code null} to keep the local-only behavior, e.g. in unit
     * tests).
     *
     * @param notification the notification
     * @param eventEntity the entity whose event fires it
     * @param byName all LOCAL entities by name (to resolve same-model relation targets)
     * @param compositionParents composition-parent map (to resolve a target's perspective)
     * @param crossModel resolver for a cross-model relation's owner facts, or {@code null}
     * @return the plan, or {@code null} if the {@code to} recipient cannot be resolved
     */
    public static Plan plan(NotificationIntent notification, EntityIntent eventEntity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, CrossModelLookup crossModel) {
        Object when = notification.getEvent()
                                  .get("when");
        return plan(notification.getTo(), notification.getSubject(), notification.getBody(), when == null ? null : when.toString(),
                eventEntity, byName, compositionParents, crossModel);
    }

    /**
     * Build the translation plan from the raw recipient / subject / body / guard, against the entity
     * the message is about. This is the entry point for every embedded <b>notify block</b> - a
     * {@code transitions[].notify}, a {@code serviceTask}'s {@code args.notify} - where there is no
     * event map because the call site itself IS the event.
     *
     * @param to the recipient: a literal address, a direct field, or a one-hop {@code relation.field}
     * @param subject the subject, with {@code {field}} / {@code {relation.field}} placeholders
     * @param body the body, with the same placeholders
     * @param when an optional guard over a direct field, or {@code null} for none
     * @param entity the entity the message is about (its fields back the paths)
     * @param byName all LOCAL entities by name (to resolve same-model relation targets)
     * @param compositionParents composition-parent map (to resolve a target's perspective)
     * @param crossModel resolver for a cross-model relation's owner facts, or {@code null}
     * @return the plan, or {@code null} if the recipient cannot be resolved
     */
    public static Plan plan(String to, String subject, String body, String when, EntityIntent entity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, CrossModelLookup crossModel) {
        return plan(to, subject, body, when, entity, null, byName, compositionParents, crossModel);
    }

    /**
     * Build the translation plan of a <b>fan-out</b> notify block: {@code entity} is the ROW every bare
     * path resolves against, and {@code anchor} is the record the rows hang off, reachable only through
     * the explicit {@code {record.<field>}} scope (see {@link NotifySupport#RECORD_SCOPE}). The
     * recipient may not be record-scoped - a fan-out sends to its rows - so a record-scoped {@code to}
     * stays unresolvable and the caller drops the block instead of mailing one address N times.
     *
     * @param to the recipient: a literal address, a direct field, or a one-hop {@code relation.field}
     * @param subject the subject, with {@code {field}} / {@code {relation.field}} /
     *        {@code {record.field}} placeholders
     * @param body the body, with the same placeholders
     * @param when an optional guard over a direct field, or {@code null} for none
     * @param entity the entity the message is about (a fan-out's row)
     * @param anchor the fan-out's anchor record, or {@code null} outside a fan-out
     * @param byName all LOCAL entities by name (to resolve same-model relation targets)
     * @param compositionParents composition-parent map (to resolve a target's perspective)
     * @param crossModel resolver for a cross-model relation's owner facts, or {@code null}
     * @return the plan, or {@code null} if the recipient cannot be resolved
     */
    public static Plan plan(String to, String subject, String body, String when, EntityIntent entity, EntityIntent anchor,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, CrossModelLookup crossModel) {
        Resolver resolver = new Resolver(entity, anchor, byName, compositionParents, crossModel);
        String recipient = resolver.value(to);
        if (recipient == null) {
            return null; // an unresolvable recipient relation.field - skip rather than email garbage
        }
        String subjectExpression = resolver.text(subject);
        String bodyExpression = resolver.text(body);
        return new Plan(resolver.loads(), guard(when), recipient, subjectExpression, bodyExpression, resolver.usesRecordUrl(),
                resolver.usesInboxUrl());
    }

    /**
     * Translate a {@code when} guard into a Java boolean expression. Supports a single comparison
     * {@code field == 'literal'} / {@code field != 'literal'} on a direct field; anything else (or
     * blank) yields {@code true}.
     *
     * @param when the guard expression, may be {@code null}
     * @return a Java boolean expression
     */
    public static String guard(String when) {
        if (when == null || when.isBlank()) {
            return "true";
        }
        Matcher matcher = SIMPLE_COMPARISON.matcher(when);
        if (!matcher.matches()) {
            return "true";
        }
        String field = "entity." + IntentNaming.pascalCase(matcher.group(1));
        String rhs = literalToJava(matcher.group(3)
                                          .trim());
        String equals = "java.util.Objects.equals(" + field + ", " + rhs + ")";
        return "==".equals(matcher.group(2)) ? equals : "!" + equals;
    }

    private static String literalToJava(String rhs) {
        if (rhs.length() >= 2 && (rhs.startsWith("'") && rhs.endsWith("'") || rhs.startsWith("\"") && rhs.endsWith("\""))) {
            return quote(rhs.substring(1, rhs.length() - 1));
        }
        if (rhs.matches("-?\\d+(\\.\\d+)?") || "true".equals(rhs) || "false".equals(rhs)) {
            return rhs;
        }
        return quote(rhs);
    }

    static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("\n", "\\n")
                           .replace("\r", "")
                + "\"";
    }

    /**
     * A resolver over one entity, for a caller that needs the path vocabulary on its own terms - the
     * declared {@code payload:} of an outward-facing entry ({@link PayloadSupport}), which classifies
     * its own value forms but must resolve a field / one-hop {@code relation.field} exactly the way a
     * notification does, and accumulate the same relation loads.
     *
     * @param entity the entity the paths resolve against
     * @param byName all LOCAL entities by name
     * @param compositionParents composition-parent map (to resolve a target's perspective)
     * @param crossModel resolver for a cross-model relation's owner facts, or {@code null}
     * @return a fresh resolver
     */
    static Resolver resolver(EntityIntent entity, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            CrossModelLookup crossModel) {
        return new Resolver(entity, null, byName, compositionParents, crossModel);
    }

    /** Resolves values/text against the event entity, accumulating the relation loads they require. */
    static final class Resolver {

        private final EntityIntent entity;
        private final EntityIntent anchor;
        private final Map<String, EntityIntent> byName;
        private final Map<String, String> compositionParents;
        private final Set<String> settingEntities;
        private final CrossModelLookup crossModel;
        private final Map<String, RelationLoad> loads = new LinkedHashMap<>();
        private boolean usesRecordUrl;
        private boolean usesInboxUrl;

        Resolver(EntityIntent entity, EntityIntent anchor, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
                CrossModelLookup crossModel) {
            this.entity = entity;
            this.anchor = anchor;
            this.byName = byName;
            this.compositionParents = compositionParents;
            this.settingEntities = IntentEntities.settingEntities(byName.values());
            this.crossModel = crossModel;
        }

        List<RelationLoad> loads() {
            return new ArrayList<>(loads.values());
        }

        boolean usesRecordUrl() {
            return usesRecordUrl;
        }

        boolean usesInboxUrl() {
            return usesInboxUrl;
        }

        /** A single value (the {@code to} recipient): literal, direct field, or relation.field. */
        String value(String raw) {
            if (raw == null || raw.isBlank()) {
                return "null";
            }
            String trimmed = raw.trim();
            if (trimmed.contains("@") || !PATH.matcher(trimmed)
                                              .matches()) {
                return quote(trimmed);
            }
            // A record-scoped recipient is deliberately NOT resolved: a fan-out mails its rows, so one
            // record-scoped address would go out once per row. It reads as a relation named `record`
            // and, finding none, drops the block - which the parser has already reported precisely.
            return access(trimmed, false); // null when an unresolvable relation.field
        }

        /**
         * Text with {@code {field}} / {@code {relation.field}} placeholders into a Java String expression.
         */
        String text(String raw) {
            if (raw == null || raw.isEmpty()) {
                return "\"\"";
            }
            List<String> terms = new ArrayList<>();
            Matcher matcher = PLACEHOLDER.matcher(raw);
            int last = 0;
            while (matcher.find()) {
                if (matcher.start() > last) {
                    terms.add(quote(raw.substring(last, matcher.start())));
                }
                String access = access(matcher.group(1), true);
                // An unresolvable placeholder degrades to the literal text rather than failing the build.
                terms.add(access == null ? quote(matcher.group()) : access);
                last = matcher.end();
            }
            if (last < raw.length()) {
                terms.add(quote(raw.substring(last)));
            }
            if (terms.isEmpty()) {
                return "\"\"";
            }
            if (terms.size() == 1 && !terms.get(0)
                                           .startsWith("\"")) {
                return "\"\" + " + terms.get(0); // force a String result
            }
            return String.join(" + ", terms);
        }

        /**
         * A Java access expression for a {@code field} or {@code relation.field} path, registering the
         * relation load when needed. Returns {@code null} for an unresolvable relation.field.
         *
         * @param path the authored path
         * @param recordScope whether the {@code record.<field>} scope may address the fan-out's anchor here
         *        (placeholders yes, the recipient no)
         */
        String access(String path, boolean recordScope) {
            if (APP_URL_TOKEN.equals(path)) {
                return APP_URL_EXPRESSION;
            }
            // The two deep links are locals the events template declares - the layer that knows the
            // generated routes. Emitting the identifier here is what keeps the route out of the intent
            // layer; the flags tell that template which of them to declare.
            if (RECORD_URL_TOKEN.equals(path)) {
                usesRecordUrl = true;
                return RECORD_URL_TOKEN;
            }
            if (INBOX_URL_TOKEN.equals(path)) {
                usesInboxUrl = true;
                return INBOX_URL_TOKEN;
            }
            if (recordScope && anchor != null && path.startsWith(NotifySupport.RECORD_SCOPE + ".")) {
                // The anchor record of a fan-out, already loaded by the generated code: one field of it,
                // never a walk on (that would need a second load per message, and the composed value
                // belongs in a field of the record).
                String field = path.substring(NotifySupport.RECORD_SCOPE.length() + 1);
                if (field.isEmpty() || field.indexOf('.') >= 0 || fieldOf(anchor, field) == null) {
                    return null;
                }
                return NotifySupport.RECORD_LOCAL + "." + IntentNaming.pascalCase(field);
            }
            int dot = path.indexOf('.');
            if (dot < 0) {
                return "entity." + IntentNaming.pascalCase(path);
            }
            String relationName = path.substring(0, dot);
            String fieldName = path.substring(dot + 1);
            RelationIntent relation = toOneRelation(relationName);
            if (relation == null) {
                return null;
            }
            String pascalField = IntentNaming.pascalCase(fieldName);
            // Cross-model relation.field (e.g. Customer.email where Customer is owned by another model):
            // resolve the owner's facts through the injected lookup and load from the OWNER's package.
            // A same-model relation resolves against the local byName map as before.
            if (relation.getModel() != null && !relation.getModel()
                                                        .isBlank()) {
                CrossModelTarget xm = crossModel == null ? null : crossModel.resolve(relation);
                if (xm == null) {
                    return null;
                }
                // Validate the field against the owner model when its properties are known; a naming-
                // convention fallback carries null propertyNames - then trust the authored field.
                if (xm.propertyNames() != null && !xm.propertyNames()
                                                     .contains(pascalField)) {
                    return null;
                }
                loads.computeIfAbsent(relationName, name -> new RelationLoad(name, relation.getTo(), xm.perspectiveName(),
                        IntentNaming.pascalCase(name), true, xm.modelAlias(), xm.project()));
                return "(" + relationName + " == null ? null : " + relationName + "." + pascalField + ")";
            }
            EntityIntent target = byName.get(relation.getTo());
            if (target == null || fieldOf(target, fieldName) == null) {
                return null;
            }
            loads.computeIfAbsent(relationName,
                    name -> new RelationLoad(name, relation.getTo(),
                            IntentEntities.resolvePerspective(relation.getTo(), compositionParents, settingEntities),
                            IntentNaming.pascalCase(name), false, "", ""));
            // The listener loads the related entity into a local named after the relation.
            return "(" + relationName + " == null ? null : " + relationName + "." + pascalField + ")";
        }

        RelationIntent toOneRelation(String name) {
            for (RelationIntent relation : entity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && name.equals(relation.getName())) {
                    return relation;
                }
            }
            return null;
        }

        private static FieldIntent fieldOf(EntityIntent entity, String name) {
            for (FieldIntent field : entity.getFields()) {
                if (name.equals(field.getName())) {
                    return field;
                }
            }
            return null;
        }
    }
}
