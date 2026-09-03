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

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * Walks a to-one <b>path</b> off the record a {@code resolves:} lookup fires for - the key or the
 * date the register is queried by, when neither is a column of the record itself.
 *
 * <pre>
 * match:   { priceList: SalesInvoice.Customer.PriceList }
 * between: { start: validFrom, end: validTo, value: SalesInvoice.date }
 * </pre>
 *
 * A line is priced by the list its document header's customer carries, and it is valid on the
 * header's date; neither is on the line. Copying them down with {@code dependsOn} is a UI-time copy
 * that never happens on a REST create, a {@code generates:} create-from or a schedule fan-out - so
 * the rows produced by exactly the automated paths would stay unresolved, which is why this is a
 * path and not a column (dirigible #7025).
 *
 * <p>
 * Every segment but the last is a <b>to-one relation</b> - the first of the record, each further
 * one of the previous target - and the last is a field OR a to-one, whose foreign key is then the
 * value compared. Each hop is loaded through the generated repositories, accumulated once per
 * distinct path PREFIX so two paths sharing a header load it once. A cross-model relation may only
 * be the last hop: a projection carries the target's own properties (so a terminal segment still
 * reads) but not its relations, so there is nothing left to traverse from there - the same line
 * {@link ProcessAssigneeSupport} draws.
 *
 * <p>
 * The class is free of Spring and IO - the cross-model owner facts arrive through the injected
 * lookup - so both callers can use it: the parser walks with no lookup (and simply does not
 * validate a cross-model terminal), the generator walks with one and renders the loads.
 */
public final class ResolvePathSupport {

    /** The generated local holding the record the event carries. */
    static final String RECORD_LOCAL = "entity";

    /** The {@link Path#terminalType()} of a path whose last segment is a to-one relation. */
    static final String RELATION_TERMINAL = "relation";

    private ResolvePathSupport() {}

    /**
     * One hop of a walked path: a related record the generated handler must load before the value can
     * be read.
     *
     * @param local the handler's local holding the loaded record ({@code hop0}, {@code hop1}, ...)
     * @param sourceExpression the null-guarded foreign-key access the record is loaded by
     * @param entity the entity reached
     * @param perspective the reached entity's perspective (its gen data subfolder)
     * @param crossModel whether the traversed relation points at an entity owned by another model
     * @param targetModel the owner model alias when {@code crossModel}, else empty
     * @param targetProject the owner project when {@code crossModel}, else empty
     */
    public record Hop(String local, String sourceExpression, String entity, String perspective, boolean crossModel, String targetModel,
            String targetProject) {
    }

    /**
     * A resolved path, or the reason it did not resolve.
     *
     * @param expression the Java expression yielding the value, null-guarded through every hop
     * @param label the path with each segment in its generated PascalCase form, for the summaries and
     *        the generated javadoc
     * @param terminalType the declared type of the terminal field, {@link #RELATION_TERMINAL} when the
     *        terminal is a to-one, or {@code null} when it sits on a cross-model target and is
     *        therefore not known here
     * @param failure the reason the path did not resolve, or {@code null} when it did
     */
    public record Path(String expression, String label, String terminalType, String failure) {

        /**
         * @return whether the path resolved
         */
        public boolean resolved() {
            return failure == null;
        }
    }

    /**
     * Whether an authored operand is a path at all. A bare property keeps its previous rendering (and
     * its case-insensitive parser check), so an existing model generates byte-identically.
     *
     * @param authored the authored operand
     * @return whether it names more than one segment
     */
    public static boolean isPath(String authored) {
        return authored != null && authored.indexOf('.') >= 0;
    }

    /**
     * Resolves the paths of ONE lookup, accumulating the hops they share.
     *
     * @param record the record the lookup fires for
     * @param byName all LOCAL entities by name
     * @param compositionParents composition-parent map (to resolve a hop's perspective)
     * @param crossModel resolver for a cross-model hop's owner facts, or {@code null} to keep the
     *        local-only behavior (the parser, which has no project context)
     * @return a fresh walker
     */
    public static Walker walker(EntityIntent record, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            NotificationSupport.CrossModelLookup crossModel) {
        return new Walker(record, byName, compositionParents, crossModel);
    }

    /** Resolves paths off one record, accumulating one hop per distinct path prefix. */
    public static final class Walker {

        private final EntityIntent record;
        private final Map<String, EntityIntent> byName;
        private final Map<String, String> compositionParents;
        private final Set<String> settingEntities;
        private final NotificationSupport.CrossModelLookup crossModel;
        private final Map<String, Step> steps = new LinkedHashMap<>();

        private Walker(EntityIntent record, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
                NotificationSupport.CrossModelLookup crossModel) {
            this.record = record;
            this.byName = byName;
            this.compositionParents = compositionParents;
            this.settingEntities = IntentEntities.settingEntities(byName.values());
            this.crossModel = crossModel;
        }

        /**
         * The hops every resolved path of this lookup needs, in load order - a prefix always precedes what
         * hangs off it, because it is registered while the longer path is still being walked.
         *
         * @return the hops
         */
        public List<Hop> hops() {
            List<Hop> hops = new ArrayList<>();
            for (Step step : steps.values()) {
                hops.add(step.hop());
            }
            return hops;
        }

