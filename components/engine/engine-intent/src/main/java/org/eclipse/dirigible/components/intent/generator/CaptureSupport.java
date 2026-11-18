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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.LoggedValue;
import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.model.CaptureFieldIntent;
import org.eclipse.dirigible.components.intent.model.CaptureIntent;
import org.eclipse.dirigible.components.intent.model.CaptureItemsIntent;
import org.eclipse.dirigible.components.intent.model.CaptureQrIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.RollupIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code captures:} construct - a scanned or photographed document a user uploads, read by a
 * vision model into a new record a person then reviews - shared by the parser, which validates it,
 * and the glue generator, which plans the generated {@code <Name>Capture} class from it. What the
 * parser accepts and what the generator emits are the same grammar, so both read it here.
 *
 * <p>
 * A capture differs from a file import in what a miss means. An import is exact: one unreadable
 * record refuses the file. A capture reads print, so a value the model could not read, a relation
 * nothing matched or a total that differs is not a failure - the record is still created and the
 * miss is a flag for the person reviewing it. Only what would make the record unsavable refuses a
 * capture.
 */
public final class CaptureSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureSupport.class);

    private static final String DEFAULT_QR_SEPARATOR = "*";

    private CaptureSupport() {}

    /**
     * The caption of a capture's button on the entity's list.
     *
     * @param capture the capture
     * @return the authored {@code label:}, else the humanized name
     */
    public static String label(CaptureIntent capture) {
        return IntentNaming.customActionLabel(capture.getName(), capture.getLabel());
    }

    /**
     * The caption of the per-record button that shows what a capture flagged.
     *
     * @param capture the capture
     * @return the caption
     */
    public static String reviewLabel(CaptureIntent capture) {
        return label(capture) + " review";
    }

    /**
     * The entities some capture creates records of - each gets a sibling capture ledger table.
     *
     * @param model the parsed intent
     * @return the entity names, in declaration order
     */
    public static Set<String> targets(IntentModel model) {
        Set<String> targets = new LinkedHashSet<>();
        for (CaptureIntent declared : model.getCaptures()) {
            if (declared.getInto() != null) {
                targets.add(declared.getInto());
            }
        }
        return targets;
    }

    /**
     * Validate every declared capture. A capture must name a local entity it can create a record of on
     * its own and read real fields and relations of it, so a mistake fails at Generate rather than on
     * the first uploaded document.
     *
     * @param model the parsed intent
     * @param issues the issue sink
     */
    public static void validate(IntentModel model, List<String> issues) {
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        Set<String> names = new HashSet<>();
        Set<String> classNames = new HashSet<>();
        for (CaptureIntent declared : model.getCaptures()) {
            String name = declared.getName();
            if (name == null || name.isBlank()) {
                issues.add("capture has no name");
                continue;
            }
            String subject = "capture [" + name + "]";
            if (!names.add(name)) {
                issues.add("duplicate capture [" + name + "]");
            } else if (!classNames.add(IntentNaming.pascalCase(name))) {
                issues.add(subject + " generates the class " + IntentNaming.pascalCase(name)
                        + "Capture, which another capture's name generates too - rename one");
            }
            EntityIntent into = declared.getInto() == null ? null : byName.get(declared.getInto());
            if (into == null) {
                issues.add(subject + " captures into unknown entity [" + declared.getInto() + "]");
                continue;
            }
            validateTarget(into, compositionParents, subject, issues);
            Set<String> qrParts = validateQr(declared.getQr(), subject, issues);
            if (declared.getFields()
                        .isEmpty()) {
                issues.add(subject + " reads no fields - name at least one field of [" + into.getName() + "] under fields:");
            }
            validateFields(declared.getFields(), into, null, qrParts, true, model, byName, subject, issues);
            validateItems(declared.getItems(), into, model, byName, subject, issues);
            validateAttachment(declared.getAttachment(), into, byName, subject, issues);
        }
    }

    /**
     * A capture creates one record of its target and, optionally, the rows it owns, so the target must
     * be a table of this model a row can be inserted into on its own.
     */
    private static void validateTarget(EntityIntent into, Map<String, String> compositionParents, String subject, List<String> issues) {
        if (into.getExtend() != null) {
            issues.add(subject + " captures into [" + into.getName() + "], which extends another model's entity and owns no table");
            return;
        }
        if (compositionParents.containsKey(into.getName())) {
            issues.add(subject + " captures into [" + into.getName() + "], a composition child - its rows are created under a ["
                    + compositionParents.get(into.getName()) + "]; capture that, and read this as its items:");
            return;
        }
        if (into.isFileChild()) {
            issues.add(subject + " captures into [" + into.getName() + "], which holds files, not a document's fields");
        }
        if (IntentEntities.primaryKeyOf(into) == null) {
            issues.add(subject + " captures into [" + into.getName() + "], which declares no primary key");
        }
    }

    /** The declared QR part names, empty when the capture decodes none. */
    private static Set<String> validateQr(CaptureQrIntent qr, String subject, List<String> issues) {
        Set<String> parts = new LinkedHashSet<>();
        if (qr == null) {
            return parts;
        }
        String separator = qr.getSeparator();
        if (separator != null && separator.isEmpty()) {
            issues.add(subject + " qr separator is empty - name the text the code's parts are separated by");
        }
        if (qr.getParts()
              .isEmpty()) {
            issues.add(
                    subject + " qr names no parts - list what the code carries, in its order, e.g. [device, receipt, date, time, total]");
        }
        for (String part : qr.getParts()) {
            if (part == null || part.isBlank()) {
                issues.add(subject + " qr names a blank part");
            } else if (!parts.add(part)) {
                issues.add(subject + " qr names the part [" + part + "] twice");
            }
        }
        return parts;
    }

    private static void validateFields(List<CaptureFieldIntent> fields, EntityIntent entity, String backReference, Set<String> qrParts,
            boolean header, IntentModel model, Map<String, EntityIntent> byName, String subject, List<String> issues) {
        Set<String> seen = new HashSet<>();
        for (CaptureFieldIntent declared : fields) {
            String name = declared.getField();
            if (name == null || name.isBlank()) {
                issues.add(subject + " reads a field without a name");
                continue;
            }
            String where = subject + " field [" + name + "]";
            if (!seen.add(name)) {
                issues.add(where + " is read twice");
                continue;
            }
            FieldIntent field = fieldOf(entity, name);
            RelationIntent relation = toOneRelation(entity, name);
            if (field == null && relation == null) {
                issues.add(where + " is not a field or a to-one relation of [" + entity.getName() + "]");
                continue;
            }
            if (relation != null) {
                validateRelation(declared, relation, backReference, model, byName, where, issues);
                continue;
            }
            if (!declared.getMatch()
                         .isEmpty()) {
                issues.add(where + " declares match:, which resolves a relation - [" + name + "] is a field");
            }
            if (field.isPrimaryKey()) {
                issues.add(where + " is the primary key of [" + entity.getName() + "], which is generated on insert");
            }
            if (captureType(field.getType()) == null) {
                issues.add(where + " has type [" + field.getType() + "], which a document does not carry - read a string, integer,"
                        + " decimal, date or boolean field");
            }
            validateWritten(declared, field, header, model, where, issues);
            String qr = declared.getQr();
            if (qr != null) {
                if (!header) {
                    issues.add(where + " declares qr:, but the code describes the document, not one of its lines");
                } else if (declared.isCompare()) {
                    issues.add(where + " declares both qr: and compare: - a compared field is not written, so the code has nothing"
                            + " to correct");
                } else if (!qrParts.contains(qr)) {
                    issues.add(where + " checks against the qr part [" + qr + "], which the capture's qr: does not name");
                }
            }
        }
    }

    /**
     * A read field is written onto the new record unless it is {@code compare:} - so it must be one a
     * caller can set, and a compared one must be one the entity fills itself.
     */
    private static void validateWritten(CaptureFieldIntent declared, FieldIntent field, boolean header, IntentModel model, String where,
            List<String> issues) {
        boolean derived = field.isCalculated() || field.isAggregate() || field.getNumber() != null || isRolledUp(model, field.getName());
        if (declared.isCompare()) {
            if (!header) {
                issues.add(where + " declares compare:, which checks the saved document - a line has nothing to compare with");
            } else if (!derived) {
                issues.add(where + " declares compare:, but nothing calculates it - there would be nothing to compare the document"
                        + " with; drop compare: to write it");
            }
            return;
        }
        if (derived) {
            issues.add(where + " is calculated by the entity, so a capture cannot write it - add compare: true to check the document's"
                    + " value against it instead");
        }
    }

    private static void validateRelation(CaptureFieldIntent declared, RelationIntent relation, String backReference, IntentModel model,
            Map<String, EntityIntent> byName, String where, List<String> issues) {
        if (relation.getName()
                    .equals(backReference)) {
            issues.add(where + " is the line's back-reference to the document, which is filled from the saved record");
            return;
        }
        if (relation.isEntityStatus()) {
            issues.add(where + " is the entity's status - a captured record starts in its initial status");
            return;
        }
        if (declared.getQr() != null || declared.isCompare()) {
            issues.add(where + " is a relation - qr: and compare: apply to fields");
        }
        if (declared.getMatch()
                    .isEmpty()) {
            issues.add(where + " is a relation to [" + relation.getTo() + "] - name under match: the fields of it the document carries"
                    + " (a registration number, a code)");
            return;
        }
        boolean crossModel = relation.getModel() != null && !relation.getModel()
                                                                     .isBlank();
        if (crossModel) {
            if (findUses(model, relation.getModel()) == null) {
                issues.add(where + " matches [" + relation.getTo() + "] of model [" + relation.getModel() + "], which uses: does not"
                        + " declare");
            }
            return; // the owner model's properties are checked at Generate, where it can be read
        }
        EntityIntent target = byName.get(relation.getTo());
        if (target == null) {
            return; // the dangling relation is reported by the relation's own validation
        }
        Set<String> keys = new HashSet<>();
        for (String key : declared.getMatch()) {
            FieldIntent by = key == null ? null : fieldOf(target, key);
            if (by == null) {
                issues.add(where + " matches by [" + key + "], which is not a field of [" + target.getName() + "]");
            } else if (!keys.add(key)) {
                issues.add(where + " matches by [" + key + "] twice");
            } else if (!isText(by.getType())) {
                issues.add(where + " matches by [" + key + "] of type [" + by.getType() + "] - match by a string field, the way a"
                        + " document prints a key");
            }
        }
    }

    private static void validateItems(CaptureItemsIntent items, EntityIntent into, IntentModel model, Map<String, EntityIntent> byName,
            String subject, List<String> issues) {
        if (items == null) {
            return;
        }
        String where = subject + " items";
        EntityIntent child = items.getInto() == null ? null : byName.get(items.getInto());
        if (child == null) {
            issues.add(where + " read into unknown entity [" + items.getInto() + "]");
            return;
        }
        String backReference = backReference(child, into.getName());
        if (backReference == null) {
            issues.add(where + " read into [" + child.getName() + "], which is not a composition child of [" + into.getName()
                    + "] - a capture creates only rows the record owns");
            return;
        }
        if (child.isFileChild()) {
            issues.add(where + " read into [" + child.getName() + "], which holds files - name it under attachment: instead");
        }
        if (items.getFields()
                 .isEmpty()) {
            issues.add(where + " read no fields - name at least one field of [" + child.getName() + "]");
        }
        validateFields(items.getFields(), child, backReference, Set.of(), false, model, byName, where, issues);
    }

    private static void validateAttachment(String attachment, EntityIntent into, Map<String, EntityIntent> byName, String subject,
            List<String> issues) {
        if (attachment == null) {
            return;
        }
        EntityIntent child = byName.get(attachment);
        if (child == null) {
            issues.add(subject + " keeps the file in unknown entity [" + attachment + "]");
            return;
        }
        if (!child.isAttachment()) {
            issues.add(subject + " keeps the file in [" + attachment + "], which is not a function: Attachment entity");
        }
        if (backReference(child, into.getName()) == null) {
            issues.add(subject + " keeps the file in [" + attachment + "], which is not a composition child of [" + into.getName() + "]");
        }
    }

    /**
     * Plan the generated capture classes - one {@code <Name>Capture} per declared capture, a controller
     * with the upload endpoint, the review endpoint and a bean method a job can call.
     *
     * @param model the parsed intent
     * @param byName all local entities by name
     * @param compositionParents composition-parent map
     * @param settings the project settings (the per-capture opt-out)
     * @param context the generation context
     * @return the glue descriptors
     */
    static List<Map<String, Object>> build(IntentModel model, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> captures = new ArrayList<>();
        if (model.getCaptures()
                 .isEmpty()) {
            return captures;
        }
        PermissionSupport.Gates gates = PermissionSupport.gates(model, settings);
        // The same project name the EDM generator prefixes the entity's convention roles with.
        String project = context.getProjectName();
        String projectName = project != null && !project.isBlank() ? project : IntentNaming.baseName(context);
        for (CaptureIntent declared : model.getCaptures()) {
            EntityIntent into = declared.getInto() == null ? null : byName.get(declared.getInto());
            if (declared.getName() == null || into == null) {
                continue;
            }
            if (!settings.shouldGenerate("captures", declared.getName())) {
                LOGGER.info("Settings opt-out: keeping existing capture class for [{}] (not generated)",
                        LoggedValue.of(declared.getName()));
                continue;
            }
            String entity = into.getName();
            String subject = "capture [" + declared.getName() + "]";
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", declared.getName());
            entry.put("nameLiteral", NotificationSupport.quote(declared.getName()));
            entry.put("className", IntentNaming.pascalCase(declared.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents, model));
            entry.put("keyProperty", IntentEntities.keyFieldName(into));
            entry.put("table", IntentNaming.tableName(context, entity));
            String rolePerspective = IntentEntities.compositionPerspective(entity, compositionParents);
            String writeGate = gates.writeGate(projectName, rolePerspective, entity);
            entry.put("roles", rolesLiteral(writeGate));
            // A reader of the record may see what its capture flagged; the write gate reads as well.
            entry.put("readRoles", rolesLiteral(gates.readGate(projectName, rolePerspective, entity) + "," + writeGate));
            String instructions = declared.getInstructions();
            entry.put("instructionsLiteral",
                    instructions == null || instructions.isBlank() ? "null" : NotificationSupport.quote(instructions));

            CaptureQrIntent qr = declared.getQr();
            boolean hasQr = qr != null && !qr.getParts()
                                             .isEmpty();
            entry.put("hasQr", hasQr);
            entry.put("qrSeparatorLiteral",
                    NotificationSupport.quote(!hasQr || qr.getSeparator() == null ? DEFAULT_QR_SEPARATOR : qr.getSeparator()));
            entry.put("qrPartsLiteral", hasQr ? literals(qr.getParts()) : "");

            Plan header = plan(declared.getFields(), into, "", model, byName, compositionParents, context, subject);
            entry.put("schemaFields", header.schema());
            entry.put("fields", header.values());
            entry.put("relations", header.relations());
            entry.put("compares", header.compares());
            entry.put("hasRelations", !header.relations()
                                             .isEmpty());
            entry.put("hasCompares", !header.compares()
                                            .isEmpty());

            CaptureItemsIntent items = declared.getItems();
            EntityIntent child = items == null || items.getInto() == null ? null : byName.get(items.getInto());
            String childBackReference = child == null ? null : backReference(child, entity);
            boolean hasItems = child != null && childBackReference != null;
            entry.put("hasItems", hasItems);
            if (hasItems) {
                Plan lines = plan(items.getFields(), child, "Item", model, byName, compositionParents, context, subject + " items");
                entry.put("schemaItems", lines.schema());
                entry.put("itemFields", lines.values());
                entry.put("itemRelations", lines.relations());
                entry.put("itemEntity", child.getName());
                entry.put("itemPerspective", IntentEntities.resolvePerspective(child.getName(), compositionParents, model));
                entry.put("itemBackRef", IntentNaming.pascalCase(childBackReference));
            } else {
                entry.put("schemaItems", List.of());
                entry.put("itemFields", List.of());
                entry.put("itemRelations", List.of());
            }

            EntityIntent attachment = declared.getAttachment() == null ? null : byName.get(declared.getAttachment());
            String attachmentBackReference = attachment == null ? null : backReference(attachment, entity);
            boolean hasAttachment = attachment != null && attachment.isAttachment() && attachmentBackReference != null;
            entry.put("hasAttachment", hasAttachment);
            if (hasAttachment) {
                entry.put("attachmentEntity", attachment.getName());
                entry.put("attachmentPerspective", IntentEntities.resolvePerspective(attachment.getName(), compositionParents, model));
                entry.put("attachmentBackRef", IntentNaming.pascalCase(attachmentBackReference));
            }
            captures.add(entry);
        }
        return captures;
    }

    /**
     * What one field list - the document's, or a line's - becomes.
     *
     * @param schema the fields the model is asked for, in order
     * @param values the fields written onto the row
     * @param relations the relations resolved by a natural key
     * @param compares the fields checked against the saved record
     */
    private record Plan(List<Map<String, Object>> schema, List<Map<String, Object>> values, List<Map<String, Object>> relations,
            List<Map<String, Object>> compares) {
    }

    private static Plan plan(List<CaptureFieldIntent> fields, EntityIntent entity, String localPrefix, IntentModel model,
            Map<String, EntityIntent> byName, Map<String, String> compositionParents, IntentGenerationContext context, String subject) {
        List<Map<String, Object>> schema = new ArrayList<>();
        List<Map<String, Object>> values = new ArrayList<>();
        List<Map<String, Object>> relations = new ArrayList<>();
        List<Map<String, Object>> compares = new ArrayList<>();
        for (CaptureFieldIntent declared : fields) {
            String name = declared.getField();
            if (name == null) {
                continue;
            }
            FieldIntent field = fieldOf(entity, name);
            RelationIntent relation = toOneRelation(entity, name);
            if (relation != null) {
                relations.add(relation(declared, relation, localPrefix, schema, model, byName, compositionParents, context, subject));
                continue;
            }
            if (field == null || captureType(field.getType()) == null) {
                continue;
            }
            String description = declared.getDescribe() != null && !declared.getDescribe()
                                                                            .isBlank() ? declared.getDescribe() : label(field);
            schema.add(schemaField(name, captureType(field.getType()), description));
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("name", name);
            value.put("nameLiteral", NotificationSupport.quote(name));
            value.put("local", "value" + localPrefix + IntentNaming.pascalCase(name));
            value.put("property", IntentNaming.pascalCase(name));
            value.put("conversion", conversion(field.getType()));
            value.put("decimal", "decimal".equals(conversion(field.getType())));
            // What a row cannot be inserted without: required, and nothing else supplies it.
            value.put("required", field.isRequired() && field.getDefaultValue() == null);
            value.put("hasQr", declared.getQr() != null);
            value.put("qrLiteral", declared.getQr() == null ? "null" : NotificationSupport.quote(declared.getQr()));
            if (declared.isCompare()) {
                compares.add(value);
            } else {
                values.add(value);
            }
        }
        return new Plan(schema, values, relations, compares);
    }

    /**
     * A relation resolved by natural keys: each key is one field the model reads, tried in the declared
     * order against the target's repository.
     */
    private static Map<String, Object> relation(CaptureFieldIntent declared, RelationIntent relation, String localPrefix,
            List<Map<String, Object>> schema, IntentModel model, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            IntentGenerationContext context, String subject) {
        String name = relation.getName();
        boolean crossModel = relation.getModel() != null && !relation.getModel()
                                                                     .isBlank();
        UsesIntent uses = crossModel ? findUses(model, relation.getModel()) : null;
        CrossModelSupport.TargetInfo target = uses == null ? null : CrossModelSupport.resolve(context, uses, relation.getTo());
        EntityIntent local = crossModel ? null : byName.get(relation.getTo());
        String described = declared.getDescribe() != null && !declared.getDescribe()
                                                                      .isBlank() ? declared.getDescribe() : IntentNaming.humanize(name);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("nameLiteral", NotificationSupport.quote(name));
        entry.put("local", "match" + localPrefix + IntentNaming.pascalCase(name));
        entry.put("property", IntentNaming.pascalCase(name));
        entry.put("required", relation.isRequired() && relation.getInit() == null && relation.getInitFlag() == null);
        entry.put("targetEntity", relation.getTo());
        entry.put("crossModel", crossModel);
        entry.put("targetModel", crossModel ? relation.getModel() : "");
        entry.put("targetPerspective",
                target != null ? target.perspectiveName() : IntentEntities.resolvePerspective(relation.getTo(), compositionParents, model));
        entry.put("targetKeyProperty", target != null ? target.keyField() : IntentEntities.keyFieldName(local));
        List<Map<String, Object>> keys = new ArrayList<>();
        for (String key : declared.getMatch()) {
            if (key == null) {
                continue;
            }
            String property = IntentNaming.pascalCase(key);
            if (target != null && target.propertyNames() != null && !target.propertyNames()
                                                                           .contains(property)) {
                throw new IntentValidationException(List.of(subject + " field [" + name + "] matches by [" + key
                        + "], which is not a property of [" + relation.getTo() + "] in its owner model"));
            }
            // One schema field per key, named after the relation so two relations may share a key name.
            String schemaName = name + "_" + key;
            schema.add(schemaField(schemaName, "STRING", "the " + IntentNaming.humanize(key)
                                                                              .toLowerCase(java.util.Locale.ROOT)
                    + " of " + described));
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("key", key);
            one.put("schemaNameLiteral", NotificationSupport.quote(schemaName));
            one.put("byProperty", property);
            keys.add(one);
        }
        entry.put("keys", keys);
        return entry;
    }

    private static Map<String, Object> schemaField(String name, String type, String description) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("nameLiteral", NotificationSupport.quote(name));
        field.put("type", type);
        field.put("descriptionLiteral", description == null || description.isBlank() ? "null" : NotificationSupport.quote(description));
        return field;
    }

    /**
     * Whether some roll-up maintains a field of that name - its total, or the balance it derives. Read
     * by name alone: the roll-up's parent may be reached through a relation of another model, and a
     * capture refused for writing a maintained total is the cheaper mistake to make than one accepted.
     */
    private static boolean isRolledUp(IntentModel model, String field) {
        for (RollupIntent rollup : model.getRollups()) {
            if (field.equals(rollup.getField()) || field.equals(rollup.getBalance())) {
                return true;
            }
        }
        return false;
    }

    /** How a field is named to the model when the capture does not describe it. */
    private static String label(FieldIntent field) {
        return field.getLabel() != null && !field.getLabel()
                                                 .isBlank() ? field.getLabel() : IntentNaming.humanize(field.getName());
    }

    /**
     * The {@code CaptureType} the model is asked for a field in, or {@code null} for a type a document
     * does not carry (a timestamp, a binary).
     */
    static String captureType(String type) {
        return switch (type == null ? "string" : type) {
            case "integer", "int", "long" -> "INTEGER";
            case "decimal", "double" -> "NUMBER";
            case "boolean" -> "BOOLEAN";
            case "date" -> "DATE";
            case "string", "text", "uuid", "month", "week" -> "STRING";
            default -> null;
        };
    }

    /** The {@code ImportValues} method converting the model's text to the field's type. */
    private static String conversion(String type) {
        return switch (type == null ? "string" : type) {
            case "integer", "int" -> "integer";
            case "long" -> "longInteger";
            // `double` is a DECIMAL column like `decimal`, so both properties are BigDecimal.
            case "decimal", "double" -> "decimal";
            case "boolean" -> "bool";
            case "date" -> "date";
            default -> "text";
        };
    }

    private static boolean isText(String type) {
        return type == null || "string".equals(type) || "text".equals(type) || "uuid".equals(type);
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

    /** The name of the composition relation of {@code child} pointing back at {@code master}. */
    private static String backReference(EntityIntent child, String master) {
        for (RelationIntent relation : child.getRelations()) {
            if (relation.isComposition() && master.equals(relation.getTo()) && relation.getName() != null) {
                return relation.getName();
            }
        }
        return null;
    }

    private static UsesIntent findUses(IntentModel model, String alias) {
        for (UsesIntent uses : model.getUses()) {
            if (alias.equals(uses.getModel())) {
                return uses;
            }
        }
        return null;
    }

    private static String rolesLiteral(String roles) {
        Set<String> literals = new LinkedHashSet<>();
        for (String role : roles.split(",")) {
            if (!role.isBlank()) {
                literals.add(NotificationSupport.quote(role.trim()));
            }
        }
        return String.join(", ", literals);
    }

    private static String literals(List<String> values) {
        List<String> literals = new ArrayList<>();
        for (String value : values) {
            literals.add(NotificationSupport.quote(value));
        }
        return String.join(", ", literals);
    }
}
