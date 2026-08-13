/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.generator.ProcessParallelSupport;
import org.eclipse.dirigible.components.intent.model.ActionIntent;
import org.eclipse.dirigible.components.intent.model.AggregateIntent;
import org.eclipse.dirigible.components.intent.model.CustomWidgetIntent;
import org.eclipse.dirigible.components.intent.model.DependsOnIntent;
import org.eclipse.dirigible.components.intent.model.NumberIntent;
import org.eclipse.dirigible.components.intent.model.CalendarIntent;
import org.eclipse.dirigible.components.intent.model.CheckIntent;
import org.eclipse.dirigible.components.intent.model.PostingIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesItemsIntent;
import org.eclipse.dirigible.components.intent.model.InboundIntent;
import org.eclipse.dirigible.components.intent.model.IntegrationIntent;
import org.eclipse.dirigible.components.intent.model.GenerateChildIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LabelExpression;
import org.eclipse.dirigible.components.intent.model.LifecycleStages;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.PostingRuleSelector;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.SlotsIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.ExpansionIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleConditionIntent;
import org.eclipse.dirigible.components.intent.model.SettlementIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.eclipse.dirigible.components.intent.model.TransitionIntent;
import org.eclipse.dirigible.components.intent.model.WidgetIntent;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;

/**
 * Parses the YAML payload of a {@code .intent} file into an {@link IntentModel} tree. SnakeYAML
 * loads the document into a generic map; that map is then round-tripped through a plain Gson
 * instance (see {@link #GSON}) so the typed-POJO mapping stays in a single place.
 *
 * <p>
 * SafeConstructor blocks the {@code !!type} / {@code !!new} tags - YAML deserialisation of intents
 * authored by an LLM or pasted from the web must never become a code-execution surface.
 *
 * <p>
 * Structural validation runs after deserialisation: duplicate names, dangling relation targets,
 * unknown field / relation / step kinds, and dangling form-entity references are surfaced via
 * {@link IntentValidationException}. The set of {@link IntentValidationException#getIssues()
 * issues} carries every problem found in one pass rather than failing fast - a usable error message
 * lists everything the author needs to fix.
 */
public final class IntentParser {

    private static final Set<String> FIELD_TYPES = Set.of("string", "text", "integer", "int", "long", "decimal", "double", "boolean",
            "date", "timestamp", "uuid", "month", "week");
    /**
     * Primary keys must be an integer type - the Dirigible model convention is integer identifiers
     * (auto-increment), and a non-integer auto-increment column is invalid SQL on most databases.
     */
    private static final Set<String> INTEGER_PK_TYPES = Set.of("integer", "int", "long");
    /** Numeric field types a sum roll-up (its field / {@code of} / capacity / balance) may use. */
    private static final Set<String> NUMERIC_TYPES = Set.of("integer", "int", "long", "decimal", "double");
    private static final Set<String> RELATION_KINDS = Set.of("oneToMany", "manyToOne", "oneToOne", "manyToMany");
    /** Implemented entity {@code function} values (lower-cased), selecting the entity's UI template. */
    private static final Set<String> ENTITY_FUNCTIONS =
            Set.of("document", "documentitem", "master", "detail", "list", "setting", "calendar", "attachment", "snapshot");
    /**
     * Entity {@code function} values whose template is reserved but not yet shipped (gated with a
     * message).
     */
    private static final Set<String> ENTITY_FUNCTIONS_RESERVED = Set.of("board", "gantt", "timeline");
    /** Implemented field {@code function} values (lower-cased). */
    private static final Set<String> FIELD_FUNCTIONS = Set.of("documenttitle");
    /** Implemented relation {@code function} values (lower-cased). */
    private static final Set<String> RELATION_FUNCTIONS = Set.of("entitystatus");
    private static final Set<String> STEP_KINDS = Set.of("userTask", "serviceTask", "decision", "script", "wait", "parallel", "end");
    /** Entity lifecycle events a declarative-glue item (notification, reaction) can bind to. */
    private static final Set<String> EVENT_KINDS = Set.of("onCreate", "onUpdate", "onDelete");
    /** Notification delivery channels supported today. */
    private static final Set<String> NOTIFICATION_CHANNELS = Set.of("email");
    /** Documents a notify block may attach: the record's own rendered print template. */
    private static final Set<String> NOTIFY_ATTACHMENTS = Set.of("print");
    /** Comparison operators a schedule's {@code where} condition may use. */
    private static final Set<String> SCHEDULE_OPERATORS = Set.of("eq", "ne", "gt", "ge", "lt", "le", "like");
    /** HTTP methods an outbound integration may use. */
    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    /**
     * Plain Gson for the YAML-Map -> JSON -> POJO round-trip. The platform's {@code JsonHelper} /
     * {@code GsonHelper} cannot be used here: they are configured with
     * {@code excludeFieldsWithoutExposeAnnotation()}, which silently maps every un-annotated model
     * field to null/empty - the parser then "succeeds" with an empty {@link IntentModel} and every
     * generator quietly skips its slice. {@code LONG_OR_DOUBLE} keeps YAML integers integral (seed row
     * {@code id: 1} must render as {@code 1} in the CSV, not {@code 1.0}).
     */
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                                                      .create();

    private IntentParser() {}