        /**
         * Resolve one authored operand.
         *
         * @param authored the path (a bare property is a path of one segment)
         * @return the resolved path, or a failure carrying the reason
         */
        public Path resolve(String authored) {
            if (authored == null || authored.isBlank()) {
                return failed(authored, "is blank");
            }
            String[] segments = authored.split("\\.", -1);
            EntityIntent current = record;
            NotificationSupport.CrossModelTarget crossTarget = null;
            String owner = RECORD_LOCAL;
            StringBuilder prefix = new StringBuilder();
            StringBuilder label = new StringBuilder();
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                if (segment.isBlank()) {
                    return failed(authored, "has an empty segment");
                }
                if (current == null) {
                    return failed(authored, "segment [" + segment + "] walks on past a cross-model relation"
                            + " - a cross-model relation can only be the last hop of a path");
                }
                if (prefix.length() > 0) {
                    prefix.append('.');
                    label.append('.');
                }
                prefix.append(segment);
                label.append(IntentNaming.pascalCase(segment));
                Step step = steps.get(prefix.toString());
                if (step == null) {
                    RelationIntent relation = toOneRelation(current, segment);
                    if (relation == null) {
                        return failed(authored, "[" + current.getName() + "] has no to-one relation [" + segment + "]");
                    }
                    boolean isCrossModel = relation.getModel() != null && !relation.getModel()
                                                                                   .isBlank();
                    NotificationSupport.CrossModelTarget target = isCrossModel && crossModel != null ? crossModel.resolve(relation) : null;
                    EntityIntent local = isCrossModel ? null : byName.get(relation.getTo());
                    if (!isCrossModel && local == null) {
                        return failed(authored, "relation [" + segment + "] targets unknown entity [" + relation.getTo() + "]");
                    }
                    Hop hop = new Hop("hop" + steps.size(), access(owner, segment), relation.getTo(),
                            isCrossModel ? crossModelPerspective(relation, target)
                                    : IntentEntities.resolvePerspective(relation.getTo(), compositionParents, settingEntities),
                            isCrossModel, isCrossModel ? relation.getModel() : "", target == null ? "" : target.project());
                    step = new Step(hop, local, target);
                    steps.put(prefix.toString(), step);
                }
                owner = step.hop()
                            .local();
                current = step.target();
                crossTarget = step.crossTarget();
            }
            String last = segments[segments.length - 1];
            if (last.isBlank()) {
                return failed(authored, "has an empty segment");
            }
            String pascal = IntentNaming.pascalCase(last);
            if (label.length() > 0) {
                label.append('.');
            }
            label.append(pascal);
            Terminal terminal = terminal(current, crossTarget, last, pascal);
            if (terminal.failure() != null) {
                return failed(authored, terminal.failure());
            }
            return new Path(access(owner, last), label.toString(), terminal.type(), null);
        }

        /**
         * The terminal segment: its declared type, or the reason it does not resolve. A terminal on a
         * cross-model target is checked against the owner's property names when those are known and
         * otherwise trusted - the same fallback a cross-model {@code relation.field} placeholder takes - so
         * its type is left unknown and the caller's type check becomes the generator's.
         */
        private Terminal terminal(EntityIntent current, NotificationSupport.CrossModelTarget crossTarget, String segment, String pascal) {
            if (current == null) {
                if (crossTarget != null && crossTarget.propertyNames() != null && !crossTarget.propertyNames()
                                                                                              .contains(pascal)) {
                    return Terminal.failed("its target does not declare [" + segment + "]");
                }
                return new Terminal(null, null);
            }
            FieldIntent field = fieldOf(current, segment);
            if (field != null) {
                return new Terminal(field.getType(), null);
            }
            if (toOneRelation(current, segment) != null) {
                return new Terminal(RELATION_TERMINAL, null);
            }
            return Terminal.failed("[" + current.getName() + "] has no field or to-one relation [" + segment + "]");
        }

        private static Path failed(String authored, String reason) {
            return new Path("", authored == null ? "" : authored, null, "[" + authored + "] " + reason);
        }

        /** A null-guarded property read off the local holding the record it belongs to. */
        private static String access(String owner, String property) {
            String pascal = IntentNaming.pascalCase(property);
            if (RECORD_LOCAL.equals(owner)) {
                return RECORD_LOCAL + "." + pascal;
            }
            return "(" + owner + " == null ? null : " + owner + "." + pascal + ")";
        }

        private static String crossModelPerspective(RelationIntent relation, NotificationSupport.CrossModelTarget target) {
            return target != null && target.perspectiveName() != null ? target.perspectiveName() : relation.getTo();
        }

        private static RelationIntent toOneRelation(EntityIntent owner, String name) {
            if (owner.getRelations() == null) {
                return null;
            }
            for (RelationIntent relation : owner.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && name.equals(relation.getName())) {
                    return relation;
                }
            }
            return null;
        }

        private static FieldIntent fieldOf(EntityIntent owner, String name) {
            if (owner.getFields() == null) {
                return null;
            }
            for (FieldIntent field : owner.getFields()) {
                if (name.equals(field.getName())) {
                    return field;
                }
            }
            return null;
        }

        /** A registered hop plus what walking on from it needs: the local entity, or the owner facts. */
        private record Step(Hop hop, EntityIntent target, NotificationSupport.CrossModelTarget crossTarget) {
        }

        /** The terminal segment's declared type, or the reason it does not resolve. */
        private record Terminal(String type, String failure) {

            private static Terminal failed(String reason) {
                return new Terminal(null, reason);
            }
        }
    }
}
