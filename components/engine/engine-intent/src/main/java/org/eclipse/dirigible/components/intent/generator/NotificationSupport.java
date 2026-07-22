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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * Translates a {@link NotificationIntent}'s author-facing fields into the Java the generated
 * {@code @Listener} pastes in. Kept free of Spring/IO so the tricky bits are unit-tested directly.
 *
 * <p>
 * A value or {@code {placeholder}} is one of: a literal, a <b>direct field</b> of the event entity
 * (rendered {@code entity.<PascalField>}), or a one-hop <b>{@code relation.field}</b> of a to-one
 * relation (rendered against a related entity the listener loads once by FK id - the same one-hop
 * mechanism the decision resolvers use, see {@link ProcessResolverSupport}). Multi-hop paths are
 * not supported. The {@code when} guard supports a single {@code field ==|!= literal} comparison on
 * a direct field.
 */
public final class NotificationSupport {

    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)\\}");
    private static final Pattern SIMPLE_COMPARISON = Pattern.compile("\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=)\\s*(.+?)\\s*");

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

    /** The translated, ready-to-render shape of a notification. */
    public record Plan(List<RelationLoad> loads, String guardExpression, String toExpression, String subjectExpression,
            String bodyExpression) {
    }

    /**
     * @param notification the notification
     * @return the lifecycle event kind it binds to, or {@code null}
     */
    public static String eventKind(NotificationIntent notification) {
        return EventBinding.kind(notification.getEvent());
    }

    /**
     * @param notification the notification
     * @return the entity named by the bound event, or {@code null}
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
        Resolver resolver = new Resolver(eventEntity, byName, compositionParents, crossModel);
        String to = resolver.value(notification.getTo());
        if (to == null) {
            return null; // an unresolvable recipient relation.field - skip rather than email garbage
        }
        String subject = resolver.text(notification.getSubject());
        String body = resolver.text(notification.getBody());
        Object when = notification.getEvent()
                                  .get("when");
        return new Plan(resolver.loads(), guard(when == null ? null : when.toString()), to, subject, body);
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

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("\n", "\\n")
                           .replace("\r", "")
                + "\"";
    }

    /** Resolves values/text against the event entity, accumulating the relation loads they require. */
    private static final class Resolver {

        private final EntityIntent entity;
        private final Map<String, EntityIntent> byName;
        private final Map<String, String> compositionParents;
        private final CrossModelLookup crossModel;
        private final Map<String, RelationLoad> loads = new LinkedHashMap<>();

        Resolver(EntityIntent entity, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
                CrossModelLookup crossModel) {
            this.entity = entity;
            this.byName = byName;
            this.compositionParents = compositionParents;
            this.crossModel = crossModel;
        }

        List<RelationLoad> loads() {
            return new ArrayList<>(loads.values());
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
            return access(trimmed); // null when an unresolvable relation.field
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
                String access = access(matcher.group(1));
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
         */
        private String access(String path) {
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
            loads.computeIfAbsent(relationName, name -> new RelationLoad(name, relation.getTo(),
                    IntentEntities.resolvePerspective(relation.getTo(), compositionParents), IntentNaming.pascalCase(name), false, "", ""));
            // The listener loads the related entity into a local named after the relation.
            return "(" + relationName + " == null ? null : " + relationName + "." + pascalField + ")";
        }

        private RelationIntent toOneRelation(String name) {
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