    /**
     * Parse and validate the given YAML source.
     *
     * @param yaml the raw YAML content of an {@code .intent} file (may be null or blank)
     * @return the typed model, never null - an empty model is returned for blank input
     * @throws IntentValidationException if structural problems are found in the model
     */
    public static IntentModel parse(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return new IntentModel();
        }
        Yaml loader = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object tree = loader.load(yaml);
        if (tree == null) {
            return new IntentModel();
        }
        rejectRemovedNumberKeys(tree);
        moveGeneratesItemLines(tree);
        // Statuses may be referenced by their seeded NAME; resolve them to ids on the raw tree so the
        // typed mapping, every validator and every generator keep seeing the integers they always saw.
        StatusSymbolResolver.resolve(tree);
        String json = GSON.toJson(tree);
        IntentModel model;
        try {
            model = GSON.fromJson(json, IntentModel.class);
        } catch (JsonSyntaxException ex) {
            // A scalar with the wrong shape (commonly a {..} YAML flow-mapping where a string is
            // expected - e.g. an unquoted brace recipient like `to: {member.email}`) fails the typed
            // mapping here, before validate() runs. Surface it as a normal validation issue so the
            // editor shows a helpful message in its problem list instead of a raw 500.
            throw new IntentValidationException(List.of("intent has a value of the wrong type: " + rootMessage(ex)
                    + " - note that brace interpolation ({...}) is only valid inside quoted subject/body strings;"
                    + " a recipient/path field must be a plain scalar (e.g. `to: member.email`, not `to: {member.email}`)"));
        }
        if (model == null) {
            return new IntentModel();
        }
        validate(model);
        return model;
    }

    /**
     * The deepest cause message - Gson wraps the informative "Expected ... path $...." in its cause.
     */
    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? ex.toString() : cause.getMessage();
    }

    /**
     * Run all structural checks. Collects every issue before throwing so authors get one complete error
     * message rather than playing whack-a-mole.
     */
    private static void validate(IntentModel model) {
        List<String> issues = new ArrayList<>();
        propagateSensitiveDerivations(model);
        Set<String> usesAliases = validateUses(model, issues);
        Set<String> entityNames = validateEntities(model, usesAliases, issues);
        validateFunctions(model, issues);
        validateViews(model, issues);
        validateDocumentItemsLayout(model, issues);
        validateOrders(model, issues);
        validateProcesses(model, entityNames, issues);
        validateForms(model, entityNames, issues);
        validateActions(model, entityNames, issues);
        validateGenerates(model, entityNames, usesAliases, issues);
        validateTransitions(model, entityNames, issues);
        validatePostings(model, usesAliases, issues);
        validateReports(model, entityNames, issues);
        validateWidgets(model, issues);
        validateSeeds(model, entityNames, issues);
        validateLanguages(model, issues);
        validateNotifications(model, entityNames, issues);
        validateSchedules(model, entityNames, usesAliases, issues);
        validateIntegrations(model, entityNames, issues);
        validateInbound(model, entityNames, issues);
        validateRollups(model, issues);
        validateExpansions(model, issues);
        validateSettlements(model, issues);
        if (!issues.isEmpty()) {
            throw new IntentValidationException(issues);
        }
    }

    /**
     * Validate the explicit {@code function} presentation role on entities, fields and relations: a
     * value known for its level, reserved-but-unimplemented values ({@code Calendar}, ...) gated with a
     * clear message, and the two consistency checks that keep the layout resolvable - a
     * {@code DocumentItem} must actually be a composition child, and a {@code Document} must resolve a
     * line-items child (a flagged / {@code *Item} child, or a single composition child).
     */
    private static void validateFunctions(IntentModel model, List<String> issues) {
        Map<String, String> compositionParent = compositionParentMap(model);
        for (EntityIntent entity : model.getEntities()) {
            String name = entity.getName();
            if (name == null) {
                continue;
            }
            String fn = entity.getFunction();
            if (fn != null && !fn.isBlank()) {
                String key = fn.trim()
                               .toLowerCase(Locale.ROOT);
                if (ENTITY_FUNCTIONS_RESERVED.contains(key)) {
                    issues.add("entity [" + name + "] function [" + fn
                            + "] is reserved for an upcoming template and is not yet available in this version");
                } else if (!ENTITY_FUNCTIONS.contains(key)) {
                    issues.add("entity [" + name + "] has unknown function [" + fn
                            + "] (valid: Document, DocumentItem, Master, Detail, List, Setting, Calendar)");
                } else if (entity.isDocumentItem() && !compositionParent.containsKey(name)) {
                    issues.add("entity [" + name
                            + "] function: DocumentItem must be a composition child (a manyToOne/oneToOne relation with composition: true)");
                } else if (entity.isDocument() && !hasItemsChild(model, compositionParent, name)) {
                    issues.add("entity [" + name
                            + "] function: Document has no line-items child - flag one composition child with function: DocumentItem"
                            + " (or give it a single composition child)");
                }
            }
            validateSnapshotLanguage(entity, model, compositionParent, issues);
            validateLocksWithMaster(entity, model, compositionParent, issues);
            for (FieldIntent field : entity.getFields()) {
                String ff = field.getFunction();
                if (ff != null && !ff.isBlank() && !FIELD_FUNCTIONS.contains(ff.trim()
                                                                               .toLowerCase(Locale.ROOT))) {
                    issues.add("entity [" + name + "] field [" + field.getName() + "] has unknown function [" + ff
                            + "] (valid: DocumentTitle)");
                }
            }
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.isLegacyDocumentStatus()) {
                    issues.add("entity [" + name + "] relation [" + relation.getName()
                            + "] uses documentStatus: true - the status role was renamed; use function: EntityStatus");
                }
                String rf = relation.getFunction();
                if (rf == null || rf.isBlank()) {
                    continue;
                }
                if ("documentstatus".equals(rf.trim()
                                              .toLowerCase(Locale.ROOT))) {
                    issues.add("entity [" + name + "] relation [" + relation.getName()
                            + "] uses function: DocumentStatus - the status role was renamed; use function: EntityStatus");
                    continue;
                }
                if (!RELATION_FUNCTIONS.contains(rf.trim()
                                                   .toLowerCase(Locale.ROOT))) {
                    issues.add("entity [" + name + "] relation [" + relation.getName() + "] has unknown function [" + rf
                            + "] (valid: EntityStatus)");
                } else if (relation.isEntityStatus()
                        && !("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                    issues.add("entity [" + name + "] relation [" + relation.getName()
                            + "] function: EntityStatus must be a manyToOne/oneToOne relation");
                }
            }
        }
    }

    /**
     * Each entity's composition parent (the target of its first {@code composition: true} to-one
     * relation), which is what resolves a document master's line-items child.
     */
    private static Map<String, String> compositionParentMap(IntentModel model) {
        Map<String, String> compositionParent = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() == null) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.isComposition() && relation.getTo() != null) {
                    compositionParent.put(entity.getName(), relation.getTo());
                    break;
                }
            }
        }
        return compositionParent;
    }

    /**
     * Whether {@code master} has a resolvable document line-items child: a composition child flagged
     * {@code function: DocumentItem} or named {@code *Item}, or a single composition child overall.
     */
    private static boolean hasItemsChild(IntentModel model, Map<String, String> compositionParent, String master) {
        int compositionChildren = 0;
        boolean flagged = false;
        for (EntityIntent entity : model.getEntities()) {
            String child = entity.getName();
            if (child == null || !master.equals(compositionParent.get(child))) {
                continue;
            }
            compositionChildren++;
            if (entity.isDocumentItem() || child.endsWith("Item")) {
                flagged = true;
            }
        }
        return flagged || compositionChildren == 1;
    }

    /**
     * The document line-items child of {@code master} (the composition child flagged
     * {@code function: DocumentItem} / named {@code *Item}, else the sole composition child), or
     * {@code null} when the master has no resolvable items child.
     */
    private static EntityIntent itemsChild(IntentModel model, Map<String, String> compositionParent, String master) {
        EntityIntent flagged = null;
        EntityIntent sole = null;
        int compositionChildren = 0;
        for (EntityIntent entity : model.getEntities()) {
            String child = entity.getName();
            if (child == null || !master.equals(compositionParent.get(child))) {
                continue;
            }
            compositionChildren++;
            sole = entity;
            if (entity.isDocumentItem() || child.endsWith("Item")) {
                flagged = entity;
            }
        }
        if (flagged != null) {
            return flagged;
        }
        return compositionChildren == 1 ? sole : null;
    }

    /**
     * Validate the optional {@code documentItemsLayout} selector on a document master: the only
     * supported value is {@code chat}; the entity must resolve a line-items child; and that child must
     * declare exactly one {@code messageBody} field plus {@code audit: true} (the bubble's author and
     * timestamp come from the audit columns). An optional {@code messageInternal} field must be
     * boolean.
     */
    private static void validateDocumentItemsLayout(IntentModel model, List<String> issues) {
        Map<String, String> compositionParent = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() == null) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.isComposition() && relation.getTo() != null) {
                    compositionParent.put(entity.getName(), relation.getTo());
                    break;
                }
            }
        }
        for (EntityIntent entity : model.getEntities()) {
            String name = entity.getName();
            String layout = entity.getDocumentItemsLayout();
            if (name == null || layout == null || layout.isBlank()) {
                continue;
            }
            if (!"chat".equalsIgnoreCase(layout.trim())) {
                issues.add("entity [" + name + "] has unknown documentItemsLayout [" + layout + "] (supported: chat)");
                continue;
            }
            if (!hasItemsChild(model, compositionParent, name)) {
                issues.add("entity [" + name + "] declares documentItemsLayout: chat but is not a document master"
                        + " (no composition line-items child)");
                continue;
            }
            EntityIntent child = itemsChild(model, compositionParent, name);
            if (child == null) {
                continue;
            }
            long bodyFields = child.getFields()
                                   .stream()
                                   .filter(FieldIntent::isMessageBody)
                                   .count();
            if (bodyFields != 1) {
                issues.add("entity [" + name + "] documentItemsLayout: chat requires its items child [" + child.getName()
                        + "] to declare exactly one messageBody field (found " + bodyFields + ")");
            }
            if (!child.isAudited()) {
                issues.add("entity [" + name + "] documentItemsLayout: chat requires its items child [" + child.getName()
                        + "] to declare audit: true (message author and timestamp)");
            }
            for (FieldIntent field : child.getFields()) {
                if (field.isMessageInternal() && !"boolean".equalsIgnoreCase(field.getType())) {
                    issues.add("entity [" + name + "] items child [" + child.getName() + "] messageInternal field [" + field.getName()
                            + "] must be boolean");
                }
            }
            // Both claim the SAME pane: the chat thread and the items calendar (the child's own
            // `view: calendar`) are two renderings of the line items, so only one may be declared.
            if (child.isCalendar()) {
                issues.add("entity [" + name + "] declares documentItemsLayout: chat while its items child [" + child.getName()
                        + "] declares view: calendar - both render the line items; drop one of the two");
            }
        }
    }

    /**
     * Validate the optional entity {@code view} selector. Only {@code calendar} is supported today, and
     * it requires a {@code calendar.start} naming a declared date/timestamp field of the entity (the
     * timeline the events sit on). {@code end}/{@code title}/{@code color}, when present, must also
     * name declared properties.
     */
    private static void validateViews(IntentModel model, List<String> issues) {
        for (EntityIntent entity : model.getEntities()) {
            String view = entity.getView();
            String name = entity.getName();
            boolean functionCalendar = entity.getFunction() != null && "calendar".equalsIgnoreCase(entity.getFunction()
                                                                                                         .trim());
            if (view == null || view.isBlank()) {
                if (!functionCalendar) {
                    continue;
                }
                // function: Calendar is the role alias for view: calendar - same rendering, same
                // required calendar block, validated below with the effective view.
                view = "calendar";
            } else if (functionCalendar && !"calendar".equalsIgnoreCase(view.trim())) {
                issues.add("entity [" + name + "] declares function: Calendar together with view: " + view
                        + " - the role implies view: calendar; drop one of the two");
                continue;
            }
            String v = view.trim();
            if (!"calendar".equalsIgnoreCase(v) && !"range".equalsIgnoreCase(v) && !"slots".equalsIgnoreCase(v)) {
                issues.add("entity [" + name + "] has unknown view [" + view + "] (supported: calendar, range, slots)");
                continue;
            }
            Set<String> fieldNames = new HashSet<>();
            Set<String> dateFieldNames = new HashSet<>();
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() == null) {
                    continue;
                }
                fieldNames.add(field.getName()
                                    .toLowerCase());
                String type = field.getType() == null ? ""
                        : field.getType()
                               .trim()
                               .toLowerCase();
                if ("date".equals(type) || "timestamp".equals(type)) {
                    dateFieldNames.add(field.getName()
                                            .toLowerCase());
                }
            }
            Set<String> relationNames = new HashSet<>();
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.getName() != null) {
                    relationNames.add(relation.getName()
                                              .toLowerCase());
                }
            }
            if ("slots".equalsIgnoreCase(v)) {
                SlotsIntent slotsCfg = entity.getSlots();
                if (slotsCfg == null || slotsCfg.getStart() == null || slotsCfg.getStart()
                                                                               .isBlank()) {
                    issues.add("entity [" + name + "] view: slots requires slots.start naming a date/timestamp field");
                    continue;
                }
                if (!dateFieldNames.contains(slotsCfg.getStart()
                                                     .trim()
                                                     .toLowerCase())) {
                    issues.add("entity [" + name + "] slots.start [" + slotsCfg.getStart() + "] is not a declared date/timestamp field");
                }
                continue;
            }
            // calendar or range
            CalendarIntent cal = entity.getCalendar();
            if (cal == null || cal.getStart() == null || cal.getStart()
                                                            .isBlank()) {
                issues.add("entity [" + name + "] view: " + v + " requires calendar.start naming a date/timestamp field");
                continue;
            }
            if (!dateFieldNames.contains(cal.getStart()
                                            .trim()
                                            .toLowerCase())) {
                issues.add("entity [" + name + "] calendar.start [" + cal.getStart() + "] is not a declared date/timestamp field");
            }
            if (cal.getEnd() != null && !cal.getEnd()
                                            .isBlank()
                    && !dateFieldNames.contains(cal.getEnd()
                                                   .trim()
                                                   .toLowerCase())) {
                issues.add("entity [" + name + "] calendar.end [" + cal.getEnd() + "] is not a declared date/timestamp field");
            }
            for (String ref : new String[] {cal.getTitle(), cal.getColor()}) {
                if (ref != null && !ref.isBlank()) {
                    String key = ref.trim()
                                    .toLowerCase();
                    if (!fieldNames.contains(key) && !relationNames.contains(key)) {
                        issues.add("entity [" + name + "] calendar references [" + ref + "] which is not a declared field or relation");
                    }
                }
            }
            if (cal.getScope() != null && !cal.getScope()
                                              .isBlank()
                    && !relationNames.contains(cal.getScope()
                                                  .trim()
                                                  .toLowerCase())) {
                issues.add("entity [" + name + "] calendar.scope [" + cal.getScope() + "] is not a declared to-one relation");
            }
        }
    }

    /**
     * Each entity's optional {@code order} lists property names (fields or to-one relations, matched
     * case-insensitively against the authored names) to sequence the generated UI controls. Every
     * listed name must resolve to a declared field or relation of that entity, and no name may repeat.
     * A partial order is fine - unlisted properties keep their default position.
     */
    private static void validateOrders(IntentModel model, List<String> issues) {
        for (EntityIntent entity : model.getEntities()) {
            List<String> order = entity.getOrder();
            if (order == null || order.isEmpty() || entity.getName() == null) {
                continue;
            }
            Set<String> known = new HashSet<>();
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() != null) {
                    known.add(field.getName()
                                   .toLowerCase(Locale.ROOT));
                }
            }
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.getName() != null) {
                    known.add(relation.getName()
                                      .toLowerCase(Locale.ROOT));
                }
            }
            Set<String> seen = new HashSet<>();
            for (String token : order) {
                if (token == null || token.isBlank()) {
                    issues.add("entity [" + entity.getName() + "] order has a blank entry");
                    continue;
                }
                String key = token.trim()
                                  .toLowerCase(Locale.ROOT);
                if (!seen.add(key)) {
                    issues.add("entity [" + entity.getName() + "] order lists [" + token + "] more than once");
                }
                if (!known.contains(key)) {
                    issues.add(
                            "entity [" + entity.getName() + "] order references [" + token + "] which is not a declared field or relation");
                }
            }
        }
    }

    /**
     * Each settlement must reference declared junction / invoice / payment entities; the junction must
     * have a to-one relation to each of them; the named amount / total / paid / pot / order fields must
     * exist; and each {@code match} must be a to-one relation of both the invoice and the payment.
     */
    private static void validateSettlements(IntentModel model, List<String> issues) {
        Map<String, EntityIntent> byName = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (SettlementIntent s : model.getSettlements()) {
            String label = s.getName() == null ? "<unnamed>" : s.getName();
            if (s.getName() == null || s.getName()
                                        .isBlank()) {
                issues.add("settlement has no name");
                continue;
            }
            if (!names.add(s.getName())) {
                issues.add("duplicate settlement [" + s.getName() + "]");
            }
            EntityIntent junction = byName.get(s.getJunction());
            EntityIntent invoice = byName.get(s.getInvoice());
            if (junction == null) {
                issues.add("settlement [" + label + "] references unknown junction entity [" + s.getJunction() + "]");
            }
            if (invoice == null) {
                issues.add("settlement [" + label + "] references unknown invoice entity [" + s.getInvoice() + "]");
            }
            if (s.getPayment() == null || s.getPayment()
                                           .isBlank()) {
                issues.add("settlement [" + label + "] must name a payment entity");
            }
            if (junction != null) {
                if (toOneRelationTo(junction, s.getInvoice()) == null) {
                    issues.add("settlement [" + label + "] junction [" + s.getJunction() + "] has no to-one relation to [" + s.getInvoice()
                            + "]");
                }
                if (toOneRelationTo(junction, s.getPayment()) == null) {
                    issues.add("settlement [" + label + "] junction [" + s.getJunction() + "] has no to-one relation to [" + s.getPayment()
                            + "]");
                }
                if (s.getAmount() == null || fieldByName(junction, s.getAmount()) == null) {
                    issues.add("settlement [" + label + "] amount [" + s.getAmount() + "] is not a field of the junction ["
                            + s.getJunction() + "]");
                }
            }
            if (invoice != null) {
                requireField(invoice, s.getTotal(), label, "total", issues);
                requireField(invoice, s.getPaid(), label, "paid", issues);
                requireField(invoice, s.getOrder(), label, "order", issues);
                if (s.getStatus() != null && !s.getStatus()
                                               .isBlank()
                        && toOneRelationByName(invoice, s.getStatus()) == null) {
                    issues.add("settlement [" + label + "] status [" + s.getStatus() + "] is not a to-one relation of [" + s.getInvoice()
                            + "]");
                }
                for (String m : s.getMatch()) {
                    if (toOneRelationByName(invoice, m) == null) {
                        issues.add("settlement [" + label + "] match [" + m + "] is not a to-one relation of the invoice [" + s.getInvoice()
                                + "]");
                    }
                }
            }
        }
    }

    private static void requireField(EntityIntent entity, String field, String label, String role, List<String> issues) {
        if (field == null || fieldByName(entity, field) == null) {
            issues.add("settlement [" + label + "] " + role + " [" + field + "] is not a field of [" + entity.getName() + "]");
        }
    }

    /** The entity's to-one relation whose target is {@code targetEntity}, or null. */
    private static RelationIntent toOneRelationTo(EntityIntent entity, String targetEntity) {
        if (entity.getRelations() == null || targetEntity == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (targetEntity.equals(relation.getTo())
                    && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Each schedule must have a unique name, a cron expression, an entity to query, supported
     * {@code where} operators, and a notify action with a valid recipient.
     */
    private static void validateSchedules(IntentModel model, Set<String> entityNames, Set<String> usesAliases, List<String> issues) {
        Map<String, EntityIntent> byName = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (ScheduleIntent schedule : model.getSchedules()) {
            String name = schedule.getName();
            if (name == null || name.isBlank()) {
                issues.add("schedule has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate schedule [" + name + "]");
            }
            if (schedule.getCron() == null || schedule.getCron()
                                                      .isBlank()) {
                issues.add("schedule [" + name + "] has no cron expression");
            }
            // A cross-model source (model: <uses alias>) lives in another model; its existence and its
            // where/map/match field references are validated at GENERATION time against the owner's
            // .model (the same design-time split relations / dependsOn / leafOnly already use), so the
            // local entity/field checks are skipped and source stays null.
            boolean crossModelSource = schedule.getModel() != null && !schedule.getModel()
                                                                               .isBlank();
            EntityIntent source = null;
            if (crossModelSource) {
                if (!usesAliases.contains(schedule.getModel())) {
                    issues.add("schedule [" + name + "] source model [" + schedule.getModel()
                            + "] is not a declared uses: alias (declare it under the model's uses:)");
                }
                if (schedule.getEntity() == null || schedule.getEntity()
                                                            .isBlank()) {
                    issues.add("schedule [" + name + "] queries unknown entity [" + schedule.getEntity() + "]");
                }
            } else if (schedule.getEntity() == null || !entityNames.contains(schedule.getEntity())) {
                issues.add("schedule [" + name + "] queries unknown entity [" + schedule.getEntity() + "]");
            } else {
                source = byName.get(schedule.getEntity());
            }
            for (ScheduleConditionIntent condition : schedule.getWhere()) {
                if (condition.getField() == null || condition.getField()
                                                             .isBlank()) {
                    issues.add("schedule [" + name + "] has a where-condition with no field");
                }
                if (!SCHEDULE_OPERATORS.contains(condition.getOp())) {
                    issues.add("schedule [" + name + "] where-condition uses unsupported operator [" + condition.getOp()
                            + "] (supported: eq/ne/gt/ge/lt/le/like)");
                }
            }
            // A schedule performs exactly one per-row action: notify (mail) or generate (create-from).
            boolean hasNotify = schedule.getNotify() != null;
            boolean hasGenerate = schedule.getGenerate() != null;
            if (hasNotify && hasGenerate) {
                issues.add("schedule [" + name + "] has both notify and generate - a schedule performs exactly one per-row action");
            } else if (!hasNotify && !hasGenerate) {
                issues.add("schedule [" + name + "] has no action (add a notify or a generate)");
            } else if (hasNotify) {
                // v1 scope: the notify machinery resolves recipients/placeholders/relation loads against a
                // LOCAL EntityIntent; a cross-model source has only TargetInfo metadata, so notify is not
                // yet supported there. Keep the schedule in the source's model, or drop model:.
                if (crossModelSource) {
                    issues.add("schedule [" + name + "] uses a cross-model source with notify - a cross-model schedule source"
                            + " supports the generate action; notify needs the source's relation metadata - keep the schedule in the"
                            + " source's model or drop model:");
                } else {
                    validateNotifyBlock(schedule.getNotify(), "schedule [" + name + "] notify", schedule.getEntity(), model, issues);
                }
            } else {
                validateScheduleGenerate(schedule, source, entityNames, usesAliases, issues);
            }
        }
    }

    /**
     * A schedule's {@code generate} action creates one target record per matching row. The row is the
     * source, so {@code from} is implicit (the schedule's {@code entity}); the author declares
     * {@code to} (this model, or another via {@code uses:}), a {@code map} (target property -> a field
     * or to-one relation of the row) and {@code defaults}. Composition-item cloning is out of scope
     * here - it needs a selected document, so it belongs to an on-demand {@code generates} action.
     */
    private static void validateScheduleGenerate(ScheduleIntent schedule, EntityIntent source, Set<String> entityNames,
            Set<String> usesAliases, List<String> issues) {
        String name = schedule.getName();
        GeneratesIntent g = schedule.getGenerate();
        if (g.getTo() == null || g.getTo()
                                  .isBlank()) {
            issues.add("schedule [" + name + "] generate has no to entity");
        }
        boolean crossModel = g.getUses() != null && !g.getUses()
                                                      .isBlank();
        if (crossModel) {
            if (!usesAliases.contains(g.getUses())) {
                issues.add("schedule [" + name + "] generate uses unknown model alias [" + g.getUses()
                        + "] (declare it under the model's uses:)");
            }
        } else if (g.getTo() != null && !g.getTo()
                                          .isBlank()
                && !entityNames.contains(g.getTo())) {
            issues.add("schedule [" + name + "] generate to references unknown entity [" + g.getTo()
                    + "] (add a uses: alias if the target lives in another model)");
        }
        validateMapSource(source, g.getMap(), "schedule [" + name + "]", "generate map", issues);
        if (g.getItems() != null || (g.getItemLines() != null && !g.getItemLines()
                                                                   .isEmpty())) {
            issues.add("schedule [" + name + "] generate declares items - item cloning is not supported for a scheduled generation;"
                    + " use an on-demand generates action for document-to-document cloning");
        }
        if (g.getChildren() != null) {
            validateGenerateChildren(name, g.getChildren(), 1, source, entityNames, usesAliases, issues);
        }
    }

    /**
     * Child blocks of a scheduled generation: each creates one child row of the just-generated parent
     * per element of a source collection. Two collection kinds - {@code forEach: &#123;
     * entity, match &#125;} (rows of a LOCAL entity whose field equals a source-row field) and
     * {@code forEach: &#123; days: workingDays &#125;} (the working days of the month, the date written
     * to {@code dayField}). {@code parent} names the child's to-one back to the generated parent
     * (resolved in the target's model at generation). Depth is capped at two levels.
     */
    private static void validateGenerateChildren(String name, List<GenerateChildIntent> children, int depth, EntityIntent source,
            Set<String> entityNames, Set<String> usesAliases, List<String> issues) {
        if (depth > 2) {
            issues.add("schedule [" + name + "] generate children nest deeper than two levels - flatten the shape");
            return;
        }
        for (GenerateChildIntent child : children) {
            String subject = "schedule [" + name + "] generate child [" + (child.getTo() == null ? "?" : child.getTo()) + "]";
            if (child.getTo() == null || child.getTo()
                                              .isBlank()) {
                issues.add("schedule [" + name + "] generate has a child with no to entity");
                continue;
            }
            if (child.getParent() == null || child.getParent()
                                                  .isBlank()) {
                issues.add(subject + " has no parent relation (the child's to-one back to the generated record)");
            }
            Object forEachEntity = child.getForEach()
                                        .get("entity");
            Object forEachDays = child.getForEach()
                                      .get("days");
            if ((forEachEntity == null) == (forEachDays == null)) {
                issues.add(subject + " forEach must declare exactly one of entity (a local collection) or days: workingDays");
                continue;
            }
            if (forEachDays != null) {
                if (!"workingDays".equals(String.valueOf(forEachDays))) {
                    issues.add(subject + " forEach days [" + forEachDays + "] is not supported - only workingDays");
                }
                if (child.getDayField() == null || child.getDayField()
                                                        .isBlank()) {
                    issues.add(subject + " uses forEach days but declares no dayField to receive each date");
                }
                if (!child.getMap()
                          .isEmpty()) {
                    issues.add(subject + " a days child cannot map from a collection row - use defaults for literals");
                }
            } else {
                String collection = String.valueOf(forEachEntity);
                // The forEach collection may itself live in another model (forEach.model: <uses alias>);
                // its existence and match-key field are then validated at generation time.
                Object forEachModel = child.getForEach()
                                           .get("model");
                boolean forEachCrossModel = forEachModel != null && !String.valueOf(forEachModel)
                                                                           .isBlank();
                if (forEachCrossModel) {
                    if (!usesAliases.contains(String.valueOf(forEachModel))) {
                        issues.add(subject + " forEach model [" + forEachModel
                                + "] is not a declared uses: alias (declare it under the model's uses:)");
                    }
                } else if (!entityNames.contains(collection)) {
                    issues.add(subject + " forEach entity [" + collection + "] is not a local entity of this model");
                }
                Object match = child.getForEach()
                                    .get("match");
                if (!(match instanceof Map) || ((Map<?, ?>) match).isEmpty()) {
                    issues.add(subject + " forEach entity requires a match: { <collection field>: <source field> } condition");
                }
            }
            if (child.getChildren() != null) {
                validateGenerateChildren(name, child.getChildren(), depth + 1, source, entityNames, usesAliases, issues);
            }
        }
    }

    /**
     * Each roll-up must have a unique name, a child entity, a {@code via} to-one relation of that child
     * pointing at a parent, and an integer {@code field} on the parent to maintain.
     */
    private static void validateRollups(IntentModel model, List<String> issues) {
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (RollupIntent rollup : model.getRollups()) {
            String name = rollup.getName();
            if (name == null || name.isBlank()) {
                issues.add("rollup has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate rollup [" + name + "]");
            }
            EntityIntent child = byName.get(rollup.getEntity());
            if (child == null) {
                issues.add("rollup [" + name + "] counts unknown entity [" + rollup.getEntity() + "]");
                continue;
            }
            RelationIntent via = null;
            for (RelationIntent relation : child.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.getName() != null && relation.getName()
                                                                   .equals(rollup.getVia())) {
                    via = relation;
                }
            }
            if (via == null) {
                issues.add("rollup [" + name + "] via [" + rollup.getVia() + "] is not a to-one relation of [" + rollup.getEntity() + "]");
                continue;
            }
            boolean sum = "sum".equals(rollup.getOp());
            boolean latest = "latest".equals(rollup.getOp());
            if (via.getModel() != null && !via.getModel()
                                              .isBlank()) {
                // A CROSS-MODEL parent (the roll-up maintains a field on an entity another model owns).
                // Its properties are not in this document, so they are validated at GENERATION time
                // against the owner's model - the same split every cross-model reference uses. Only the
                // capacity/balance/status variants stay local-only: they need the parent's own status
                // seeds and stamp a capacity guard that reads the parent's table, which is a deeper
                // change than resolving coordinates.
                if (rollup.getCapacity() != null || rollup.getBalance() != null || rollup.getStatus() != null) {
                    issues.add("rollup [" + name + "] maintains a cross-model parent [" + via.getModel() + ":" + via.getTo()
                            + "], so capacity / balance / status are not supported - keep those in the model that owns the parent");
                }
                if (sum && (rollup.getOf() == null || rollup.getOf()
                                                            .isBlank())) {
                    issues.add("rollup [" + name + "] with op: sum requires `of`");
                }
                if (latest && (rollup.getOf() == null || rollup.getOf()
                                                               .isBlank()
                        || rollup.getBy() == null || rollup.getBy()
                                                           .isBlank())) {
                    issues.add("rollup [" + name + "] with op: latest requires both `of` and `by`");
                }
                continue;
            }
            EntityIntent parent = byName.get(via.getTo());
            FieldIntent counter = parent == null ? null : fieldByName(parent, rollup.getField());
            if (counter == null) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] is not a field of parent [" + via.getTo() + "]");
            } else if (sum && !NUMERIC_TYPES.contains(counter.getType())) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] must be a numeric type to hold a sum");
            } else if (!sum && !latest && !INTEGER_PK_TYPES.contains(counter.getType())) {
                issues.add("rollup [" + name + "] field [" + rollup.getField() + "] must be an integer type to hold a count");
            }
            if (latest) {
                // latest copies the child `of` value from the row with the greatest `by` date onto the
                // parent field; `of`+`by` required, `by` must be date/timestamp, and the parent field
                // should hold the same type as `of` (checked leniently: same logical type).
                FieldIntent of = fieldByName(child, rollup.getOf());
                FieldIntent by = fieldByName(child, rollup.getBy());
                if (rollup.getOf() == null || rollup.getOf()
                                                    .isBlank()) {
                    issues.add("rollup [" + name + "] with op latest must declare `of` (the child field to copy)");
                } else if (of == null) {
                    issues.add("rollup [" + name + "] of [" + rollup.getOf() + "] is not a field of [" + rollup.getEntity() + "]");
                }
                if (rollup.getBy() == null || rollup.getBy()
                                                    .isBlank()) {
                    issues.add(
                            "rollup [" + name + "] with op latest must declare `by` (the child date/timestamp field that orders the rows)");
                } else if (by == null) {
                    issues.add("rollup [" + name + "] by [" + rollup.getBy() + "] is not a field of [" + rollup.getEntity() + "]");
                } else if (!"date".equals(by.getType()) && !"timestamp".equals(by.getType())) {
                    issues.add("rollup [" + name + "] by [" + rollup.getBy() + "] must be a date/timestamp field");
                }
                if (of != null && counter != null && of.getType() != null && !of.getType()
                                                                                .equals(counter.getType())) {
                    issues.add("rollup [" + name + "] field [" + rollup.getField() + "] type [" + counter.getType()
                            + "] must match the copied `of` field type [" + of.getType() + "]");
                }
            }
            if (sum) {
                // sum needs a numeric child field to add up; capacity / balance (optional) are numeric parent
                // fields and status (optional) a to-one relation of the parent - see the balance/status roll-up.
                FieldIntent of = fieldByName(child, rollup.getOf());
                if (rollup.getOf() == null || rollup.getOf()
                                                    .isBlank()) {
                    issues.add("rollup [" + name + "] with op sum must declare `of` (the child field to sum)");
                } else if (of == null) {
                    issues.add("rollup [" + name + "] of [" + rollup.getOf() + "] is not a field of [" + rollup.getEntity() + "]");
                } else if (!NUMERIC_TYPES.contains(of.getType())) {
                    issues.add("rollup [" + name + "] of [" + rollup.getOf() + "] must be a numeric field to sum");
                }
                requireNumericParentField(parent, rollup.getCapacity(), name, "capacity", via.getTo(), issues);
                requireNumericParentField(parent, rollup.getBalance(), name, "balance", via.getTo(), issues);
                if (rollup.getStatus() != null && !rollup.getStatus()
                                                         .isBlank()
                        && (parent == null || toOneRelationByName(parent, rollup.getStatus()) == null)) {
                    issues.add(
                            "rollup [" + name + "] status [" + rollup.getStatus() + "] is not a to-one relation of [" + via.getTo() + "]");
                }
            }
        }
    }

    /**
     * A derived field that sums or copies a {@code sensitive:} child field re-exposes on its target
     * exactly what the child hides whenever the target entity has a personal (my) surface - the leak
     * class where the leaf value is scrubbed from the personal wire but its total still travels it.
     * Close it by construction: before validation, every rollup target field ({@code op: sum} /
     * {@code latest}), every {@code aggregate: true} master field, and every {@code aggregates:} target
     * field fed by a sensitive source becomes {@code sensitive} automatically when the target entity is
     * personal-surfaced (an own personal owner relation, or the scope inherited through a composition
     * parent chain). A target without a personal surface keeps the authored visibility - there is
     * nothing to leak there.
     */
    private static void propagateSensitiveDerivations(IntentModel model) {
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        // rollups: the child's `of` field feeds the parent's `field`
        for (RollupIntent rollup : model.getRollups()) {
            EntityIntent child = byName.get(rollup.getEntity());
            if (child == null) {
                continue;
            }
            RelationIntent via = toOneRelationByName(child, rollup.getVia());
            EntityIntent parent = via == null ? null : byName.get(via.getTo());
            FieldIntent of = rollup.getOf() == null || parent == null ? null : fieldByName(child, rollup.getOf());
            FieldIntent target = parent == null ? null : fieldByName(parent, rollup.getField());
            if (of != null && target != null && of.isSensitive() && !target.isSensitive()
                    && hasPersonalSurface(byName, parent, new HashSet<>())) {
                target.setSensitive(true);
            }
        }
        // aggregate: true master fields recomputed from the same-named field of a composition child
        for (EntityIntent parent : model.getEntities()) {
            if (!hasPersonalSurface(byName, parent, new HashSet<>())) {
                continue;
            }
            for (FieldIntent target : parent.getFields()) {
                if (!target.isAggregate() || target.isSensitive()) {
                    continue;
                }
                for (EntityIntent child : model.getEntities()) {
                    for (RelationIntent relation : child.getRelations()) {
                        if (relation.isComposition() && parent.getName() != null && parent.getName()
                                                                                          .equals(relation.getTo())) {
                            FieldIntent source = fieldByName(child, target.getName());
                            if (source != null && source.isSensitive()) {
                                target.setSensitive(true);
                            }
                        }
                    }
                }
            }
        }
        // aggregates: the source entity's `sum` field feeds the target entity's `field`, keyed by the
        // shared FKs. Same leak shape one entity further out - the keyed aggregate materialises the total
        // of a hidden figure into a SEPARATE entity, which is exactly what a personal surface over that
        // target would then serve.
        for (AggregateIntent aggregate : model.getAggregates()) {
            EntityIntent source = byName.get(aggregate.getOf());
            EntityIntent target = byName.get(aggregate.getInto());
            if (source == null || target == null || aggregate.getSum() == null) {
                continue;
            }
            FieldIntent of = fieldByName(source, aggregate.getSum());
            FieldIntent field = aggregate.getField() == null ? null : fieldByName(target, aggregate.getField());
            if (of != null && field != null && of.isSensitive() && !field.isSensitive()
                    && hasPersonalSurface(byName, target, new HashSet<>())) {
                field.setSensitive(true);
            }
        }
    }

    /**
     * Whether the entity gets a personal (my) surface: it declares a personal owner relation of its
     * own, or inherits the scope through a composition parent chain (cycle-guarded).
     */
    private static boolean hasPersonalSurface(java.util.Map<String, EntityIntent> byName, EntityIntent entity, Set<String> seen) {
        if (entity == null || entity.getName() == null || !seen.add(entity.getName())) {
            return false;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.isPersonal()) {
                return true;
            }
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.isComposition() && hasPersonalSurface(byName, byName.get(relation.getTo()), seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A period expansion must name a declared master ({@code from}) and child ({@code into}) entity,
     * where the child has a to-one relation back to the master; {@code between.start}/{@code end} are
     * {@code date} fields of the master; {@code unit} is day / week / month; {@code skipDays} (day unit
     * only) are weekday indexes 0..6; {@code map} assigns the {@code period} token to a {@code date}
     * field of the child; {@code defaults} name child fields; {@code spread} divides a numeric master
     * field over a numeric child field; {@code count} names a numeric master field for the row count.
     */
    private static void validateExpansions(IntentModel model, List<String> issues) {
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (ExpansionIntent expansion : model.getExpansions()) {
            String name = expansion.getName();
            if (name == null || name.isBlank()) {
                issues.add("expansion has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate expansion [" + name + "]");
            }
            String subject = "expansion [" + name + "]";
            EntityIntent master = byName.get(expansion.getFrom());
            if (master == null) {
                issues.add(subject + " expands unknown entity [" + expansion.getFrom() + "]");
                continue;
            }
            EntityIntent child = byName.get(expansion.getInto());
            if (child == null) {
                issues.add(subject + " generates into unknown entity [" + expansion.getInto() + "]");
                continue;
            }
            RelationIntent back = null;
            for (RelationIntent relation : child.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && expansion.getFrom()
                                      .equals(relation.getTo())) {
                    back = relation;
                    break;
                }
            }
            if (back == null) {
                issues.add(
                        subject + " requires a to-one relation from [" + expansion.getInto() + "] back to [" + expansion.getFrom() + "]");
            }
            String unit = expansion.getUnit() == null || expansion.getUnit()
                                                                  .isBlank() ? "day"
                                                                          : expansion.getUnit()
                                                                                     .trim()
                                                                                     .toLowerCase(Locale.ROOT);
            if (!"day".equals(unit) && !"week".equals(unit) && !"month".equals(unit)) {
                issues.add(subject + " has unknown unit [" + expansion.getUnit() + "] (supported: day, week, month)");
            }
            if (!expansion.getSkipDays()
                          .isEmpty()) {
                if (!"day".equals(unit)) {
                    issues.add(subject + " skipDays applies to unit day only");
                }
                for (Integer d : expansion.getSkipDays()) {
                    if (d == null || d < 0 || d > 6) {
                        issues.add(subject + " skipDays entries must be weekday indexes 0 (Sunday) .. 6 (Saturday)");
                        break;
                    }
                }
            }
            if (expansion.getBetween() == null) {
                issues.add(subject + " requires between: { start, end } naming date fields of [" + expansion.getFrom() + "]");
            } else {
                requireDateField(master, expansion.getBetween()
                                                  .getStart(),
                        subject, "between.start", issues);
                requireDateField(master, expansion.getBetween()
                                                  .getEnd(),
                        subject, "between.end", issues);
            }
            if (expansion.getMap()
                         .isEmpty()) {
                issues.add(subject + " requires map: { <childDateField>: period }");
            }
            for (java.util.Map.Entry<String, String> entry : expansion.getMap()
                                                                      .entrySet()) {
                if (!"period".equals(entry.getValue())) {
                    issues.add(subject + " map value [" + entry.getValue() + "] is not supported (only the `period` token)");
                }
                requireDateField(child, entry.getKey(), subject, "map field", issues);
            }
            for (String field : expansion.getDefaults()
                                         .keySet()) {
                if (fieldByName(child, field) == null) {
                    issues.add(subject + " defaults field [" + field + "] is not a field of [" + expansion.getInto() + "]");
                }
            }
            if (expansion.getSpread() != null) {
                ExpansionIntent.Spread spread = expansion.getSpread();
                requireNumericFieldOf(master, spread.getTotal(), subject, "spread.total", expansion.getFrom(), issues);
                requireNumericFieldOf(child, spread.getInto(), subject, "spread.into", expansion.getInto(), issues);
                if (spread.getRound() != null && (spread.getRound() < 0 || spread.getRound() > 6)) {
                    issues.add(subject + " spread.round must be between 0 and 6");
                }
            }
            if (expansion.getCount() != null && !expansion.getCount()
                                                          .isBlank()) {
                requireNumericFieldOf(master, expansion.getCount(), subject, "count", expansion.getFrom(), issues);
            }
        }
    }

    /** Validate a required {@code date} field reference on an expansion. */
    private static void requireDateField(EntityIntent entity, String field, String subject, String role, List<String> issues) {
        if (field == null || field.isBlank()) {
            issues.add(subject + " requires " + role);
            return;
        }
        FieldIntent resolved = fieldByName(entity, field);
        if (resolved == null || !"date".equals(resolved.getType())) {
            issues.add(subject + " " + role + " [" + field + "] is not a date field of [" + entity.getName() + "]");
        }
    }

    /** Validate a numeric field reference on an expansion (spread total/into, count). */
    private static void requireNumericFieldOf(EntityIntent entity, String field, String subject, String role, String entityName,
            List<String> issues) {
        if (field == null || field.isBlank()) {
            issues.add(subject + " requires " + role);
            return;
        }
        FieldIntent resolved = fieldByName(entity, field);
        if (resolved == null || !NUMERIC_TYPES.contains(resolved.getType())) {
            issues.add(subject + " " + role + " [" + field + "] is not a numeric field of [" + entityName + "]");
        }
    }

    /** Validate an optional numeric parent field named on a roll-up (capacity / balance). */
    private static void requireNumericParentField(EntityIntent parent, String field, String rollup, String role, String parentName,
            List<String> issues) {
        if (field == null || field.isBlank()) {
            return;
        }
        FieldIntent f = parent == null ? null : fieldByName(parent, field);
        if (f == null || !NUMERIC_TYPES.contains(f.getType())) {
            issues.add("rollup [" + rollup + "] " + role + " [" + field + "] must be a numeric field of [" + parentName + "]");
        }
    }

    private static FieldIntent fieldByName(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name != null && name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * Each inbound webhook must have a unique name, a path, and a declared entity to create from the
     * posted payload.
     */
    private static void validateInbound(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (InboundIntent inbound : model.getInbound()) {
            String name = inbound.getName();
            if (name == null || name.isBlank()) {
                issues.add("inbound webhook has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate inbound webhook [" + name + "]");
            }
            if (inbound.getPath() == null || inbound.getPath()
                                                    .isBlank()) {
                issues.add("inbound webhook [" + name + "] has no path");
            }
            if (inbound.getCreate() == null || !entityNames.contains(inbound.getCreate())) {
                issues.add("inbound webhook [" + name + "] creates unknown entity [" + inbound.getCreate() + "]");
            }
        }
    }

    /**
     * Each integration must have a unique name, bind to exactly one entity lifecycle event of a
     * declared entity, use a supported HTTP method, and name a target URL.
     */
    private static void validateIntegrations(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (IntegrationIntent integration : model.getIntegrations()) {
            String name = integration.getName();
            if (name == null || name.isBlank()) {
                issues.add("integration has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate integration [" + name + "]");
            }
            int eventCount = 0;
            for (String kind : EVENT_KINDS) {
                Object target = integration.getEvent()
                                           .get(kind);
                if (target != null) {
                    eventCount++;
                    if (!entityNames.contains(target.toString())) {
                        issues.add("integration [" + name + "] " + kind + " references unknown entity [" + target + "]");
                    }
                }
            }
            if (eventCount != 1) {
                issues.add("integration [" + name + "] must declare exactly one of onCreate/onUpdate/onDelete");
            }
            String method = integration.getMethod();
            if (method != null && !method.isBlank() && !HTTP_METHODS.contains(method.trim()
                                                                                    .toUpperCase(Locale.ROOT))) {
                issues.add("integration [" + name + "] has unsupported HTTP method [" + method + "]");
            }
            if (integration.getUrl() == null || integration.getUrl()
                                                           .isBlank()) {
                issues.add("integration [" + name + "] has no url");
            }
        }
    }

    /**
     * Each notification must have a unique name, bind to exactly one entity lifecycle event
     * ({@code onCreate}/{@code onUpdate}/{@code onDelete}) of a declared entity, use a supported
     * channel, and name a recipient. The {@code when} guard and the {@code to} resolver path are
     * carried through to the generator (a later increment), which validates the path against the entity
     * at generation time.
     */
    private static void validateNotifications(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (NotificationIntent notification : model.getNotifications()) {
            String name = notification.getName();
            if (name == null || name.isBlank()) {
                issues.add("notification has no name");
                continue;
            }
            if (!names.add(name)) {
                issues.add("duplicate notification [" + name + "]");
            }
            int eventCount = 0;
            for (String kind : EVENT_KINDS) {
                Object target = notification.getEvent()
                                            .get(kind);
                if (target != null) {
                    eventCount++;
                    if (!entityNames.contains(target.toString())) {
                        issues.add("notification [" + name + "] " + kind + " references unknown entity [" + target + "]");
                    }
                }
            }
            if (eventCount != 1) {
                issues.add("notification [" + name + "] must declare exactly one of onCreate/onUpdate/onDelete");
            }
            // The event entity is what an `attach: print` renders, so resolve it for the shared checks.
            String eventEntity = null;
            for (String kind : EVENT_KINDS) {
                Object target = notification.getEvent()
                                            .get(kind);
                if (target != null) {
                    eventEntity = target.toString();
                }
            }
            validateNotifyBlock(notification, "notification [" + name + "]", eventEntity, model, issues);
        }
    }

    /**
     * The reusable <b>notify block</b> - the one shape authored by a {@code notifications[]} entry, a
     * {@code schedules[].notify}, a {@code transitions[].notify} and a {@code serviceTask}'s
     * {@code args.notify}. Checks the channel, the recipient rule (a literal address, a direct field or
     * a one-hop {@code relation.field} - the generator resolves a single to-one relation by FK id), and
     * the {@code attach} switch: {@code print} is the only value, and it renders the record's own
     * {@code .print} template, so the entity the block is about must be a printable document master (a
     * line-items child, hence a generated print feeder). Anything else would generate a mail that
     * claims an attachment it cannot produce.
     *
     * @param notify the block, may be {@code null} (nothing to validate)
     * @param subject the message prefix identifying the call site
     * @param aboutEntity the entity the message is about, or {@code null} when it is already unknown
     * @param model the parsed model (to resolve the document-master shape)
     * @param issues the collected issues
     */
    private static void validateNotifyBlock(NotificationIntent notify, String subject, String aboutEntity, IntentModel model,
            List<String> issues) {
        if (notify == null) {
            return;
        }
        String channel = notify.getChannel();
        if (channel != null && !channel.isBlank() && !NOTIFICATION_CHANNELS.contains(channel)) {
            issues.add(subject + " has unsupported channel [" + channel + "] (supported: email)");
        }
        String to = notify.getTo();
        if (to == null || to.isBlank()) {
            issues.add(subject + " has no recipient (to)");
        } else if (!to.contains("@") && to.chars()
                                          .filter(c -> c == '.')
                                          .count() >= 2) {
            issues.add(subject + " recipient [" + to
                    + "] uses a multi-hop path, which is not supported - use a direct field, a one-hop relation.field, or a literal address");
        }
        // A fan-out sends one message per row of a related entity instead of one about the record, so
        // from here on every path (the recipient, the placeholders, the attachment) is about the ROW -
        // which is what `aboutEntity` becomes.
        String forEach = notify.getForEach();
        if (forEach != null && !forEach.isBlank()) {
            String rows = forEach.trim();
            EntityIntent rowEntity = entityByName(model, rows);
            if (rowEntity == null) {
                issues.add(subject + " forEach references unknown entity [" + rows + "]");
                return;
            }
            if (aboutEntity != null && backReferencesTo(rowEntity, aboutEntity) != 1) {
                issues.add(subject + " forEach [" + rows + "] must have exactly ONE to-one relation back to [" + aboutEntity
                        + "] - that relation is what selects the rows to send about");
                return;
            }
            aboutEntity = rows;
        }
        String attach = notify.getAttach();
        boolean hasLanguage = notify.getLanguage() != null && !notify.getLanguage()
                                                                     .isBlank();
        boolean hasLanguageFrom = notify.getLanguageFrom() != null && !notify.getLanguageFrom()
                                                                             .isBlank();
        if (attach == null || attach.isBlank()) {
            if (hasLanguage || hasLanguageFrom) {
                issues.add(subject + " declares language/languageFrom without attach: print - they select the attached render's language");
            }
            return;
        }
        if (!NOTIFY_ATTACHMENTS.contains(attach.trim()
                                               .toLowerCase(Locale.ROOT))) {
            issues.add(subject + " has unsupported attach [" + attach + "] (supported: print)");
        } else if (aboutEntity != null && !isPrintableDocument(model, aboutEntity)) {
            issues.add(subject + " attach: print needs [" + aboutEntity
                    + "] to be a document (header + line-items child) - only a document has a print template to render");
        }
        if (hasLanguage && hasLanguageFrom) {
            issues.add(subject + " declares both language and languageFrom - they are mutually exclusive");
        } else if (hasLanguageFrom && aboutEntity != null) {
            validateLanguageFromPath(notify.getLanguageFrom(), aboutEntity, subject + " languageFrom", model, issues);
        }
    }

    /**
     * The render-language knob of a {@code function: Snapshot} child: a literal {@code language:} code
     * or a {@code languageFrom: relation.field} path resolved on the snapshot's composition MASTER (the
     * document whose copy is minted) - mutually exclusive, meaningless anywhere else. Absent both, the
     * mint falls back to the first entry of the tenant-resolved application language set at run time.
     */
    /**
     * Validate {@code locksWithMaster: false} - the declaration that a child collection does NOT freeze
     * when its master becomes immutable (the settlement case: an issued invoice's lines are frozen, its
     * payment allocations are not). It is only meaningful on a composition child OF a master that
     * actually locks, so both are required rather than silently ignored: an inert declaration reads as
     * a working one, and the author only finds out when the affordance is still missing in production.
     */
    private static void validateLocksWithMaster(EntityIntent entity, IntentModel model, Map<String, String> compositionParent,
            List<String> issues) {
        if (entity.locksWithMaster()) {
            return;
        }
        String name = entity.getName();
        String master = compositionParent.get(name);
        if (master == null) {
            issues.add("entity [" + name + "] declares locksWithMaster: false but is not a composition child"
                    + " - only a child collection can outlive its master's lock");
            return;
        }
        EntityIntent parent = entityByName(model, master);
        boolean masterLocks = parent == null || Boolean.TRUE.equals(parent.getImmutable())
                || (parent.getImmutableWhen() != null && !parent.getImmutableWhen()
                                                                .isBlank());
        if (!masterLocks) {
            issues.add("entity [" + name + "] declares locksWithMaster: false but its master [" + master
                    + "] never locks (no immutableWhen / immutable) - the declaration would have no effect");
        }
    }

    private static void validateSnapshotLanguage(EntityIntent entity, IntentModel model, Map<String, String> compositionParent,
            List<String> issues) {
        boolean hasLanguage = entity.getLanguage() != null && !entity.getLanguage()
                                                                     .isBlank();
        boolean hasLanguageFrom = entity.getLanguageFrom() != null && !entity.getLanguageFrom()
                                                                             .isBlank();
        if (!hasLanguage && !hasLanguageFrom) {
            return;
        }
        String name = entity.getName();
        if (!entity.isSnapshot()) {
            issues.add("entity [" + name + "] declares language/languageFrom, which apply to function: Snapshot children only");
            return;
        }
        if (hasLanguage && hasLanguageFrom) {
            issues.add("entity [" + name + "] declares both language and languageFrom - they are mutually exclusive");
            return;
        }
        if (hasLanguageFrom) {
            String master = compositionParent.get(name);
            if (master == null) {
                issues.add("entity [" + name + "] languageFrom needs a composition master (the document) to resolve against");
                return;
            }
            validateLanguageFromPath(entity.getLanguageFrom(), master, "entity [" + name + "] languageFrom", model, issues);
        }
    }

    /**
     * A {@code languageFrom} path is a one-hop {@code relation.field}: the relation a to-one of the
     * entity the render is about, the field a string-typed field (a language code) of its target. A
     * cross-model target's field is validated at generation against the owner's model, like every other
     * cross-model reference.
     */
    private static void validateLanguageFromPath(String path, String aboutEntity, String subject, IntentModel model, List<String> issues) {
        String trimmed = path.trim();
        int dot = trimmed.indexOf('.');
        if (dot <= 0 || dot == trimmed.length() - 1 || trimmed.indexOf('.', dot + 1) >= 0) {
            issues.add(subject + " [" + path + "] must be a one-hop relation.field path on [" + aboutEntity + "]");
            return;
        }
        String relationName = trimmed.substring(0, dot)
                                     .trim();
        String fieldName = trimmed.substring(dot + 1)
                                  .trim();
        EntityIntent about = entityByName(model, aboutEntity);
        if (about == null) {
            return; // the dangling entity is reported by the structural checks
        }
        RelationIntent relation = null;
        for (RelationIntent candidate : about.getRelations()) {
            boolean toOne = "manyToOne".equals(candidate.getKind()) || "oneToOne".equals(candidate.getKind());
            if (toOne && relationName.equals(candidate.getName())) {
                relation = candidate;
            }
        }
        if (relation == null) {
            issues.add(subject + " [" + path + "]: [" + relationName + "] is not a to-one relation of [" + aboutEntity + "]");
            return;
        }
        if (relation.getModel() != null && !relation.getModel()
                                                    .isBlank()) {
            return; // cross-model target: field checked at generation against the owner's model
        }
        EntityIntent target = entityByName(model, relation.getTo() == null ? "" : relation.getTo());
        if (target == null) {
            return; // the dangling relation target is reported by the relations check
        }
        FieldIntent field = null;
        for (FieldIntent candidate : target.getFields()) {
            if (fieldName.equals(candidate.getName())) {
                field = candidate;
            }
        }
        if (field == null) {
            issues.add(subject + " [" + path + "]: [" + fieldName + "] is not a field of [" + relation.getTo() + "]");
            return;
        }
        String type = field.getType() == null ? "string" : field.getType();
        if (!"string".equals(type) && !"text".equals(type) && !"uuid".equals(type)) {
            issues.add(subject + " [" + path + "]: [" + fieldName + "] must be a string field holding a language code, not [" + type + "]");
        }
    }

    /** The declared entity with that exact name, or {@code null}. */
    private static EntityIntent entityByName(IntentModel model, String name) {
        for (EntityIntent entity : model.getEntities()) {
            if (name.equals(entity.getName())) {
                return entity;
            }
        }
        return null;
    }

    /**
     * How many to-one relations of {@code rows} point at the entity named {@code target}. A fan-out
     * needs exactly one: zero means the rows are not related to the record at all, and two or more make
     * the intended collection ambiguous - guessing would silently mail about the wrong set.
     */
    private static int backReferencesTo(EntityIntent rows, String target) {
        int count = 0;
        if (rows.getRelations() != null) {
            for (RelationIntent relation : rows.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && target.equals(relation.getTo())) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Whether the entity is a <b>document master</b> in the print sense - it has a line-items child, so
     * a {@code .print} template and a print feeder are generated for it and its records can be
     * rendered.
     * <p>
     * This deliberately mirrors the print generator's own resolution: a composition child flagged
     * {@code function: DocumentItem} (or legacy-named {@code *Item}), or - for a master explicitly
     * flagged {@code function: Document} - its single composition child. It is intentionally STRICTER
     * than {@link #hasItemsChild} (which accepts any sole composition child, for the
     * {@code function: Document} consistency check): accepting more here would let a notify block
     * declare an attachment the generator cannot produce, and the mail would go out without the
     * document it promised.
     */
    private static boolean isPrintableDocument(IntentModel model, String master) {
        Map<String, String> compositionParent = compositionParentMap(model);
        int compositionChildren = 0;
        for (EntityIntent entity : model.getEntities()) {
            String child = entity.getName();
            if (child == null || !master.equals(compositionParent.get(child))) {
                continue;
            }
            compositionChildren++;
            if (entity.isDocumentItem() || child.endsWith("Item")) {
                return true;
            }
        }
        EntityIntent entity = null;
        for (EntityIntent candidate : model.getEntities()) {
            if (master.equals(candidate.getName())) {
                entity = candidate;
            }
        }
        return entity != null && entity.isDocument() && compositionChildren == 1;
    }

    /**
     * Each {@code uses[]} entry must name a non-blank, unique model alias. Returns the set of declared
     * aliases so {@link #validateEntities} can resolve cross-model relation targets against it.
     */
    private static Set<String> validateUses(IntentModel model, List<String> issues) {
        Set<String> aliases = new HashSet<>();
        for (org.eclipse.dirigible.components.intent.model.UsesIntent uses : model.getUses()) {
            String alias = uses.getModel();
            if (alias == null || alias.isBlank()) {
                issues.add("uses entry has no model");
                continue;
            }
            if (!aliases.add(alias)) {
                issues.add("duplicate uses model [" + alias + "]");
            }
        }
        return aliases;
    }

    private static Set<String> validateEntities(IntentModel model, Set<String> usesAliases, List<String> issues) {
        Set<String> entityNames = new HashSet<>();
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        for (EntityIntent entity : model.getEntities()) {
            String name = entity.getName();
            if (name == null || name.isBlank()) {
                issues.add("entity has no name");
                continue;
            }
            if (!entityNames.add(name)) {
                issues.add("duplicate entity [" + name + "]");
            }
            Set<String> fieldNames = new HashSet<>();
            int idCount = 0;
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() == null || field.getName()
                                                    .isBlank()) {
                    issues.add("entity [" + name + "] has a field with no name");
                    continue;
                }
                if (!fieldNames.add(field.getName())) {
                    issues.add("entity [" + name + "] declares field [" + field.getName() + "] twice");
                }
                if (field.getType() != null && !FIELD_TYPES.contains(field.getType()
                                                                          .toLowerCase(Locale.ROOT))) {
                    issues.add("entity [" + name + "] field [" + field.getName() + "] has unknown type [" + field.getType() + "]");
                }
                if (field.isPrimaryKey()) {
                    idCount++;
                    String type = field.getType() == null ? null
                            : field.getType()
                                   .toLowerCase(Locale.ROOT);
                    if (!INTEGER_PK_TYPES.contains(type)) {
                        issues.add("entity [" + name + "] primary-key field [" + field.getName()
                                + "] must be an integer type (integer/int/long) - identifiers are integer by convention"
                                + (type == null ? "" : ", got [" + field.getType() + "]"));
                    }
                }
                if (field.getSize() != null && (field.getSize() < 1 || field.getSize() > 12)) {
                    issues.add("entity [" + name + "] field [" + field.getName() + "] size [" + field.getSize()
                            + "] must be a 12-column grid span between 1 and 12 (typically 3/4/6/12)");
                }
                if (field.getDependsOn() != null) {
                    String subject = "entity [" + name + "] field [" + field.getName() + "]";
                    if (field.isPrimaryKey()) {
                        issues.add(subject + " is a primary key so it cannot declare dependsOn");
                    } else {
                        validateDependsOn(entity, subject, field.getDependsOn(), null, byName, issues);
                    }
                }
                if (field.getNumber() != null) {
                    validateNumber(entity, "entity [" + name + "] field [" + field.getName() + "]", field, issues);
                }
                if (!isBlank(field.getPattern())) {
                    validatePattern("entity [" + name + "] field [" + field.getName() + "]", field, issues);
                }
                if (!isBlank(field.getFormat())) {
                    validateFormat("entity [" + name + "] field [" + field.getName() + "]", field, issues);
                }
                if (field.isSensitive()) {
                    if (field.isPrimaryKey()) {
                        issues.add("entity [" + name + "] field [" + field.getName()
                                + "] is the primary key so it cannot be sensitive (the personal surface needs it)");
                    }
                    if (field.getName()
                             .equals(entity.getIdentity())) {
                        issues.add("entity [" + name + "] field [" + field.getName() + "] is the identity field so it cannot be sensitive");
                    }
                }
            }
            if (idCount > 1) {
                issues.add("entity [" + name + "] declares " + idCount + " primary-key fields - exactly one is allowed");
            }
        }
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() == null) {
                continue;
            }
            int personalCount = 0;
            int partnerCount = 0;
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.getName() == null || relation.getName()
                                                          .isBlank()) {
                    issues.add("entity [" + entity.getName() + "] has a relation with no name");
                    continue;
                }
                if (relation.getKind() != null && !RELATION_KINDS.contains(relation.getKind())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has unknown kind ["
                            + relation.getKind() + "]");
                }
                if (relation.getSize() != null && (relation.getSize() < 1 || relation.getSize() > 12)) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] size [" + relation.getSize()
                            + "] must be a 12-column grid span between 1 and 12 (typically 3/4/6/12)");
                }
                if (relation.isComposition() && !"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName()
                            + "] is marked composition but only a manyToOne/oneToOne relation can be a composition");
                }
                boolean crossModel = relation.isCrossModel();
                if (crossModel) {
                    // A cross-model relation references an entity owned by another intent model declared in
                    // uses:. It can only be a to-one association (the FK + dropdown live on this side); it
                    // cannot compose a detail that lives in another model, and its target is validated
                    // against the referenced .model at generation time, not here.
                    if (!usesAliases.contains(relation.getModel())) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] references undeclared model ["
                                + relation.getModel() + "] - add it to uses:");
                    }
                    if (!"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] is cross-model (model: "
                                + relation.getModel() + ") so it must be a manyToOne/oneToOne association");
                    }
                    if (relation.isComposition()) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName()
                                + "] is cross-model so it cannot be a composition - a detail cannot be owned across models");
                    }
                    if (relation.getTo() == null || relation.getTo()
                                                            .isBlank()) {
                        issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has no target");
                    }
                } else if (relation.getTo() == null || relation.getTo()
                                                               .isBlank()) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has no target");
                } else if (!entityNames.contains(relation.getTo())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] points to unknown entity ["
                            + relation.getTo() + "]");
                }
                if (relation.getDependsOn() != null) {
                    String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "]";
                    boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                    if (!toOne) {
                        issues.add(subject + " declares dependsOn but only a manyToOne/oneToOne relation can depend on another");
                    } else if (relation.isEntityStatus()) {
                        issues.add(subject + " is an EntityStatus (a read-only badge) so it cannot declare dependsOn");
                    } else {
                        validateDependsOn(entity, subject, relation.getDependsOn(), relation, byName, issues);
                    }
                }
                if (relation.getWhere() != null) {
                    validateWhere(entity, relation, byName, issues);
                }
                if (relation.isLeafOnly()) {
                    validateLeafOnly(entity, relation, byName, issues);
                }
                if (relation.isCalculated()) {
                    validateRelationCalculatedAction(entity, relation, issues);
                }
                if (relation.isPersonal()) {
                    personalCount++;
                    validatePersonal(entity, relation, byName, issues);
                }
                if (relation.isPartner()) {
                    partnerCount++;
                    validatePartner(entity, relation, byName, issues);
                }
            }
            if (personalCount > 1) {
                issues.add("entity [" + entity.getName() + "] declares " + personalCount
                        + " personal relations - exactly one owner is allowed");
            }
            if (partnerCount > 1) {
                issues.add(
                        "entity [" + entity.getName() + "] declares " + partnerCount + " partner relations - exactly one owner is allowed");
            }
            if (entity.getHierarchy() != null && !entity.getHierarchy()
                                                        .isBlank()) {
                validateHierarchy(entity, issues);
            }
            if (entity.getIdentity() != null && !entity.getIdentity()
                                                       .isBlank()) {
                validateIdentity(entity, issues);
            }
            if (entity.getLabel() != null && !entity.getLabel()
                                                    .isBlank()) {
                validateLabel(entity, byName, issues);
            }
            if (entity.getImmutableIn() != null && !entity.getImmutableIn()
                                                          .isEmpty()) {
                issues.add("entity [" + entity.getName()
                        + "] uses immutableIn - renamed; author immutableWhen: \"<Status> == <seed id>\" (terms joined with ||)");
            }
            if (Boolean.TRUE.equals(entity.getImmutable()) && entity.getImmutableWhen() != null && !entity.getImmutableWhen()
                                                                                                          .isBlank()) {
                issues.add("entity [" + entity.getName()
                        + "] declares both immutable: true and immutableWhen - always-immutable subsumes any status scope; keep one");
            } else if (entity.getImmutableWhen() != null && !entity.getImmutableWhen()
                                                                   .isBlank()) {
                validateImmutableWhen(entity, issues);
            }
            if (entity.getChecks() != null) {
                for (CheckIntent check : entity.getChecks()) {
                    validateCheck(entity, check, byName, model.getAggregates(), issues);
                }
            }
        }
        return entityNames;
    }

    /** The compiled shape of one {@code immutableWhen} term: {@code <Status> == <seed id>}. */
    private static final java.util.regex.Pattern IMMUTABLE_WHEN_TERM = java.util.regex.Pattern.compile("\\s*(\\w+)\\s*==\\s*(\\d+)\\s*");

    /**
     * Upper bound for an authored field {@code pattern} - a compile-time guard against pathological
     * regexes.
     */
    private static final int PATTERN_MAX_LENGTH = 512;

    /**
     * {@code immutableWhen: "<Status> == <seed id> [|| ...]"} makes the record read-only for USER
     * writes while its EntityStatus satisfies the expression (workflow/system writes through the
     * repository stay possible - corrections are reversals, not edits). It therefore requires the
     * entity to declare a {@code function: EntityStatus} relation, every term must reference THAT
     * relation by its authored name, and the seed ids must be positive integers.
     */
    private static void validateImmutableWhen(EntityIntent entity, List<String> issues) {
        String subject = "entity [" + entity.getName() + "] immutableWhen";
        RelationIntent status = null;
        if (entity.getRelations() != null) {
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.isEntityStatus()) {
                    status = relation;
                    break;
                }
            }
        }
        if (status == null) {
            issues.add(subject + " requires a `function: EntityStatus` relation - immutability keys on the status");
            return;
        }
        for (String term : entity.getImmutableWhen()
                                 .split("\\|\\|")) {
            java.util.regex.Matcher matcher = IMMUTABLE_WHEN_TERM.matcher(term);
            if (!matcher.matches()) {
                issues.add(subject + " term [" + term.trim() + "] must be `<Status relation> == <seed id>` (terms joined with ||)");
                continue;
            }
            if (!matcher.group(1)
                        .equals(status.getName())) {
                issues.add(subject + " term [" + term.trim() + "] must reference the EntityStatus relation [" + status.getName() + "]");
            }
            if (Integer.parseInt(matcher.group(2)) <= 0) {
                issues.add(subject + " seed ids must be positive");
            }
        }
    }

    /**
     * A {@code hierarchy} declaration names the entity's own to-one SELF-relation that forms the tree
     * edge. It must resolve to a declared to-one relation targeting the entity itself, and it cannot be
     * a composition (a composition parent is the master-detail owner, a different concept) or required
     * (a required parent leaves no way to author a root node).
     */
    private static void validateHierarchy(EntityIntent entity, List<String> issues) {
        String subject = "entity [" + entity.getName() + "] hierarchy [" + entity.getHierarchy() + "]";
        RelationIntent edge = toOneRelationByName(entity, entity.getHierarchy());
        if (edge == null) {
            issues.add(subject + " does not name a to-one relation of the entity");
            return;
        }
        if (!entity.getName()
                   .equals(edge.getTo())
                || edge.isCrossModel()) {
            issues.add(subject + " must target the entity itself (a self-relation) - it targets [" + edge.getTo() + "]");
        }
        if (edge.isComposition()) {
            issues.add(subject + " cannot be a composition (the tree edge is a plain optional self-association)");
        }
        if (edge.isRequired()) {
            issues.add(subject + " must be optional - a required parent leaves no way to author a root node");
        }
    }

    /**
     * {@code identity: <field>} names the field of this entity matched against the logged-in username
     * (the personal-surface mapping). It must be an own string field - the natural shape is a unique
     * e-mail/username column.
     */
    private static void validateIdentity(EntityIntent entity, List<String> issues) {
        String subject = "entity [" + entity.getName() + "] identity [" + entity.getIdentity() + "]";
        FieldIntent field = null;
        if (entity.getFields() != null) {
            for (FieldIntent f : entity.getFields()) {
                if (entity.getIdentity()
                          .equals(f.getName())) {
                    field = f;
                    break;
                }
            }
        }
        if (field == null) {
            issues.add(subject + " does not name a field of the entity");
            return;
        }
        String type = field.getType() == null ? "string"
                : field.getType()
                       .toLowerCase(Locale.ROOT);
        if (!"string".equals(type) && !"text".equals(type)) {
            issues.add(subject + " must be a string field (it is matched against the login username), got [" + field.getType() + "]");
        }
    }

    /**
     * {@code personal: true} marks the to-one relation whose target record IS the logged-in user - the
     * owner the personal surface scopes by. The target must declare {@code identity}; a same-model
     * target is checked here, a cross-model one at generation against the resolved owner model (like
     * the relation target itself).
     */
    private static void validatePersonal(EntityIntent entity, RelationIntent relation, java.util.Map<String, EntityIntent> byName,
            List<String> issues) {
        String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "]";
        boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
        if (!toOne) {
            issues.add(subject + " declares personal but only a manyToOne/oneToOne relation can own the record");
            return;
        }
        if (relation.isComposition()) {
            issues.add(subject + " is a composition parent - a child inherits the personal scope through it; mark the parent's relation");
            return;
        }
        if (!relation.isCrossModel()) {
            EntityIntent target = byName.get(relation.getTo());
            if (target != null && (target.getIdentity() == null || target.getIdentity()
                                                                         .isBlank())) {
                issues.add(subject + " declares personal but its target [" + relation.getTo() + "] declares no identity");
            }
        }
    }

    /**
     * {@code partner: true} - the exact mirror of {@link #validatePersonal} for the external Partner
     * shell: a to-one owner relation whose target declares {@code identity}, not a composition parent
     * (children inherit the scope). A same-model target's identity is checked here; a cross-model one
     * at generation.
     */
    private static void validatePartner(EntityIntent entity, RelationIntent relation, java.util.Map<String, EntityIntent> byName,
            List<String> issues) {
        String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "]";
        boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
        if (!toOne) {
            issues.add(subject + " declares partner but only a manyToOne/oneToOne relation can own the record");
            return;
        }
        if (relation.isComposition()) {
            issues.add(subject + " is a composition parent - a child inherits the partner scope through it; mark the parent's relation");
            return;
        }
        if (!relation.isCrossModel()) {
            EntityIntent target = byName.get(relation.getTo());
            if (target != null && (target.getIdentity() == null || target.getIdentity()
                                                                         .isBlank())) {
                issues.add(subject + " declares partner but its target [" + relation.getTo() + "] declares no identity");
            }
        }
    }

    /**
     * {@code label: "..."} - a display-label expression generating the stored read-only {@code Name}
     * property. Tokens are own fields or one-hop to-one relation properties; a same-model target
     * property is checked here (the target's own generated {@code Name} counts), a cross-model one at
     * generation. A label is redundant next to an authored {@code name} field, and it must never embed
     * a sensitive field (the Name is visible on the personal surface).
     */
    private static void validateLabel(EntityIntent entity, java.util.Map<String, EntityIntent> byName, List<String> issues) {
        String subject = "entity [" + entity.getName() + "] label";
        if (entity.getFields() != null && entity.getFields()
                                                .stream()
                                                .anyMatch(f -> "name".equalsIgnoreCase(f.getName()))) {
            issues.add(subject + " is redundant - the entity already declares a name field");
            return;
        }
        java.util.List<LabelExpression.Part> parts;
        try {
            parts = LabelExpression.parse(entity.getLabel());
        } catch (IllegalArgumentException e) {
            issues.add(subject + " is malformed: " + e.getMessage());
            return;
        }
        for (LabelExpression.Part part : parts) {
            if (part.isLiteral()) {
                continue;
            }
            if (part.relation() == null) {
                FieldIntent field = fieldByName(entity, part.property());
                if (field == null) {
                    issues.add(subject + " token [" + part.property() + "] does not name a field of the entity");
                } else if (field.isSensitive()) {
                    issues.add(subject + " token [" + part.property()
                            + "] is a sensitive field - the generated Name is visible on the personal surface");
                }
                continue;
            }
            RelationIntent relation = toOneRelationByName(entity, part.relation());
            if (relation == null) {
                issues.add(
                        subject + " token [" + part.relation() + "." + part.property() + "] does not name a to-one relation of the entity");
                continue;
            }
            if (relation.isCrossModel()) {
                continue; // resolved against the owner model at generation
            }
            EntityIntent target = byName.get(relation.getTo());
            if (target == null) {
                continue;
            }
            boolean targetHasIt = fieldByName(target, part.property()) != null
                    || ("name".equalsIgnoreCase(part.property()) && target.getLabel() != null && !target.getLabel()
                                                                                                        .isBlank());
            if (!targetHasIt) {
                issues.add(subject + " token [" + part.relation() + "." + part.property() + "] does not name a field of ["
                        + relation.getTo() + "]");
            } else {
                FieldIntent targetField = fieldByName(target, part.property());
                if (targetField != null && targetField.isSensitive()) {
                    issues.add(subject + " token [" + part.relation() + "." + part.property() + "] is a sensitive field of ["
                            + relation.getTo() + "] - it must not leak into a label");
                }
            }
        }
    }

    /**
     * A relation's {@code calculatedActionOnCreate}/{@code calculatedActionOnUpdate} assigns the FK
     * column, so it needs a single FK to assign: a to-one relation that is not a composition parent
     * (the parent is preset by the layout, never derived) and not an EntityStatus badge (whose value
     * belongs to the workflow's transitions, not to a create-time default).
     */
    private static void validateRelationCalculatedAction(EntityIntent entity, RelationIntent relation, List<String> issues) {
        String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "]";
        boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
        if (!toOne) {
            issues.add(subject + " declares a calculated action but only a manyToOne/oneToOne relation has an FK column to assign");
            return;
        }
        if (relation.isComposition()) {
            issues.add(subject + " is a composition parent (preset by the layout) so it cannot declare a calculated action");
        }
        if (relation.isEntityStatus()) {
            issues.add(subject + " is an EntityStatus (owned by the workflow transitions) so it cannot declare a calculated action"
                    + " - use init: for its starting value");
        }
    }

    /**
     * {@code leafOnly: true} restricts a to-one relation to leaf nodes of its target's hierarchy, so
     * the target must declare one. A same-model target is checked here; a cross-model target is
     * validated at generation against the resolved owner model (like the relation target itself).
     */
    private static void validateLeafOnly(EntityIntent entity, RelationIntent relation, java.util.Map<String, EntityIntent> byName,
            List<String> issues) {
        String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "]";
        boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
        if (!toOne) {
            issues.add(subject + " declares leafOnly but only a manyToOne/oneToOne relation has a picker to restrict");
            return;
        }
        if (relation.isComposition()) {
            issues.add(subject + " is a composition parent (preset by the layout, never picked) so it cannot declare leafOnly");
            return;
        }
        if (!relation.isCrossModel()) {
            EntityIntent target = byName.get(relation.getTo());
            if (target != null && (target.getHierarchy() == null || target.getHierarchy()
                                                                          .isBlank())) {
                issues.add(subject + " declares leafOnly but its target [" + relation.getTo() + "] declares no hierarchy");
            }
        }
    }

    /**
     * A {@code checks} entry is one of three kinds. {@code exactlyOne} is row-level: at least two own
     * fields, no status gate (it must hold on every write). {@code itemsSumEqual}/{@code itemsMin} are
     * document-level: the entity must own a composition child (the items), the {@code over} fields must
     * be two numeric fields OF THE ITEMS entity, and a {@code status} gate (an EntityStatus seed id) is
     * mandatory - without it the check would forbid drafting the document item by item.
     */
    /**
     * A guard's {@code outcome} decides what a violation does, and each outcome needs its own companion
     * key: {@code block} (the default) throws and takes neither; {@code task} needs a boolean
     * {@code marker} field to stamp as the process's branch input; {@code reject} needs a
     * {@code setStatus} seed id and an {@code function: EntityStatus} relation to write it to. A
     * companion key belonging to another outcome is an authoring mistake, not something to ignore
     * silently - the write would appear guarded and do nothing.
     */
    private static void validateGuardOutcome(EntityIntent entity, CheckIntent check, String subject, List<String> issues) {
        String outcome = check.getOutcome() == null || check.getOutcome()
                                                            .isBlank() ? "block"
                                                                    : check.getOutcome()
                                                                           .trim()
                                                                           .toLowerCase(java.util.Locale.ROOT);
        if (!"block".equals(outcome) && !"task".equals(outcome) && !"reject".equals(outcome)) {
            issues.add(subject + " has unknown `outcome` [" + check.getOutcome() + "] - expected block, task or reject");
            return;
        }
        if (!"task".equals(outcome) && check.getMarker() != null) {
            issues.add(subject + " declares `marker` but its outcome is [" + outcome + "] - marker applies to outcome: task");
        }
        if (!"reject".equals(outcome) && check.getSetStatus() != null) {
            issues.add(subject + " declares `setStatus` but its outcome is [" + outcome + "] - setStatus applies to outcome: reject");
        }
        if ("task".equals(outcome)) {
            if (check.getMarker() == null || check.getMarker()
                                                  .isBlank()) {
                issues.add(subject + " with `outcome: task` requires `marker`: a boolean field of this entity to stamp");
                return;
            }
            FieldIntent marker = fieldByName(entity, check.getMarker());
            if (marker == null) {
                issues.add(subject + " marker [" + check.getMarker() + "] does not name a field of [" + entity.getName() + "]");
            } else if (!"boolean".equalsIgnoreCase(marker.getType())) {
                issues.add(subject + " marker [" + check.getMarker() + "] must be a boolean field, not [" + marker.getType() + "]");
            }
        }
        if ("reject".equals(outcome)) {
            if (check.getSetStatus() == null) {
                issues.add(subject + " with `outcome: reject` requires `setStatus`: the EntityStatus seed id to force");
                return;
            }
            boolean hasStatus = entity.getRelations()
                                      .stream()
                                      .anyMatch(r -> "EntityStatus".equalsIgnoreCase(r.getFunction()));
            if (!hasStatus) {
                issues.add(subject + " with `outcome: reject` requires a `function: EntityStatus` relation on [" + entity.getName()
                        + "] to write the status to");
            }
        }
    }

    private static void validateCheck(EntityIntent entity, CheckIntent check, java.util.Map<String, EntityIntent> byName,
            List<org.eclipse.dirigible.components.intent.model.AggregateIntent> aggregates, List<String> issues) {
        String subject = "entity [" + entity.getName() + "] check [" + (check.getKind() == null ? "?" : check.getKind()) + "]";
        String kind = check.getKind();
        if ("guard".equals(kind)) {
            // An aggregate guard names an aggregates: entry whose `of` is THIS entity (v1: the guarded
            // entity is the aggregate source, so the sum is recomputed race-free from the local store).
            if (check.getAggregate() == null || check.getAggregate()
                                                     .isBlank()) {
                issues.add(subject + " requires `aggregate`: the name of an aggregates: entry over this entity");
                return;
            }
            org.eclipse.dirigible.components.intent.model.AggregateIntent agg = null;
            if (aggregates != null) {
                for (org.eclipse.dirigible.components.intent.model.AggregateIntent a : aggregates) {
                    if (check.getAggregate()
                             .equals(a.getName())) {
                        agg = a;
                        break;
                    }
                }
            }
            if (agg == null) {
                issues.add(subject + " references unknown aggregate [" + check.getAggregate() + "]");
                return;
            }
            if (!entity.getName()
                       .equals(agg.getOf())) {
                issues.add(subject + " aggregate [" + check.getAggregate() + "] is over [" + agg.getOf()
                        + "], not this entity - v1 supports only a guard on the aggregate's own source entity");
            }
            if (agg.getSum() == null || agg.getSum()
                                           .isBlank()) {
                issues.add(subject + " aggregate [" + check.getAggregate() + "] must be a `sum` aggregate to guard");
            }
            validateGuardOutcome(entity, check, subject, issues);
            return;
        }
        if ("exactlyOne".equals(kind)) {
            if (check.getFields() == null || check.getFields()
                                                  .size() < 2) {
                issues.add(subject + " requires `fields`: at least two of the entity's own fields");
                return;
            }
            if (check.getStatus() != null) {
                issues.add(subject + " is row-level and cannot carry a `status` gate - it must hold on every write");
            }
            for (String field : check.getFields()) {
                if (fieldByName(entity, field) == null) {
                    issues.add(subject + " field [" + field + "] is not a field of [" + entity.getName() + "]");
                }
            }
            return;
        }
        if ("itemsSumEqual".equals(kind) || "itemsMin".equals(kind)) {
            EntityIntent items = compositionChildOf(entity, byName);
            if (items == null) {
                issues.add(subject + " requires the entity to own a composition child (the document's items)");
                return;
            }
            if (check.getStatus() == null || check.getStatus() <= 0) {
                issues.add(subject + " requires a `status` gate (an EntityStatus seed id) - without one the check would"
                        + " forbid drafting the document item by item");
            }
            boolean hasStatus = false;
            if (entity.getRelations() != null) {
                for (RelationIntent relation : entity.getRelations()) {
                    if (relation.isEntityStatus()) {
                        hasStatus = true;
                        break;
                    }
                }
            }
            if (!hasStatus) {
                issues.add(subject + " requires the entity to declare a `function: EntityStatus` relation for the gate");
            }
            if ("itemsSumEqual".equals(kind)) {
                if (check.getOver() == null || check.getOver()
                                                    .size() != 2) {
                    issues.add(subject + " requires `over`: exactly two numeric fields of the items entity");
                } else {
                    for (String field : check.getOver()) {
                        FieldIntent itemsField = fieldByName(items, field);
                        if (itemsField == null) {
                            issues.add(subject + " over [" + field + "] is not a field of the items entity [" + items.getName() + "]");
                        }
                    }
                }
            } else if (check.getCount() == null || check.getCount() < 1) {
                issues.add(subject + " requires `count`: the minimum number of items (>= 1)");
            }
            return;
        }
        issues.add(subject + " has unknown kind - expected exactlyOne, itemsSumEqual or itemsMin");
    }

    /** Whether the name matches (case-insensitively) a field or to-one relation of the entity. */
    private static boolean hasPropertyIgnoreCase(EntityIntent entity, String name) {
        if (entity.getFields() != null) {
            for (FieldIntent field : entity.getFields()) {
                if (name.equalsIgnoreCase(field.getName())) {
                    return true;
                }
            }
        }
        if (entity.getRelations() != null) {
            for (RelationIntent relation : entity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && name.equalsIgnoreCase(relation.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether a posting {@code rule(<column>)} reference names a field or a relation of the rule entity
     * (authored lower-camel matched case-insensitively against the PascalCase property).
     */
    private static boolean isRuleColumn(EntityIntent ruleEntity, String column) {
        if (column == null || column.isBlank()) {
            return false;
        }
        if (ruleEntity.getFields() != null) {
            for (FieldIntent field : ruleEntity.getFields()) {
                if (column.equalsIgnoreCase(field.getName())) {
                    return true;
                }
            }
        }
        if (ruleEntity.getRelations() != null) {
            for (RelationIntent relation : ruleEntity.getRelations()) {
                if (column.equalsIgnoreCase(relation.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The entity's composition child (the first entity declaring a composition to-one back to it). */
    private static EntityIntent compositionChildOf(EntityIntent entity, java.util.Map<String, EntityIntent> byName) {
        for (EntityIntent candidate : byName.values()) {
            if (candidate.getRelations() == null) {
                continue;
            }
            for (RelationIntent relation : candidate.getRelations()) {
                if (relation.isComposition() && entity.getName()
                                                      .equals(relation.getTo())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * A {@code where} declaration (a static dropdown option filter) is a single
     * {@code <target property>: <scalar literal>} pair on a user-picked to-one relation. The property
     * must exist on the relation's target (same-model targets checked here; cross-model at generation
     * time, like the relation target itself). A composition parent FK is preset by the layout - never
     * picked - so a filter there is authoring noise and rejected.
     */
    private static void validateWhere(EntityIntent entity, RelationIntent relation, java.util.Map<String, EntityIntent> byName,
            List<String> issues) {
        String subject = "entity [" + entity.getName() + "] relation [" + relation.getName() + "]";
        boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
        if (!toOne) {
            issues.add(subject + " declares where but only a manyToOne/oneToOne relation has a dropdown to filter");
            return;
        }
        if (relation.isComposition()) {
            issues.add(subject + " is a composition parent (preset by the layout, never picked) so it cannot declare where");
            return;
        }
        if (relation.isEntityStatus()) {
            issues.add(subject + " is an EntityStatus (a read-only badge) so it cannot declare where");
            return;
        }
        if (relation.getWhere()
                    .size() != 1) {
            issues.add(subject + " where must be a single `<target property>: <literal>` pair (multiple conditions are not supported yet)");
            return;
        }
        java.util.Map.Entry<String, Object> condition = relation.getWhere()
                                                                .entrySet()
                                                                .iterator()
                                                                .next();
        Object value = condition.getValue();
        if (value == null || value instanceof java.util.Collection || value instanceof java.util.Map) {
            issues.add(subject + " where [" + condition.getKey() + "] value must be a scalar literal");
            return;
        }
        if (!relation.isCrossModel()) {
            EntityIntent target = byName.get(relation.getTo());
            if (target != null && fieldByName(target, condition.getKey()) == null
                    && toOneRelationByName(target, condition.getKey()) == null) {
                issues.add(subject + " where [" + condition.getKey() + "] is not a field or to-one relation of [" + relation.getTo() + "]");
            }
        }
    }

    /**
     * A {@code dependsOn} declaration (on a field or a to-one relation) must name a sibling to-one
     * relation as its trigger, and its {@code valueFrom}/{@code filterBy} must resolve to properties of
     * the trigger's / the owning relation's target entity. Cross-model targets are validated against
     * the referenced {@code .model} at generation time, not here (same contract as the relation target
     * itself); a same-model target is checked immediately so a typo fails at parse time.
     */
    /**
     * A {@code number:} declaration must sit on a non-key <b>string</b> field, name a {@code series},
     * use a known {@code stampOn} ({@code create}/{@code issue}), and an optional {@code per} must name
     * a non-status to-one relation of the entity (the series partition, e.g. {@code Company}). The
     * removed keys ({@code format}/{@code scope}/{@code resetOn}) are rejected on the raw YAML tree in
     * {@code rejectRemovedNumberKeys} - the typed mapping would silently drop them.
     */
    /**
     * Rejects the REMOVED {@code number:} keys ({@code format}, {@code scope}, {@code resetOn}) on the
     * raw YAML tree, before the typed Gson mapping silently drops them. An intent still carrying
     * {@code format:} would otherwise "parse fine" and quietly lose the author's shape - the exact
     * silent failure this feature forbids everywhere else.
     *
     * @param tree the SnakeYAML-loaded raw tree
     * @throws IntentValidationException naming every removed key found, with the migration target
     */
    /**
     * A {@code generates[].items} may be EITHER an object (the mirror form ->
     * {@link GeneratesItemsIntent}) or a LIST of computed line rows (issue #6555 ->
     * {@code GeneratesIntent.itemLines}). Gson maps a field by its static type, so a list-valued
     * {@code items:} would fail the typed mapping against the object-typed {@code items} field. Rehome
     * a list-valued {@code items:} to the {@code itemLines} key on the raw tree, BEFORE the typed
     * mapping, so the two shapes stay in distinct typed fields. A mapping-valued {@code items:} is left
     * untouched.
     *
     * @param tree the SnakeYAML-loaded raw tree
     */
    private static void moveGeneratesItemLines(Object tree) {
        if (!(tree instanceof Map<?, ?> root)) {
            return;
        }
        if (root.get("generates") instanceof List<?> generates) {
            for (Object generateNode : generates) {
                rehomeItemLines(generateNode);
            }
        }
        // A schedule's generate rejects items entirely (validated below); rehome a list-valued items:
        // here too, so the invalid combination surfaces as that clear message rather than a Gson crash.
        if (root.get("schedules") instanceof List<?> schedules) {
            for (Object scheduleNode : schedules) {
                if (scheduleNode instanceof Map<?, ?> schedule) {
                    rehomeItemLines(schedule.get("generate"));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void rehomeItemLines(Object generateNode) {
        if (!(generateNode instanceof Map<?, ?> generate) || !(generate.get("items") instanceof List<?> itemLines)) {
            return;
        }
        Map<Object, Object> mutable = (Map<Object, Object>) generate;
        mutable.put("itemLines", itemLines);
        mutable.remove("items");
    }

    private static void rejectRemovedNumberKeys(Object tree) {
        if (!(tree instanceof Map<?, ?> root)) {
            return;
        }
        List<String> issues = new ArrayList<>();
        if (root.get("entities") instanceof List<?> entities) {
            for (Object entityNode : entities) {
                if (!(entityNode instanceof Map<?, ?> entity) || !(entity.get("fields") instanceof List<?> fields)) {
                    continue;
                }
                for (Object fieldNode : fields) {
                    if (!(fieldNode instanceof Map<?, ?> field) || !(field.get("number") instanceof Map<?, ?> number)) {
                        continue;
                    }
                    String subject = "entity [" + entity.get("name") + "] field [" + field.get("name") + "]";
                    if (number.containsKey("format")) {
                        issues.add(subject + " number declares `format` - removed: a number is prefix + zero-padded sequence, and its"
                                + " shape (prefix, size) is declared in the module's `.numbers` artefact and configured per tenant in"
                                + " the Document Numbering settings, never in the model");
                    }
                    if (number.containsKey("scope")) {
                        issues.add(subject + " number declares `scope` - removed: partition a series with `per: <to-one relation>`"
                                + " (e.g. `per: Company`) instead");
                    }
                    if (number.containsKey("resetOn")) {
                        issues.add(subject + " number declares `resetOn` - removed: sequences are continuous and never auto-reset;"
                                + " a jurisdiction that restarts numbering is an administrator setting the prefix and the next value"
                                + " in the Document Numbering settings");
                    }
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IntentValidationException(issues);
        }
    }

    private static void validateNumber(EntityIntent entity, String subject, FieldIntent field, List<String> issues) {
        NumberIntent number = field.getNumber();
        if (field.isPrimaryKey()) {
            issues.add(subject + " is a primary key so it cannot declare number");
            return;
        }
        if (!"string".equalsIgnoreCase(field.getType())) {
            issues.add(subject + " declares number but only a string field can carry a document number (got [" + field.getType() + "])");
        }
        if (isBlank(number.getSeries())) {
            issues.add(subject + " number requires `series`: the series this field draws from (several fields may reference the same"
                    + " series to share one running sequence). Its prefix and width are defined in the module's `.numbers` artefact.");
        }
        String stampOn = number.getStampOn();
        if (!isBlank(stampOn) && !"create".equals(stampOn) && !"issue".equals(stampOn)) {
            issues.add(subject + " number `stampOn` must be `create` or `issue`, got [" + stampOn + "]");
        }
        // `per` partitions the series - each value of the named to-one gets its own sequence. It must be a
        // relation, not a field: the partition identifies a RECORD (the company that owes the range), and a
        // scalar would silently change the partition when someone edits it.
        if (!isBlank(number.getPer())) {
            RelationIntent partition = toOneRelationByName(entity, number.getPer());
            if (partition == null) {
                issues.add(subject + " number `per` [" + number.getPer() + "] is not a to-one relation of [" + entity.getName()
                        + "] - it names the relation whose value partitions the series (e.g. `per: Company`)");
            } else if (partition.isEntityStatus()) {
                issues.add(subject + " number `per` [" + number.getPer() + "] is an EntityStatus - a status must not partition a number"
                        + " series, or the number would depend on the document's state");
            }
        }
    }

    /** The named field formats (#6463). A preset over `pattern`, so each maps to a canonical regex. */
    private static final Set<String> FIELD_FORMATS = Set.of("email");

    /**
     * A field's named {@code format} (#6463): a preset over {@link FieldIntent#getPattern()}. String
     * fields only, for the same reason a raw pattern is - on a numeric property the emitted
     * {@code widgetPattern} is read as the DISPLAY format. Declaring both {@code format} and
     * {@code pattern} is rejected rather than silently resolved: which one wins would be invisible to
     * the author, and both land on the same attribute.
     */
    private static void validateFormat(String subject, FieldIntent field, List<String> issues) {
        String format = field.getFormat()
                             .trim()
                             .toLowerCase();
        if (!FIELD_FORMATS.contains(format)) {
            issues.add(subject + " unknown `format` [" + field.getFormat() + "] - supported: " + FIELD_FORMATS);
            return;
        }
        String type = field.getType() == null ? ""
                : field.getType()
                       .toLowerCase();
        if (!"string".equals(type)) {
            issues.add(subject + " `format` applies to a string field - got type [" + field.getType() + "]");
        }
        if (!isBlank(field.getPattern())) {
            issues.add(subject + " declares both `format` and `pattern` - they set the same validation, so declare one");
        }
    }

    /**
     * A field's input-format {@code pattern} (#6336): a compilable regex on a string-typed field. The
     * type restriction is not cosmetic - on a numeric property the emitted {@code widgetPattern} is
     * read as the DISPLAY format, so a regex there would silently corrupt how the number renders.
     */
    private static void validatePattern(String subject, FieldIntent field, List<String> issues) {
        String type = field.getType() == null ? ""
                : field.getType()
                       .toLowerCase();
        if (!"string".equals(type) && !"text".equals(type)) {
            issues.add(subject + " `pattern` applies to a string/text field - got type [" + field.getType()
                    + "] (on a numeric field the pattern is the display format, not a regex)");
            return;
        }
        if (field.getPattern()
                 .length() > PATTERN_MAX_LENGTH) {
            issues.add(subject + " `pattern` exceeds " + PATTERN_MAX_LENGTH + " characters");
            return;
        }
        try {
            // Compiled ONLY to validate the developer-authored model source at parse time; the result is
            // discarded and never matched against runtime input, so there is no injection surface here.
            java.util.regex.Pattern.compile(field.getPattern()); // lgtm[java/regex-injection]
        } catch (java.util.regex.PatternSyntaxException ex) {
            issues.add(subject + " `pattern` is not a valid regular expression: " + ex.getDescription());
        }
    }

    private static void validateDependsOn(EntityIntent entity, String subject, DependsOnIntent dependsOn, RelationIntent ownRelation,
            java.util.Map<String, EntityIntent> byName, List<String> issues) {
        String triggerName = dependsOn.getRelation();
        if (triggerName == null || triggerName.isBlank()) {
            issues.add(subject + " dependsOn requires `relation`: the sibling to-one relation that triggers it");
            return;
        }
        // A dotted `relation` is the header-mediated form (#6358): the trigger is not a sibling of this
        // entity but a to-one of the open document header, reached through the composition parent.
        if (triggerName.indexOf('.') >= 0) {
            validateHeaderMediatedDependsOn(entity, subject, dependsOn, ownRelation, triggerName, byName, issues);
            return;
        }
        if (ownRelation != null && triggerName.equals(ownRelation.getName())) {
            issues.add(subject + " dependsOn cannot reference itself as the trigger");
            return;
        }
        RelationIntent trigger = toOneRelationByName(entity, triggerName);
        if (trigger == null) {
            issues.add(subject + " dependsOn relation [" + triggerName + "] is not a to-one relation of [" + entity.getName() + "]");
            return;
        }
        if (trigger.isEntityStatus()) {
            issues.add(subject + " dependsOn relation [" + triggerName + "] is an EntityStatus (a read-only badge) so it cannot trigger");
        }
        if (ownRelation == null) {
            // A scalar field is auto-populated - it needs the source property and has no option list.
            if (!dependsOn.hasValueFrom()) {
                issues.add(subject + " dependsOn requires `valueFrom`: the trigger target's property to copy the value from");
            }
            if (dependsOn.getFilterBy() != null && !dependsOn.getFilterBy()
                                                             .isBlank()) {
                issues.add(subject + " dependsOn `filterBy` applies only to a relation (a dropdown) - a field has no option list");
            }
        } else if (!dependsOn.hasValueFrom() && isBlank(dependsOn.getFilterBy())) {
            issues.add(subject + " dependsOn requires `valueFrom` and/or `filterBy` - with neither, the filter would compare the target's"
                    + " primary key against the trigger's primary key");
        }
        // The conditional valueFrom form (#6358): { by, cases, default? } - fields only.
        java.util.Map<String, Object> conditional = dependsOn.getValueFromConditional();
        if (conditional != null) {
            if (ownRelation != null) {
                issues.add(subject + " dependsOn conditional valueFrom is supported on a field (auto-populate), not on a relation");
            } else {
                validateConditionalValueFrom(entity, subject, conditional, trigger, byName, issues);
            }
        }
        // valueFrom lives on the TRIGGER's target entity; filterBy on the OWNING relation's target.
        validateDependsOnProperty(subject, "valueFrom", dependsOn.getValueFrom(), trigger, byName, issues);
        if (ownRelation != null) {
            validateDependsOnProperty(subject, "filterBy", dependsOn.getFilterBy(), ownRelation, byName, issues);
        }
    }

    /**
     * The header-mediated trigger form (#6358): {@code relation: <composition parent>.<header to-one>}
     * on a document ITEM field, so the line defaults a value from a record the DOCUMENT points at (the
     * canonical case: a line discount defaulting from the header partner's standard discount). The
     * trigger lives on the header, so there is no option list to cascade - fields only, and
     * {@code valueFrom} is mandatory exactly as for a sibling-triggered field.
     */
    private static void validateHeaderMediatedDependsOn(EntityIntent entity, String subject, DependsOnIntent dependsOn,
            RelationIntent ownRelation, String triggerName, java.util.Map<String, EntityIntent> byName, List<String> issues) {
        if (ownRelation != null) {
            issues.add(subject + " dependsOn header-mediated `relation` [" + triggerName
                    + "] is supported on a field (auto-populate), not on a relation - the header's selection cannot filter this dropdown");
            return;
        }
        String[] segments = triggerName.split("\\.");
        if (segments.length != 2) {
            issues.add(subject + " dependsOn `relation` [" + triggerName
                    + "] must be `<composition parent relation>.<header to-one relation>`");
            return;
        }
        RelationIntent compositionParent = compositionParentRelation(entity, segments[0]);
        if (compositionParent == null) {
            issues.add(subject + " dependsOn `relation` [" + triggerName + "]: [" + segments[0]
                    + "] is not the composition parent relation of [" + entity.getName() + "]");
            return;
        }
        if (!dependsOn.hasValueFrom()) {
            issues.add(subject + " dependsOn requires `valueFrom`: the property to copy from the header's [" + segments[1] + "] record");
        }
        if (!isBlank(dependsOn.getFilterBy())) {
            issues.add(subject + " dependsOn `filterBy` applies only to a relation (a dropdown) - a field has no option list");
        }
        if (compositionParent.isCrossModel()) {
            return; // the header lives in another model - resolved at generation time
        }
        EntityIntent header = byName.get(compositionParent.getTo());
        if (header == null) {
            return; // the dangling composition target is reported separately
        }
        RelationIntent trigger = toOneRelationByName(header, segments[1]);
        if (trigger == null) {
            issues.add(subject + " dependsOn `relation` [" + triggerName + "]: [" + segments[1] + "] is not a to-one relation of ["
                    + compositionParent.getTo() + "]");
            return;
        }
        if (trigger.isEntityStatus()) {
            issues.add(subject + " dependsOn relation [" + triggerName + "] is an EntityStatus (a read-only badge) so it cannot trigger");
        }
        java.util.Map<String, Object> conditional = dependsOn.getValueFromConditional();
        if (conditional != null) {
            validateConditionalValueFrom(entity, subject, conditional, trigger, byName, issues);
        }
        validateDependsOnProperty(subject, "valueFrom", dependsOn.getValueFrom(), trigger, byName, issues);
    }

    /**
     * The composition parent relation of an item entity by name, or null when the entity has no such
     * relation - i.e. the name does not denote the open document header.
     */
    private static RelationIntent compositionParentRelation(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.isComposition() && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    /**
     * The conditional {@code valueFrom: { by, cases, default? }} form (#6358): {@code by} is a 1-3
     * segment classifier path - an own property, a one-hop {@code <OwnRelation>.<property>}, or (on a
     * composition item) a path starting at the composition parent relation, i.e. the open document
     * header; {@code cases} maps classifier literals to properties of the TRIGGER's target (validated
     * like a plain {@code valueFrom}); {@code default} is the optional no-match property.
     */
    private static void validateConditionalValueFrom(EntityIntent entity, String subject, java.util.Map<String, Object> conditional,
            RelationIntent trigger, java.util.Map<String, EntityIntent> byName, List<String> issues) {
        for (Object key : conditional.keySet()) {
            if (!"by".equals(key) && !"cases".equals(key) && !"default".equals(key)) {
                issues.add(subject + " dependsOn conditional valueFrom supports `by`, `cases` and `default` - got [" + key + "]");
            }
        }
        Object casesValue = conditional.get("cases");
        if (!(casesValue instanceof java.util.Map) || ((java.util.Map<?, ?>) casesValue).isEmpty()) {
            issues.add(subject + " dependsOn conditional valueFrom requires `cases`: a non-empty `<classifier literal>: <property>` map");
        } else {
            for (Object property : ((java.util.Map<?, ?>) casesValue).values()) {
                validateDependsOnProperty(subject, "cases", String.valueOf(property), trigger, byName, issues);
            }
        }
        Object defaultValue = conditional.get("default");
        if (defaultValue != null) {
            validateDependsOnProperty(subject, "default", String.valueOf(defaultValue), trigger, byName, issues);
        }
        Object by = conditional.get("by");
        if (!(by instanceof String) || ((String) by).isBlank()) {
            issues.add(subject + " dependsOn conditional valueFrom requires `by`: the classifier path");
            return;
        }
        String[] segments = ((String) by).split("\\.");
        // Resolve the path start: an own property (1 segment), an own to-one (2 segments), or the
        // composition parent relation - the open document header (2-3 segments, items only).
        String first = segments[0];
        RelationIntent compositionParent = compositionParentRelation(entity, first);
        if (compositionParent != null) {
            EntityIntent header = compositionParent.isCrossModel() ? null : byName.get(compositionParent.getTo());
            if (segments.length == 2) {
                requirePathProperty(subject, by, segments[1], header, compositionParent.getTo(), issues);
            } else if (segments.length == 3) {
                RelationIntent headerRelation = header == null ? null : toOneRelationByName(header, segments[1]);
                if (header != null && headerRelation == null) {
                    issues.add(subject + " dependsOn `by` [" + by + "]: [" + segments[1] + "] is not a to-one relation of ["
                            + compositionParent.getTo() + "]");
                } else if (headerRelation != null && !headerRelation.isCrossModel()) {
                    requirePathProperty(subject, by, segments[2], byName.get(headerRelation.getTo()), headerRelation.getTo(), issues);
                }
            } else {
                issues.add(subject + " dependsOn `by` [" + by + "]: a header-started path needs a header property" + " (`" + first
                        + ".<property>` or `" + first + ".<Relation>.<property>`)");
            }
            return;
        }
        if (segments.length == 1) {
            if (fieldByName(entity, first) == null && toOneRelationByName(entity, first) == null) {
                issues.add(subject + " dependsOn `by` [" + by + "] is not a field or to-one relation of [" + entity.getName() + "]");
            }
        } else if (segments.length == 2) {
            RelationIntent hop = toOneRelationByName(entity, first);
            if (hop == null) {
                issues.add(subject + " dependsOn `by` [" + by + "]: [" + first + "] is not a to-one relation of [" + entity.getName()
                        + "] (or the composition parent relation of an item)");
            } else if (!hop.isCrossModel()) {
                requirePathProperty(subject, by, segments[1], byName.get(hop.getTo()), hop.getTo(), issues);
            }
        } else {
            issues.add(subject + " dependsOn `by` [" + by + "]: a 3-segment path must start at the composition parent relation");
        }
    }

    private static void requirePathProperty(String subject, Object path, String property, EntityIntent target, String targetName,
            List<String> issues) {
        if (target == null) {
            return; // dangling/cross-model target reported (or validated) elsewhere
        }
        if (fieldByName(target, property) == null && toOneRelationByName(target, property) == null) {
            issues.add(subject + " dependsOn `by` [" + path + "]: [" + property + "] is not a field or to-one relation of [" + targetName
                    + "]");
        }
    }

    private static void validateDependsOnProperty(String subject, String attribute, String property, RelationIntent targetRelation,
            java.util.Map<String, EntityIntent> byName, List<String> issues) {
        if (property == null || property.isBlank() || targetRelation.isCrossModel()) {
            return;
        }
        EntityIntent target = byName.get(targetRelation.getTo());
        if (target == null) {
            return; // the dangling relation target is reported separately
        }
        if (fieldByName(target, property) == null && toOneRelationByName(target, property) == null) {
            issues.add(subject + " dependsOn " + attribute + " [" + property + "] is not a field or to-one relation of ["
                    + targetRelation.getTo() + "]");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void validateProcesses(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> processNames = new HashSet<>();
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                issues.add("process has no name");
                continue;
            }
            if (!processNames.add(process.getName())) {
                issues.add("duplicate process [" + process.getName() + "]");
            }
            int triggerEvents = 0;
            String triggerEntity = null;
            for (String kind : EVENT_KINDS) {
                Object target = process.getTrigger()
                                       .get(kind);
                if (target != null) {
                    triggerEvents++;
                    triggerEntity = target.toString();
                    if (!entityNames.contains(target.toString())) {
                        issues.add("process [" + process.getName() + "] trigger " + kind + " references unknown entity [" + target + "]");
                    }
                }
            }
            if (triggerEvents > 1) {
                issues.add("process [" + process.getName() + "] trigger must declare at most one of onCreate/onUpdate/onDelete");
            }
            // An optional businessKey flags which trigger-entity field becomes the started process
            // instance's BPM business key; it must be a field of the triggered entity.
            Object businessKey = process.getTrigger()
                                        .get("businessKey");
            FieldIntent businessKeyField = null;
            if (businessKey != null) {
                if (triggerEntity == null) {
                    issues.add("process [" + process.getName()
                            + "] trigger declares businessKey but no onCreate/onUpdate/onDelete event to start on");
                } else {
                    EntityIntent triggered = byName.get(triggerEntity);
                    businessKeyField = triggered == null ? null : fieldByName(triggered, businessKey.toString());
                    if (triggered != null && businessKeyField == null) {
                        issues.add("process [" + process.getName() + "] trigger businessKey [" + businessKey + "] is not a field of ["
                                + triggerEntity + "]");
                    }
                }
            }
            // An optional businessKeyStrategy mints the businessKey field's value when blank. Only
            // "timestamp" (a yyyyMMddHHmmss string) is supported today, and it needs a string field.
            Object strategy = process.getTrigger()
                                     .get("businessKeyStrategy");
            if (strategy != null) {
                if (!"timestamp".equals(strategy.toString())) {
                    issues.add("process [" + process.getName() + "] trigger businessKeyStrategy [" + strategy
                            + "] is not supported (supported: timestamp)");
                } else if (businessKey == null) {
                    issues.add("process [" + process.getName() + "] trigger businessKeyStrategy needs a businessKey field to populate");
                } else if (businessKeyField != null && businessKeyField.getType() != null && !"string".equals(businessKeyField.getType())
                        && !"text".equals(businessKeyField.getType())) {
                    issues.add("process [" + process.getName() + "] trigger businessKey field [" + businessKey
                            + "] must be a string/text field to hold a generated timestamp");
                }
            }
            Set<String> stepNames = new HashSet<>();
            for (StepIntent step : process.getSteps()) {
                if (step.getName() == null || step.getName()
                                                  .isBlank()) {
                    issues.add("process [" + process.getName() + "] has a step with no name");
                    continue;
                }
                if (!stepNames.add(step.getName())) {
                    issues.add("process [" + process.getName() + "] declares step [" + step.getName() + "] twice");
                }
                if (step.getKind() != null && !STEP_KINDS.contains(step.getKind())) {
                    issues.add(
                            "process [" + process.getName() + "] step [" + step.getName() + "] has unknown kind [" + step.getKind() + "]");
                }
            }
            validateDecisionTargets(process, issues);
            validateSetFieldSteps(process, triggerEntity, byName, model, issues);
            validateWaitSteps(process, triggerEntity, byName, issues);
            validateUserTaskTimers(process, triggerEntity, byName, issues);
            validateAbortOn(process, triggerEntity, byName, issues);
            validateParallelSteps(process, issues);
            validateTaskFormActions(process, model, issues);
        }
    }

    /**
     * A user task is a <b>decision point</b> exactly when its form offers more than one completing
     * action (e.g. Approve / Reject - the auto-added {@code close} button never completes the task). In
     * that case the task must be <b>immediately followed by a decision</b> that branches on the chosen
     * {@code action}, or the extra buttons would all funnel into the same linear successor and do
     * nothing different - almost always an authoring mistake. A single-action task (e.g. {@code issue})
     * needs no decision: it flows on linearly (typically to a status {@code setField} and the next user
     * task). Enforced so the author sees, at parse time, what the chosen actions actually do.
     */
    /**
     * A {@code kind: parallel} step forks over {@code args.branches} (at least two declared steps, run
     * concurrently) and joins before {@code args.next}. Each branch is a <b>chain</b>: it continues
     * through its steps' own routing ({@code next}, a decision's {@code then}/{@code else}, a boundary
     * timer's {@code then}) and may itself be a nested {@code parallel}. Everything reachable that way
     * is the branch <b>region</b> ({@link ProcessParallelSupport#regions}), and the rules make the
     * region a closed sub-flow:
     *
     * <ul>
     * <li>a step with no routing at all is a branch terminal and joins implicitly; the literal
     * {@code join} converges on the join explicitly, and means nothing outside a branch;
     * <li>{@code end} is not reachable from inside a branch - a token that ends there never arrives at
     * the join, and the instance hangs on it forever;
     * <li>no step belongs to two branches - a step entered by two concurrent tokens would run twice and
     * still leave the join waiting;
     * <li>nothing outside a branch may route into one, which is also how "a branch routed to the fork's
     * own {@code next} instead of converging on {@code join}" surfaces;
     * <li>a top-level fork needs a {@code next} (a declared step or {@code end}) on the main flow; a
     * nested fork may omit it, and then joins into its own enclosing join.
     * </ul>
     */
    private static void validateParallelSteps(ProcessIntent process, List<String> issues) {
        Map<String, StepIntent> byName = new HashMap<>();
        for (StepIntent step : process.getSteps()) {
            if (step.getName() != null) {
                byName.put(step.getName(), step);
            }
        }
        ProcessParallelSupport.Regions regions = ProcessParallelSupport.regions(process.getSteps());
        String prefix = "process [" + process.getName() + "] ";
        for (StepIntent step : process.getSteps()) {
            if (!ProcessParallelSupport.isParallel(step)) {
                continue;
            }
            String subject = prefix + "parallel [" + step.getName() + "]";
            Map<String, Object> args = step.getArgs() == null ? Map.of() : step.getArgs();
            List<?> branches = args.get("branches") instanceof List<?> list ? list : List.of();
            if (branches.size() < 2) {
                issues.add(subject + " needs a `branches` list of at least two step names");
            }
            boolean nested = regions.contains(step.getName());
            String nextStep = trimmedOrNull(args.get("next"));
            if (nextStep == null && !nested) {
                issues.add(subject + " needs a `next` step to join into");
            } else if (nextStep != null && !nested && !"end".equalsIgnoreCase(nextStep) && !byName.containsKey(nextStep)) {
                issues.add(subject + " next [" + nextStep + "] is not a declared step or `end`");
            }
            Set<String> declared = new HashSet<>();
            for (Object branchRaw : branches) {
                String branch = String.valueOf(branchRaw);
                if (!declared.add(branch)) {
                    issues.add(subject + " lists branch [" + branch + "] twice");
                }
                if (branch.equals(step.getName())) {
                    issues.add(subject + " lists itself as a branch");
                } else if (!byName.containsKey(branch)) {
                    issues.add(subject + " branch [" + branch + "] is not a declared step");
                }
            }
        }
        for (String shared : regions.shared()) {
            issues.add(prefix + "step [" + shared + "] is reachable from more than one parallel branch - a step run by two"
                    + " concurrent tokens runs twice and still leaves the join waiting");
        }
        validateParallelRouting(process, byName, regions, issues);
    }

    /**
     * Validate every step's routing against the parallel branch regions: {@code join} is meaningful
     * only inside a branch, a branch may never reach the process end event, and nothing outside a
     * branch may point into one (see {@link #validateParallelSteps} for why).
     */
    private static void validateParallelRouting(ProcessIntent process, Map<String, StepIntent> byName,
            ProcessParallelSupport.Regions regions, List<String> issues) {
        String prefix = "process [" + process.getName() + "] ";
        for (StepIntent step : process.getSteps()) {
            if (step.getName() == null) {
                continue;
            }
            String join = regions.joinOf(step.getName());
            String subject = prefix + "step [" + step.getName() + "]";
            if (ProcessParallelSupport.JOIN_TARGET.equalsIgnoreCase(step.getName())) {
                issues.add(subject + " uses the reserved name `join` - that is the routing literal for a parallel branch's join gateway");
            }
            if (join != null && "end".equalsIgnoreCase(step.getKind())) {
                issues.add(subject + " is an `end` step inside a parallel branch - a branch must reach its join, so route to"
                        + " `join` and end after the fork instead");
            }
            for (String target : ProcessParallelSupport.routingTargets(step)) {
                if (ProcessParallelSupport.JOIN_TARGET.equalsIgnoreCase(target)) {
                    if (join == null) {
                        issues.add(subject + " routes to `join`, which is only valid inside a parallel branch");
                    }
                } else if (join == null && regions.contains(target)) {
                    // A branch absorbs whatever its steps route to, so a step still on the main flow
                    // pointing into a branch means the two claims collide. The fork's own `next` is the
                    // common case (a branch routed to it instead of converging on `join`) - and reporting
                    // it from the fork names both halves of the mistake.
                    issues.add(ProcessParallelSupport.isParallel(step)
                            ? prefix + "parallel [" + step.getName() + "] next [" + target + "] is also reachable from inside one of its"
                                    + " branches - a branch converges on `join`, it must not route to the fork's own `next`"
                            : subject + " routes to [" + target + "], which is inside a parallel branch - a branch is entered through its"
                                    + " fork only");
                } else if (join != null && isEndStep(target, byName)) {
                    issues.add(subject + " routes to `end` from inside a parallel branch - the join would wait for a token that"
                            + " never arrives; route to `join` instead");
                }
            }
        }
    }

    /** Whether a routing target is the process end event: the literal {@code end} or an `end` step. */
    private static boolean isEndStep(String target, Map<String, StepIntent> byName) {
        StepIntent step = byName.get(target);
        return "end".equalsIgnoreCase(target) || (step != null && "end".equalsIgnoreCase(step.getKind()));
    }

    /**
     * A routing target that names no step: the literal {@code end} (the process end event) or
     * {@code join} (the enclosing parallel branch's join gateway). Where {@code join} is actually
     * allowed is checked by {@link #validateParallelRouting} - it is only meaningful inside a branch.
     */
    private static boolean isRoutingLiteral(String target) {
        return "end".equalsIgnoreCase(target) || ProcessParallelSupport.JOIN_TARGET.equalsIgnoreCase(target);
    }

    /** A trimmed non-empty string form of a raw arg value, or {@code null}. */
    private static String trimmedOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString()
                           .trim();
        return text.isEmpty() ? null : text;
    }

    /** The entity a process trigger starts on (onCreate/onUpdate/onDelete target), or null. */
    private static String triggerEntityName(ProcessIntent process) {
        if (process.getTrigger() == null) {
            return null;
        }
        for (String kind : EVENT_KINDS) {
            Object target = process.getTrigger()
                                   .get(kind);
            if (target != null) {
                return target.toString();
            }
        }
        return null;
    }

    private static EntityIntent entityByNameInsensitive(IntentModel model, String name) {
        for (EntityIntent entity : model.getEntities()) {
            if (name.equalsIgnoreCase(entity.getName())) {
                return entity;
            }
        }
        return null;
    }

    private static void validateTaskFormActions(ProcessIntent process, IntentModel model, List<String> issues) {
        Map<String, FormIntent> formsByName = new HashMap<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() != null) {
                formsByName.put(form.getName(), form);
            }
        }
        List<StepIntent> steps = process.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            StepIntent step = steps.get(i);
            if (!"userTask".equals(step.getKind()) || step.getArgs() == null) {
                continue;
            }
            Object assignee = step.getArgs()
                                  .get("assignee");
            if ("personal".equals(assignee)) {
                // The per-user assignment resolves through the trigger entity's personal owner - the
                // trigger listener seeds __personalUser from the identity mapping at start time.
                String triggerEntity = triggerEntityName(process);
                EntityIntent target = triggerEntity == null ? null : entityByNameInsensitive(model, triggerEntity);
                boolean hasPersonal = target != null && target.getRelations() != null && target.getRelations()
                                                                                               .stream()
                                                                                               .anyMatch(RelationIntent::isPersonal);
                if (!hasPersonal) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] declares assignee: personal but the trigger entity has no personal relation to resolve the owner from");
                }
            }
            Object formArg = step.getArgs()
                                 .get("form");
            FormIntent form = formArg == null ? null : formsByName.get(formArg.toString());
            if (form == null) {
                continue;
            }
            List<String> completing = new ArrayList<>();
            for (String action : form.getActions()) {
                if (action != null && !action.isBlank() && !"close".equalsIgnoreCase(action)) {
                    completing.add(action);
                }
            }
            if (completing.size() <= 1) {
                continue; // single (or no) completing action -> linear flow, no decision required
            }
            StepIntent successor = successorStep(step, steps, i);
            if (successor == null || !"decision".equals(successor.getKind())) {
                issues.add("user task [" + step.getName() + "] in process [" + process.getName() + "] uses form [" + form.getName()
                        + "] with multiple actions " + completing + " but is not immediately followed by a decision - a multi-option"
                        + " task must branch on the chosen action via a decision (e.g. `kind: decision, args: { if: \"action == '"
                        + completing.get(0) + "'\", then: ..., else: ... }`), or reduce the form to a single action");
            }
        }
    }

    /**
     * The step a user task flows to: its {@code next} arg when set, otherwise the next declared step.
     */
    private static StepIntent successorStep(StepIntent step, List<StepIntent> steps, int index) {
        Object next = step.getArgs() == null ? null
                : step.getArgs()
                      .get("next");
        if (next != null && !next.toString()
                                 .isBlank()) {
            for (StepIntent candidate : steps) {
                if (next.toString()
                        .equals(candidate.getName())) {
                    return candidate;
                }
            }
            return null; // next names `end` or an unknown step (the latter is reported elsewhere)
        }
        return index + 1 < steps.size() ? steps.get(index + 1) : null;
    }

    /**
     * A {@code serviceTask} declaring {@code setField} must name a {@code string}/{@code text} field of
     * the process's trigger entity and carry a {@code value} (the literal to assign). Any step may
     * carry a {@code next} that routes its outgoing flow to a declared step or {@code end} (used to
     * make two decision branches converge). Without these checks a typo would surface only at runtime.
     * A {@code serviceTask} may instead declare a {@code notify} block - the step SENDS (see
     * {@link #validateNotifyBlock}) - which is its whole work and therefore stands alone.
     */
    private static void validateSetFieldSteps(ProcessIntent process, String triggerEntity, Map<String, EntityIntent> byName,
            IntentModel model, List<String> issues) {
        Set<String> stepNames = new HashSet<>();
        for (StepIntent step : process.getSteps()) {
            if (step.getName() != null) {
                stepNames.add(step.getName());
            }
        }
        EntityIntent trigger = triggerEntity == null ? null : byName.get(triggerEntity);
        for (StepIntent step : process.getSteps()) {
            if (step.getName() == null) {
                continue;
            }
            String setField = stepArg(step, "setField");
            if (setField != null && !setField.isBlank()) {
                if (!"serviceTask".equals(step.getKind())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] uses setField but is not a serviceTask");
                } else if (trigger == null) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] uses setField but the process has no trigger entity to set it on");
                } else {
                    FieldIntent field = fieldByName(trigger, setField);
                    if (field == null) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setField [" + setField
                                + "] is not a field of [" + triggerEntity + "]");
                    } else if (field.getType() != null && !"string".equals(field.getType()) && !"text".equals(field.getType())) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setField [" + setField
                                + "] must be a string/text field (only literal string values are supported)");
                    }
                    if (stepArg(step, "value") == null || stepArg(step, "value").isBlank()) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setField [" + setField
                                + "] must declare a value");
                    }
                }
            }
            String setRelationField = stepArg(step, "setRelationField");
            if (setRelationField != null && !setRelationField.isBlank()) {
                if (!"serviceTask".equals(step.getKind()) && !"userTask".equals(step.getKind())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] uses setRelationField but is not a serviceTask or userTask");
                } else if (trigger == null) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] uses setRelationField but the process has no trigger entity to set it on");
                } else {
                    RelationIntent relation = toOneRelationByName(trigger, setRelationField);
                    if (relation == null) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setRelationField [" + setRelationField
                                + "] is not a manyToOne/oneToOne relation of [" + triggerEntity + "]");
                    }
                    String value = stepArg(step, "value");
                    if (value == null || value.isBlank()) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setRelationField [" + setRelationField
                                + "] must declare a value (the related record id)");
                    } else if (!value.matches("-?\\d+")) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] setRelationField [" + setRelationField
                                + "] value [" + value + "] must be an integer record id");
                    }
                }
            }
            String delegate = stepArg(step, "delegate");
            if (delegate != null && !delegate.isBlank()) {
                if (!"serviceTask".equals(step.getKind())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] uses delegate but is not a serviceTask");
                }
                boolean hasCall = stepArg(step, "call") != null && !stepArg(step, "call").isBlank();
                if ((setField != null && !setField.isBlank()) || (setRelationField != null && !setRelationField.isBlank()) || hasCall) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] delegate cannot be combined with setField/setRelationField/call");
                }
                Object fields = step.getArgs() == null ? null
                        : step.getArgs()
                              .get("fields");
                if (fields != null && !(fields instanceof Map)) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName()
                            + "] delegate `fields` must be a map of name: value pairs");
                } else if (fields instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getValue() instanceof Map || entry.getValue() instanceof List) {
                            issues.add("process [" + process.getName() + "] step [" + step.getName() + "] delegate field [" + entry.getKey()
                                    + "] must be a scalar value");
                        }
                    }
                }
            }
            // A sending step: the reusable notify block, about the process's trigger entity ("after
            // Issue, mail the invoice to its customer"). It IS the step's work, so it stands alone.
            Object notifyArg = step.getArgs() == null ? null
                    : step.getArgs()
                          .get("notify");
            if (notifyArg != null) {
                String stepSubject = "process [" + process.getName() + "] step [" + step.getName() + "] notify";
                if (!"serviceTask".equals(step.getKind())) {
                    issues.add(stepSubject + " is only available on a serviceTask");
                } else if (!(notifyArg instanceof Map)) {
                    issues.add(stepSubject + " must be a map of to/subject/body (optionally attach: print)");
                } else if (trigger == null) {
                    issues.add(stepSubject + " needs a trigger entity - the record the message is about");
                } else {
                    boolean hasCall = stepArg(step, "call") != null && !stepArg(step, "call").isBlank();
                    if ((setField != null && !setField.isBlank()) || (setRelationField != null && !setRelationField.isBlank()) || hasCall
                            || (delegate != null && !delegate.isBlank())) {
                        issues.add(stepSubject + " cannot be combined with setField/setRelationField/call/delegate - give the send its own"
                                + " serviceTask");
                    }
                    validateNotifyBlock(NotificationIntent.fromMap(notifyArg), stepSubject, triggerEntity, model, issues);
                }
            }
            String next = stepArg(step, "next");
            if (next != null && !next.isBlank() && !isRoutingLiteral(next) && !stepNames.contains(next)) {
                issues.add(
                        "process [" + process.getName() + "] step [" + step.getName() + "] `next` references unknown step [" + next + "]");
            }
        }
    }

    /**
     * A {@code wait} step parks the process on an entity lifecycle event: exactly one of
     * {@code onCreate}/{@code onUpdate} naming a declared entity; when that entity is not the trigger
     * entity itself, {@code via:} must name the to-one relation of the <b>event</b> entity that walks
     * to the trigger entity (the record carrying the parked instance's {@code ProcessId}). Without
     * these checks a typo would leave the process parked forever instead of failing at parse time.
     */
    private static void validateWaitSteps(ProcessIntent process, String triggerEntity, Map<String, EntityIntent> byName,
            List<String> issues) {
        for (StepIntent step : process.getSteps()) {
            if (!"wait".equals(step.getKind()) || step.getName() == null) {
                continue;
            }
            if (stepArg(step, "onDelete") != null) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName()
                        + "] cannot bind onDelete - a deleted record cannot resume a wait (use onCreate/onUpdate)");
            }
            int events = 0;
            String eventEntity = null;
            for (String kind : List.of("onCreate", "onUpdate")) {
                String target = stepArg(step, kind);
                if (target != null) {
                    events++;
                    eventEntity = target;
                }
            }
            if (events != 1) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName()
                        + "] must declare exactly one of onCreate/onUpdate naming the resuming entity event");
                continue;
            }
            if (!byName.containsKey(eventEntity)) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName() + "] references unknown entity [" + eventEntity
                        + "]");
                continue;
            }
            if (triggerEntity == null) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName()
                        + "] needs a process trigger entity - its ProcessId identifies the parked instance to resume");
                continue;
            }
            String via = stepArg(step, "via");
            if (eventEntity.equals(triggerEntity)) {
                if (via != null && !via.isBlank()) {
                    issues.add("process [" + process.getName() + "] wait [" + step.getName() + "] must not declare via - the event entity ["
                            + eventEntity + "] is the trigger entity itself");
                }
                continue;
            }
            if (via == null || via.isBlank()) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName() + "] must declare via - the to-one relation of ["
                        + eventEntity + "] that walks to the trigger entity [" + triggerEntity + "]");
                continue;
            }
            RelationIntent relation = toOneRelationByName(byName.get(eventEntity), via);
            if (relation == null) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName() + "] via [" + via
                        + "] is not a manyToOne/oneToOne relation of [" + eventEntity + "]");
            } else if (relation.isCrossModel()) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName() + "] via [" + via
                        + "] must be a same-model relation (cross-model waits are not supported)");
            } else if (!triggerEntity.equals(relation.getTo())) {
                issues.add("process [" + process.getName() + "] wait [" + step.getName() + "] via [" + via + "] targets ["
                        + relation.getTo() + "] but must target the trigger entity [" + triggerEntity + "]");
            }
        }
    }

    /**
     * Boundary timers on a user task: {@code timeout: { after: <ISO-8601 duration>, then: <step> }}
     * (non-cancelling reminder/escalation) and {@code expire: { until: <date field>, then: <step> }}
     * (cancelling, date-field-driven expiry). {@code then} must reference a declared step or the
     * literal {@code end}, exactly like a decision branch; {@code until} must name a
     * {@code date}/{@code timestamp} field of the trigger entity, re-read at task entry.
     */
    private static void validateUserTaskTimers(ProcessIntent process, String triggerEntity, Map<String, EntityIntent> byName,
            List<String> issues) {
        Set<String> stepNames = new HashSet<>();
        for (StepIntent step : process.getSteps()) {
            if (step.getName() != null) {
                stepNames.add(step.getName());
            }
        }
        EntityIntent trigger = triggerEntity == null ? null : byName.get(triggerEntity);
        for (StepIntent step : process.getSteps()) {
            if (step.getName() == null || step.getArgs() == null) {
                continue;
            }
            for (String timer : List.of("timeout", "expire")) {
                Object raw = step.getArgs()
                                 .get(timer);
                if (raw == null) {
                    continue;
                }
                if (!"userTask".equals(step.getKind())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] declares " + timer
                            + " but is not a userTask - boundary timers attach to user tasks only");
                    continue;
                }
                if (!(raw instanceof Map<?, ?> map)) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] " + timer + " must be a map (e.g. `"
                            + timer + ": { " + ("timeout".equals(timer) ? "after: P3D" : "until: validUntil") + ", then: <step> }`)");
                    continue;
                }
                Object then = map.get("then");
                if (then == null || then.toString()
                                        .isBlank()) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] " + timer + " must declare `then`");
                } else if (!isRoutingLiteral(then.toString()) && !stepNames.contains(then.toString())) {
                    issues.add("process [" + process.getName() + "] step [" + step.getName() + "] " + timer
                            + " `then` references unknown step [" + then + "]");
                }
                if ("timeout".equals(timer)) {
                    Object after = map.get("after");
                    if (after == null || after.toString()
                                              .isBlank()) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName()
                                + "] timeout must declare `after` (an ISO-8601 duration, e.g. PT4H or P3D)");
                    } else if (!isIso8601Duration(after.toString())) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName() + "] timeout `after` [" + after
                                + "] is not an ISO-8601 duration (e.g. PT4H, P3D)");
                    }
                } else {
                    Object until = map.get("until");
                    if (until == null || until.toString()
                                              .isBlank()) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName()
                                + "] expire must declare `until` (a date/timestamp field of the trigger entity)");
                    } else if (trigger == null) {
                        issues.add("process [" + process.getName() + "] step [" + step.getName()
                                + "] expire needs a process trigger entity to read `until` from");
                    } else {
                        FieldIntent field = fieldByName(trigger, until.toString());
                        if (field == null) {
                            issues.add("process [" + process.getName() + "] step [" + step.getName() + "] expire `until` [" + until
                                    + "] is not a field of [" + triggerEntity + "]");
                        } else if (!"date".equals(field.getType()) && !"timestamp".equals(field.getType())) {
                            issues.add("process [" + process.getName() + "] step [" + step.getName() + "] expire `until` [" + until
                                    + "] must be a date/timestamp field");
                        }
                    }
                }
            }
        }
    }

    /**
     * A process {@code abortOn: { status: [ids] | id, then: <step> }} cancels the in-flight instance
     * when the trigger entity transitions into a listed EntityStatus seed id. Requires a trigger entity
     * carrying a {@code function: EntityStatus} relation; {@code status} is a non-empty list of integer
     * ids (a bare integer is accepted); the optional {@code then} names the literal {@code end}
     * (terminate, the default) or a declared {@code serviceTask} cleanup carrying a {@code setField} /
     * {@code setRelationField} (a non-interactive abort-only step - it must not be routed to from the
     * main flow).
     */
    private static void validateAbortOn(ProcessIntent process, String triggerEntity, Map<String, EntityIntent> byName,
            List<String> issues) {
        Map<String, Object> abortOn = process.getAbortOn();
        if (abortOn == null || abortOn.isEmpty()) {
            return;
        }
        Object statusRaw = abortOn.get("status");
        if (statusRaw == null) {
            issues.add("process [" + process.getName() + "] abortOn must declare `status` (an EntityStatus seed id or a list of ids)");
            return;
        }
        List<Object> statusItems = statusRaw instanceof List<?> list ? new ArrayList<>(list) : List.of(statusRaw);
        if (statusItems.isEmpty()) {
            issues.add("process [" + process.getName() + "] abortOn `status` must not be empty");
        }
        for (Object item : statusItems) {
            if (item == null || !item.toString()
                                     .trim()
                                     .matches("-?\\d+")) {
                issues.add("process [" + process.getName() + "] abortOn `status` [" + item + "] must be an integer EntityStatus seed id");
            }
        }
        EntityIntent trigger = triggerEntity == null ? null : byName.get(triggerEntity);
        if (trigger == null) {
            issues.add("process [" + process.getName()
                    + "] abortOn needs a process trigger entity - its transition and ProcessId identify the instance to abort");
        } else if (!hasEntityStatusRelation(trigger)) {
            issues.add("process [" + process.getName() + "] abortOn requires the trigger entity [" + triggerEntity
                    + "] to declare a function: EntityStatus relation to match the abort statuses against");
        }
        Object thenRaw = abortOn.get("then");
        if (thenRaw != null) {
            String then = thenRaw.toString()
                                 .trim();
            if (!then.isEmpty() && !"end".equalsIgnoreCase(then)) {
                StepIntent thenStep = null;
                for (StepIntent step : process.getSteps()) {
                    if (then.equals(step.getName())) {
                        thenStep = step;
                    }
                }
                if (thenStep == null) {
                    issues.add("process [" + process.getName() + "] abortOn `then` references unknown step [" + then + "]");
                } else if (!"serviceTask".equals(thenStep.getKind())) {
                    issues.add("process [" + process.getName() + "] abortOn `then` [" + then
                            + "] must be a serviceTask cleanup (setField/setRelationField) or the literal `end` - an abort handler cannot wait on a user task");
                } else if (stepArg(thenStep, "setField") == null && stepArg(thenStep, "setRelationField") == null) {
                    issues.add("process [" + process.getName() + "] abortOn `then` [" + then
                            + "] must set a field/relation (setField/setRelationField) - it runs unattended on the abort path");
                } else if (isRoutedToFromMainFlow(process, then)) {
                    issues.add("process [" + process.getName() + "] abortOn `then` step [" + then
                            + "] is abort-only and must not be reachable from the main flow (remove it from the step chain / any next/then/else)");
                }
            }
        }
    }

    /** Whether the entity declares a {@code function: EntityStatus} to-one relation. */
    private static boolean hasEntityStatusRelation(EntityIntent entity) {
        if (entity == null || entity.getRelations() == null) {
            return false;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.isEntityStatus()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a step is EXPLICITLY routed to from the process's main flow - the target of some
     * {@code next} / {@code then} / {@code else}. An {@code abortOn.then} cleanup must be abort-only,
     * so it must fail this (the BPMN generator pulls it out of the linear chain, so mere positional
     * adjacency is harmless - only an explicit reference would leave a dangling edge to a removed
     * node).
     */
    private static boolean isRoutedToFromMainFlow(ProcessIntent process, String stepName) {
        for (StepIntent step : process.getSteps()) {
            if (stepName.equals(stepArg(step, "next")) || stepName.equals(stepArg(step, "then"))
                    || stepName.equals(stepArg(step, "else"))) {
                return true;
            }
        }
        return false;
    }

    /** Whether the value parses as an ISO-8601 duration ({@code PT4H}) or period ({@code P3D}). */
    private static boolean isIso8601Duration(String value) {
        try {
            java.time.Duration.parse(value);
            return true;
        } catch (java.time.format.DateTimeParseException ignoredDuration) {
            try {
                java.time.Period.parse(value);
                return true;
            } catch (java.time.format.DateTimeParseException ignoredPeriod) {
                return false;
            }
        }
    }

    /**
     * The to-one ({@code manyToOne}/{@code oneToOne}) relation of the entity with the given name, or
     * null.
     */
    private static RelationIntent toOneRelationByName(EntityIntent entity, String name) {
        if (entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (name.equals(relation.getName()) && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Decision steps must declare {@code if} and {@code then}; {@code then} and the optional
     * {@code else} must reference a declared step of the same process (or the literal {@code end}).
     * Without this check a typo silently produces BPMN that Flowable rejects on the next
     * synchronization cycle.
     */
    private static void validateDecisionTargets(ProcessIntent process, List<String> issues) {
        Set<String> stepNames = new HashSet<>();
        for (StepIntent step : process.getSteps()) {
            if (step.getName() != null) {
                stepNames.add(step.getName());
            }
        }
        for (StepIntent step : process.getSteps()) {
            if (!"decision".equals(step.getKind()) || step.getName() == null) {
                continue;
            }
            String condition = stepArg(step, "if");
            String thenTarget = stepArg(step, "then");
            if (condition == null || condition.isBlank() || thenTarget == null || thenTarget.isBlank()) {
                issues.add("process [" + process.getName() + "] decision [" + step.getName() + "] must declare both `if` and `then`");
                continue;
            }
            checkDecisionTarget(process, step, "then", thenTarget, stepNames, issues);
            String elseTarget = stepArg(step, "else");
            if (elseTarget != null && !elseTarget.isBlank()) {
                checkDecisionTarget(process, step, "else", elseTarget, stepNames, issues);
            }
        }
    }

    private static void checkDecisionTarget(ProcessIntent process, StepIntent step, String arg, String target, Set<String> stepNames,
            List<String> issues) {
        if (!isRoutingLiteral(target) && !stepNames.contains(target)) {
            issues.add("process [" + process.getName() + "] decision [" + step.getName() + "] `" + arg + "` references unknown step ["
                    + target + "]");
        }
    }

    private static String stepArg(StepIntent step, String key) {
        Object value = step.getArgs() == null ? null
                : step.getArgs()
                      .get(key);
        return value == null ? null : value.toString();
    }

    private static void validateForms(IntentModel model, Set<String> entityNames, List<String> issues) {
        Map<String, EntityIntent> byName = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> formNames = new HashSet<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() == null || form.getName()
                                              .isBlank()) {
                issues.add("form has no name");
                continue;
            }
            if (!formNames.add(form.getName())) {
                issues.add("duplicate form [" + form.getName() + "]");
            }
            EntityIntent bound = null;
            if (form.getForEntity() != null && !form.getForEntity()
                                                    .isBlank()) {
                if (!entityNames.contains(form.getForEntity())) {
                    issues.add("form [" + form.getName() + "] references unknown entity [" + form.getForEntity() + "]");
                } else {
                    bound = byName.get(form.getForEntity());
                }
            }
            validateFormRelationFields(form, bound, byName, issues);
            validateFormEditable(form, bound, issues);
        }
    }

    /**
     * Validate the {@code actions} block: each on-demand action needs a unique name, a known
     * {@code forEntity}, a {@code scope} of {@code entity} or {@code page}, and a same-origin
     * {@code page} to open. The generator contributes each into the app's
     * {@code <project>-custom-action} extension point so it renders on the entity's view (see the
     * ActionIntentGenerator).
     */
    private static void validateActions(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> actionNames = new HashSet<>();
        for (ActionIntent action : model.getActions()) {
            if (action.getName() == null || action.getName()
                                                  .isBlank()) {
                issues.add("action has no name");
                continue;
            }
            String name = action.getName();
            if (!actionNames.add(name)) {
                issues.add("duplicate action [" + name + "]");
            }
            if (action.getForEntity() == null || action.getForEntity()
                                                       .isBlank()) {
                issues.add("action [" + name + "] has no forEntity");
            } else if (!entityNames.contains(action.getForEntity())) {
                issues.add("action [" + name + "] references unknown entity [" + action.getForEntity() + "]");
            }
            String scope = action.getScope();
            if (!"entity".equals(scope) && !"page".equals(scope)) {
                issues.add("action [" + name + "] has invalid scope [" + scope + "] (expected 'entity' or 'page')");
            }
            if (action.getPage() == null || action.getPage()
                                                  .isBlank()) {
                issues.add("action [" + name + "] has no page (a same-origin path to open)");
            }
        }
    }

    /**
     * Validate the {@code generates} block: each create-from action needs a unique name, a known
     * {@code from} entity in this model, a {@code to} target (in this model, or in a declared
     * {@code uses} model), a {@code forEntity} that renders the button, and a {@code scope} of
     * {@code entity} or {@code page}. Every {@code map} value must be a field or to-one relation of the
     * source entity (one-hop {@code relation.field} paths are not yet supported); {@code items} follow
     * the same rules against the source child entity. Target property names are resolved (and, when the
     * target model is available, validated) at generation time by the {@code GlueIntentGenerator}.
     */
    /**
     * A {@code postings} entry: the trigger names a source entity ({@code model:} alias for a
     * cross-model source, which must be in {@code uses:}) - {@code onTransition} with a mandatory
     * {@code when} status guard ({@code <Property> == <seed id>}), or {@code onCreate} for a source
     * with no status lifecycle (the guard is optional there); {@code creates} is a LOCAL document
     * entity owning a composition items child; {@code backReference} its to-one relation to the source
     * (the at-most-once guard); {@code rule.entity} a local entity with a single {@code match}
     * selector; item rows assign fields/relations of the items entity from {@code rule(<column>)}
     * references or source expressions, with an optional {@code when} row guard.
     */
    private static void validatePostings(IntentModel model, Set<String> usesAliases, List<String> issues) {
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        for (PostingIntent posting : model.getPostings()) {
            if (posting.getName() == null || posting.getName()
                                                    .isBlank()) {
                issues.add("posting has no name");
                continue;
            }
            String subject = "posting [" + posting.getName() + "]";
            // event: exactly one trigger - `onTransition` (a status write; requires the `when`
            // status guard) or `onCreate` (the source's insert - the trigger for a source with no
            // status lifecycle at all, e.g. a booked payment whose only event is being created;
            // `when` stays optional there as a plain `<Property> == <number>` guard).
            Object onTransition = posting.getEvent() == null ? null
                    : posting.getEvent()
                             .get("onTransition");
            Object onCreate = posting.getEvent() == null ? null
                    : posting.getEvent()
                             .get("onCreate");
            if (onTransition == null && onCreate == null) {
                issues.add(subject + " requires `event: { onTransition: <SourceEntity>, ... }`"
                        + " or `event: { onCreate: <SourceEntity>, ... }`");
            } else if (onTransition != null && onCreate != null) {
                issues.add(subject + " event declares both onTransition and onCreate - exactly one trigger is allowed");
            } else {
                String source = String.valueOf(onTransition != null ? onTransition : onCreate);
                Object alias = posting.getEvent()
                                      .get("model");
                if (alias != null && !usesAliases.contains(String.valueOf(alias))) {
                    issues.add(subject + " event model [" + alias + "] is not declared in uses:");
                }
                if (alias == null && !byName.containsKey(source)) {
                    issues.add(subject + " event source [" + source
                            + "] is not a declared entity (declare `model:` for a cross-model source)");
                }
                Object when = posting.getEvent()
                                     .get("when");
                if (onTransition != null) {
                    if (when == null || !String.valueOf(when)
                                               .matches("\\s*\\w+\\s*==\\s*\\d+\\s*")) {
                        issues.add(subject + " event requires `when: \"<Property> == <status seed id>\"`");
                    }
                } else if (when != null && !String.valueOf(when)
                                                  .matches("\\s*\\w+\\s*==\\s*\\d+\\s*")) {
                    issues.add(subject + " event when [" + when + "] must be `<Property> == <numeric value>`");
                }
            }
            // Reversal mode: creates/backReference/rule/map/items are inherited from the reversed
            // sibling; the reversal declares only its own event + the storno self-link.
            if (posting.getReverses() != null && !posting.getReverses()
                                                         .isBlank()) {
                PostingIntent sibling = null;
                for (PostingIntent candidate : model.getPostings()) {
                    if (candidate != posting && posting.getReverses()
                                                       .equals(candidate.getName())) {
                        sibling = candidate;
                    }
                }
                if (sibling == null) {
                    issues.add(subject + " reverses unknown posting [" + posting.getReverses() + "] - it must name a sibling"
                            + " posting in this block");
                    continue;
                }
                if (posting.getCreates() != null || posting.getBackReference() != null || posting.getRule() != null
                        || posting.getMap() != null || (posting.getItems() != null && !posting.getItems()
                                                                                              .isEmpty())) {
                    issues.add(subject + " is a reversal - creates/backReference/rule/map/items are inherited from ["
                            + posting.getReverses() + "] and must not be declared");
                }
                EntityIntent reversed = sibling.getCreates() == null ? null : byName.get(sibling.getCreates());
                if (posting.getStorno() == null || posting.getStorno()
                                                          .isBlank()) {
                    issues.add(subject + " requires `storno: <self relation>` - the created entity's link to the reversed document");
                } else if (reversed != null) {
                    RelationIntent storno = toOneRelationByName(reversed, posting.getStorno());
                    if (storno == null || !reversed.getName()
                                                   .equals(storno.getTo())
                            || storno.isCrossModel()) {
                        issues.add(subject + " storno [" + posting.getStorno() + "] must be a to-one SELF-relation of ["
                                + reversed.getName() + "]");
                    }
                }
                continue;
            }
            if (posting.getStorno() != null && !posting.getStorno()
                                                       .isBlank()) {
                issues.add(subject + " declares storno without reverses - the storno link belongs to the reversal posting");
            }
            // creates + items child + backReference
            EntityIntent creates = posting.getCreates() == null ? null : byName.get(posting.getCreates());
            if (creates == null) {
                issues.add(subject + " `creates` must name a local entity");
                continue;
            }
            EntityIntent itemsEntity = compositionChildOf(creates, byName);
            if (itemsEntity == null) {
                issues.add(subject + " `creates` entity [" + creates.getName() + "] must own a composition items child");
                continue;
            }
            if (posting.getBackReference() == null || toOneRelationByName(creates, posting.getBackReference()) == null) {
                issues.add(subject + " `backReference` must name a to-one relation of [" + creates.getName()
                        + "] pointing at the source document");
            }
            if (posting.getMap() != null) {
                for (String key : posting.getMap()
                                         .keySet()) {
                    if (!hasPropertyIgnoreCase(creates, key)) {
                        issues.add(subject + " map [" + key + "] is not a field or to-one relation of [" + creates.getName() + "]");
                    }
                }
            }
            // rule
            EntityIntent ruleEntity = null;
            if (posting.getRule() != null) {
                ruleEntity = byName.get(String.valueOf(posting.getRule()
                                                              .get("entity")));
                Object match = posting.getRule()
                                      .get("match");
                if (ruleEntity == null) {
                    issues.add(subject + " rule.entity must name a local entity");
                } else if (!(match instanceof java.util.Map) || ((java.util.Map<?, ?>) match).size() != 1) {
                    issues.add(subject + " rule.match must be a single `column: literal` selector");
                }
            }
            // items
            if (posting.getItems() == null || posting.getItems()
                                                     .isEmpty()) {
                issues.add(subject + " requires at least one items row");
                continue;
            }
            // The event source entity - resolvable here only for a LOCAL source; a cross-model source
            // (event.model alias) is resolved at generation time via CrossModelSupport, so its relations
            // cannot be deep-checked at parse time. The FK-copy item cell (issue #6533) is therefore
            // shape-validated always, and target-entity-matched only when the source is local.
            Object eventAlias = posting.getEvent() == null ? null
                    : posting.getEvent()
                             .get("model");
            EntityIntent postingSource =
                    eventAlias == null ? byName.get(String.valueOf(onTransition != null ? onTransition : onCreate)) : null;
            for (java.util.Map<String, String> row : posting.getItems()) {
                for (java.util.Map.Entry<String, String> cell : row.entrySet()) {
                    String key = cell.getKey();
                    String value = cell.getValue() == null ? "" : cell.getValue();
                    if ("when".equals(key)) {
                        if (!value.matches("\\s*\\w+\\s*[!=]=\\s*\\d+(\\.\\d+)?\\s*")) {
                            issues.add(subject + " item when [" + value + "] must be `<SourceField> ==|!= <number>`");
                        }
                        continue;
                    }
                    if (!hasPropertyIgnoreCase(itemsEntity, key)) {
                        issues.add(subject + " item [" + key + "] is not a field or to-one relation of [" + itemsEntity.getName() + "]");
                    }
                    // Conditional rule column (#6534): the rule-row column is chosen by a source
                    // classifier - `rule(by: <field>, cases: { <id>: <column>, ... }, default: <column>? )`.
                    // The by/cases selector already branches the account, so it replaces the when:-gated
                    // row pair; a when: on the same row is redundant and rejected.
                    java.util.Optional<PostingRuleSelector> selector = PostingRuleSelector.parse(value);
                    if (selector.isPresent()) {
                        PostingRuleSelector sel = selector.get();
                        if (ruleEntity == null) {
                            issues.add(subject + " item [" + key + "] references rule(by: ...) but the posting declares no rule");
                        }
                        if (row.containsKey("when")) {
                            issues.add(subject + " item [" + key + "] combines a conditional rule(by: ...) with a when: guard"
                                    + " - the by/cases selector already branches the account; drop the when:");
                        }
                        if (sel.cases()
                               .isEmpty()) {
                            issues.add(subject + " item [" + key + "] rule(by: ...) declares no cases");
                        }
                        // `by` reads the source at runtime (Calc, as a number); deep-check it only for a
                        // LOCAL source - a cross-model source is resolved at generation time.
                        if (postingSource != null && !hasPropertyIgnoreCase(postingSource, sel.by())) {
                            issues.add(subject + " rule(by: " + sel.by() + ") is not a field or to-one relation of the source ["
                                    + postingSource.getName() + "]");
                        }
                        for (java.util.Map.Entry<String, String> caseEntry : sel.cases()
                                                                                .entrySet()) {
                            if (!caseEntry.getKey()
                                          .matches("-?\\d+(\\.\\d+)?")) {
                                issues.add(subject + " rule(by: ...) case key [" + caseEntry.getKey()
                                        + "] must be a number (the classifier's seed id)");
                            }
                            if (ruleEntity != null && !isRuleColumn(ruleEntity, caseEntry.getValue())) {
                                issues.add(subject + " rule(by: ...) case column [" + caseEntry.getValue()
                                        + "] is not a field or to-one relation of [" + ruleEntity.getName() + "]");
                            }
                        }
                        if (sel.defaultColumn() != null && ruleEntity != null && !isRuleColumn(ruleEntity, sel.defaultColumn())) {
                            issues.add(subject + " rule(by: ...) default column [" + sel.defaultColumn()
                                    + "] is not a field or to-one relation of [" + ruleEntity.getName() + "]");
                        }
                        continue;
                    }
                    java.util.regex.Matcher ruleRef = java.util.regex.Pattern.compile("\\s*rule\\((\\w+)\\)\\s*")
                                                                             .matcher(value);
                    if (ruleRef.matches()) {
                        if (ruleEntity == null) {
                            issues.add(subject + " item [" + key + "] references rule(...) but the posting declares no rule");
                        } else if (!isRuleColumn(ruleEntity, ruleRef.group(1))) {
                            issues.add(subject + " rule(" + ruleRef.group(1) + ") is not a field or to-one relation of ["
                                    + ruleEntity.getName() + "]");
                        }
                    } else if (toOneRelationByName(itemsEntity, key) != null) {
                        // Source-FK copy (issue #6533): a to-one relation item cell copies a source
                        // to-one FK onto the line (the counterparty dimension). Its value must be a bare
                        // source relation name, not a Calc expression - you cannot arithmetic-evaluate a
                        // FK. When the source is local, the copied relation must exist on it and be
                        // to-one to the SAME entity as the item relation.
                        String rhs = value.trim();
                        if (!rhs.matches("\\w+")) {
                            issues.add(subject + " item [" + key + "] is a to-one relation - its value must copy a source"
                                    + " to-one relation (a bare source relation name), not an expression [" + value + "]");
                        } else if (postingSource != null) {
                            RelationIntent itemRelation = toOneRelationByName(itemsEntity, key);
                            RelationIntent sourceRelation = toOneRelationByName(postingSource, rhs);
                            if (sourceRelation == null) {
                                issues.add(subject + " item [" + key + "] copies [" + rhs + "] which is not a to-one relation of the"
                                        + " source entity [" + postingSource.getName() + "]");
                            } else if (!java.util.Objects.equals(itemRelation.getTo(), sourceRelation.getTo())
                                    || !java.util.Objects.equals(itemRelation.getModel(), sourceRelation.getModel())) {
                                issues.add(subject + " item [" + key + "] and its copied source [" + rhs + "] must be to-one to the same"
                                        + " entity (item -> [" + itemRelation.getTo() + "], source -> [" + sourceRelation.getTo() + "])");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void validateGenerates(IntentModel model, Set<String> entityNames, Set<String> usesAliases, List<String> issues) {
        Map<String, EntityIntent> byName = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (GeneratesIntent g : model.getGenerates()) {
            if (g.getName() == null || g.getName()
                                        .isBlank()) {
                issues.add("generates action has no name");
                continue;
            }
            String name = g.getName();
            if (!names.add(name)) {
                issues.add("duplicate generates action [" + name + "]");
            }
            // A cross-model SOURCE (fromUses:) is resolved from the OWNER's .model at generation time,
            // exactly as a cross-model target is - nothing about it is checkable against this model's
            // entities, so every local check below is skipped for it (the glue generator fails loudly
            // if the owner model cannot be resolved).
            boolean crossModelSource = g.isCrossModelSource();
            if (crossModelSource && !usesAliases.contains(g.getFromUses())) {
                issues.add("generates [" + name + "] fromUses unknown model alias [" + g.getFromUses()
                        + "] (declare it under the model's uses:)");
            }
            if (g.getSourceStatus() != null && !crossModelSource) {
                // The completion hook flips the SOURCE's status after the target is created - it
                // needs the EntityStatus relation to write to.
                EntityIntent from = g.getFrom() == null ? null : byName.get(g.getFrom());
                boolean hasStatus = false;
                if (from != null) {
                    for (RelationIntent relation : from.getRelations()) {
                        if (relation.isEntityStatus()) {
                            hasStatus = true;
                        }
                    }
                }
                if (from != null && !hasStatus) {
                    issues.add("generates [" + name + "] sourceStatus requires the from entity [" + g.getFrom()
                            + "] to declare a function: EntityStatus relation");
                }
            }
            EntityIntent source = null;
            if (g.getFrom() == null || g.getFrom()
                                        .isBlank()) {
                issues.add("generates [" + name + "] has no from entity");
            } else if (crossModelSource) {
                source = null; // owned elsewhere - resolved against the owner's .model, not this one
            } else if (!entityNames.contains(g.getFrom())) {
                issues.add("generates [" + name + "] from references unknown entity [" + g.getFrom()
                        + "] (add a fromUses: alias if the source lives in another model)");
            } else {
                source = byName.get(g.getFrom());
            }
            if (g.getTo() == null || g.getTo()
                                      .isBlank()) {
                issues.add("generates [" + name + "] has no to entity");
            }
            boolean crossModel = g.getUses() != null && !g.getUses()
                                                          .isBlank();
            if (crossModel) {
                if (!usesAliases.contains(g.getUses())) {
                    issues.add(
                            "generates [" + name + "] uses unknown model alias [" + g.getUses() + "] (declare it under the model's uses:)");
                }
            } else if (g.getTo() != null && !g.getTo()
                                              .isBlank()
                    && !entityNames.contains(g.getTo())) {
                issues.add("generates [" + name + "] to references unknown entity [" + g.getTo()
                        + "] (add a uses: alias if the target lives in another model)");
            }
            String forEntity = g.getForEntity();
            if (forEntity == null || forEntity.isBlank()) {
                issues.add("generates [" + name + "] has no forEntity");
            } else if (crossModelSource) {
                // The button is contributed onto the SOURCE's view, which the owner model generates and
                // which lives in the owner's project. Hosting it on some other view would need a record
                // of that view to carry the source id - there is none.
                if (!forEntity.equals(g.getFrom())) {
                    issues.add("generates [" + name + "] has a cross-model source (fromUses [" + g.getFromUses()
                            + "]), so forEntity must be the source entity [" + g.getFrom() + "] - the button is contributed onto "
                            + "the owner model's view; it cannot be hosted on a local view [" + forEntity + "]");
                }
            } else if (!entityNames.contains(forEntity)) {
                issues.add("generates [" + name + "] forEntity references unknown entity [" + forEntity + "]");
            }
            String scope = g.getScope();
            if (!"entity".equals(scope) && !"page".equals(scope)) {
                issues.add("generates [" + name + "] has invalid scope [" + scope + "] (expected 'entity' or 'page')");
            }
            validateMapSource(source, g.getMap(), "generates [" + name + "]", "map", issues);
            if (g.getItems() != null) {
                GeneratesItemsIntent items = g.getItems();
                EntityIntent itemSource = null;
                if (items.getFrom() == null || items.getFrom()
                                                    .isBlank()) {
                    issues.add("generates [" + name + "] items has no from entity");
                } else if (crossModelSource) {
                    // The source's items belong to the source - i.e. to the owner model, resolved there.
                    itemSource = null;
                } else if (!entityNames.contains(items.getFrom())) {
                    issues.add("generates [" + name + "] items from references unknown entity [" + items.getFrom() + "]");
                } else {
                    itemSource = byName.get(items.getFrom());
                }
                if (items.getTo() == null || items.getTo()
                                                  .isBlank()) {
                    issues.add("generates [" + name + "] items has no to entity");
                }
                validateMapSource(itemSource, items.getMap(), "generates [" + name + "]", "items map", issues);
            }
            validateGeneratesItemLines(g, name, source, byName, crossModel, issues);
        }
    }

    /**
     * Validate the computed line-items form ({@code itemLines}, issue #6555): a fixed set of synthetic
     * target lines whose cells are expressions over the SOURCE record. The two item forms are mutually
     * exclusive. For a SAME-model target the cell keys must be fields / to-one relations of the target
     * document's composition line-items child (resolved automatically, never named), a to-one relation
     * cell copies a bare source relation, a {@code {field}} placeholder / bare-identifier string cell
     * references a real source property, and a {@code when} guard has the {@code <field> ==|!= <n>}
     * shape. A CROSS-model target's items child lives in the owner model (not loaded here), so only the
     * always-checkable shapes are validated - the cell keys are checked at generation, the same
     * deferral the mirror form's cross-model {@code map} uses.
     */
    private static void validateGeneratesItemLines(GeneratesIntent g, String name, EntityIntent source, Map<String, EntityIntent> byName,
            boolean crossModel, List<String> issues) {
        List<Map<String, String>> itemLines = g.getItemLines();
        if (itemLines == null || itemLines.isEmpty()) {
            return;
        }
        String subject = "generates [" + name + "]";
        if (g.getItems() != null) {
            issues.add(subject + " declares both an items mapping (object) and computed item lines (list) - use exactly one");
        }
        EntityIntent itemsChild = null;
        if (!crossModel && g.getTo() != null && byName.get(g.getTo()) != null) {
            itemsChild = compositionChildOf(byName.get(g.getTo()), byName);
            if (itemsChild == null) {
                issues.add(
                        subject + " declares computed item lines but the target [" + g.getTo() + "] has no composition line-items child");
            }
        }
        Set<String> sourceProperties = new HashSet<>();
        if (source != null) {
            if (source.getFields() != null) {
                for (FieldIntent field : source.getFields()) {
                    if (field.getName() != null) {
                        sourceProperties.add(field.getName()
                                                  .toLowerCase(Locale.ROOT));
                    }
                }
            }
            if (source.getRelations() != null) {
                for (RelationIntent relation : source.getRelations()) {
                    if (relation.getName() != null) {
                        sourceProperties.add(relation.getName()
                                                     .toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        for (Map<String, String> row : itemLines) {
            boolean hasCell = false;
            for (Map.Entry<String, String> cell : row.entrySet()) {
                String key = cell.getKey();
                String value = cell.getValue() == null ? ""
                        : cell.getValue()
                              .trim();
                if ("when".equalsIgnoreCase(key)) {
                    if (!value.matches("\\s*\\w+\\s*[!=]=\\s*\\d+(\\.\\d+)?\\s*")) {
                        issues.add(subject + " item line when [" + value + "] must be `<SourceField> ==|!= <number>`");
                    }
                    continue;
                }
                hasCell = true;
                if (itemsChild != null && !hasPropertyIgnoreCase(itemsChild, key)) {
                    issues.add(subject + " item line cell [" + key + "] is not a field or to-one relation of the target items child ["
                            + itemsChild.getName() + "]");
                    continue;
                }
                if (itemsChild != null && toOneRelationByName(itemsChild, key) != null) {
                    // A to-one relation cell copies a bare source foreign key (issue #6533 parity) - it
                    // cannot be arithmetic-evaluated. Its value must name a to-one relation of the source.
                    if (!value.matches("\\w+")) {
                        issues.add(subject + " item line cell [" + key + "] is a to-one relation - its value must copy a source to-one"
                                + " relation (a bare source relation name), not an expression [" + value + "]");
                    } else if (source != null && toOneRelationByName(source, value) == null) {
                        issues.add(subject + " item line cell [" + key + "] copies [" + value + "] which is not a to-one relation of the"
                                + " source entity [" + source.getName() + "]");
                    }
                } else if (source != null) {
                    // A string {field} placeholder / bare-identifier copy must reference a real source
                    // property; a numeric Calc expression's identifiers are validated at runtime (a null
                    // field reads as 0, the calculated-field contract), so only the string refs are checked.
                    java.util.regex.Matcher placeholders = java.util.regex.Pattern.compile("\\{(\\w+)\\}")
                                                                                  .matcher(value);
                    while (placeholders.find()) {
                        if (!sourceProperties.contains(placeholders.group(1)
                                                                   .toLowerCase(Locale.ROOT))) {
                            issues.add(subject + " item line cell [" + key + "] interpolates {" + placeholders.group(1)
                                    + "} which is not a property of the source entity ["
                                    + (source.getName() == null ? g.getFrom() : source.getName()) + "]");
                        }
                    }
                }
            }
            if (!hasCell) {
                issues.add(subject + " has a computed item line with no cells");
            }
        }
    }

    /** The compiled shape of a transition {@code when} guard: {@code <Field> ==|!= <number>}. */
    private static final java.util.regex.Pattern TRANSITION_WHEN =
            java.util.regex.Pattern.compile("\\s*(\\w+)\\s*(==|!=)\\s*(-?\\d+(?:\\.\\d+)?)\\s*");

    /**
     * A {@code transitions} declaration is a guarded on-demand status flip: it requires the entity to
     * declare a {@code function: EntityStatus} relation (the column it writes), a non-empty
     * {@code from} list of allowed source seed ids, and a positive {@code setStatus} target outside
     * that list. The optional {@code when} guard is a single {@code <Field> ==|!= <number>} comparison
     * over an own field of the entity (the postings row-guard grammar - evaluated with the Calc
     * semantics, where a null field reads as 0).
     */
    private static void validateTransitions(IntentModel model, Set<String> entityNames, List<String> issues) {
        Map<String, EntityIntent> byName = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> names = new HashSet<>();
        for (TransitionIntent t : model.getTransitions()) {
            if (t.getName() == null || t.getName()
                                        .isBlank()) {
                issues.add("transition has no name");
                continue;
            }
            String subject = "transition [" + t.getName() + "]";
            if (!names.add(t.getName())) {
                issues.add("duplicate " + subject);
            }
            EntityIntent entity = null;
            if (t.getForEntity() == null || t.getForEntity()
                                             .isBlank()) {
                issues.add(subject + " has no forEntity");
            } else if (!entityNames.contains(t.getForEntity())) {
                issues.add(subject + " forEntity references unknown entity [" + t.getForEntity() + "]");
            } else {
                entity = byName.get(t.getForEntity());
            }
            if (entity != null) {
                boolean hasStatus = false;
                if (entity.getRelations() != null) {
                    for (RelationIntent relation : entity.getRelations()) {
                        if (relation.isEntityStatus()) {
                            hasStatus = true;
                        }
                    }
                }
                if (!hasStatus) {
                    issues.add(subject + " requires the entity [" + entity.getName()
                            + "] to declare a function: EntityStatus relation - the transition writes the status");
                }
            }
            if (t.getFrom() == null || t.getFrom()
                                        .isEmpty()) {
                issues.add(subject + " has no from statuses - list the seed ids the transition is allowed from");
            } else {
                for (Integer from : t.getFrom()) {
                    if (from == null || from <= 0) {
                        issues.add(subject + " from seed ids must be positive");
                        break;
                    }
                }
            }
            if (t.getSetStatus() == null || t.getSetStatus() <= 0) {
                issues.add(subject + " has no setStatus - the target status seed id");
            } else if (t.getFrom() != null && t.getFrom()
                                               .contains(t.getSetStatus())) {
                issues.add(subject + " setStatus [" + t.getSetStatus() + "] is also in from - a transition must change the status");
            }
            if (t.getWhen() != null && !t.getWhen()
                                         .isBlank()) {
                java.util.regex.Matcher matcher = TRANSITION_WHEN.matcher(t.getWhen());
                if (!matcher.matches()) {
                    issues.add(subject + " when [" + t.getWhen() + "] must be `<Field> == <number>` or `<Field> != <number>`");
                } else if (entity != null && !hasPropertyIgnoreCase(entity, matcher.group(1))) {
                    // The identifier follows the Calc convention (PascalCase entity property), while
                    // the field is authored camelCase - resolve case-insensitively.
                    issues.add(subject + " when references [" + matcher.group(1) + "] which is not a field or to-one relation of ["
                            + entity.getName() + "]");
                }
            }
            // Optional outbound mail after the flip ("on Void, mail the counterparty"), about the
            // transitioned record itself.
            validateNotifyBlock(t.getNotify(), subject + " notify", entity == null ? null : entity.getName(), model, issues);
        }
    }

    /**
     * Each {@code map} value must name a field or a to-one relation of the source entity; a one-hop
     * {@code relation.field} path is rejected (not yet supported). Skipped when the source is unknown -
     * that error is already reported.
     */
    private static void validateMapSource(EntityIntent source, Map<String, String> map, String subject, String role, List<String> issues) {
        if (source == null || map == null) {
            return;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String sourceProp = entry.getValue();
            if (sourceProp == null || sourceProp.isBlank()) {
                issues.add(subject + " " + role + " [" + entry.getKey() + "] has no source property");
                continue;
            }
            if (sourceProp.indexOf('.') >= 0) {
                issues.add(subject + " " + role + " [" + entry.getKey() + "] maps a relation.field path [" + sourceProp
                        + "] which is not yet supported - map a direct field or to-one relation of [" + source.getName() + "]");
                continue;
            }
            if (fieldByName(source, sourceProp) == null && toOneRelationByName(source, sourceProp) == null) {
                issues.add(subject + " " + role + " source [" + sourceProp + "] is not a field or to-one relation of [" + source.getName()
                        + "]");
            }
        }
    }

    /**
     * An {@code editable} field (the per-field opt-out of a BPM task form's read-only default) must be
     * a plain, displayed field of the bound entity. Any field type is allowed: the generated Writer
     * coerces the form's process variable to the field's Java type ({@code date}/{@code timestamp}/
     * {@code number}/{@code boolean}/{@code string}). A {@code relation.field} can never be editable
     * (editing it would not write back).
     */
    private static void validateFormEditable(FormIntent form, EntityIntent bound, List<String> issues) {
        Set<String> displayed = new HashSet<>(form.getFields());
        for (String field : form.getEditable()) {
            if (field == null || field.isBlank()) {
                continue;
            }
            if (field.indexOf('.') >= 0) {
                issues.add("form [" + form.getName() + "] editable [" + field + "] is a relation.field, which cannot be edited");
                continue;
            }
            if (!displayed.contains(field)) {
                issues.add("form [" + form.getName() + "] editable [" + field + "] is not in the form's fields - only a displayed field"
                        + " can be made editable");
                continue;
            }
            if (bound == null) {
                continue; // the unknown-forEntity issue is already reported above
            }
            FieldIntent bf = fieldByName(bound, field);
            if (bf == null) {
                issues.add("form [" + form.getName() + "] editable [" + field + "] is not a field of [" + form.getForEntity() + "]");
                continue;
            }
            // Any plain entity field type is editable: the generated Writer coerces the form's process
            // variable to the field's Java type (LocalDate / Instant / Integer / Long / BigDecimal /
            // Double / Boolean / String). Only a relation.field (handled above) can never be written back.
        }
    }

    /**
     * A {@code relation.field} form field must be a one-hop to-one relation of the form's bound entity
     * with the field present on the target - so it can be resolved into a process variable at runtime
     * (the same one-hop scope as decision conditions). Multi-hop paths are not supported.
     */
    private static void validateFormRelationFields(FormIntent form, EntityIntent bound, Map<String, EntityIntent> byName,
            List<String> issues) {
        for (String field : form.getFields()) {
            if (field == null || field.indexOf('.') < 0) {
                continue;
            }
            if (bound == null) {
                issues.add("form [" + form.getName() + "] field [" + field
                        + "] uses a relation.field path but the form has no (valid) forEntity to resolve it against");
                continue;
            }
            int dot = field.indexOf('.');
            String relationName = field.substring(0, dot);
            String fieldName = field.substring(dot + 1);
            if (fieldName.indexOf('.') >= 0) {
                issues.add("form [" + form.getName() + "] field [" + field
                        + "] uses a multi-hop path, which is not supported - use a direct field or a one-hop relation.field");
                continue;
            }
            RelationIntent relation = toOneRelation(bound, relationName);
            if (relation == null) {
                issues.add("form [" + form.getName() + "] field [" + field + "] is not a to-one relation.field of [" + form.getForEntity()
                        + "]");
                continue;
            }
            EntityIntent target = byName.get(relation.getTo());
            if (target == null || fieldByName(target, fieldName) == null) {
                issues.add("form [" + form.getName() + "] field [" + field + "] references unknown field [" + fieldName + "] on ["
                        + relation.getTo() + "]");
            }
        }
    }

    private static RelationIntent toOneRelation(EntityIntent owner, String name) {
        for (RelationIntent relation : owner.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static void validateReports(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> reportNames = new HashSet<>();
        for (ReportIntent report : model.getReports()) {
            if (report.getName() == null || report.getName()
                                                  .isBlank()) {
                issues.add("report has no name");
                continue;
            }
            if (!reportNames.add(report.getName())) {
                issues.add("duplicate report [" + report.getName() + "]");
            }
            if (report.getSource() == null || report.getSource()
                                                    .isBlank()) {
                issues.add("report [" + report.getName() + "] has no source");
            } else if (!entityNames.contains(report.getSource())) {
                issues.add("report [" + report.getName() + "] sources from unknown entity [" + report.getSource() + "]");
            }
            if (report.getWidget() != null) {
                validateReportWidget(report, issues);
            }
            if (report.getChart() != null && !report.getChart()
                                                    .isBlank()
                    && !REPORT_CHART_KINDS.contains(report.getChart()
                                                          .trim())) {
                issues.add("report [" + report.getName() + "] has unknown chart [" + report.getChart() + "] - expected one of "
                        + REPORT_CHART_KINDS);
            }
            validateAgeingDimensions(model, report, issues);
            validateBalanceReport(model, report, issues);
            validateReportScope(model, report, issues);
        }
    }

    /**
     * A report's {@code scope} restricts the query to the lifecycle rows that a stage classifies, so
     * "economically live only" stops being a hand-written predicate over positional seed ids. It is
     * therefore only meaningful over a source carrying a {@code function: EntityStatus}, and a stage
     * scope needs that nomenclature's seed rows to be classified <em>in this model</em> - a cross-model
     * status entity is seeded elsewhere and nothing here can resolve its ids, which must fail loudly
     * rather than emit a query missing its predicate.
     */
    private static void validateReportScope(IntentModel model, ReportIntent report, List<String> issues) {
        String scope = report.getNormalizedScope();
        if (scope == null) {
            return;
        }
        String subject = "report [" + report.getName() + "] scope [" + report.getScope()
                                                                             .trim()
                + "]";
        if (!LifecycleStages.SCOPE_ALL.equals(scope) && !LifecycleStages.STAGES.contains(scope)) {
            issues.add(subject + " is unknown - expected `" + LifecycleStages.SCOPE_ALL + "` or one of "
                    + new java.util.TreeSet<>(LifecycleStages.STAGES));
            return;
        }
        EntityIntent source = null;
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null && entity.getName()
                                                  .equals(report.getSource())) {
                source = entity;
            }
        }
        if (source == null) {
            return; // a missing / unknown source is reported separately
        }
        RelationIntent status = LifecycleStages.statusRelation(source);
        if (status == null) {
            issues.add(subject + " requires the source [" + source.getName() + "] to declare a `function: EntityStatus` relation"
                    + " - a scope restricts the query by the lifecycle status");
            return;
        }
        if (LifecycleStages.SCOPE_ALL.equals(scope)) {
            return;
        }
        if (status.isCrossModel()) {
            issues.add(subject + " cannot resolve: the status nomenclature [" + status.getTo() + "] belongs to model [" + status.getModel()
                    + "], so its stages are not declared here - use an explicit `filter` instead");
            return;
        }
        Map<String, List<Integer>> stages = LifecycleStages.stagesOf(model, status.getTo());
        if (stages.isEmpty()) {
            issues.add(subject + " requires the seed rows of [" + status.getTo() + "] to declare `stage:` - without the"
                    + " classification there is nothing to resolve the scope against");
        } else if (!stages.containsKey(scope)) {
            issues.add(subject + " matches no seed row - none of [" + status.getTo() + "] declares `stage: " + scope + "`");
        }
    }

    /** {@code ageing(field, [30, 60, 90])} - the date field in group 1, the thresholds in group 2. */
    private static final java.util.regex.Pattern REPORT_AGEING = java.util.regex.Pattern.compile(
            "\\s*ageing\\s*\\(\\s*([^,\\[]+?)\\s*,\\s*\\[\\s*([^\\]]+?)\\s*\\]\\s*\\)\\s*", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * An {@code ageing(field, [30, 60, 90])} dimension: the thresholds must be ascending positive day
     * counts, and the bucketed field must be a {@code date}/{@code timestamp} - the generated SQL
     * compares it against {@code CURRENT_DATE - INTERVAL 'n' DAY}, so a non-temporal column would fail
     * at query time instead of at authoring time.
     */
    private static void validateAgeingDimensions(IntentModel model, ReportIntent report, List<String> issues) {
        for (String dimension : report.getDimensions()) {
            if (dimension == null || dimension.isBlank()) {
                continue;
            }
            java.util.regex.Matcher matcher = REPORT_AGEING.matcher(dimension.trim());
            if (!matcher.matches()) {
                continue;
            }
            String subject = "report [" + report.getName() + "] ageing dimension [" + dimension.trim() + "]";
            int previous = 0;
            for (String token : matcher.group(2)
                                       .split(",")) {
                String raw = token.trim();
                int value;
                try {
                    value = Integer.parseInt(raw);
                } catch (NumberFormatException ex) {
                    issues.add(subject + " threshold [" + raw + "] is not an integer number of days");
                    continue;
                }
                if (value <= 0) {
                    issues.add(subject + " thresholds must be positive day counts - got [" + value + "]");
                } else if (value <= previous) {
                    issues.add(subject + " thresholds must ascend - got [" + value + "] after [" + previous + "]");
                } else {
                    previous = value;
                }
            }
            validateAgeingField(model, report, subject, matcher.group(1)
                                                               .trim(),
                    issues);
        }
    }

    /**
     * The bucketed field: an own {@code date}/{@code timestamp} of the source, or a one-hop relation's.
     */
    private static void validateAgeingField(IntentModel model, ReportIntent report, String subject, String path, List<String> issues) {
        EntityIntent source = null;
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null && entity.getName()
                                                  .equals(report.getSource())) {
                source = entity;
            }
        }
        if (source == null) {
            return; // an unknown source is reported separately
        }
        String[] segments = path.split("\\.");
        if (segments.length > 2) {
            issues.add(subject + " field [" + path + "] may reference the source or ONE relation hop");
            return;
        }
        EntityIntent owner = source;
        if (segments.length == 2) {
            RelationIntent hop = toOneRelationByName(source, segments[0]);
            if (hop == null) {
                issues.add(subject + " [" + segments[0] + "] is not a to-one relation of [" + source.getName() + "]");
                return;
            }
            if (hop.isCrossModel()) {
                return; // resolved at generation against the owner model
            }
            owner = null;
            for (EntityIntent entity : model.getEntities()) {
                if (entity.getName() != null && entity.getName()
                                                      .equals(hop.getTo())) {
                    owner = entity;
                }
            }
            if (owner == null) {
                return; // the dangling relation target is reported separately
            }
        }
        FieldIntent field = fieldByName(owner, segments[segments.length - 1]);
        if (field == null) {
            issues.add(subject + " field [" + path + "] is not a field of [" + owner.getName() + "]");
            return;
        }
        String type = field.getType() == null ? ""
                : field.getType()
                       .toLowerCase();
        if (!"date".equals(type) && !"timestamp".equals(type)) {
            issues.add(subject + " buckets by age, so [" + path + "] must be a date/timestamp field - got [" + field.getType() + "]");
        }
    }

    /**
     * {@code kind: balance} - the accounting balance report. Requires {@code date} (the window field),
     * {@code debit} and {@code credit} (the summed amounts) and at least one dimension; forbids ad-hoc
     * {@code measures} because the six opening / period / closing totals ARE the measures.
     */
    private static void validateBalanceReport(IntentModel model, ReportIntent report, List<String> issues) {
        boolean balanceInputs = report.getDate() != null || report.getDebit() != null || report.getCredit() != null;
        if (report.getKind() == null || report.getKind()
                                              .isBlank()) {
            if (balanceInputs) {
                issues.add("report [" + report.getName() + "] declares date/debit/credit but is not kind: balance");
            }
            return;
        }
        if (!report.isBalance()) {
            issues.add("report [" + report.getName() + "] has unknown kind [" + report.getKind() + "] - expected balance");
            return;
        }
        String prefix = "balance report [" + report.getName() + "]";
        if (!report.getMeasures()
                   .isEmpty()) {
            issues.add(prefix + " must not declare measures - it computes the opening/period/closing debit and credit totals");
        }
        if (report.getDimensions()
                  .stream()
                  .noneMatch(d -> d != null && !d.isBlank())) {
            issues.add(prefix + " needs at least one dimension to balance by");
        }
        EntityIntent source = null;
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null && entity.getName()
                                                  .equals(report.getSource())) {
                source = entity;
            }
        }
        if (source == null) {
            return; // the missing/unknown source is already reported
        }
        validateBalanceDate(model, source, report, issues, prefix);
        requireNumericBalanceField(source, report.getDebit(), "debit", issues, prefix);
        requireNumericBalanceField(source, report.getCredit(), "credit", issues, prefix);
    }

    /**
     * The balance {@code date} must resolve to a {@code date}-typed field - directly on the source or
     * through a one-hop to-one {@code relation.field} path (a cross-model target is checked at
     * generation, like every cross-model reference). A {@code timestamp} is rejected deliberately: the
     * window parameters are dates, and comparing a timestamp against the {@code toDate} midnight would
     * silently exclude that day's intra-day entries.
     */
    private static void validateBalanceDate(IntentModel model, EntityIntent source, ReportIntent report, List<String> issues,
            String prefix) {
        String reference = report.getDate();
        if (reference == null || reference.isBlank()) {
            issues.add(prefix + " needs date: the date field driving the period window");
            return;
        }
        reference = reference.trim();
        FieldIntent field;
        int dot = reference.indexOf('.');
        if (dot > 0) {
            RelationIntent relation = toOneRelation(source, reference.substring(0, dot));
            if (relation == null) {
                issues.add(prefix + " date [" + reference + "] does not start with a to-one relation of [" + source.getName() + "]");
                return;
            }
            if (relation.isCrossModel()) {
                return;
            }
            EntityIntent target = null;
            for (EntityIntent entity : model.getEntities()) {
                if (entity.getName() != null && entity.getName()
                                                      .equals(relation.getTo())) {
                    target = entity;
                }
            }
            field = target == null ? null : fieldByName(target, reference.substring(dot + 1));
        } else {
            field = fieldByName(source, reference);
        }
        if (field == null) {
            issues.add(prefix + " date [" + reference + "] does not resolve to a field");
        } else if (!"date".equalsIgnoreCase(field.getType() == null ? "" : field.getType())) {
            issues.add(prefix + " date [" + reference + "] must be a date field (found [" + field.getType() + "])");
        }
    }

    /** The balance {@code debit}/{@code credit} must be a numeric field of the source entity itself. */
    private static void requireNumericBalanceField(EntityIntent source, String value, String role, List<String> issues, String prefix) {
        if (value == null || value.isBlank()) {
            issues.add(prefix + " needs " + role + ": the numeric field holding the " + role + " amount");
            return;
        }
        FieldIntent field = fieldByName(source, value.trim());
        if (field == null) {
            issues.add(prefix + " " + role + " [" + value + "] is not a field of [" + source.getName() + "]");
        } else if (!NUMERIC_TYPES.contains(field.getType())) {
            issues.add(prefix + " " + role + " [" + value + "] must be numeric (found [" + field.getType() + "])");
        }
    }

    /** Chart types a report page can render (Chart.js). */
    private static final Set<String> REPORT_CHART_KINDS = Set.of("bar", "line", "pie", "doughnut", "polarArea", "radar");
    private static final Set<String> WIDGET_KINDS = Set.of("count", "value", "list");
    private static final Set<String> CUSTOM_WIDGET_KINDS = Set.of("kpi", "page");

    /**
     * Top-level {@code widgets:} — custom dashboard widgets: {@code kind: kpi} fetches {@code {value,
     * description?}} from the developer's REST endpoint, {@code kind: page} embeds the developer's HTML
     * page. The URL is the developer's own contract (typically code under {@code custom/}), so only its
     * shape is checked: same-origin (an absolute or relative path, no scheme/host) to keep the
     * dashboard's fetch/iframe inside the application.
     */
    private static void validateWidgets(IntentModel model, List<String> issues) {
        Set<String> widgetNames = new HashSet<>();
        for (CustomWidgetIntent widget : model.getWidgets()) {
            if (widget.getName() == null || widget.getName()
                                                  .isBlank()) {
                issues.add("widget has no name");
                continue;
            }
            String prefix = "widget [" + widget.getName() + "]";
            if (!widgetNames.add(widget.getName())) {
                issues.add("duplicate widget [" + widget.getName() + "]");
            }
            String kind = widget.getKind() == null ? "kpi"
                    : widget.getKind()
                            .trim();
            if (!CUSTOM_WIDGET_KINDS.contains(kind)) {
                issues.add(prefix + " has unknown kind [" + widget.getKind() + "] - expected one of " + CUSTOM_WIDGET_KINDS);
            }
            String url = widget.getUrl();
            if (url == null || url.isBlank()) {
                issues.add(prefix + " has no url");
            } else if (url.contains("://") || url.startsWith("//")) {
                issues.add(prefix + " url must be a same-origin path (no scheme/host): [" + url + "]");
            }
        }
    }

    /**
     * A report {@code widget:} block turns the report into a dashboard KPI tile. {@code kind: count}
     * (default) shows the report's record count; {@code kind: value} shows one aggregate cell -
     * {@code value} names a declared measure and {@code at} pins declared dimensions to a token
     * ({@code now}) or a literal; {@code kind: list} shows the report's first {@code limit} rows.
     * Alias/type resolution happens in the report generator (same leniency as report filters).
     */
    private static void validateReportWidget(ReportIntent report, List<String> issues) {
        WidgetIntent widget = report.getWidget();
        String prefix = "report [" + report.getName() + "] widget";
        String kind = widget.getKind() == null ? (widget.getValue() != null ? "value" : "count")
                : widget.getKind()
                        .trim();
        if (!WIDGET_KINDS.contains(kind)) {
            issues.add(prefix + " has unknown kind [" + widget.getKind() + "] - expected one of " + WIDGET_KINDS);
            return;
        }
        if ("value".equals(kind)) {
            if (widget.getValue() == null || widget.getValue()
                                                   .isBlank()) {
                issues.add(prefix + " of kind [value] requires `value` naming a declared measure");
            } else if (report.getMeasures()
                             .stream()
                             .noneMatch(m -> m != null && normalizeExpression(m).equals(normalizeExpression(widget.getValue())))) {
                issues.add(prefix + " value [" + widget.getValue() + "] does not name a declared measure");
            }
        } else if (widget.getValue() != null) {
            issues.add(prefix + " of kind [" + kind + "] must not declare `value` - use kind [value]");
        }
        for (Map.Entry<String, Object> pin : widget.getAt()
                                                   .entrySet()) {
            String dimension = pin.getKey();
            if (report.getDimensions()
                      .stream()
                      .noneMatch(d -> d != null && normalizeExpression(d).equals(normalizeExpression(dimension)))) {
                issues.add(prefix + " pins unknown dimension [" + dimension + "]");
            }
            Object value = pin.getValue();
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
                issues.add(prefix + " pin [" + dimension + "] must be a scalar token or literal");
            }
        }
        if (widget.getLimit() != null) {
            if (!"list".equals(kind)) {
                issues.add(prefix + " of kind [" + kind + "] must not declare `limit` - it applies to kind [list] only");
            } else if (widget.getLimit() < 1) {
                issues.add(prefix + " limit must be a positive number");
            }
        }
    }

    /** Whitespace/case-insensitive compare key for measure and dimension expressions. */
    private static String normalizeExpression(String expression) {
        return expression.replaceAll("\\s+", "")
                         .toLowerCase(Locale.ROOT);
    }

    private static void validateSeeds(IntentModel model, Set<String> entityNames, List<String> issues) {
        java.util.Map<String, EntityIntent> byName = new java.util.HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                byName.put(entity.getName(), entity);
            }
        }
        Set<String> nomenclatures = statusNomenclatures(model);
        Set<String> seedNames = new HashSet<>();
        for (SeedIntent seed : model.getSeeds()) {
            if (seed.getName() == null || seed.getName()
                                              .isBlank()) {
                issues.add("seed has no name");
                continue;
            }
            if (!seedNames.add(seed.getName())) {
                issues.add("duplicate seed [" + seed.getName() + "]");
            }
            if (seed.getEntity() == null || seed.getEntity()
                                                .isBlank()) {
                issues.add("seed [" + seed.getName() + "] has no entity");
            } else if (!entityNames.contains(seed.getEntity())) {
                issues.add("seed [" + seed.getName() + "] targets unknown entity [" + seed.getEntity() + "]");
            }
            if (seed.isFileSeed()) {
                // The seed data lives in an authored CSV: inline rows are mutually exclusive, and the
                // file must sit in a subfolder - root-level .csv files are owned and scrubbed by the
                // intent regeneration, which would delete the authored data.
                if (!seed.getRows()
                         .isEmpty()) {
                    issues.add("seed [" + seed.getName() + "] declares both `file` and inline `rows` - use exactly one");
                }
                String file = seed.getFile()
                                  .trim();
                if (file.startsWith("/") || file.contains("..")) {
                    issues.add("seed [" + seed.getName() + "] file [" + file + "] must be a project-relative path");
                } else if (!file.contains("/")) {
                    issues.add("seed [" + seed.getName() + "] file [" + file + "] must live in a subfolder (e.g. data/" + file
                            + ") - root-level .csv files are owned and scrubbed by the intent regeneration");
                }
            } else if (seed.getRows()
                           .isEmpty()) {
                issues.add("seed [" + seed.getName() + "] has no rows");
            }
            if (seed.isLanguageSeed()) {
                validateLanguageSeed(seed, byName.get(seed.getEntity()), issues);
            } else {
                validateSeedStages(seed, byName.get(seed.getEntity()), nomenclatures, issues);
            }
        }
    }

    /**
     * The names of the entities this model uses as a status nomenclature - the same-model targets of a
     * {@code function: EntityStatus} relation. A cross-model status entity is excluded: its seeds live
     * in the owning model, so nothing here can classify them.
     */
    private static Set<String> statusNomenclatures(IntentModel model) {
        Set<String> nomenclatures = new HashSet<>();
        for (EntityIntent entity : model.getEntities()) {
            RelationIntent status = LifecycleStages.statusRelation(entity);
            if (status != null && status.getTo() != null && !status.isCrossModel()) {
                nomenclatures.add(status.getTo());
            }
        }
        return nomenclatures;
    }

    /**
     * A seed row's optional {@code stage:} marker classifies what that status MEANS to the lifecycle
     * ({@code draft} / {@code live} / {@code cancelled} / {@code void}) so consumers - chiefly a
     * report's {@code scope} - stop expressing "economically live" as a hand-written predicate over
     * positional seed ids. It is metadata, never a column, so it must carry the row's {@code id} (what
     * it classifies) and stay inside the vocabulary. A status nomenclature that declares its OWN
     * {@code stage} property collides with the marker and is rejected: the row key cannot be both data
     * and classification.
     */
    private static void validateSeedStages(SeedIntent seed, EntityIntent entity, Set<String> statusNomenclatures, List<String> issues) {
        String subject = "seed [" + seed.getName() + "]";
        boolean anyStage = false;
        String idField = entity == null ? "id" : seedIdField(entity);
        for (Map<String, Object> row : seed.getRows()) {
            if (!row.containsKey(LifecycleStages.STAGE_KEY)) {
                continue;
            }
            anyStage = true;
            Object raw = row.get(LifecycleStages.STAGE_KEY);
            String stage = raw == null ? ""
                    : String.valueOf(raw)
                            .trim()
                            .toLowerCase(Locale.ROOT);
            if (!LifecycleStages.STAGES.contains(stage)) {
                issues.add(
                        subject + " row declares stage [" + raw + "] - expected one of " + new java.util.TreeSet<>(LifecycleStages.STAGES));
            }
            if (row.get(idField) == null) {
                issues.add(subject + " row declares a stage but no [" + idField + "] - the stage classifies a status seed id");
            }
        }
        if (anyStage && entity != null && LifecycleStages.declaresStageProperty(entity) && statusNomenclatures.contains(entity.getName())) {
            issues.add(subject + " uses the lifecycle stage marker but entity [" + entity.getName()
                    + "] declares its own `stage` property - rename that property, the seed key cannot be both data and"
                    + " lifecycle classification");
        }
    }

    /** The field name a seed row keys the entity's primary key by ({@code id} by convention). */
    private static String seedIdField(EntityIntent entity) {
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() && field.getName() != null) {
                return field.getName();
            }
        }
        return "id";
    }

    /**
     * A translation seed ({@code language: bg}) targets a multilingual entity's language table: the
     * code is a short lowercase language code, and its rows carry only the base row's {@code id} plus
     * translatable (string/text, non-PK) fields of the entity.
     */
    private static void validateLanguageSeed(SeedIntent seed, EntityIntent entity, List<String> issues) {
        if (!seed.getLanguage()
                 .matches("[a-z]{2,3}")) {
            issues.add("seed [" + seed.getName() + "] language [" + seed.getLanguage()
                    + "] must be a short lowercase language code (e.g. bg)");
        }
        if (entity == null) {
            return; // the unknown entity is reported separately
        }
        if (!entity.isMultilingual()) {
            issues.add("seed [" + seed.getName() + "] carries translations but entity [" + entity.getName()
                    + "] is not multilingual - add `multilingual: true` to the entity");
            return;
        }
        Set<String> allowed = new HashSet<>();
        for (FieldIntent field : entity.getFields()) {
            if (field.getName() == null) {
                continue;
            }
            String type = field.getType() == null ? "string"
                    : field.getType()
                           .toLowerCase(Locale.ROOT);
            if (field.isPrimaryKey() || "string".equals(type) || "text".equals(type)) {
                allowed.add(field.getName());
            }
        }
        for (java.util.Map<String, Object> row : seed.getRows()) {
            for (String key : row.keySet()) {
                if (!allowed.contains(key)) {
                    issues.add("seed [" + seed.getName() + "] row references [" + key
                            + "] which is not the id or a translatable (string/text) field of [" + entity.getName() + "]");
                }
            }
        }
    }

    private static void validateLanguages(IntentModel model, List<String> issues) {
        for (String language : model.getLanguages()) {
            if (language == null || !language.matches("[a-z]{2,3}")) {
                issues.add("languages entry [" + language + "] must be a short lowercase language code (e.g. en, bg)");
            }
        }
    }
}
