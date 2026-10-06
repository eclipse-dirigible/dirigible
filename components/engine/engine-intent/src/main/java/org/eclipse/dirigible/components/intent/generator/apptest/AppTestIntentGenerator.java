/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.apptest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.LoggedValue;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.generator.CheckSupport;
import org.eclipse.dirigible.components.intent.generator.PickableSupport;
import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.model.CheckIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Emits one {@code <name>.test} manifest per module — the app-integration-test counterpart of the
 * generated application. The manifest is a JSON description of WHAT the generated app promises
 * (entities, layouts, per-field metadata, seeds, languages, REST + shell coordinates); the generic
 * Playwright runner {@code @aerokit/test} reads it and knows HOW to verify each promise against the
 * generated Harmonia UI and the reused REST controllers. So one runner, versioned with the
 * templates whose markup it drives, checks every generated app the same way — no per-entity spec
 * files to drift.
 *
 * <p>
 * The manifest carries the {@code .test} extension — a free extension; JS {@code *.test.js} files
 * carry extension {@code js}, so there is no clash — and is written <b>once</b>
 * ({@link IntentGenerationContext#keepExistingModelFile(String)} guards
 * {@link IntentGenerationContext#writeModelFile(String, String)}), like the {@code .print} document
 * template: Generate scaffolds it for a new module, and from then on it belongs to the developer. A
 * behavioural test is the one artifact that must NOT be re-derived from the intent by the same
 * toolchain that generates the code — a test built from the parsed model inherits the generator's
 * blind spots and so passes precisely when the generator is consistently wrong. It is worth
 * something only when a human states independently what the module must do, which means the file
 * has to be hand-enhanceable, which means Generate must not clobber it (dirigible #6755). An
 * existing manifest is also kept out of the stale-output scrub, which owns the extension.
 *
 * <p>
 * Runs at {@code @Order(900)} — after the {@code EdmIntentGenerator} (200) has written the
 * {@code .model}, whose resolved per-entity metadata (perspective → REST path, {@code dataName} →
 * table, {@code menuLabel} → plural label, layout type, nav group, {@code multilingual}) is the
 * same source the Harmonia templates consume; this generator reads it back rather than re-deriving
 * it. The logical per-field/relation data (types, required, unique, length, major) comes straight
 * from the intent model.
 */
@Component
@Order(900)
public class AppTestIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppTestIntentGenerator.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
                                                      .disableHtmlEscaping()
                                                      .create();

    @Override
    public String name() {
        return "test";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        String baseName = IntentNaming.baseName(context);
        String fileName = baseName + ".test";
        // Write-once: the manifest is a scaffold the developer owns once it exists, so a regeneration
        // neither rewrites it nor lets the scrub take it. Checked before anything is built - there is
        // no output to produce for a module that already has its manifest.
        if (context.keepExistingModelFile(fileName)) {
            LOGGER.debug("Keeping the existing app-test manifest [{}] — it is developer-owned after the first Generate",
                    LoggedValue.of(fileName));
            return;
        }

        Map<String, Map<String, Object>> edmEntities = readModelEntities(context, baseName);
        if (edmEntities.isEmpty()) {
            LOGGER.debug("Skipping app-test manifest for [{}] — no .model entities to describe", LoggedValue.of(baseName));
            return;
        }

        Map<String, Object> manifest = buildManifest(baseName, context.getProjectName(), model, edmEntities, context);
        context.writeModelFile(fileName, GSON.toJson(manifest) + "\n");
        LOGGER.debug("Generated app-test manifest [{}]", LoggedValue.of(fileName));
    }

    /**
     * Assembles the manifest map from the intent model and the EDM-derived per-entity metadata — the
     * pure, repository-free core of this generator (so it is unit-testable independently of the
     * workspace I/O).
     *
     * @param baseName the intent base name (module id + web/gen folder)
     * @param project the workspace project folder
     * @param model the parsed intent model (logical field/relation data)
     * @param edmEntities the {@code .model} entities indexed by name (resolved perspective, table,
     *        labels, layout, nav group, multilingual)
     * @return the ordered manifest map ready to serialize as {@code <name>.test}
     */
    public static Map<String, Object> buildManifest(String baseName, String project, IntentModel model,
            Map<String, Map<String, Object>> edmEntities) {
        return buildManifest(baseName, project, model, edmEntities, null);
    }

    /**
     * The full variant carrying the generation context, which cross-model relation resolution needs (a
     * {@code null} context falls back to the naming-convention target coordinates - unit tests).
     */
    public static Map<String, Object> buildManifest(String baseName, String project, IntentModel model,
            Map<String, Map<String, Object>> edmEntities, IntentGenerationContext context) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("module", baseName);
        manifest.put("standaloneShell", "/services/web/" + project + "/gen/" + baseName + "/index.html");
        manifest.put("restBase", "/services/java/" + project + "/gen/" + sanitizeJavaIdentifier(baseName) + "/api");
        manifest.put("idProperty", idProperty(edmEntities));
        manifest.put("languages", languages(model));

        List<Map<String, Object>> entities = new ArrayList<>();
        for (EntityIntent entity : model.getEntities()) {
            Map<String, Object> edm = edmEntities.get(entity.getName());
            if (edm == null) {
                continue;
            }
            // A composition detail child is exercised through its master, not as its own list; a
            // cross-model projection has no local table or UI to drive.
            if ("MANAGE_DETAILS".equals(string(edm.get("layoutType"))) || "PROJECTION".equals(string(edm.get("type")))) {
                continue;
            }
            entities.add(entityManifest(entity, edm, model, context, edmEntities));
        }
        manifest.put("entities", entities);
        return manifest;
    }

    private static Map<String, Object> entityManifest(EntityIntent entity, Map<String, Object> edm, IntentModel model,
            IntentGenerationContext context, Map<String, Map<String, Object>> edmEntities) {
        Map<String, Object> out = new LinkedHashMap<>();
        String name = entity.getName();
        out.put("name", name);
        out.put("label", stringOr(edm.get("entityLabel"), IntentNaming.humanize(name)));
        out.put("labelPlural", stringOr(edm.get("menuLabel"), IntentNaming.pluralize(IntentNaming.humanize(name))));
        out.put("layout", layout(string(edm.get("layoutType")), "true".equals(string(edm.get("calendarView"))),
                "true".equals(string(edm.get("slotsView")))));
        // A composition child gets no power page of its own - the UI generator gives list and form
        // pages to PRIMARY (and SETTING) entities only - even when it carries a layout that is not
        // MANAGE_DETAILS (a child owning line items is a MANAGE_DOCUMENT, a Payslip under its
        // PayrollRun). It is still described, for its REST and personal flows, but with no route the
        // runner would open onto "Page not found" (dirigible #7545).
        if (!"DEPENDENT".equals(string(edm.get("type")))) {
            out.put("route", "#/" + name);
        }
        // list: (#7614) - the entity's list shows exactly these columns, in order, whatever each
        // field's own `major` says. Without it in the manifest the generic list flow asserts a header
        // per non-`major: false` field and fails on the first instance that holds a row (#7664).
        List<String> listColumns = csv(string(edm.get("listOrder")));
        if (!listColumns.isEmpty()) {
            out.put("list", listColumns);
        }
        out.put("navGroup", string(edm.get("perspectiveNavId")));
        out.put("api", "/" + sanitizeJavaIdentifier(string(edm.get("perspectiveName"))) + "/" + name + "Controller");
        out.put("table", string(edm.get("dataName")));

        // A hierarchical entity renders its list as a tree (role=treeitem, no table/columnheaders),
        // so the runner must branch on it.
        if (entity.getHierarchy() != null && !entity.getHierarchy()
                                                    .isBlank()) {
            out.put("hierarchy", true);
        }
        boolean multilingual = "true".equals(string(edm.get("multilingual")));
        if (multilingual) {
            out.put("multilingual", true);
            Map<String, Object> sample = multilingualSample(entity, model, context);
            if (sample != null) {
                out.put("multilingualSample", sample);
            }
        }
        if (hasSeed(model, name)) {
            out.put("expectSeedData", true);
        }
        List<String> deleteGuards = processDeleteGuards(edm);
        if (!deleteGuards.isEmpty()) {
            out.put("deleteGuardedByProcess", deleteGuards);
        }
        // personal (my) surface: a `personal: true` to-one relation makes the entity a personal
        // root - the generator emits an ADDITIONAL scoped <Entity>MyController whose contract the
        // runner's my flow drives: reads filtered to the identity-mapped user, the owner FK forced
        // server-side, sensitive fields stripped from the wire, foreign rows 404.
        for (RelationIntent relation : entity.getRelations()) {
            if (!relation.isPersonal()) {
                continue;
            }
            Map<String, Object> personal = new LinkedHashMap<>();
            personal.put("api", "/" + sanitizeJavaIdentifier(string(edm.get("perspectiveName"))) + "/" + name + "MyController");
            personal.put("owner", IntentNaming.pascalCase(relation.getName()));
            List<String> sensitive = new ArrayList<>();
            for (FieldIntent field : entity.getFields()) {
                if (field.isSensitive()) {
                    sensitive.add(IntentNaming.pascalCase(field.getName()));
                }
            }
            if (!sensitive.isEmpty()) {
                personal.put("sensitive", sensitive);
            }
            // UI parity (wave 2): what the personal PAGE renders, so the runner can drive it live -
            // the /my route, the layout family the page belongs to, and the relation columns the
            // personal list shows (each must resolve to its referenced label, never a raw FK id).
            personal.put("route", "#/my/" + name);
            String layout = "list";
            if (entity.isCalendar() || entity.isRange()) {
                layout = "calendar";
            } else if (entity.isSlots()) {
                layout = "slots";
            } else if (entity.isDocument()) {
                layout = entity.isChatItems() ? "document-chat" : "document";
            }
            personal.put("layout", layout);
            if ("list".equals(layout)) {
                List<String> fkColumns = new ArrayList<>();
                for (RelationIntent column : entity.getRelations()) {
                    boolean toOne = "manyToOne".equals(column.getKind()) || "oneToOne".equals(column.getKind());
                    if (toOne && !column.isPersonal()) {
                        fkColumns.add(IntentNaming.pascalCase(column.getName()));
                    }
                }
                if (!fkColumns.isEmpty()) {
                    personal.put("fkColumns", fkColumns);
                }
            }
            out.put("personal", personal);
            break;
        }
        // exactlyOne checks: exactly one of the named fields may be non-null - a sample record
        // filling all of them is rejected with 400, so the runner keeps only the first
        List<List<String>> exactlyOne = new ArrayList<>();
        // compare checks: the record's own field must stand in a relation to a second value - another
        // of its fields, or a literal (#7338). The sample values are per-type constants, so two dates
        // come out EQUAL and a strict comparison (gt/lt/ne) would reject the sample record with 400 -
        // and a sample quantity of 1 fails `gt 10` just as surely. The runner derives the left operand
        // from whichever right-hand side the check names.
        List<Map<String, Object>> compare = new ArrayList<>();
        // agree checks: two to-one relations whose targets must point at the same onProperty - the
        // first row of each target is an arbitrary pair, so the runner picks rows that agree
        List<Map<String, Object>> agree = new ArrayList<>();
        for (CheckIntent check : entity.getChecks() == null ? List.<CheckIntent>of() : entity.getChecks()) {
            if ("exactlyOne".equals(check.getKind()) && check.getFields() != null && !check.getFields()
                                                                                           .isEmpty()) {
                exactlyOne.add(check.getFields()
                                    .stream()
                                    .map(IntentNaming::pascalCase)
                                    .toList());
            }
            if ("compare".equals(check.getKind()) && check.getField() != null && check.getOp() != null
                    && (check.getThan() != null || check.getValue() != null)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("field", IntentNaming.pascalCase(check.getField()));
                entry.put("op", check.getOp()
                                     .trim()
                                     .toLowerCase(java.util.Locale.ROOT));
                if (check.getThan() != null) {
                    entry.put("than", IntentNaming.pascalCase(check.getThan()));
                } else {
                    entry.put("value", check.getValue());
                }
                compare.add(entry);
            }
            if ("agree".equals(check.getKind()) && check.getRelations() != null && check.getRelations()
                                                                                        .size() > 1
                    && check.getOnProperty() != null) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("relations", check.getRelations()
                                            .stream()
                                            .map(IntentNaming::pascalCase)
                                            .toList());
                entry.put("onProperty", IntentNaming.pascalCase(check.getOnProperty()));
                agree.add(entry);
            }
        }
        if (!exactlyOne.isEmpty()) {
            out.put("exactlyOne", exactlyOne);
        }
        if (!compare.isEmpty()) {
            out.put("compare", compare);
        }
        if (!agree.isEmpty()) {
            out.put("agree", agree);
        }
        // composite business keys: a second row carrying the same combination is refused with 409, and
        // the first seeded row of each relation is the combination a seed most likely already holds -
        // the runner chooses a combination no live row carries (dirigible #7545)
        List<List<String>> uniqueKeys = entity.getUnique()
                                              .stream()
                                              .filter(unique -> !unique.getFields()
                                                                       .isEmpty())
                                              .map(unique -> unique.getFields()
                                                                   .stream()
                                                                   .map(IntentNaming::pascalCase)
                                                                   .toList())
                                              .toList();
        if (!uniqueKeys.isEmpty()) {
            out.put("uniqueKeys", uniqueKeys);
        }
        out.put("fields", fields(entity, edm));
        List<Map<String, Object>> relations = relations(entity, model, context, edmEntities);
        if (!relations.isEmpty()) {
            out.put("relations", relations);
        }
        return out;
    }

    /**
     * Non-PK, non-generated fields, with the metadata the runner needs to fill and assert them. The two
     * attributes the generated form's own behaviour decides - whether the field has an editable input
     * at all, and the shape a value must have to be accepted - are read back off the {@code .model}
     * property rather than re-derived here, so the manifest cannot disagree with the app it describes.
     */
    private static List<Map<String, Object>> fields(EntityIntent entity, Map<String, Object> edm) {
        Map<String, Map<String, Object>> properties = propertiesByName(edm);
        List<Map<String, Object>> fields = new ArrayList<>();
        for (FieldIntent field : entity.getFields()) {
            if (field.isPrimaryKey() || field.isGenerated()) {
                continue;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", IntentNaming.pascalCase(field.getName()));
            out.put("type", logicalType(field.getType()));
            if (field.isRequired()) {
                out.put("required", true);
            }
            if (field.isUnique()) {
                out.put("unique", true);
            }
            if (field.getLength() != null) {
                out.put("length", field.getLength());
            }
            Map<String, Object> property = properties.get(String.valueOf(out.get("name")));
            // Read-only must mirror the generated form exactly, or the runner waits forever on an
            // input that is not there: an author-marked field and a uuid render in the read-only
            // details block (no #f_<Name> input), a calculated field renders as a non-editable
            // input, an aggregate renders in the document totals footer, and a dependsOn field is
            // auto-populated by its trigger relation's watcher (the runner must not fill it). The
            // EDM's own `isReadOnlyProperty` joins them so a platform-owned field is covered by the
            // one decision that already made it read-only - a `number:` field above all (dirigible
            // #7411): the DAO stamps and then PRESERVES that column, so a runner that wrote it read
            // its own value back unchanged and reported a correct module as broken.
            if (field.isReadOnly() || "uuid".equalsIgnoreCase(field.getType()) || field.isCalculated() || field.isAggregate()
                    || field.getDependsOn() != null || (property != null && "true".equals(string(property.get("isReadOnlyProperty"))))) {
                out.put("readOnly", true);
            }
            // The input-format regex the generated controller rejects a non-matching value with (an
            // authored `pattern:`, or the canonical address regex behind `format: email`), so the
            // runner's sample generator can produce a value of that shape instead of a 400. Strings
            // only: on a numeric property the same attribute is the DISPLAY format, not a guard.
            if ("string".equals(out.get("type")) && property != null && !string(property.get("widgetPattern")).isBlank()) {
                out.put("pattern", string(property.get("widgetPattern")));
            }
            out.put("major", field.isMajor());
            fields.add(out);
        }
        return fields;
    }

    /**
     * The user-pickable to-one relations rendered as dropdowns. A cross-model relation's target lives
     * in another module — its option rows are resolved through an {@code apiAbsolute} controller URL
     * (the same owner-project coordinates the generated dropdown uses), so the runner can fill the
     * required FK without the target being in this manifest. A {@code function: EntityStatus} relation
     * is marked {@code entityStatus} — it renders as a status pill / is excluded from the editable
     * inputs by the form templates, and its value comes from the {@code init:} DB default, so the
     * runner must neither pick nor post it.
     */
    /**
     * The terms of this entity's {@code forbidWhen} checks that reach through the given relation (issue
     * #7667). A term reads {@code <Relation>.<Property> ==|!= <literal>} and refuses the write while it
     * holds, so a runner building the target must land OUTSIDE every one of them. A rule scoped by
     * {@code verbs:} carries them (#7710); one without covers create, update and delete.
     *
     * @param entity the record the checks are declared on
     * @param relation the to-one being described
     * @return the forbidding conditions, in authored order, each with the check's own message
     */
    private static List<Map<String, Object>> forbiddenTarget(EntityIntent entity, RelationIntent relation) {
        List<Map<String, Object>> forbidden = new ArrayList<>();
        for (CheckIntent check : entity.getChecks() == null ? List.<CheckIntent>of() : entity.getChecks()) {
            if (!"forbidWhen".equals(check.getKind())) {
                continue;
            }
            for (String authored : CheckSupport.terms(check.getWhen())) {
                CheckSupport.Comparison comparison = CheckSupport.parse(authored);
                int dot = comparison == null ? -1
                        : comparison.property()
                                    .indexOf('.');
                if (dot <= 0 || !comparison.property()
                                           .substring(0, dot)
                                           .equalsIgnoreCase(relation.getName())) {
                    continue; // a term about this record's own fields, or about another relation
                }
                Map<String, Object> condition = new LinkedHashMap<>();
                condition.put("by", IntentNaming.pascalCase(comparison.property()
                                                                      .substring(dot + 1)));
                condition.put("op", comparison.equal() ? "eq" : "ne");
                condition.put("value", literal(CheckSupport.unquote(comparison.literal())));
                if (check.getMessage() != null && !check.getMessage()
                                                        .isBlank()) {
                    condition.put("message", check.getMessage());
                }
                // A scoped rule (#7710) states the verbs it refuses, so a flow expects the others - a
                // delete of a row whose parent is PAID, for "no new allocation onto a PAID invoice" - to
                // succeed. Absent = create, update and delete (#7372).
                if (check.getVerbs() != null) {
                    condition.put("verbs", java.util.stream.Stream.of("create", "update", "delete")
                                                                  .filter(check::coversVerb)
                                                                  .toList());
                }
                forbidden.add(condition);
            }
        }
        return forbidden;
    }

    /** A manifest literal: a number stays a number, so the runner compares it with the stored id. */
    private static Object literal(String authored) {
        try {
            return Long.valueOf(authored.trim());
        } catch (NumberFormatException notANumber) {
            return authored;
        }
    }

    /** The comma-separated attribute as a list, empty when the attribute is absent. */
    private static List<String> csv(String value) {
        List<String> parts = new ArrayList<>();
        for (String part : value == null ? new String[0] : value.split(",")) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        return parts;
    }

    private static List<Map<String, Object>> relations(EntityIntent entity, IntentModel model, IntentGenerationContext context,
            Map<String, Map<String, Object>> edmEntities) {
        Map<String, UsesIntent> usesByAlias = new LinkedHashMap<>();
        for (UsesIntent uses : model.getUses()) {
            if (uses.getModel() != null) {
                usesByAlias.put(uses.getModel(), uses);
            }
        }
        Map<String, Map<String, Object>> properties = propertiesByName(edmEntities.getOrDefault(entity.getName(), Map.of()));
        List<Map<String, Object>> relations = new ArrayList<>();
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (!toOne || relation.getTo() == null) {
                continue;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", IntentNaming.pascalCase(relation.getName()));
            out.put("kind", relation.getKind());
            out.put("to", relation.getTo());
            if (relation.isRequired() || relation.isComposition()) {
                out.put("required", true);
            }
            out.put("widget", "dropdown");
            if (relation.isEntityStatus()) {
                out.put("entityStatus", true);
            }
            // The form renders a relation read-only (a disabled combobox, or no input at all) by the
            // same rule as a field, so the runner must not try to pick it (dirigible #7554): an
            // update-time recompute (calculatedActionOnUpdate, #6696) or a platform-owned FK. A
            // create-only action is a server-side DEFAULT and stays editable.
            if (notBlank(relation.getCalculatedActionOnUpdate()) || rendersReadOnly(properties.get(String.valueOf(out.get("name"))))) {
                out.put("readOnly", true);
            }
            // dependsOn cascade: the option list narrows to target rows whose filterBy equals the
            // trigger sibling's value - the runner must pick MATCHING samples (the dependent row
            // first, then its FK as the trigger's sample), not independent first rows.
            if (relation.getDependsOn() != null) {
                Map<String, Object> dependsOn = new LinkedHashMap<>();
                dependsOn.put("relation", IntentNaming.pascalCase(relation.getDependsOn()
                                                                          .getRelation()));
                if (relation.getDependsOn()
                            .getFilterBy() != null) {
                    dependsOn.put("filterBy", IntentNaming.pascalCase(relation.getDependsOn()
                                                                              .getFilterBy()));
                }
                out.put("dependsOn", dependsOn);
            }
            // where: static option filter - only matching target rows are offered as options
            if (relation.getWhere() != null && relation.getWhere()
                                                       .size() == 1) {
                Map.Entry<String, Object> condition = relation.getWhere()
                                                              .entrySet()
                                                              .iterator()
                                                              .next();
                Map<String, Object> where = new LinkedHashMap<>();
                where.put("by", IntentNaming.pascalCase(condition.getKey()));
                where.put("value", condition.getValue());
                out.put("where", where);
            }
            // pickable: (#7496) - a picker rule over the TARGET's rows. Without it the runner samples
            // the first target row, the picker never offers it, and the record never saves (#7663).
            // The terms carry the manifest's own vocabulary (`by`, as `where` does), plus whether a
            // failing row is hidden outright or listed disabled.
            if (relation.getPickable() != null) {
                Map<String, Object> pickable = new LinkedHashMap<>();
                List<Map<String, Object>> terms = new ArrayList<>();
                for (String authored : CheckSupport.terms(relation.getPickable()
                                                                  .getWhen())) {
                    PickableSupport.Term term = PickableSupport.parse(authored);
                    if (term == null) {
                        continue; // the parser refused it; nothing to describe
                    }
                    Map<String, Object> condition = new LinkedHashMap<>();
                    condition.put("by", IntentNaming.pascalCase(term.property()));
                    condition.put("op", term.op());
                    if (!term.presence()) {
                        condition.put("value", literal(CheckSupport.unquote(term.literal())));
                    }
                    terms.add(condition);
                }
                if (!terms.isEmpty()) {
                    pickable.put("when", terms);
                    pickable.put("hide", PickableSupport.hides(relation.getPickable()));
                    out.put("pickable", pickable);
                }
            }
            // A state the TARGET must not be in for this record to be accepted at all (#7667): the
            // child's own `forbidWhen` reaching one hop through this relation - "a credit note may
            // only correct an issued invoice". The picker rule above is the UI half of the same thing
            // and is often absent; this one is what the REST flow needs, since the create is refused
            // with 400 whatever the form offered.
            List<Map<String, Object>> forbidden = forbiddenTarget(entity, relation);
            if (!forbidden.isEmpty()) {
                out.put("forbiddenTarget", forbidden);
            }
            if (relation.isCrossModel()) {
                UsesIntent uses = usesByAlias.get(relation.getModel());
                if (uses == null) {
                    continue;
                }
                CrossModelSupport.TargetInfo info;
                try {
                    info = CrossModelSupport.resolve(context, uses, relation.getTo());
                } catch (RuntimeException ex) {
                    // the EDM generator (order 200) fails loudly for a truly unresolvable target;
                    // reaching here means a degraded context - omit the relation rather than emit a
                    // guessed URL
                    LOGGER.warn("Omitting cross-model relation [{}] of [{}] from the app-test manifest - target unresolved",
                            LoggedValue.of(relation.getName()), LoggedValue.of(entity.getName()), ex);
                    continue;
                }
                out.put("crossModel", true);
                out.put("apiAbsolute", "/services/java/" + uses.resolveProject() + "/gen/" + sanitizeJavaIdentifier(uses.getModel())
                        + "/api/" + sanitizeJavaIdentifier(info.perspectiveName()) + "/" + relation.getTo() + "Controller");
                out.put("labelFrom", info.labelField());
                // leafOnly: the generated validation rejects a non-leaf target - the runner must
                // pick a row no other row references via the target's hierarchy edge
                if (relation.isLeafOnly() && info.hierarchyProperty() != null) {
                    out.put("leafOnly", Map.of("hierarchyProperty", info.hierarchyProperty()));
                }
            } else {
                // relative controller path of the same-model target - resolvable even when the
                // target is a composition detail (excluded from this manifest's entities list)
                Map<String, Object> targetEdm = edmEntities.get(relation.getTo());
                if (targetEdm != null) {
                    out.put("api",
                            "/" + sanitizeJavaIdentifier(string(targetEdm.get("perspectiveName"))) + "/" + relation.getTo() + "Controller");
                }
                out.put("labelFrom", labelFieldOf(relation.getTo(), model));
                if (relation.isLeafOnly()) {
                    for (EntityIntent target : model.getEntities()) {
                        if (relation.getTo()
                                    .equals(target.getName())
                                && target.getHierarchy() != null) {
                            out.put("leafOnly", Map.of("hierarchyProperty", IntentNaming.pascalCase(target.getHierarchy())));
                        }
                    }
                }
            }
            relations.add(out);
        }
        return relations;
    }

    /**
     * The label property of a relation target: its {@code name} field PascalCased, else {@code Name}.
     */
    private static String labelFieldOf(String targetName, IntentModel model) {
        for (EntityIntent entity : model.getEntities()) {
            if (!targetName.equals(entity.getName())) {
                continue;
            }
            for (FieldIntent field : entity.getFields()) {
                if ("name".equalsIgnoreCase(field.getName())) {
                    return IntentNaming.pascalCase(field.getName());
                }
            }
            for (FieldIntent field : entity.getFields()) {
                if (isStringType(field.getType()) && !field.isPrimaryKey()) {
                    return IntentNaming.pascalCase(field.getName());
                }
            }
        }
        return "Name";
    }

    /**
     * A concrete {@code {language, base, translated}} sample for the multilingual read overlay, derived
     * from the entity's inline base + {@code language:} seeds — or null when it cannot be derived, in
     * which case the runner skips the translation assertion. File-backed seeds carry no inline rows and
     * are skipped quietly; inline base and translated seeds that yield no sample are reported as a
     * generation issue (dirigible #7534), since the dropped assertion is otherwise invisible.
     */
    private static Map<String, Object> multilingualSample(EntityIntent entity, IntentModel model, IntentGenerationContext context) {
        SeedIntent base = null;
        SeedIntent translated = null;
        for (SeedIntent seed : model.getSeeds()) {
            if (!entity.getName()
                       .equals(seed.getEntity())
                    || seed.getRows() == null || seed.getRows()
                                                     .isEmpty()) {
                continue;
            }
            if (seed.isLanguageSeed()) {
                if (translated == null) {
                    translated = seed;
                }
            } else if (base == null) {
                base = seed;
            }
        }
        if (base == null || translated == null) {
            return null;
        }
        Map<String, Object> baseRow = base.getRows()
                                          .get(0);
        Object baseId = baseRow.get("id");
        Map<String, Object> translatedRow = null;
        for (Map<String, Object> row : translated.getRows()) {
            if (baseId != null && baseId.equals(row.get("id"))) {
                translatedRow = row;
                break;
            }
        }
        if (translatedRow == null) {
            warnNoSample(context, entity, translated, "no row of seed [" + translated.getName() + "] has the id [" + baseId
                    + "] of the first row of seed [" + base.getName() + "]");
            return null;
        }
        String key = firstTranslatableKey(entity, baseRow, translatedRow);
        if (key == null) {
            warnNoSample(context, entity, translated,
                    "no string field with a language column is set in both the first row of seed [" + base.getName()
                            + "] and its translated row in seed [" + translated.getName() + "] (id [" + baseId
                            + "]) - mark a key field `translatable: false` or translate a label field");
            return null;
        }
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("language", translated.getLanguage());
        sample.put("base", baseRow.get(key));
        sample.put("translated", translatedRow.get(key));
        return sample;
    }

    private static void warnNoSample(IntentGenerationContext context, EntityIntent entity, SeedIntent translated, String reason) {
        String issue = "multilingual entity [" + entity.getName() + "] gets no translation sample in the app-test manifest, so its ["
                + translated.getLanguage() + "] translation is not asserted: " + reason;
        if (context != null) {
            context.addIssue(issue);
        }
    }

    /**
     * The seeded property the translation sample is taken from: the first string field that actually
     * HAS a language column and is set in BOTH the base row and its translated row. A field marked
     * {@code translatable: false} is a key rather than a label and no translation seed may set it; a
     * translatable field the translated row leaves out (an ISO code not yet marked
     * {@code translatable: false}) has no translated value to assert. Choosing either would drop the
     * whole translation assertion from the generated runner (dirigible #7534).
     */
    private static String firstTranslatableKey(EntityIntent entity, Map<String, Object> baseRow, Map<String, Object> translatedRow) {
        for (FieldIntent field : entity.getFields()) {
            if (field.hasLanguageColumn() && isStringType(field.getType()) && baseRow.get(field.getName()) != null
                    && translatedRow.get(field.getName()) != null) {
                return field.getName();
            }
        }
        return null;
    }

    private static boolean hasSeed(IntentModel model, String entityName) {
        for (SeedIntent seed : model.getSeeds()) {
            if (entityName.equals(seed.getEntity()) && !seed.isLanguageSeed()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> languages(IntentModel model) {
        List<String> languages = model.getLanguages();
        return (languages == null || languages.isEmpty()) ? List.of("en") : languages;
    }

    /** The manifest's shared primary-key property name, taken from the model's PK column. */
    private static String idProperty(Map<String, Map<String, Object>> edmEntities) {
        for (Map<String, Object> entity : edmEntities.values()) {
            Object properties = entity.get("properties");
            if (!(properties instanceof List<?> list)) {
                continue;
            }
            for (Object property : list) {
                if (property instanceof Map<?, ?> map && "true".equals(String.valueOf(map.get("dataPrimaryKey")))) {
                    String name = String.valueOf(map.get("name"));
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
        }
        return "Id";
    }

    /**
     * The runner's layout token from the EDM layout type. A calendar or slots view keeps the entity's
     * layout intact but takes over its landing route (the layout's list moves to
     * {@code /<Entity>/list}), so the token the runner drives at {@code #/<Entity>} is that view - it
     * must not expect columns/rows there.
     */
    private static String layout(String layoutType, boolean calendarView, boolean slotsView) {
        if (calendarView) {
            return "calendar";
        }
        if (slotsView) {
            return "slots";
        }
        return switch (layoutType == null ? "" : layoutType) {
            case "MANAGE_DOCUMENT" -> "document";
            default -> "manage-list";
        };
    }

    /** Maps an intent logical field type to the small set the runner's sample generator understands. */
    private static String logicalType(String type) {
        return switch (type == null ? "string" : type) {
            case "text", "uuid" -> "string";
            case "int", "long" -> "integer";
            case "double" -> "decimal";
            default -> type;
        };
    }

    private static boolean isStringType(String type) {
        return "string".equals(type) || "text".equals(type) || "uuid".equals(type);
    }

    /**
     * Reads back the {@code .model} written by the EDM generator and indexes its entities by name.
     * Returns an empty map when the model is absent or unreadable (nothing to describe).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> readModelEntities(IntentGenerationContext context, String baseName) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        try {
            IRepository repository = context.getRepository();
            if (repository == null || context.getProjectRoot() == null) {
                return byName; // no repository/project to read back from (a dry run over an unsaved proposal)
            }
            IResource resource = repository.getResource(context.getProjectRoot() + "/" + baseName + ".model");
            if (!resource.exists()) {
                return byName;
            }
            String json = new String(resource.getContent(), StandardCharsets.UTF_8);
            Map<String, Object> document = GSON.fromJson(json, Map.class);
            Object modelNode = document.get("model");
            Map<String, Object> modelMap = (modelNode instanceof Map) ? (Map<String, Object>) modelNode : document;
            Object entities = modelMap.get("entities");
            if (entities instanceof List<?> list) {
                for (Object entity : list) {
                    if (entity instanceof Map<?, ?> map) {
                        Object name = map.get("name");
                        if (name != null) {
                            byName.put(String.valueOf(name), (Map<String, Object>) map);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read the .model for the app-test manifest of [{}]; skipping", LoggedValue.of(baseName), e);
        }
        return byName;
    }

    /** Mirrors the template engine's {@code sanitizeJavaIdentifier} so REST paths match the backend. */
    private static String sanitizeJavaIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return "_";
        }
        String sanitized = name.toLowerCase()
                               .replaceAll("[^a-z0-9_]", "_");
        return Character.isDigit(sanitized.charAt(0)) ? "_" + sanitized : sanitized;
    }

    /**
     * The labels of the processes whose {@code whenDeleted: refuse} guards this entity's REST delete
     * (dirigible #7074) - the {@code .model}'s {@code processDeleteGuards} attribute, a comma-separated
     * list of {@code <Process>:<Process label>} pairs. The runner must expect the delete of such a
     * record to be REFUSED while its instance runs, instead of reporting the guard doing its job as a
     * failure (dirigible #7411).
     */
    private static List<String> processDeleteGuards(Map<String, Object> edm) {
        List<String> labels = new ArrayList<>();
        String guards = string(edm.get("processDeleteGuards"));
        for (String guard : guards.split(",")) {
            String label = guard.contains(":") ? guard.substring(guard.indexOf(':') + 1) : guard;
            if (!label.isBlank()) {
                labels.add(label.trim());
            }
        }
        return labels;
    }

    /** The {@code .model} entity's properties indexed by their PascalCase name. */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> propertiesByName(Map<String, Object> edm) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        if (edm.get("properties") instanceof List<?> list) {
            for (Object property : list) {
                if (property instanceof Map<?, ?> map && map.get("name") != null) {
                    byName.put(String.valueOf(map.get("name")), (Map<String, Object>) map);
                }
            }
        }
        return byName;
    }

    /**
     * Whether the generated form renders this {@code .model} property without an editable input - the
     * exact condition of the Harmonia form templates: {@code isReadOnlyProperty}, or a calculated
     * property that is DERIVED (a create/update expression or an update-time action).
     */
    private static boolean rendersReadOnly(Map<String, Object> property) {
        if (property == null) {
            return false;
        }
        if ("true".equals(string(property.get("isReadOnlyProperty")))) {
            return true;
        }
        return "true".equals(string(property.get("isCalculatedProperty")))
                && (notBlank(string(property.get("calculatedPropertyExpressionCreate")))
                        || notBlank(string(property.get("calculatedPropertyExpressionUpdate")))
                        || notBlank(string(property.get("calculatedActionOnUpdate"))));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String stringOr(Object value, String fallback) {
        String string = string(value);
        return string.isBlank() ? fallback : string;
    }
}
