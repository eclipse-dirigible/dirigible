/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentSettings;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.generator.TriggerSupport;
import org.eclipse.dirigible.components.intent.model.CalendarIntent;
import org.eclipse.dirigible.components.intent.model.CustomWidgetIntent;
import org.eclipse.dirigible.components.intent.model.DependsOnIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.LabelExpression;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.SlotsIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits {@code <intent>.edm} (XML) and its JSON twin {@code <intent>.model} for every intent that
 * declares one or more entities. The pair is the canonical entity-data-model file consumed by the
 * EDM editor in the IDE and by the downstream "Generate from EDM" template engine, which turns the
 * model into UI / Java / SQL artefacts in a second step. The intent layer never emits those
 * second-stage artefacts itself - that contract belongs to the existing template generators.
 *
 * <p>
 * Conventions mirrored from real EDM documents (see
 * {@code tests-integrations/.../DependsOnScenariosTestProject/sales-order.edm}):
 * <ul>
 * <li>{@code dataName} is prefixed with the intent name: {@code <INTENT>_<ENTITY>} (e.g.
 * {@code ORDERS_COUNTRY}). This keeps physical table names unique across projects and away from SQL
 * reserved words like {@code ORDER}; the {@code .report} and {@code .csvim} generators use the same
 * convention so all three artefacts agree on the table name.</li>
 * <li>A {@code manyToOne}/{@code oneToOne} relation marked {@code composition: true} is a
 * <b>composition</b>: the FK property carries the {@code relationship*} attributes, the owning
 * entity becomes {@code DEPENDENT} with {@code MANAGE_DETAILS} layout and inherits the perspective
 * of its (transitively resolved) composition parent (and the FK is NOT NULL). Every other relation
 * is an <b>association</b>: a plain DROPDOWN property whose FK is NOT NULL when {@code required},
 * and the entity stays {@code PRIMARY} with its own perspective. Composition is opt-in (matching
 * the Dirigible convention, where it is an explicit {@code relationshipType="COMPOSITION"});
 * {@code required} alone only means the FK column is NOT NULL.</li>
 * <li>Dropdown key / value and the relation's {@code referencedProperty} are derived from the
 * target entity's actual fields (its primary key and its {@code name}-like field), never
 * hardcoded.</li>
 * <li>The {@code .model} JSON carries {@code entities} / {@code perspectives} /
 * {@code navigations}; relations appear only in the XML as {@code <relation>} elements interleaved
 * with their owning {@code <entity>}.</li>
 * </ul>
 * An {@code mxGraphModel} diagram block IS emitted with a deterministic grid layout: the EDM editor
 * renders the canvas exclusively by decoding {@code mxGraphModel}, so without it the editor opens
 * empty. See {@link #appendMxGraphModel}.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(200)
public class EdmIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdmIntentGenerator.class);

    private static final String DEFAULT_ICON = "/services/web/resources/unicons/file.svg";

    /** Id of the shell-provided perspective every {@code SETTING} entity is grouped under. */
    private static final String SETTINGS_PERSPECTIVE = "Settings";

    @Override
    public String name() {
        return "edm";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getEntities()
                 .isEmpty()) {
            return;
        }
        String baseName = IntentNaming.baseName(context);
        IntentSettings.Branding branding = context.getSettings() != null ? context.getSettings()
                                                                                  .getBranding()
                : new IntentSettings.Branding();
        EdmDocument document = buildDocument(context, model, baseName, branding);
        context.writeModelFile(baseName + ".model", JsonHelper.toJson(document.modelJson));
        context.writeModelFile(baseName + ".edm", renderEdmXml(document));
    }

    /**
     * Test seam: build the {@code .model} JSON for the given model without a repository. With a null
     * context, cross-model targets resolve via {@link CrossModelSupport} convention fallbacks (the
     * owner-model-reading path is exercised by the integration test). Never use in production code.
     */
    static Map<String, Object> buildModelJsonForTest(IntentModel model, String intentName) {
        return buildDocument(null, model, intentName, new IntentSettings.Branding()).modelJson;
    }

    /** The two views over one model tree: the {@code .model} JSON root and the XML extras. */
    private static final class EdmDocument {
        /** Root of the {@code .model} JSON: {@code {model: {entities, perspectives, navigations}}}. */
        final Map<String, Object> modelJson = new LinkedHashMap<>();
        /** Top-level {@code <relation>} elements per owning entity name, XML-only. */
        final Map<String, List<Map<String, Object>>> relationsByEntity = new LinkedHashMap<>();
    }

    private static EdmDocument buildDocument(IntentGenerationContext context, IntentModel model, String intentName,
            IntentSettings.Branding branding) {
        List<EntityIntent> entities = model.getEntities();
        Map<String, EntityIntent> byName = indexEntities(entities);
        Map<String, String> compositionParents = computeCompositionParents(entities);
        Set<String> settingEntities = settingEntities(entities);
        Set<String> triggerTargets = TriggerSupport.triggerTargetEntities(model);
        Map<String, UsesIntent> usesByAlias = new LinkedHashMap<>();
        for (UsesIntent uses : model.getUses()) {
            if (uses.getModel() != null) {
                usesByAlias.put(uses.getModel(), uses);
            }
        }
        String workspaceName = context != null && notBlank(context.getWorkspaceName()) ? context.getWorkspaceName() : "workspace";
        // Cross-model targets become read-only PROJECTION entities (dedup by target name; parameterUtils
        // links a consuming FK to its projection by matching relationshipEntityName == projection name).
        Map<String, Map<String, Object>> projectionEntities = new LinkedHashMap<>();

        EdmDocument document = new EdmDocument();
        List<Map<String, Object>> entityList = new ArrayList<>();
        List<Map<String, Object>> perspectiveList = new ArrayList<>();
        String tablePrefix = IntentNaming.upperSnake(intentName);
        String projectName = context != null && notBlank(context.getProjectName()) ? context.getProjectName() : intentName;
        // Document (header-items) layout: a master that owns a composition child whose name ends in
        // "Item" (SalesInvoice -> SalesInvoiceItem) renders as a document - header form, inline items
        // table, totals footer - rather than the default master-detail. Maps master -> its items entity.
        Map<String, String> documentItems = documentMasters(entities, compositionParents);
        // Capacity guard: a child of a capacity-bearing (balance-pattern) roll-up rejects a create/update
        // that would drive the parent's balance negative. Stamp the guard metadata onto the child so its
        // generated DAO enforces it synchronously (the DAO is generated from this .model, which otherwise
        // does not see the roll-ups - those live in the .glue).
        Map<String, Map<String, Object>> rollupGuards = buildRollupGuards(model, byName, compositionParents);
        int perspectiveOrder = 1;

        for (EntityIntent entity : entities) {
            String name = entity.getName();
            if (name == null || name.isBlank()) {
                LOGGER.warn("Skipping unnamed entity in intent");
                continue;
            }
            boolean setting = settingEntities.contains(name);
            boolean dependent = !setting && compositionParents.containsKey(name);
            // A setting lives under the global Settings perspective (provided by the shell); it does not
            // own a generated perspective.
            String perspective = perspectiveFor(name, compositionParents, settingEntities);
            String rolePerspective = resolvePerspective(name, compositionParents);
            Map<String, Object> entityMap = entityDefaults(name, entity.getDescription(), entity.getIcon(), dependent, setting, perspective,
                    tablePrefix, perspectiveOrder, projectName, rolePerspective);
            // A navigation-group id makes the generated perspective nest under that group in the shared
            // application shell (the standalone shell is unaffected). Defaults to empty (top-level).
            if (notBlank(entity.getGroup())) {
                entityMap.put("perspectiveNavId", entity.getGroup());
            }
            if (rollupGuards.containsKey(name)) {
                entityMap.put("rollupGuard", rollupGuards.get(name));
            }
            // A calendar entity renders its records as events on a Harmonia x-h-calendar instead of a
            // list/manage table. It keeps its own perspective/nav; the calendar page reuses the generated
            // REST controller (fetch) and the generated create/edit form (opened in a dialog on
            // date-click / event-click). The property names are PascalCased to match the .model property
            // (and the Jackson-serialized controller row) the calendar page reads.
            if (entity.isCalendar()) {
                CalendarIntent cal = entity.getCalendar();
                if (dependent) {
                    // A composition child stays a DETAIL of its master (MANAGE_DETAILS layout, the
                    // detail registry, the master-filtered controller) - the calendar is HOW the
                    // master page renders this child's panel: an embedded x-h-calendar instead of
                    // the detail table. The flag + the calendar* properties ride the .model into
                    // detail-register.js; no standalone calendar page is generated.
                    entityMap.put("detailCalendar", "true");
                } else {
                    entityMap.put("layoutType", "MANAGE_CALENDAR");
                }
                if (cal != null) {
                    if (notBlank(cal.getStart())) {
                        entityMap.put("calendarStartProperty", IntentNaming.pascalCase(cal.getStart()));
                    }
                    if (notBlank(cal.getEnd())) {
                        entityMap.put("calendarEndProperty", IntentNaming.pascalCase(cal.getEnd()));
                    }
                    if (notBlank(cal.getTitle())) {
                        entityMap.put("calendarTitleProperty", IntentNaming.pascalCase(cal.getTitle()));
                    }
                    if (notBlank(cal.getColor())) {
                        entityMap.put("calendarColorProperty", IntentNaming.pascalCase(cal.getColor()));
                    }
                    if (notBlank(cal.getScope())) {
                        entityMap.put("calendarScopeProperty", IntentNaming.pascalCase(cal.getScope()));
                    }
                    entityMap.put("calendarInitialView", notBlank(cal.getInitialView()) ? cal.getInitialView()
                                                                                             .trim()
                                                                                             .toLowerCase()
                            : "month");
                } else {
                    entityMap.put("calendarInitialView", "month");
                }
                // A range entity is a span (start..end) - events render as all-day multi-day bars.
                if (entity.isRange()) {
                    entityMap.put("calendarRange", "true");
                }
            }
            // A slots entity renders as a Harmonia x-h-slot-picker for appointment/booking: free time
            // slots are bookable and open the create form prefilled with the chosen datetime. Reuses the
            // generated controller (existing records mark their slots taken) and the shared form.
            else if (entity.isSlots()) {
                SlotsIntent slotsCfg = entity.getSlots();
                entityMap.put("layoutType", "MANAGE_SLOTS");
                if (slotsCfg != null) {
                    if (notBlank(slotsCfg.getStart())) {
                        entityMap.put("slotStartProperty", IntentNaming.pascalCase(slotsCfg.getStart()));
                    }
                    entityMap.put("slotOpen", notBlank(slotsCfg.getOpen()) ? slotsCfg.getOpen()
                                                                                     .trim()
                            : "08:00");
                    entityMap.put("slotClose", notBlank(slotsCfg.getClose()) ? slotsCfg.getClose()
                                                                                       .trim()
                            : "18:00");
                    entityMap.put("slotStep", Integer.toString(slotsCfg.getStep() == null ? 30 : slotsCfg.getStep()));
                    if (slotsCfg.getDisabledDays() != null && !slotsCfg.getDisabledDays()
                                                                       .isEmpty()) {
                        StringBuilder days = new StringBuilder();
                        for (Integer d : slotsCfg.getDisabledDays()) {
                            if (days.length() > 0) {
                                days.append(",");
                            }
                            days.append(d);
                        }
                        entityMap.put("slotDisabledDays", days.toString());
                    }
                } else {
                    entityMap.put("slotOpen", "08:00");
                    entityMap.put("slotClose", "18:00");
                    entityMap.put("slotStep", "30");
                }
            }
            // A document master keeps its own perspective/nav but swaps the master-detail layout for the
            // document layout; it names its line-items entity so the document page renders that child as
            // the inline table (and any other composition children as ordinary detail panels).
            else if (documentItems.containsKey(name)) {
                String itemsEntity = documentItems.get(name);
                entityMap.put("layoutType", "MANAGE_DOCUMENT");
                entityMap.put("documentItemsEntity", itemsEntity);
                // Humanized labels for the document editor: the master's singular ("Sales Invoice") and
                // the line-items' humanized + pluralized label ("Sales Invoice Items").
                entityMap.put("documentLabel", IntentNaming.humanize(name));
                entityMap.put("documentItemsLabel", IntentNaming.pluralize(IntentNaming.humanize(itemsEntity)));
                // duplicable: the document view offers a built-in Duplicate action that clones the
                // current document (header + line items) into a new draft (see the document template).
                if (entity.isDuplicable()) {
                    entityMap.put("duplicable", "true");
                }
                // Chat items: render the line-items pane as a conversation thread instead of the editable
                // table. Resolve which child property is the message body (and the optional internal
                // flag) from the items child's field roles; author + timestamp come from its audit
                // columns. The Harmonia document template branches on documentItemsLayout == "chat".
                if (entity.isChatItems()) {
                    entityMap.put("documentItemsLayout", "chat");
                    EntityIntent itemsChild = byName.get(itemsEntity);
                    if (itemsChild != null) {
                        for (FieldIntent itemField : itemsChild.getFields()) {
                            if (itemField.isMessageBody()) {
                                entityMap.put("chatBodyProperty", IntentNaming.pascalCase(itemField.getName()));
                            }
                            if (itemField.isMessageInternal()) {
                                entityMap.put("chatInternalProperty", IntentNaming.pascalCase(itemField.getName()));
                            }
                        }
                    }
                }
            }
            // A plain PRIMARY entity with NO composition children of its own is standalone, not a
            // master-detail. Give it the fuller MANAGE list layout (search / sort / per-column filter,
            // humanized title) rather than the default MANAGE_MASTER, which is meant for entities that
            // actually own detail children (a value in compositionParents).
            else if (!dependent && !setting && !compositionParents.containsValue(name)) {
                entityMap.put("layoutType", "MANAGE");
            }
            // A document master (owns a *Item / DocumentItem composition child) gets a generated .print
            // template + feeder, so its edit surface can offer a Print button. Flag it independently of
            // the layout: the MANAGE_DOCUMENT layout renders Print in the document view already, but a
            // document master whose UI is overridden to a calendar/slots view reuses the plain manage
            // form for edit - the flag is what lets that shared form show Print too.
            if (documentItems.containsKey(name)) {
                entityMap.put("hasPrint", "true");
            }
            // multilingual: the entity keeps per-language values in a sibling <TABLE>_LANG table. The
            // schema template generates that table and the Java DAO template overlays translated values
            // on every read for the caller's Accept-Language (same attribute the EDM editor writes).
            if (entity.isMultilingual()) {
                entityMap.put("multilingual", "true");
            }
            // Custom Java imports for the generated entity Repository (e.g. a calculated-field action's
            // CalculatedField class). Base64-encoded to match the EDM editor's serialization, which the
            // DAO template's parameterUtils decodes before emitting them into the import block.
            if (notBlank(entity.getImports())) {
                entityMap.put("importsCode", Base64.getEncoder()
                                                   .encodeToString(entity.getImports()
                                                                         .getBytes(StandardCharsets.UTF_8)));
            }
            if (!dependent && !setting) {
                perspectiveList.add(perspectiveEntry(name, perspectiveOrder, iconUrl(entity.getIcon())));
                perspectiveOrder++;
            }

            List<Map<String, Object>> properties = new ArrayList<>();
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() == null || field.getName()
                                                    .isBlank()) {
                    continue;
                }
                Map<String, Object> property = propertyMap(name, field);
                // A scalar field with dependsOn is auto-populated from the trigger's target record
                // (no filterBy - there is no option list to narrow).
                putDependsOn(property, entity, field.getDependsOn(), null, null, byName, usesByAlias, context);
                properties.add(property);
            }
            // A label expression (intent `label:`) synthesizes a stored, read-only Name property -
            // recomputed by the generated repository on every write, so lookups and dropdowns show
            // a meaningful display name everywhere (the platform's label resolution prefers "Name").
            if (entity.getLabel() != null && !entity.getLabel()
                                                    .isBlank()) {
                properties.add(labelNameProperty(name));
            }
            // An entity a process starts on create carries a ProcessId back-reference (the runtime
            // listener/handler writes the started process-instance id here). See TriggerSupport.
            if (triggerTargets.contains(name)) {
                properties.add(processIdProperty(name));
            }
            // The four standard audit columns, populated by the platform's audit annotations downstream.
            if (entity.isAudited()) {
                properties.addAll(auditProperties(name));
            }
            List<Map<String, Object>> relations = new ArrayList<>();
            boolean compositionAssigned = false;
            for (RelationIntent relation : entity.getRelations()) {
                if (!"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    continue;
                }
                if (relation.getName() == null || relation.getTo() == null) {
                    continue;
                }
                // A cross-model relation references an entity owned by another intent model: emit a
                // PROJECTION to that model plus a local integer FK + dropdown, and no <relation> link (the
                // target is not an entity in this model's XML). The owner owns the table / DAO / controller.
                if (relation.isCrossModel()) {
                    UsesIntent uses = usesByAlias.get(relation.getModel());
                    if (uses == null) {
                        LOGGER.warn("Skipping cross-model relation [{}] of [{}] - model [{}] is not in uses:", relation.getName(), name,
                                relation.getModel());
                        continue;
                    }
                    CrossModelSupport.TargetInfo info = CrossModelSupport.resolve(context, uses, relation.getTo());
                    Map<String, Object> fkProperty = crossModelRelationProperty(name, relation, info);
                    putDependsOn(fkProperty, entity, relation.getDependsOn(), info.keyField(), info.propertyNames(), byName, usesByAlias,
                            context);
                    putOptionsFilter(fkProperty, relation, info.propertyNames());
                    putLeafOnly(fkProperty, relation, info.hierarchyProperty(), info.resolved());
                    putPersonal(fkProperty, relation, info.identityProperty(), info.labelField(), info.resolved());
                    putPartner(fkProperty, relation, info.identityProperty(), info.labelField(), info.resolved());
                    properties.add(fkProperty);
                    projectionEntities.computeIfAbsent(relation.getTo(), target -> projectionEntity(uses, target, info, workspaceName));
                    continue;
                }
                boolean composition = !compositionAssigned && relation.isComposition();
                compositionAssigned |= composition;
                EntityIntent target = byName.get(relation.getTo());
                String targetPerspective = perspectiveFor(relation.getTo(), compositionParents, settingEntities);
                Map<String, Object> fkProperty = relationProperty(name, relation, target, composition, targetPerspective);
                putDependsOn(fkProperty, entity, relation.getDependsOn(), target == null ? "Id" : keyFieldName(target), null, byName,
                        usesByAlias, context);
                putOptionsFilter(fkProperty, relation, null);
                putLeafOnly(fkProperty, relation,
                        target == null || target.getHierarchy() == null ? null : IntentNaming.pascalCase(target.getHierarchy()), true);
                putPersonal(fkProperty, relation,
                        target == null || target.getIdentity() == null ? null : IntentNaming.pascalCase(target.getIdentity()),
                        target == null ? null : labelFieldName(target), true);
                putPartner(fkProperty, relation,
                        target == null || target.getIdentity() == null ? null : IntentNaming.pascalCase(target.getIdentity()),
                        target == null ? null : labelFieldName(target), true);
                properties.add(fkProperty);
                relations.add(relationLink(name, relation, target, compositionParents, settingEntities));
            }
            // Explicit UI control order (intent `order:`): reorder the properties so the generated
            // form/list controls follow the author's sequence (fields and to-one relations interleaved)
            // instead of the default fields-then-relations layout. Unlisted properties keep their
            // relative position, appended after the listed ones.
            properties = applyOrder(properties, entity.getOrder());
            if (Boolean.TRUE.equals(entity.getImmutable())) {
                // Append-only (intent `immutable: true`): every record is read-only for user writes from
                // the moment it is created - e.g. the snapshot stored when a document is sent.
                entityMap.put("immutableAlways", "true");
            } else if (entity.getImmutableWhen() != null && !entity.getImmutableWhen()
                                                                   .isBlank()) {
                // Status-scoped (intent `immutableWhen: "<Status> == <id> [|| ...]"`): compiled here into
                // the status property + the seed-id list the controller template consumes - user writes
                // are rejected while the EntityStatus FK holds one of these ids (system writes stay
                // possible).
                RelationIntent status = entityStatusRelation(entity);
                if (status != null) {
                    java.util.List<String> ids = new java.util.ArrayList<>();
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("==\\s*(\\d+)")
                                                                             .matcher(entity.getImmutableWhen());
                    while (matcher.find()) {
                        ids.add(matcher.group(1));
                    }
                    entityMap.put("immutableStatusProperty", IntentNaming.pascalCase(status.getName()));
                    entityMap.put("immutableStatusValues", String.join(",", ids));
                }
            }
            if (entity.getHierarchy() != null && !entity.getHierarchy()
                                                        .isBlank()) {
                // The tree edge: the FK property of the entity's self-relation named by `hierarchy`.
                // The Harmonia list renders a tree off it and pickers indent/leaf-filter by it.
                entityMap.put("hierarchyProperty", IntentNaming.pascalCase(entity.getHierarchy()));
            }
            if (entity.getIdentity() != null && !entity.getIdentity()
                                                       .isBlank()) {
                // The current-user mapping: the field of THIS entity matched against the logged-in
                // username. Consumers referencing this entity (incl. cross-model, via TargetInfo)
                // read it to scope their personal surfaces.
                entityMap.put("identityProperty", IntentNaming.pascalCase(entity.getIdentity()));
            }
            if (entity.getLabel() != null && !entity.getLabel()
                                                    .isBlank()) {
                // Template-ready label parts (a List - .model twin only): literals interleaved with
                // field / one-hop relation tokens, property names PascalCased to the generated model.
                // A relation token's `relation` is the FK PROPERTY name; the DAO template loads the
                // target through the repository parameterUtils derives for that FK.
                entityMap.put("labelExpression", entity.getLabel());
                entityMap.put("labelParts", buildLabelParts(entity, byName));
            }
            List<Map<String, Object>> checkMaps = buildChecks(entity, byName);
            if (!checkMaps.isEmpty()) {
                // Declarative validations. A List, so it lives only in the .model twin (the scalar-only
                // .edm XML skips it via the Iterable guard), consumed by the DAO/REST templates.
                entityMap.put("checks", checkMaps);
            }
            entityMap.put("properties", properties);
            entityList.add(entityMap);
            if (!relations.isEmpty()) {
                document.relationsByEntity.put(name, relations);
            }
        }
        // Append the synthesized PROJECTION entities (read-only cross-model references). They carry no
        // perspective so they stay out of this app's navigation, and downstream filters skip them for
        // table / DAO / controller / role generation.
        entityList.addAll(projectionEntities.values());

        Map<String, Object> body = new LinkedHashMap<>();
        // Model-level caption for the generated app (the Harmonia shell title / sidebar header). The
        // intent's `name` (humanised) is more meaningful than the raw project name; the `description`
        // rides along as a subtitle/tooltip. Both are ignored by tooling that only reads entities.
        // Branding precedence: .settings branding (developer-owned, per-deployment) wins over the
        // intent's own name/description/icon, which win over the defaults. So one model can be
        // rebranded per deployment by editing .settings, without touching the intent.
        String title = notBlank(branding.getTitle()) ? branding.getTitle() : IntentNaming.humanize(model.getName());
        body.put("title", title);
        String description = notBlank(branding.getDescription()) ? branding.getDescription() : model.getDescription();
        if (notBlank(description)) {
            body.put("description", description);
        }
        // Optional brand icon (Lucide name or image URL) for the shell header; the UI template
        // defaults it when absent.
        String icon = notBlank(branding.getIcon()) ? branding.getIcon() : model.getIcon();
        if (notBlank(icon)) {
            body.put("icon", icon);
        }
        // Data-language codes the app offers (intent `languages:`). Carried on the .model root (JSON
        // only - the XML root renders no attributes); the Harmonia shell's Region & Language setting
        // lists them and sends the choice as Accept-Language on every request.
        if (!model.getLanguages()
                  .isEmpty()) {
            body.put("languages", new ArrayList<>(model.getLanguages()));
        }
        // Custom dashboard widgets (top-level `widgets:`): developer-supplied REST KPIs (kind kpi -
        // the url returns {value, description?}) and embedded pages (kind page - the url is iframed).
        // Carried on the .model root; the Harmonia shell template bakes them into the dashboard.
        List<Map<String, Object>> customWidgets = buildCustomWidgets(model);
        if (!customWidgets.isEmpty()) {
            body.put("widgets", customWidgets);
        }
        body.put("entities", entityList);
        body.put("perspectives", perspectiveList);
        body.put("navigations", new ArrayList<>());
        // Process glue (triggers, decision resolvers) is NOT part of the EDM - it lives in the
        // sibling <intent>.glue file (GlueIntentGenerator). The EDM keeps only the persisted
        // ProcessId column it adds to a trigger-target entity.
        document.modelJson.put("model", body);
        return document;
    }

    /** Names of entities declared as settings / nomenclature ({@code kind: setting}). */
    private static Set<String> settingEntities(List<EntityIntent> entities) {
        Set<String> settings = new LinkedHashSet<>();
        for (EntityIntent entity : entities) {
            if (entity.getName() != null && entity.isSetting()) {
                settings.add(entity.getName());
            }
        }
        return settings;
    }

    /**
     * The perspective an entity (or relation target) lives under: the global {@code Settings}
     * perspective for a setting entity, otherwise its composition-resolved perspective.
     */
    /**
     * Build the capacity-guard metadata per child entity from the model's roll-ups. A guard applies to
     * a child of a capacity-bearing sum roll-up (one carrying {@code capacity} + {@code of}); it
     * rejects a create/update that would push the parent's balance below zero. Keyed by child entity
     * name; the DAO template ({@code #rollupGuardCheck}) reads {@code rollupGuard} off the entity.
     */
    private static Map<String, Map<String, Object>> buildRollupGuards(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents) {
        Map<String, Map<String, Object>> guards = new HashMap<>();
        for (RollupIntent rollup : model.getRollups()) {
            if (!"sum".equals(rollup.getOp()) || rollup.getCapacity() == null || rollup.getCapacity()
                                                                                       .isBlank()
                    || rollup.getOf() == null || rollup.getOf()
                                                       .isBlank()) {
                continue;
            }
            EntityIntent child = byName.get(rollup.getEntity());
            RelationIntent via = child == null ? null : toOneRelationByName(child, rollup.getVia());
            EntityIntent parent = via == null ? null : byName.get(via.getTo());
            if (parent == null || guards.containsKey(rollup.getEntity())) {
                continue;
            }
            Map<String, Object> guard = new LinkedHashMap<>();
            guard.put("parentEntity", parent.getName());
            guard.put("parentPerspective", resolvePerspective(parent.getName(), compositionParents));
            guard.put("fkProperty", IntentNaming.pascalCase(rollup.getVia()));
            guard.put("capacityField", IntentNaming.pascalCase(rollup.getCapacity()));
            guard.put("ofField", IntentNaming.pascalCase(rollup.getOf()));
            guard.put("childIdField", "Id");
            guards.put(rollup.getEntity(), guard);
        }
        return guards;
    }

    private static String perspectiveFor(String entityName, Map<String, String> compositionParents, Set<String> settingEntities) {
        if (settingEntities.contains(entityName)) {
            return SETTINGS_PERSPECTIVE;
        }
        return resolvePerspective(entityName, compositionParents);
    }

    private static Map<String, EntityIntent> indexEntities(List<EntityIntent> entities) {
        Map<String, EntityIntent> index = new HashMap<>();
        for (EntityIntent entity : entities) {
            if (entity.getName() != null) {
                index.put(entity.getName(), entity);
            }
        }
        return index;
    }

    /**
     * Document masters: each entity that is the composition parent of a child whose name ends in
     * {@code Item} maps to that child (the line-items entity). Iterated in entity-declaration order so
     * the first {@code *Item} child wins deterministically when a master has several. Such a master
     * renders with the document (header-items) layout instead of master-detail.
     */
    private static Map<String, String> documentMasters(List<EntityIntent> entities, Map<String, String> compositionParents) {
        Map<String, String> masters = new LinkedHashMap<>();
        // 1. A composition child that is the document's line-items - explicit `function: DocumentItem`,
        // or the legacy `*Item` naming - makes its composition parent a document master.
        for (EntityIntent entity : entities) {
            String child = entity.getName();
            if (child == null) {
                continue;
            }
            String parent = compositionParents.get(child);
            if (parent == null) {
                continue;
            }
            if ((entity.isDocumentItem() || child.endsWith("Item")) && !masters.containsKey(parent)) {
                masters.put(parent, child);
            }
        }
        // 2. `function: Document` on a master whose single composition child is neither flagged nor
        // `*Item`-named: the sole child is the items (the author declared the document intent).
        for (EntityIntent entity : entities) {
            String name = entity.getName();
            if (name == null || !entity.isDocument() || masters.containsKey(name)) {
                continue;
            }
            String sole = soleCompositionChild(entities, compositionParents, name);
            if (sole != null) {
                masters.put(name, sole);
            }
        }
        return masters;
    }

    /** The single composition child of {@code master}, or {@code null} when it has zero or several. */
    private static String soleCompositionChild(List<EntityIntent> entities, Map<String, String> compositionParents, String master) {
        String only = null;
        for (EntityIntent entity : entities) {
            if (entity.getName() != null && master.equals(compositionParents.get(entity.getName()))) {
                if (only != null) {
                    return null; // ambiguous - more than one composition child
                }
                only = entity.getName();
            }
        }
        return only;
    }

    /**
     * Map each entity to its composition parent: the target of its first {@code composition: true}
     * {@code manyToOne} / {@code oneToOne} relation. Entities present as keys are DEPENDENT; their
     * perspective is the parent's, resolved transitively by {@link #resolvePerspective}.
     */
    private static Map<String, String> computeCompositionParents(List<EntityIntent> entities) {
        Map<String, String> parents = new LinkedHashMap<>();
        for (EntityIntent entity : entities) {
            if (entity.getName() == null) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.isComposition() && relation.getTo() != null) {
                    parents.put(entity.getName(), relation.getTo());
                    break;
                }
            }
        }
        return parents;
    }

    /**
     * Follow the composition-parent chain to the first PRIMARY entity; that entity's name is the
     * perspective every entity in the chain lives under. A cycle (mutually required relations) falls
     * back to the starting entity itself.
     */
    private static String resolvePerspective(String entityName, Map<String, String> compositionParents) {
        String current = entityName;
        Set<String> visited = new LinkedHashSet<>();
        while (compositionParents.containsKey(current)) {
            if (!visited.add(current)) {
                LOGGER.warn("Composition cycle detected at entity [{}] - keeping its own perspective", entityName);
                return entityName;
            }
            current = compositionParents.get(current);
        }
        return current;
    }

    private static final String UNICONS_BASE = "/services/web/resources/unicons/";

    /**
     * Resolve an intent icon name to a unicons SVG URL (for the AngularJS perspective); blank →
     * default.
     */
    private static String iconUrl(String icon) {
        if (icon == null || icon.isBlank()) {
            return DEFAULT_ICON;
        }
        String n = icon.trim();
        return (n.startsWith("/") || n.startsWith("http")) ? n : UNICONS_BASE + n + ".svg";
    }

    /** The raw icon name (a Lucide icon name for the Harmonia sidebar); blank → a neutral default. */
    private static String iconName(String icon) {
        return (icon == null || icon.isBlank()) ? "list" : icon.trim();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * The custom dashboard widgets for the {@code .model} root: name, kind ({@code kpi} default /
     * {@code page}), the developer's same-origin URL, presentation defaults, and a {@code tId} that
     * lands in the model's translation catalog. Unnamed/duplicate widgets are skipped with a warning
     * (the parser already reports them as validation issues).
     */
    private static List<Map<String, Object>> buildCustomWidgets(IntentModel model) {
        List<Map<String, Object>> widgets = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CustomWidgetIntent widget : model.getWidgets()) {
            if (widget.getName() == null || widget.getName()
                                                  .isBlank()
                    || !seen.add(widget.getName())) {
                LOGGER.warn("Skipping unnamed or duplicate custom widget in intent [{}]", model.getName());
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", widget.getName());
            entry.put("kind", notBlank(widget.getKind()) ? widget.getKind()
                                                                 .trim()
                    : "kpi");
            entry.put("url", widget.getUrl());
            entry.put("label", notBlank(widget.getLabel()) ? widget.getLabel() : IntentNaming.humanize(widget.getName()));
            entry.put("tId", "widget" + widget.getName()
                                              .replace(" ", "")
                                              .replace("_", "")
                                              .replace(".", "")
                                              .replace(":", ""));
            entry.put("icon", notBlank(widget.getIcon()) ? widget.getIcon() : "gauge");
            if (notBlank(widget.getDescription())) {
                entry.put("description", widget.getDescription());
            }
            widgets.add(entry);
        }
        return widgets;
    }

    private static Map<String, Object> perspectiveEntry(String name, int order, String icon) {
        Map<String, Object> perspective = new LinkedHashMap<>();
        perspective.put("name", name);
        perspective.put("label", name);
        perspective.put("icon", icon);
        perspective.put("order", Integer.toString(order));
        return perspective;
    }

    private static Map<String, Object> entityDefaults(String name, String description, String icon, boolean dependent, boolean setting,
            String perspective, String tablePrefix, int order, String projectName, String rolePerspective) {
        String dataName = tablePrefix + "_" + IntentNaming.upperSnake(name);
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("dataName", dataName);
        entity.put("dataCount", "SELECT COUNT(*) AS COUNT FROM \"${tablePrefix}" + dataName + "\"");
        entity.put("dataQuery", "");
        entity.put("type", setting ? "SETTING" : (dependent ? "DEPENDENT" : "PRIMARY"));
        entity.put("title", name);
        entity.put("caption", "Manage entity " + name);
        entity.put("description", description != null && !description.isBlank() ? description : "Manage entity " + name);
        entity.put("tooltip", name);
        entity.put("icon", iconUrl(icon));
        // Raw icon name for the Harmonia sidebar (Lucide). Defaults to "list" when unset.
        entity.put("iconName", iconName(icon));
        entity.put("menuKey", name.toLowerCase(Locale.ROOT));
        // Navigation label: humanized + pluralized so the menu reads naturally
        // (SalesInvoice -> "Sales Invoices", Book -> "Books").
        entity.put("menuLabel", IntentNaming.pluralize(IntentNaming.humanize(name)));
        // Singular humanized label for in-page UI text (titles, "New X", "Delete X", "X #id").
        entity.put("entityLabel", IntentNaming.humanize(name));
        entity.put("menuIndex", "100");
        entity.put("layoutType", dependent ? "MANAGE_DETAILS" : "MANAGE_MASTER");
        entity.put("perspectiveName", perspective);
        entity.put("perspectiveLabel", perspective);
        entity.put("perspectiveHeader", "");
        entity.put("perspectiveIcon", iconUrl(icon));
        entity.put("perspectiveOrder", Integer.toString(order));
        entity.put("perspectiveNavId", "");
        entity.put("perspectiveRole", "");
        entity.put("generateReport", "false");
        entity.put("generateDefaultRoles", "true");
        entity.put("roleRead", projectName + "." + rolePerspective + "." + name + "ReadOnly");
        entity.put("roleWrite", projectName + "." + rolePerspective + "." + name + "FullAccess");
        return entity;
    }

    /**
     * Reorder an entity's generated properties by the intent's explicit {@code order} (a list of
     * property names, matched case-insensitively against each property's model name). Listed properties
     * come first in the given order; every property not named keeps its relative order and is appended
     * after. A blank/empty order returns the list unchanged. A name that matches no property is skipped
     * (the parser already validates order names, so this only guards system properties like audit
     * columns that the author never lists).
     *
     * @param properties the properties in their default order (fields, then to-one relations)
     * @param order the authored property-name sequence (may be empty)
     * @return a new list in the requested order, or {@code properties} when no order is given
     */
    private static List<Map<String, Object>> applyOrder(List<Map<String, Object>> properties, List<String> order) {
        if (order == null || order.isEmpty()) {
            return properties;
        }
        List<Map<String, Object>> ordered = new ArrayList<>(properties.size());
        Set<Integer> placed = new HashSet<>();
        for (String wanted : order) {
            if (wanted == null) {
                continue;
            }
            String token = wanted.trim();
            for (int i = 0; i < properties.size(); i++) {
                if (placed.contains(i)) {
                    continue;
                }
                if (token.equalsIgnoreCase(String.valueOf(properties.get(i)
                                                                    .get("name")))) {
                    ordered.add(properties.get(i));
                    placed.add(i);
                    break;
                }
            }
        }
        for (int i = 0; i < properties.size(); i++) {
            if (!placed.contains(i)) {
                ordered.add(properties.get(i));
            }
        }
        return ordered;
    }

    private static Map<String, Object> propertyMap(String entityName, FieldIntent field) {
        String column = IntentNaming.upperSnake(entityName) + "_" + IntentNaming.upperSnake(field.getName());
        String dataType = mapDataType(field.getType());
        Map<String, Object> p = new LinkedHashMap<>();
        // Property names are PascalCase (Dirigible convention); the physical column dataName
        // stays UPPER_SNAKE, derived from the authored field name above.
        p.put("name", IntentNaming.pascalCase(field.getName()));
        p.put("description", field.getDescription() == null ? "" : field.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", dataType);
        p.put("dataNullable", field.isRequired() || field.isPrimaryKey() ? "false" : "true");
        // Read-only in generated forms (rendered in the read-only details block, not an editable input):
        // an author-marked field, or a uuid (a system/business key, not user input).
        if (field.isReadOnly() || "uuid".equalsIgnoreCase(field.getType())) {
            p.put("isReadOnlyProperty", "true");
        }
        if (field.isSensitive()) {
            // Hidden from the personal (my) surface: absent from its pages and stripped from the
            // personal REST controller's responses. The power surface ignores this attribute.
            p.put("sensitiveProperty", "true");
        }
        if (field.isPrimaryKey()) {
            p.put("dataPrimaryKey", "true");
        } else if (field.isRequired()) {
            // The generated REST controller's required-value validation keys on isRequiredProperty
            // (not dataNullable); set it so a required field is actually validated. PKs are excluded
            // (auto-generated).
            p.put("isRequiredProperty", "true");
        }
        // Auto-increment is a DB identity/sequence - valid only on integer columns (a VARCHAR/uuid
        // AUTO_INCREMENT is rejected by the database). The parser already enforces integer primary keys;
        // this keeps the generator correct on its own.
        if (field.isPrimaryKey() && field.isGenerated() && ("INTEGER".equals(dataType) || "BIGINT".equals(dataType))) {
            p.put("dataAutoIncrement", "true");
        }
        Integer length = fieldLength(field);
        if (length != null && length > 0) {
            p.put("dataLength", length.toString());
        }
        if ("DECIMAL".equals(dataType)) {
            p.put("dataPrecision", field.getPrecision() != null ? field.getPrecision()
                                                                       .toString()
                    : "16");
            p.put("dataScale", field.getScale() != null ? field.getScale()
                                                               .toString()
                    : "2");
        }
        if (field.isUnique()) {
            p.put("dataUnique", "true");
        }
        if (field.getDefaultValue() != null && !field.getDefaultValue()
                                                     .isBlank()) {
            p.put("dataDefaultValue", field.getDefaultValue());
        }
        // A calculated property is assigned by the generated repository on insert/update - either from
        // the authored expression (emitted verbatim into the chosen runtime) or, when an action class is
        // given, by a server-side call-out to that CalculatedField implementation (which the DAO template
        // gives precedence over the expression).
        if (field.isCalculated()) {
            p.put("isCalculatedProperty", "true");
            if (notBlank(field.getCalculatedOnCreate())) {
                p.put("calculatedPropertyExpressionCreate", field.getCalculatedOnCreate());
            }
            if (notBlank(field.getCalculatedOnUpdate())) {
                p.put("calculatedPropertyExpressionUpdate", field.getCalculatedOnUpdate());
            }
            if (notBlank(field.getCalculatedActionOnCreate())) {
                p.put("calculatedActionOnCreate", field.getCalculatedActionOnCreate());
            }
            if (notBlank(field.getCalculatedActionOnUpdate())) {
                p.put("calculatedActionOnUpdate", field.getCalculatedActionOnUpdate());
            }
        }
        // Render hint for the document (header-items) layout: show this property in the totals footer
        // under the items table rather than in the header form. Presentational only.
        if (field.isAggregate()) {
            p.put("aggregate", "true");
        }
        p.put("auditType", "NONE");
        // Document role: the number/title field renders in the document form's title, not as an input.
        p.put("widgetType", field.isDocumentTitle() ? "DOCUMENT_NUMBER" : widgetForField(field, dataType));
        p.put("widgetSize", field.getSize() == null ? ""
                : field.getSize()
                       .toString());
        p.put("widgetLength", length == null ? "20" : length.toString());
        // Whether the field is a column in the entity list table; `major: false` keeps it off the list
        // (still shown in forms + the details pane). Defaults to true when unset.
        p.put("widgetIsMajor", field.isMajor() ? "true" : "false");
        return p;
    }

    /**
     * The {@code ProcessId} back-reference property added to an entity that a process starts on create.
     * A plain VARCHAR holding the started process-instance id; the runtime trigger handler writes it.
     * Not a major widget - it is system-managed, not user input.
     */
    private static Map<String, Object> processIdProperty(String entityName) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", "ProcessId");
        p.put("description", "Process instance started for this record");
        p.put("tooltip", "");
        p.put("dataName", IntentNaming.upperSnake(entityName) + "_" + IntentNaming.upperSnake("ProcessId"));
        p.put("dataType", "VARCHAR");
        p.put("dataNullable", "true");
        p.put("dataLength", "100");
        p.put("auditType", "NONE");
        // System-managed back-reference: read-only in forms (shown in the details block, not editable).
        p.put("isReadOnlyProperty", "true");
        p.put("widgetType", "TEXTBOX");
        p.put("widgetSize", "");
        p.put("widgetLength", "100");
        p.put("widgetIsMajor", "false");
        return p;
    }

    /**
     * FK property added to the owning entity for a {@code manyToOne}/{@code oneToOne} relation. Renders
     * as a DROPDOWN keyed by the target entity's actual primary-key field and labelled by its
     * {@code name}-like field. Only the entity's composition relation (its first
     * {@code composition: true} to-one) carries the {@code relationship*} attributes that make the EDM
     * editor treat the owner as a detail of the target - every other relation stays a plain association
     * (NOT NULL when {@code required}), mirroring how the EDM editor writes multi-FK entities.
     */
    private static Map<String, Object> relationProperty(String ownerEntity, RelationIntent relation, EntityIntent target,
            boolean composition, String targetPerspective) {
        String column = IntentNaming.upperSnake(ownerEntity) + "_" + IntentNaming.upperSnake(relation.getName());
        FieldIntent targetPk = primaryKeyOf(target);
        String fkType = targetPk == null ? "INTEGER" : mapDataType(targetPk.getType());
        boolean oneToOne = "oneToOne".equals(relation.getKind());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", IntentNaming.pascalCase(relation.getName()));
        p.put("description", relation.getDescription() == null ? "" : relation.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", fkType);
        // A composition FK is always NOT NULL (the detail cannot exist without its parent), even if
        // `required` was not also set; otherwise nullability follows `required`.
        boolean notNull = relation.isRequired() || composition;
        p.put("dataNullable", notNull ? "false" : "true");
        if (notNull) {
            p.put("isRequiredProperty", "true");
        }
        if ("VARCHAR".equals(fkType) && targetPk != null) {
            Integer length = fieldLength(targetPk);
            if (length != null && length > 0) {
                p.put("dataLength", length.toString());
            }
        }
        // init: the FK's database-level default (a target seed id) - a new row gets this FK on insert
        // when the column is left unset (e.g. an initial document status, race-free vs a process step).
        if (relation.getInit() != null && !relation.getInit()
                                                   .isBlank()) {
            p.put("dataDefaultValue", relation.getInit());
        }
        p.put("auditType", "NONE");
        // Relationship metadata the generation reads (Dirigible .model convention): composition vs
        // association + cardinality (composition 1_n; association n_1 for manyToOne, 1_1 for oneToOne);
        // relationshipName is the FK constraint name <owner>_<target>; relationshipEntityName and
        // relationshipEntityPerspectiveName drive the dropdown's data-service URL
        // (api/<perspective>/<entity>Service.ts) and the create-detail dialog.
        p.put("relationshipType", composition ? "COMPOSITION" : "ASSOCIATION");
        p.put("relationshipCardinality", composition ? "1_n" : (oneToOne ? "1_1" : "n_1"));
        p.put("relationshipName", ownerEntity + "_" + relation.getTo());
        p.put("relationshipEntityName", relation.getTo());
        p.put("relationshipEntityPerspectiveName", targetPerspective);
        p.put("relationshipEntityPerspectiveLabel", "Entities");
        // Document role: a status FK renders as a read-only coloured pill in the document title bar; it
        // keeps the dropdown lookup metadata so the UI can resolve the status name to display.
        p.put("widgetType", relation.isEntityStatus() ? "DOCUMENT_STATUS" : "DROPDOWN");
        p.put("widgetSize", relation.getSize() == null ? ""
                : relation.getSize()
                          .toString());
        p.put("widgetLength", "20");
        p.put("widgetIsMajor", "true");
        p.put("widgetDropDownKey", keyFieldName(target));
        p.put("widgetDropDownValue", labelFieldName(target));
        putLookupColumns(p, relation);
        return p;
    }

    /**
     * FK property for a cross-model {@code manyToOne}/{@code oneToOne} relation. Like
     * {@link #relationProperty} but the target lives in another model: the dropdown is sourced from the
     * owner's REST service (via the sibling {@code PROJECTION} entity, matched by
     * {@code relationshipEntityName}), the FK type / key / label / perspective come from the resolved
     * owner model, and there is never a composition (the parser forbids cross-model ownership).
     */
    private static Map<String, Object> crossModelRelationProperty(String ownerEntity, RelationIntent relation,
            CrossModelSupport.TargetInfo info) {
        String column = IntentNaming.upperSnake(ownerEntity) + "_" + IntentNaming.upperSnake(relation.getName());
        boolean oneToOne = "oneToOne".equals(relation.getKind());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", IntentNaming.pascalCase(relation.getName()));
        p.put("description", relation.getDescription() == null ? "" : relation.getDescription());
        p.put("tooltip", "");
        p.put("dataName", column);
        p.put("dataType", info.fkType());
        boolean notNull = relation.isRequired();
        p.put("dataNullable", notNull ? "false" : "true");
        if (notNull) {
            p.put("isRequiredProperty", "true");
        }
        // init: FK database-level default (a target seed id), as in relationProperty.
        if (relation.getInit() != null && !relation.getInit()
                                                   .isBlank()) {
            p.put("dataDefaultValue", relation.getInit());
        }
        p.put("auditType", "NONE");
        p.put("relationshipType", "ASSOCIATION");
        p.put("relationshipCardinality", oneToOne ? "1_1" : "n_1");
        p.put("relationshipName", ownerEntity + "_" + relation.getTo());
        p.put("relationshipEntityName", relation.getTo());
        // The owner's perspective for the target drives the dropdown's REST URL
        // (api/<perspective>/<Entity>Controller). Settings live under "Settings", primaries under their
        // own name - resolved from the owner model when present.
        p.put("relationshipEntityPerspectiveName", info.perspectiveName());
        p.put("relationshipEntityPerspectiveLabel", "Entities");
        p.put("widgetType", relation.isEntityStatus() ? "DOCUMENT_STATUS" : "DROPDOWN");
        p.put("widgetSize", relation.getSize() == null ? ""
                : relation.getSize()
                          .toString());
        p.put("widgetLength", "20");
        p.put("widgetIsMajor", "true");
        p.put("widgetDropDownKey", info.keyField());
        p.put("widgetDropDownValue", info.labelField());
        putLookupColumns(p, relation);
        return p;
    }

    /**
     * Emit the relation's {@code show} target fields as {@code lookupColumns} (PascalCase name +
     * humanized label) so the Harmonia detail table renders them as extra read-only columns resolved
     * from the already-fetched lookup row. Nothing is emitted when {@code show} is absent/empty.
     */
    private static void putLookupColumns(Map<String, Object> p, RelationIntent relation) {
        if (relation.getShow() == null || relation.getShow()
                                                  .isEmpty()) {
            return;
        }
        List<Map<String, Object>> columns = new ArrayList<>();
        for (String field : relation.getShow()) {
            if (field == null || field.isBlank()) {
                continue;
            }
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("name", IntentNaming.pascalCase(field));
            column.put("label", IntentNaming.humanize(field));
            columns.add(column);
        }
        if (!columns.isEmpty()) {
            p.put("lookupColumns", columns);
        }
    }

    /**
     * Emit the Depends-On widget attributes for a property that declares {@code dependsOn}: the trigger
     * (a sibling to-one relation), the value source on the trigger's target ({@code valueFrom},
     * defaulting to its primary key - the cascade case), and for a dropdown the option filter on its
     * own target ({@code filterBy}, defaulting to that target's primary key - the narrow-to-referenced
     * case). All four are scalar strings, so they flow into the {@code .edm} XML and the mxGraph
     * property cells like any other widget attribute. Same-model property references were validated by
     * the parser; a cross-model reference is validated here against the resolved owner model's property
     * names ({@code null} when unresolved - convention fallback in unit tests - where the check is
     * skipped).
     *
     * @param ownTargetKeyField the dependent's own target PK property (the {@code filterBy} default);
     *        {@code null} for a scalar field, which has no option list and gets no {@code filterBy}
     * @param ownTargetPropertyNames the dependent's own target property names when it is a resolved
     *        cross-model target (for {@code filterBy} validation), else {@code null}
     */
    private static void putDependsOn(Map<String, Object> p, EntityIntent owner, DependsOnIntent dependsOn, String ownTargetKeyField,
            Set<String> ownTargetPropertyNames, Map<String, EntityIntent> byName, Map<String, UsesIntent> usesByAlias,
            IntentGenerationContext context) {
        if (dependsOn == null) {
            return;
        }
        RelationIntent trigger = toOneRelationByName(owner, dependsOn.getRelation());
        if (trigger == null) {
            // The parser already reported the dangling trigger; stay lenient here.
            return;
        }
        String triggerKeyField = "Id";
        Set<String> triggerPropertyNames = null;
        if (trigger.isCrossModel()) {
            UsesIntent uses = usesByAlias.get(trigger.getModel());
            if (uses != null) {
                // The same resolve the trigger's own FK property performs - no new failure mode.
                CrossModelSupport.TargetInfo triggerInfo = CrossModelSupport.resolve(context, uses, trigger.getTo());
                triggerKeyField = triggerInfo.keyField();
                triggerPropertyNames = triggerInfo.propertyNames();
            }
        } else {
            EntityIntent triggerTarget = byName.get(trigger.getTo());
            if (triggerTarget != null) {
                triggerKeyField = keyFieldName(triggerTarget);
            }
        }
        String valueFrom = notBlank(dependsOn.getValueFrom()) ? IntentNaming.pascalCase(dependsOn.getValueFrom()) : triggerKeyField;
        requireTargetProperty(valueFrom, triggerPropertyNames, "valueFrom", trigger.getTo());
        p.put("widgetDependsOnProperty", IntentNaming.pascalCase(trigger.getName()));
        p.put("widgetDependsOnEntity", trigger.getTo());
        p.put("widgetDependsOnValueFrom", valueFrom);
        if (ownTargetKeyField != null) {
            String filterBy = notBlank(dependsOn.getFilterBy()) ? IntentNaming.pascalCase(dependsOn.getFilterBy()) : ownTargetKeyField;
            requireTargetProperty(filterBy, ownTargetPropertyNames, "filterBy", String.valueOf(p.get("relationshipEntityName")));
            p.put("widgetDependsOnFilterBy", filterBy);
        }
    }

    /**
     * Emit the static option-filter attributes for a relation that declares {@code where}: a single
     * constant {@code <target property> = <literal>} condition narrowing the dropdown's option list
     * (e.g. a stock line's Product picker showing only real products, never services). Two scalar
     * attributes ({@code widgetOptionsFilterBy} / {@code widgetOptionsFilterValue}), so they flow into
     * the {@code .edm} XML and the {@code .model} like any other widget attribute; the Harmonia chooser
     * dropdowns consume them via the controller's {@code /search}, while label-resolution lookups keep
     * the full set so historical rows still resolve. The parser validated the shape and same-model
     * property; a cross-model property is validated here against the resolved owner model (skipped when
     * unresolved - the unit-test convention fallback).
     */
    private static void putOptionsFilter(Map<String, Object> p, RelationIntent relation, Set<String> targetPropertyNames) {
        if (relation.getWhere() == null || relation.getWhere()
                                                   .size() != 1) {
            return;
        }
        Map.Entry<String, Object> condition = relation.getWhere()
                                                      .entrySet()
                                                      .iterator()
                                                      .next();
        String by = IntentNaming.pascalCase(condition.getKey());
        requireTargetProperty(by, targetPropertyNames, "where", relation.getTo());
        Object value = condition.getValue();
        String literal = value instanceof Number ? stripTrailingZero((Number) value) : String.valueOf(value);
        p.put("widgetOptionsFilterBy", by);
        p.put("widgetOptionsFilterValue", literal);
    }

    /**
     * Emit the leaf-only attributes for a relation that declares {@code leafOnly: true}: the picker
     * offers only childless nodes of the (hierarchical) target and the generated REST validation
     * rejects an FK to a node with children. Two scalar attrs: {@code widgetLeafOnly} plus
     * {@code widgetHierarchyProperty} - the TARGET's tree-edge FK property, which both the picker (to
     * compute depth/leaves client-side) and the validation (to count children) key on. The parser
     * checked a same-model target declares a hierarchy; a resolved cross-model target without one fails
     * loudly here, while an unresolved target (the unit-test convention fallback) is skipped.
     */
    private static void putLeafOnly(Map<String, Object> p, RelationIntent relation, String targetHierarchyProperty, boolean resolved) {
        if (!relation.isLeafOnly()) {
            return;
        }
        if (targetHierarchyProperty == null || targetHierarchyProperty.isBlank()) {
            if (resolved) {
                throw new IntentValidationException(java.util.List.of("relation [" + relation.getName()
                        + "] declares leafOnly but its target [" + relation.getTo() + "] declares no hierarchy"));
            }
            return;
        }
        p.put("widgetLeafOnly", "true");
        p.put("widgetHierarchyProperty", targetHierarchyProperty);
    }

    /**
     * Emit the personal-owner attributes for a relation that declares {@code personal: true}: the
     * generated personal (my) REST controller scopes reads by this FK and forces it on writes, and the
     * personal UI renders it as a locked value. Two scalar attrs: {@code relationshipPersonal} plus
     * {@code relationshipIdentityProperty} - the TARGET's identity field (PascalCase), which the
     * generated current-user resolution matches against the logged-in username. The parser checked a
     * same-model target declares an identity; a resolved cross-model target without one fails loudly
     * here, while an unresolved target (the unit-test convention fallback) is skipped.
     */
    private static void putPersonal(Map<String, Object> p, RelationIntent relation, String targetIdentityProperty,
            String targetIdentityLabel, boolean resolved) {
        if (!relation.isPersonal()) {
            return;
        }
        if (targetIdentityProperty == null || targetIdentityProperty.isBlank()) {
            if (resolved) {
                throw new IntentValidationException(java.util.List.of("relation [" + relation.getName()
                        + "] declares personal but its target [" + relation.getTo() + "] declares no identity"));
            }
            return;
        }
        p.put("relationshipPersonal", "true");
        if (relation.isPersonalReadOnly()) {
            // The personal surface is see-only for the owner: the my controller's write methods
            // 405 and the my pages drop New/Edit/Delete (parameterUtils -> the rest/UI templates).
            p.put("relationshipPersonalReadOnly", "true");
        }
        p.put("relationshipIdentityProperty", targetIdentityProperty);
        // The identity entity's display/label field - the personal controller's /me returns it so the
        // personal pages can show "New/Edit <Doc> for <owner>". Falls back to the identity match field.
        p.put("relationshipIdentityLabel",
                (targetIdentityLabel == null || targetIdentityLabel.isBlank()) ? targetIdentityProperty : targetIdentityLabel);
    }

    /**
     * Emit the partner-owner attributes for a relation that declares {@code partner: true} - the exact
     * mirror of {@link #putPersonal} for the external Partner shell. The generated partner REST
     * controller scopes reads by this FK against the logged-in partner's identity record and forces it
     * on writes; the partner perspective registers on {@code application-partner-perspectives}. Reuses
     * the target's {@code identity} field (Customer / Supplier carry {@code email}).
     */
    private static void putPartner(Map<String, Object> p, RelationIntent relation, String targetIdentityProperty,
            String targetIdentityLabel, boolean resolved) {
        if (!relation.isPartner()) {
            return;
        }
        if (targetIdentityProperty == null || targetIdentityProperty.isBlank()) {
            if (resolved) {
                throw new IntentValidationException(java.util.List.of("relation [" + relation.getName()
                        + "] declares partner but its target [" + relation.getTo() + "] declares no identity"));
            }
            return;
        }
        p.put("relationshipPartner", "true");
        p.put("relationshipPartnerIdentityProperty", targetIdentityProperty);
        p.put("relationshipPartnerIdentityLabel",
                (targetIdentityLabel == null || targetIdentityLabel.isBlank()) ? targetIdentityProperty : targetIdentityLabel);
    }

    /** YAML integers arrive as Long/Double - render {@code 1} not {@code 1.0} for whole numbers. */
    private static String stripTrailingZero(Number value) {
        double d = value.doubleValue();
        return d == Math.rint(d) && !Double.isInfinite(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /**
     * The entity's {@code checks} as template-ready maps: {@code exactlyOne} carries the PascalCased
     * own fields; the document-level kinds additionally carry the resolved items entity, the items'
     * back-reference FK property, and the EntityStatus gate property - everything the DAO/REST
     * templates need without re-deriving model structure.
     */
    private static List<Map<String, Object>> buildChecks(EntityIntent entity, Map<String, EntityIntent> byName) {
        List<Map<String, Object>> checkMaps = new ArrayList<>();
        if (entity.getChecks() == null) {
            return checkMaps;
        }
        for (org.eclipse.dirigible.components.intent.model.CheckIntent check : entity.getChecks()) {
            Map<String, Object> checkMap = new LinkedHashMap<>();
            checkMap.put("kind", check.getKind());
            checkMap.put("message", check.getMessage() == null ? "Validation failed" : check.getMessage());
            if ("exactlyOne".equals(check.getKind())) {
                checkMap.put("fields", check.getFields()
                                            .stream()
                                            .map(IntentNaming::pascalCase)
                                            .toList());
            } else {
                EntityIntent items = null;
                String itemsFk = null;
                for (EntityIntent candidate : byName.values()) {
                    if (candidate.getRelations() == null) {
                        continue;
                    }
                    for (RelationIntent relation : candidate.getRelations()) {
                        if (relation.isComposition() && entity.getName()
                                                              .equals(relation.getTo())) {
                            items = candidate;
                            itemsFk = IntentNaming.pascalCase(relation.getName());
                            break;
                        }
                    }
                    if (items != null) {
                        break;
                    }
                }
                if (items == null) {
                    continue; // the parser already reported it
                }
                checkMap.put("itemsEntity", items.getName());
                checkMap.put("itemsFk", itemsFk);
                checkMap.put("status", String.valueOf(check.getStatus()));
                RelationIntent status = entityStatusRelation(entity);
                if (status == null) {
                    continue; // the parser already reported it
                }
                checkMap.put("statusProperty", IntentNaming.pascalCase(status.getName()));
                if ("itemsSumEqual".equals(check.getKind())) {
                    checkMap.put("overA", IntentNaming.pascalCase(check.getOver()
                                                                       .get(0)));
                    checkMap.put("overB", IntentNaming.pascalCase(check.getOver()
                                                                       .get(1)));
                } else {
                    checkMap.put("count", String.valueOf(check.getCount()));
                }
            }
            checkMaps.add(checkMap);
        }
        return checkMaps;
    }

    /** The entity's {@code function: EntityStatus} relation, or null. */
    private static RelationIntent entityStatusRelation(EntityIntent entity) {
        if (entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            if (relation.isEntityStatus()) {
                return relation;
            }
        }
        return null;
    }

    /** The to-one relation of the entity with the given name, or null. */
    private static RelationIntent toOneRelationByName(EntityIntent entity, String name) {
        if (name == null || entity.getRelations() == null) {
            return null;
        }
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Generation-time check of a {@code dependsOn} property reference against a resolved cross-model
     * target's property names - the cross-model counterpart of the parser's same-model validation
     * (which cannot see another model's entities). Skipped when the names are unavailable (convention
     * fallback).
     */
    private static void requireTargetProperty(String property, Set<String> targetPropertyNames, String attribute, String targetEntity) {
        if (targetPropertyNames == null || targetPropertyNames.contains(property)) {
            return;
        }
        throw new IntentValidationException(List.of(
                "dependsOn " + attribute + " [" + property + "] is not a property of the cross-model target [" + targetEntity + "]"));
    }

    /**
     * A read-only {@code PROJECTION} entity standing in for a cross-model target. It generates no table
     * / DAO / controller (downstream filters skip {@code type=PROJECTION}); it carries the owner's
     * table name and primary-key column so the {@code .schema} foreign key resolves to the owner's
     * table, and a blank {@code perspectiveName} so it never shows up in this app's navigation. The
     * {@code projectionReferencedModel} path {@code /<workspace>/<project>/<model>.model} is the format
     * the template's {@code parameterUtils} splits to find the owner project + gen folder.
     */
    private static Map<String, Object> projectionEntity(UsesIntent uses, String targetEntity, CrossModelSupport.TargetInfo info,
            String workspaceName) {
        String project = uses.resolveProject();
        String alias = uses.getModel();
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("name", targetEntity);
        e.put("dataName", info.tableDataName());
        e.put("dataCount", "");
        e.put("dataQuery", "");
        e.put("type", "PROJECTION");
        e.put("title", targetEntity);
        e.put("caption", "Projection of " + targetEntity);
        e.put("description", "Cross-model reference to " + targetEntity + " owned by " + project);
        e.put("tooltip", targetEntity);
        e.put("icon", DEFAULT_ICON);
        e.put("iconName", "list");
        e.put("menuKey", targetEntity.toLowerCase(Locale.ROOT));
        e.put("menuLabel", IntentNaming.pluralize(IntentNaming.humanize(targetEntity)));
        e.put("menuIndex", "100");
        e.put("layoutType", "");
        // No perspective: keeps the projection out of parameterUtils' perspective/navigation building.
        e.put("perspectiveName", "");
        e.put("perspectiveLabel", "");
        e.put("perspectiveHeader", "");
        e.put("perspectiveIcon", DEFAULT_ICON);
        e.put("perspectiveOrder", "0");
        e.put("perspectiveNavId", "");
        e.put("perspectiveRole", "");
        e.put("generateReport", "false");
        e.put("generateDefaultRoles", "false");
        e.put("projectionReferencedModel", "/" + workspaceName + "/" + project + "/" + alias + ".model");
        e.put("projectionReferencedEntity", targetEntity);
        List<Map<String, Object>> properties = new ArrayList<>();
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("name", info.keyField());
        key.put("description", "");
        key.put("tooltip", "");
        key.put("dataName", info.keyColumn());
        key.put("dataType", info.fkType());
        key.put("dataNullable", "false");
        key.put("dataPrimaryKey", "true");
        key.put("auditType", "NONE");
        key.put("widgetType", widgetForType(info.fkType()));
        key.put("widgetIsMajor", "false");
        properties.add(key);
        Map<String, Object> label = new LinkedHashMap<>();
        label.put("name", info.labelField());
        label.put("description", "");
        label.put("tooltip", "");
        label.put("dataName", IntentNaming.upperSnake(targetEntity) + "_" + IntentNaming.upperSnake(info.labelField()));
        label.put("dataType", "VARCHAR");
        label.put("dataNullable", "true");
        label.put("auditType", "NONE");
        label.put("widgetType", "TEXTBOX");
        label.put("widgetIsMajor", "true");
        properties.add(label);
        e.put("properties", properties);
        return e;
    }

    /**
     * The four standard audit columns (populated downstream by the {@code @CreatedAt}/etc.
     * annotations).
     */
    private static List<Map<String, Object>> auditProperties(String entityName) {
        List<Map<String, Object>> audit = new ArrayList<>();
        audit.add(auditProperty(entityName, "CreatedAt", "TIMESTAMP", "CREATED_AT", 0));
        audit.add(auditProperty(entityName, "CreatedBy", "VARCHAR", "CREATED_BY", 128));
        audit.add(auditProperty(entityName, "UpdatedAt", "TIMESTAMP", "UPDATED_AT", 0));
        audit.add(auditProperty(entityName, "UpdatedBy", "VARCHAR", "UPDATED_BY", 128));
        return audit;
    }

    private static Map<String, Object> auditProperty(String entityName, String fieldName, String dataType, String auditType, int length) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", fieldName);
        p.put("description", "");
        p.put("tooltip", "");
        p.put("dataName", IntentNaming.upperSnake(entityName) + "_" + IntentNaming.upperSnake(fieldName));
        p.put("dataType", dataType);
        p.put("dataNullable", "true");
        if (length > 0) {
            p.put("dataLength", Integer.toString(length));
        }
        p.put("auditType", auditType);
        // Audit values are system-managed, not user input: read-only in forms (shown in the details block).
        p.put("isReadOnlyProperty", "true");
        p.put("widgetType", widgetForType(dataType));
        p.put("widgetSize", "");
        p.put("widgetLength", length > 0 ? Integer.toString(length) : "20");
        p.put("widgetIsMajor", "false");
        return p;
    }

    /**
     * Top-level {@code <relation>} element interleaved with its owning {@code <entity>} in the XML.
     * {@code relationshipEntityPerspectiveName} is the target's <i>resolved</i> perspective - for a
     * dependent target that is its composition parent's perspective, mirroring how the EDM editor
     * writes these links.
     */
    private static Map<String, Object> relationLink(String ownerEntity, RelationIntent relation, EntityIntent target,
            Map<String, String> compositionParents, Set<String> settingEntities) {
        Map<String, Object> link = new LinkedHashMap<>();
        String linkName = ownerEntity + "_" + IntentNaming.pascalCase(relation.getName());
        link.put("name", linkName);
        link.put("type", "relation");
        link.put("entity", ownerEntity);
        link.put("relationName", linkName);
        link.put("relationshipEntityPerspectiveName", perspectiveFor(relation.getTo(), compositionParents, settingEntities));
        link.put("relationshipEntityPerspectiveLabel", "Entities");
        link.put("property", IntentNaming.pascalCase(relation.getName()));
        link.put("referenced", relation.getTo());
        link.put("referencedProperty", keyFieldName(target));
        return link;
    }

    /** The target entity's primary-key field, or null when the target is unknown or has no PK. */
    private static FieldIntent primaryKeyOf(EntityIntent entity) {
        if (entity == null) {
            return null;
        }
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() && field.getName() != null) {
                return field;
            }
        }
        return null;
    }

    /**
     * The dropdown key: the target's actual PK field name (PascalCase); {@code Id} as a last resort.
     */
    private static String keyFieldName(EntityIntent target) {
        FieldIntent pk = primaryKeyOf(target);
        return pk == null ? "Id" : IntentNaming.pascalCase(pk.getName());
    }

    /**
     * The dropdown label: the target's {@code name} field (case-insensitive), else its first
     * string-typed field, else its PK.
     */
    /**
     * The synthesized {@code Name} property for an entity with a {@code label:} expression: a stored
     * VARCHAR the generated repository recomputes on every write. Read-only and shown on lists so the
     * display name is first-class everywhere.
     */
    private static Map<String, Object> labelNameProperty(String entityName) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", "Name");
        p.put("description", "");
        p.put("tooltip", "");
        p.put("dataName", IntentNaming.upperSnake(entityName) + "_NAME");
        p.put("dataType", "VARCHAR");
        p.put("dataLength", "512");
        p.put("dataNullable", "true");
        p.put("isReadOnlyProperty", "true");
        p.put("widgetType", "TEXTBOX");
        p.put("widgetLength", "512");
        p.put("widgetIsMajor", "true");
        p.put("auditType", "NONE");
        return p;
    }

    /**
     * The template-ready parts of a {@code label:} expression. Property names are PascalCased to the
     * generated model; a relation part's {@code relation} is the FK property name and {@code property}
     * the target's property - {@code name} resolves to the target's own display label (its authored
     * name field or its synthesized {@code Name}).
     */
    private static List<Map<String, Object>> buildLabelParts(EntityIntent entity, Map<String, EntityIntent> byName) {
        List<Map<String, Object>> parts = new ArrayList<>();
        for (LabelExpression.Part part : LabelExpression.parse(entity.getLabel())) {
            Map<String, Object> map = new LinkedHashMap<>();
            if (part.isLiteral()) {
                map.put("kind", "literal");
                map.put("text", part.literal());
            } else if (part.relation() == null) {
                map.put("kind", "field");
                map.put("property", IntentNaming.pascalCase(part.property()));
            } else {
                map.put("kind", "relation");
                map.put("relation", IntentNaming.pascalCase(part.relation()));
                map.put("property", IntentNaming.pascalCase(part.property()));
            }
            if (part.format() != null && !part.format()
                                              .isBlank()) {
                map.put("format", part.format());
            }
            parts.add(map);
        }
        return parts;
    }

    private static String labelFieldName(EntityIntent target) {
        if (target == null) {
            return "Name";
        }
        if (target.getLabel() != null && !target.getLabel()
                                                .isBlank()) {
            // The target's stored label (intent `label:`) IS its display name.
            return "Name";
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && "name".equalsIgnoreCase(field.getName())) {
                return IntentNaming.pascalCase(field.getName());
            }
        }
        for (FieldIntent field : target.getFields()) {
            if (field.getName() != null && "VARCHAR".equals(mapDataType(field.getType())) && !field.isPrimaryKey()) {
                return IntentNaming.pascalCase(field.getName());
            }
        }
        return keyFieldName(target);
    }

    /**
     * Declared length, with type-derived defaults ({@code uuid} -> 36, {@code month} -> 7, {@code week}
     * -> 8).
     */
    private static Integer fieldLength(FieldIntent field) {
        if (field.getLength() != null) {
            return field.getLength();
        }
        if (field.getType() != null) {
            switch (field.getType()
                         .toLowerCase(Locale.ROOT)) {
                case "uuid":
                    return 36;
                case "month": // YYYY-MM
                    return 7;
                case "week": // YYYY-Www
                    return 8;
                default:
                    break;
            }
        }
        return defaultLength(mapDataType(field.getType()));
    }

    private static String mapDataType(String type) {
        if (type == null) {
            return "VARCHAR";
        }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "integer":
            case "int":
                return "INTEGER";
            case "long":
                return "BIGINT";
            case "decimal":
            case "double":
                return "DECIMAL";
            case "boolean":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "timestamp":
                return "TIMESTAMP";
            case "text":
                return "CLOB";
            case "uuid":
            case "string":
            case "month": // stored as the picker's YYYY-MM string
            case "week": // stored as the picker's YYYY-Www ISO-week string
            default:
                return "VARCHAR";
        }
    }

    /**
     * Widget for a field. {@code month}/{@code week} are both stored as {@code VARCHAR}, so they are
     * indistinguishable at the JDBC-type level - the picker widget is chosen from the logical type (the
     * same reason {@code documentTitle} is special-cased at the call site).
     */
    private static String widgetForField(FieldIntent field, String dataType) {
        String type = field.getType() == null ? ""
                : field.getType()
                       .toLowerCase(Locale.ROOT);
        switch (type) {
            case "month":
                return "MONTH";
            case "week":
                return "WEEK";
            default:
                return widgetForType(dataType);
        }
    }

    private static String widgetForType(String dataType) {
        switch (dataType) {
            case "INTEGER":
            case "BIGINT":
            case "DECIMAL":
                return "NUMBER";
            case "DATE":
                return "DATE";
            case "TIMESTAMP":
                return "DATETIME-LOCAL";
            case "BOOLEAN":
                return "CHECKBOX";
            case "CLOB":
                return "TEXTAREA";
            case "VARCHAR":
            default:
                return "TEXTBOX";
        }
    }

    private static Integer defaultLength(String dataType) {
        switch (dataType) {
            case "VARCHAR":
                return 100;
            case "DECIMAL":
                return 16;
            case "INTEGER":
            case "BIGINT":
                return 20;
            default:
                return null;
        }
    }

    /**
     * Render the EDM XML shape: entities with their relations interleaved, the perspectives and
     * navigations blocks, then the {@code mxGraphModel} diagram. The mxGraphModel is mandatory, not
     * optional: the EDM editor renders the canvas <b>exclusively</b> by decoding {@code mxGraphModel}
     * (see {@code editor-entity/js/editor.js} - {@code codec.decode(... getElementsByTagName(
     * 'mxGraphModel')[0] ...)}); without it the editor opens to an empty canvas. The diagram is laid
     * out deterministically in a grid so re-generation is byte-stable.
     */
    @SuppressWarnings("unchecked")
    private static String renderEdmXml(EdmDocument document) {
        Map<String, Object> body = (Map<String, Object>) document.modelJson.get("model");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) body.get("entities");
        List<Map<String, Object>> perspectives = (List<Map<String, Object>>) body.get("perspectives");

        StringBuilder sb = new StringBuilder(8192);
        sb.append("<model>\n");
        sb.append(" <entities>\n");
        for (Map<String, Object> entity : entities) {
            sb.append("  <entity");
            List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.getOrDefault("properties", List.of());
            for (Map.Entry<String, Object> attr : entity.entrySet()) {
                if ("properties".equals(attr.getKey())) {
                    continue;
                }
                appendAttribute(sb, attr.getKey(), attr.getValue());
            }
            sb.append(">\n");
            for (Map<String, Object> property : properties) {
                sb.append("   <property");
                for (Map.Entry<String, Object> attr : property.entrySet()) {
                    appendAttribute(sb, attr.getKey(), attr.getValue());
                }
                sb.append("></property>\n");
            }
            sb.append("  </entity>\n");
            List<Map<String, Object>> relations = document.relationsByEntity.get(entity.get("name"));
            if (relations != null) {
                for (Map<String, Object> relation : relations) {
                    sb.append("  <relation");
                    for (Map.Entry<String, Object> attr : relation.entrySet()) {
                        appendAttribute(sb, attr.getKey(), attr.getValue());
                    }
                    sb.append(">\n  </relation>\n");
                }
            }
        }
        sb.append(" </entities>\n");
        sb.append(" <perspectives>\n");
        for (Map<String, Object> perspective : perspectives) {
            sb.append("  <perspective><name>")
              .append(escapeXmlText(perspective.get("name")))
              .append("</name><label>")
              .append(escapeXmlText(perspective.get("label")))
              .append("</label><icon>")
              .append(escapeXmlText(perspective.get("icon")))
              .append("</icon><order>")
              .append(escapeXmlText(perspective.get("order")))
              .append("</order></perspective>\n");
        }
        sb.append(" </perspectives>\n");
        sb.append(" <navigations>\n");
        sb.append(" </navigations>\n");
        appendMxGraphModel(sb, document, entities);
        sb.append("</model>\n");
        return sb.toString();
    }

    /** Entity box width and row heights for the deterministic grid layout. */
    private static final int CELL_WIDTH = 200;
    private static final int TITLE_HEIGHT = 28;
    private static final int ROW_HEIGHT = 26;
    private static final int GRID_COLUMNS = 3;
    private static final int COLUMN_GAP = 80;
    private static final int ROW_GAP = 40;
    private static final int GRID_ORIGIN = 20;

    /**
     * Append the {@code mxGraphModel} the EDM editor decodes to render the canvas: one
     * {@code style="entity"} vertex per entity carrying an {@code <Entity>} value, a child vertex per
     * property carrying a {@code <Property>} value, and one edge per foreign-key relation linking the
     * owner's FK property to the target entity's primary-key property. Entities are placed in a fixed
     * grid so the output is deterministic across regenerations.
     */
    private static void appendMxGraphModel(StringBuilder sb, EdmDocument document, List<Map<String, Object>> entities) {
        // Pre-compute cell ids and the per-entity primary-key property cell id (edge targets).
        Map<String, String> entityCellId = new HashMap<>();
        Map<String, Map<String, String>> propertyCellId = new HashMap<>();
        Map<String, String> pkCellIdByEntity = new HashMap<>();
        for (Map<String, Object> entity : entities) {
            String name = (String) entity.get("name");
            String entCell = "ent_" + sanitizeId(name);
            entityCellId.put(name, entCell);
            Map<String, String> props = new LinkedHashMap<>();
            List<Map<String, Object>> properties = propertiesOf(entity);
            for (Map<String, Object> property : properties) {
                String propName = (String) property.get("name");
                String propCell = entCell + "_p_" + sanitizeId(propName);
                props.put(propName, propCell);
                if ("true".equals(property.get("dataPrimaryKey")) && !pkCellIdByEntity.containsKey(name)) {
                    pkCellIdByEntity.put(name, propCell);
                }
            }
            propertyCellId.put(name, props);
        }

        // Placement pass: pick an entity order that clusters FK-related entities, then pack each into
        // the currently shortest column. Emission below still walks `entities` in declaration order (so
        // the XML element order stays stable across regenerations); positions come from this map.
        Map<String, int[]> placement = placeEntities(entities, document);

        sb.append(" <mxGraphModel>\n  <root>\n");
        sb.append("   <mxCell id=\"0\"/>\n");
        sb.append("   <mxCell id=\"1\" parent=\"0\"/>\n");

        for (Map<String, Object> entity : entities) {
            String name = (String) entity.get("name");
            List<Map<String, Object>> properties = propertiesOf(entity);
            int[] pos = placement.get(name);
            int x = pos[0];
            int y = pos[1];
            int height = pos[2];

            sb.append("   <mxCell id=\"")
              .append(entityCellId.get(name))
              .append("\" style=\"entity\" parent=\"1\" vertex=\"1\">\n");
            appendEntityValue(sb, entity);
            sb.append("    <mxGeometry x=\"")
              .append(x)
              .append("\" y=\"")
              .append(y)
              .append("\" width=\"")
              .append(CELL_WIDTH)
              .append("\" height=\"")
              .append(height)
              .append("\" as=\"geometry\"><mxRectangle width=\"")
              .append(CELL_WIDTH)
              .append("\" height=\"")
              .append(TITLE_HEIGHT)
              .append("\" as=\"alternateBounds\"/></mxGeometry>\n");
            sb.append("   </mxCell>\n");

            int propIndex = 0;
            for (Map<String, Object> property : properties) {
                sb.append("   <mxCell id=\"")
                  .append(propertyCellId.get(name)
                                        .get(property.get("name")))
                  .append("\" parent=\"")
                  .append(entityCellId.get(name))
                  .append("\" vertex=\"1\" connectable=\"0\">\n");
                appendPropertyValue(sb, property);
                sb.append("    <mxGeometry y=\"")
                  .append(TITLE_HEIGHT + propIndex * ROW_HEIGHT)
                  .append("\" width=\"")
                  .append(CELL_WIDTH)
                  .append("\" height=\"")
                  .append(ROW_HEIGHT)
                  .append("\" as=\"geometry\"/>\n");
                sb.append("   </mxCell>\n");
                propIndex++;
            }
        }

        // Edges: owner FK property -> target entity primary-key property.
        for (Map.Entry<String, List<Map<String, Object>>> entry : document.relationsByEntity.entrySet()) {
            String owner = entry.getKey();
            for (Map<String, Object> relation : entry.getValue()) {
                String property = (String) relation.get("property");
                String referenced = (String) relation.get("referenced");
                String source = propertyCellId.getOrDefault(owner, Map.of())
                                              .get(property);
                String target = pkCellIdByEntity.get(referenced);
                if (source == null || target == null) {
                    continue;
                }
                sb.append("   <mxCell id=\"edge_")
                  .append(sanitizeId(owner))
                  .append("_")
                  .append(sanitizeId(property))
                  .append("\" parent=\"1\" source=\"")
                  .append(source)
                  .append("\" target=\"")
                  .append(target)
                  .append("\" edge=\"1\"><mxGeometry relative=\"1\" as=\"geometry\"/></mxCell>\n");
            }
        }

        sb.append("  </root>\n </mxGraphModel>\n");
    }

    /**
     * Compute the {@code [x, y, height]} grid position of every entity. Entities are visited in a
     * relationship-aware order - a breadth-first walk over the (undirected) FK graph seeded in
     * declaration order - so connected entities are laid out consecutively and land near each other;
     * each is then packed into the currently shortest of the {@link #GRID_COLUMNS} columns so the
     * columns stay balanced regardless of how tall individual cards are. Deterministic: the seed order
     * and the shortest-column tie-break (lowest index) are both stable.
     */
    private static Map<String, int[]> placeEntities(List<Map<String, Object>> entities, EdmDocument document) {
        Map<String, List<Map<String, Object>>> byName = new LinkedHashMap<>();
        for (Map<String, Object> entity : entities) {
            byName.put((String) entity.get("name"), propertiesOf(entity));
        }
        List<String> order = relationshipOrder(byName.keySet(), document);

        int[] columnBottom = new int[GRID_COLUMNS];
        for (int i = 0; i < GRID_COLUMNS; i++) {
            columnBottom[i] = GRID_ORIGIN;
        }
        Map<String, int[]> placement = new HashMap<>();
        for (String name : order) {
            int column = shortestColumn(columnBottom);
            int x = GRID_ORIGIN + column * (CELL_WIDTH + COLUMN_GAP);
            int y = columnBottom[column];
            int height = TITLE_HEIGHT + ROW_HEIGHT * Math.max(byName.get(name)
                                                                    .size(),
                    1);
            columnBottom[column] = y + height + ROW_GAP;
            placement.put(name, new int[] {x, y, height});
        }
        return placement;
    }

    /**
     * A breadth-first ordering of the entities over the undirected FK graph, seeded in declaration
     * order, so each connected cluster of entities comes out contiguously (and any entity with no
     * relations still appears, in declaration order).
     */
    private static List<String> relationshipOrder(Set<String> names, EdmDocument document) {
        Map<String, List<String>> adjacency = new LinkedHashMap<>();
        for (String name : names) {
            adjacency.put(name, new ArrayList<>());
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : document.relationsByEntity.entrySet()) {
            String owner = entry.getKey();
            for (Map<String, Object> relation : entry.getValue()) {
                String referenced = (String) relation.get("referenced");
                if (adjacency.containsKey(owner) && adjacency.containsKey(referenced)) {
                    adjacency.get(owner)
                             .add(referenced);
                    adjacency.get(referenced)
                             .add(owner);
                }
            }
        }
        List<String> order = new ArrayList<>(names.size());
        Set<String> seen = new HashSet<>();
        for (String seed : names) {
            if (seen.contains(seed)) {
                continue;
            }
            Queue<String> queue = new java.util.ArrayDeque<>();
            queue.add(seed);
            seen.add(seed);
            while (!queue.isEmpty()) {
                String current = queue.poll();
                order.add(current);
                for (String neighbour : adjacency.get(current)) {
                    if (seen.add(neighbour)) {
                        queue.add(neighbour);
                    }
                }
            }
        }
        return order;
    }

    /** Index of the column whose stacked cards currently reach the smallest Y (ties: lowest index). */
    private static int shortestColumn(int[] columnBottom) {
        int best = 0;
        for (int i = 1; i < columnBottom.length; i++) {
            if (columnBottom[i] < columnBottom[best]) {
                best = i;
            }
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> propertiesOf(Map<String, Object> entity) {
        return (List<Map<String, Object>>) entity.getOrDefault("properties", List.of());
    }

    /**
     * The {@code <Entity>} cell value. {@code type="Entity"} is the constant cell-kind marker the
     * editor keys on; the PRIMARY/DEPENDENT distinction is carried in {@code entityType} (omitted for
     * PRIMARY), matching the editor's own serializer.
     */
    private static void appendEntityValue(StringBuilder sb, Map<String, Object> entity) {
        sb.append("    <Entity");
        appendAttribute(sb, "name", entity.get("name"));
        // PRIMARY is the editor's default (omitted); DEPENDENT and SETTING are carried explicitly so
        // the EDM editor restyles the cell and the template engine routes the entity correctly.
        Object entityType = entity.get("type");
        if ("DEPENDENT".equals(entityType) || "SETTING".equals(entityType)) {
            appendAttribute(sb, "entityType", entityType);
        }
        for (String key : new String[] {"dataName", "dataCount", "dataQuery", "title", "caption", "description", "tooltip", "menuKey",
                "menuLabel", "layoutType", "perspectiveName", "importsCode", "generateDefaultRoles", "roleRead", "roleWrite"}) {
            if (entity.get(key) != null) {
                appendAttribute(sb, key, entity.get(key));
            }
        }
        appendAttribute(sb, "type", "Entity");
        sb.append(" as=\"value\"/>\n");
    }

    /** The {@code <Property>} cell value - the property's scalar attributes verbatim. */
    private static void appendPropertyValue(StringBuilder sb, Map<String, Object> property) {
        sb.append("    <Property");
        for (Map.Entry<String, Object> attr : property.entrySet()) {
            // EDM attributes are scalar; a structured value (e.g. lookupColumns, .model-JSON-only) is not
            // an EDM attribute and would render as a junk string - it belongs only in the .model twin.
            if (attr.getValue() instanceof Iterable || attr.getValue() instanceof Map) {
                continue;
            }
            appendAttribute(sb, attr.getKey(), attr.getValue());
        }
        sb.append(" as=\"value\"/>\n");
    }

    /** mxGraph cell ids must be attribute-safe and stable; keep only word characters. */
    private static String sanitizeId(String raw) {
        return raw == null ? "" : raw.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static void appendAttribute(StringBuilder sb, String key, Object value) {
        sb.append(' ')
          .append(key)
          .append("=\"")
          .append(escapeXml(value == null ? "" : value.toString()))
          .append("\"");
    }

    private static String escapeXmlText(Object value) {
        return escapeXml(value == null ? "" : value.toString());
    }

    private static String escapeXml(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

}
