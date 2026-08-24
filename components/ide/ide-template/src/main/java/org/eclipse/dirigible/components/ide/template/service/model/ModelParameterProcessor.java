/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMap;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.isTrue;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.putNumber;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.strOr;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.truthy;

/**
 * Derives the generation parameters from an entity model, in place.
 *
 * <p>
 * Everything the templates read beyond the raw model is computed here: Java identifiers and package
 * fragments, per-property type names and widget flags, the dropdown lookup URLs, the personal and
 * partner surfaces, the label parts, the perspectives and the default roles. The model's own flags
 * arrive as the strings {@code "true"} / {@code "false"} and are coerced to real booleans, because
 * the templates test them as booleans.
 *
 * <p>
 * The passes run in a fixed order and later ones read what earlier ones wrote - notably the
 * personal / partner inheritance and the {@code dependsOn} resolution both need every entity's own
 * property pass to have completed, which is why they are separate sweeps rather than folded in.
 */
final class ModelParameterProcessor {

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelParameterProcessor.class);

    /** The name of the datasource used when neither the model nor the parameters name one. */
    private static final String DEFAULT_DATASOURCE_NAME_KEY = "DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT";

    /** The fallback name of the default datasource. */
    private static final String DEFAULT_DATASOURCE_NAME = "DefaultDB";

    /** The display pattern applied to a float property that declares none. */
    private static final String DEFAULT_FLOAT_PATTERN = "### ### ### ##0.00";

    /**
     * Not instantiable.
     */
    private ModelParameterProcessor() {}

    /**
     * Processes the model, mutating both the model and the parameters.
     *
     * @param model the entity model
     * @param parameters the generation parameters
     */
    static void process(Map<String, Object> model, Map<String, Object> parameters) {
        parameters.put("javaGenFolderName", NamingHelper.sanitizeJavaIdentifier(str(parameters, "genFolderName")));

        List<Map<String, Object>> entities = asMaps(model.get("entities"));
        for (Map<String, Object> entity : entities) {
            processEntity(entity, entities, parameters);
        }
        // Before the scoped-surface passes below: they carry the flag onto the child panels they build.
        collectRestrictedProperties(entities);
        if (truthy(parameters, "javaRuntime")) {
            // Before the master-lock pass: a child inherits its master's period guard along with the
            // status one, so the master's own must be resolved first.
            resolvePeriodLock(entities, parameters);
            inheritMasterLock(entities, parameters);
            inheritPersonalScope(entities, parameters);
            inheritPartnerScope(entities, parameters);
            collectSensitiveProperties(entities);
            collectScopedChildren(entities);
            resolveLabelParts(entities);
            resolveRelatedRegisters(entities, parameters);
        }
        resolveDependentWidgets(entities);
        collectPerspectives(entities, parameters);
        collectRoles(entities, parameters);
    }

    /**
     * Derives one entity's parameters and those of each of its properties.
     *
     * @param entity the entity
     * @param entities every entity in the model, for cross-entity lookups
     * @param parameters the generation parameters
     */
    private static void processEntity(Map<String, Object> entity, List<Map<String, Object>> entities, Map<String, Object> parameters) {
        resolveDataSource(entity, parameters);
        entity.put("javaPerspectiveName", NamingHelper.sanitizeJavaIdentifier(str(entity, "perspectiveName")));
        String tablePrefix = resolveTablePrefix(parameters);
        if (entity.get("dataCount") != null) {
            entity.put("dataCount", str(entity, "dataCount").replace("${tablePrefix}", tablePrefix));
        }
        if (entity.get("dataQuery") != null) {
            entity.put("dataQuery", str(entity, "dataQuery").replace("${tablePrefix}", tablePrefix));
        }

        resolveReferencedProjection(entity, entities);
        String importsCode = str(entity, "importsCode");
        if (importsCode != null && !importsCode.isEmpty()) {
            entity.put("importsCode", new String(Base64.getDecoder()
                                                       .decode(importsCode),
                    StandardCharsets.UTF_8));
        }
        entity.put("referencedProjections", new ArrayList<>());
        splitChecks(entity);
        resolveDataOrder(entity);

        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            processProperty(property, entity, entities, parameters);
        }
    }

    /**
     * Resolves the entity's datasource. Note that the parameter-provided datasource only survives for
     * an entity that declares none; in every other case both the entity and the parameters are reset to
     * the platform default.
     *
     * @param entity the entity
     * @param parameters the generation parameters
     */
    private static void resolveDataSource(Map<String, Object> entity, Map<String, Object> parameters) {
        if (truthy(parameters, "dataSource") && !truthy(entity, "dataSource")) {
            entity.put("dataSource", parameters.get("dataSource"));
        } else {
            String defaultDataSourceName = Configuration.get(DEFAULT_DATASOURCE_NAME_KEY, DEFAULT_DATASOURCE_NAME);
            entity.put("dataSource", defaultDataSourceName);
            parameters.put("dataSource", defaultDataSourceName);
        }
    }

    /**
     * Normalizes the table prefix onto the parameters, appending the separating underscore a
     * hand-authored prefix may be missing.
     *
     * @param parameters the generation parameters
     * @return the normalized prefix, possibly empty
     */
    private static String resolveTablePrefix(Map<String, Object> parameters) {
        String tablePrefix = strOr(parameters, "tablePrefix", "");
        if (!tablePrefix.isEmpty() && !tablePrefix.endsWith("_")) {
            tablePrefix = tablePrefix + "_";
        }
        parameters.put("tablePrefix", tablePrefix);
        return tablePrefix;
    }

    /**
     * Marks a details entity whose composition child is a projection owned by another model, so its UI
     * links to the owner's application rather than to a local view that does not exist.
     *
     * @param entity the entity
     * @param entities every entity in the model
     */
    private static void resolveReferencedProjection(Map<String, Object> entity, List<Map<String, Object>> entities) {
        String layoutType = str(entity, "layoutType");
        if (!"DEPENDENT".equals(str(entity, "type")) || !("LIST_DETAILS".equals(layoutType) || "MANAGE_DETAILS".equals(layoutType))) {
            return;
        }
        String relationshipEntityName = null;
        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            if ("COMPOSITION".equals(str(property, "relationshipType")) && "1_n".equals(str(property, "relationshipCardinality"))) {
                relationshipEntityName = str(property, "relationshipEntityName");
                break;
            }
        }
        if (relationshipEntityName == null) {
            return;
        }
        for (Map<String, Object> candidate : entities) {
            if (relationshipEntityName.equals(str(candidate, "name")) && "PROJECTION".equals(str(candidate, "type"))) {
                entity.put("hasReferencedProjection", Boolean.TRUE);
                ProjectionOwner owner = ProjectionOwner.of(str(candidate, "projectionReferencedModel"));
                if (owner == null) {
                    entity.remove("referencedProjectionProjectName");
                } else {
                    entity.put("referencedProjectionProjectName", owner.project());
                }
                carry(entity, "referencedProjectionPerspectiveName", candidate, "perspectiveName");
                return;
            }
        }
    }

    /**
     * Splits the declarative checks by the scope that enforces them: a row-level check goes to the REST
     * validation, a guard to the repository's create/update precondition, and everything else to the
     * repository's document-level block.
     *
     * @param entity the entity
     */
    private static void splitChecks(Map<String, Object> entity) {
        List<Map<String, Object>> checks = asMaps(entity.get("checks"));
        if (checks.isEmpty()) {
            return;
        }
        List<Object> rowChecks = new ArrayList<>();
        List<Object> guardChecks = new ArrayList<>();
        List<Object> documentChecks = new ArrayList<>();
        for (Map<String, Object> check : checks) {
            String kind = str(check, "kind");
            if ("exactlyOne".equals(kind)) {
                rowChecks.add(check);
            } else if ("guard".equals(kind)) {
                guardChecks.add(check);
            } else {
                documentChecks.add(check);
            }
        }
        entity.put("rowChecks", rowChecks);
        entity.put("guardChecks", guardChecks);
        entity.put("documentChecks", documentChecks);
    }

    /**
     * Lifts the ordering declared on the entity's properties onto the entity itself.
     *
     * @param entity the entity
     */
    private static void resolveDataOrder(Map<String, Object> entity) {
        List<Map<String, Object>> ordered = new ArrayList<>();
        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            if (property.containsKey("dataOrderBy")) {
                ordered.add(property);
            }
        }
        if (ordered.isEmpty()) {
            return;
        }
        entity.put("dataOrderBy", ordered.get(0)
                                         .get("dataOrderBy"));
        List<String> names = new ArrayList<>(ordered.size());
        for (Map<String, Object> property : ordered) {
            names.add(str(property, "name"));
        }
        entity.put("dataOrderBySort", String.join(",", names));
    }

    /**
     * Derives one property's parameters.
     *
     * @param property the property
     * @param entity the owning entity
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void processProperty(Map<String, Object> property, Map<String, Object> entity, List<Map<String, Object>> entities,
            Map<String, Object> parameters) {
        // dataNotNull reads the original string, so it has to be derived before dataNullable is
        // replaced by its boolean.
        property.put("dataNotNull", "false".equals(property.get("dataNullable")));
        property.put("dataAutoIncrement", isTrue(property, "dataAutoIncrement"));
        property.put("dataNullable", "true".equals(property.get("dataNullable")));
        property.put("dataPrimaryKey", isTrue(property, "dataPrimaryKey"));
        property.put("dataUnique", isTrue(property, "dataUnique"));
        property.put("isRequiredProperty", isTrue(property, "isRequiredProperty"));
        property.put("isCalculatedProperty", isTrue(property, "isCalculatedProperty"));
        property.put("isReadOnlyProperty", isTrue(property, "isReadOnlyProperty"));
        property.put("widgetIsMajor", isTrue(property, "widgetIsMajor"));
        property.put("widgetLabel", strOr(property, "widgetLabel", NamingHelper.humanizeIdentifier(str(property, "name"))));

        String name = str(property, "name");
        if ("ProcessId".equals(name)) {
            entity.put("hasProcess", Boolean.TRUE);
        }
        property.put("widgetDropdownUrl", "");
        property.put("widgetDropdownControllerUrl", "");

        ModelDataTypes.DataType dataType = ModelDataTypes.parse(str(property, "dataType"));
        property.put("dataTypeJava", dataType.java());
        property.put("dataTypeTypescript", dataType.typescript());
        property.put("dataTypeJavaClass", ModelDataTypes.resolveJavaClass(dataType.javaClass(), str(property, "auditType")));

        if (Boolean.TRUE.equals(property.get("dataPrimaryKey"))) {
            if (!(entity.get("primaryKeys") instanceof List)) {
                entity.put("primaryKeys", new ArrayList<>());
            }
            List<Object> primaryKeys = ModelValues.asList(entity.get("primaryKeys"));
            primaryKeys.add(name);
            List<String> asStrings = new ArrayList<>(primaryKeys.size());
            for (Object key : primaryKeys) {
                asStrings.add(String.valueOf(key));
            }
            entity.put("primaryKeysString", String.join(", ", asStrings));
        }
        if ("COMPOSITION".equals(str(property, "relationshipType")) && "1_n".equals(str(property, "relationshipCardinality"))) {
            entity.put("masterEntity", property.get("relationshipEntityName"));
            entity.put("masterEntityId", name);
            property.put("widgetIsMajor", Boolean.FALSE);
        }

        resolveWidgetLengths(property, entity, dataType);
        property.put("inputRule", strOr(property, "widgetPattern", ""));
        collectMasterProperties(property, entity);
        collectReferencedProjections(property, entity, entities);
        resolveDropdown(property, entity, parameters);
        resolveMultiselect(property, entity, entities, parameters);
    }

    /**
     * Derives the length bounds and the numeric / date widget flags from the property's type.
     *
     * @param property the property
     * @param entity the owning entity
     * @param dataType the resolved type
     */
    private static void resolveWidgetLengths(Map<String, Object> property, Map<String, Object> entity, ModelDataTypes.DataType dataType) {
        switch (dataType.typescript()) {
            case "string" -> {
                // A minimum length is not expressible in the model, so it is always zero.
                putNumber(property, "minLength", 0);
                double widgetLength = parseIntLenient(property.get("widgetLength"));
                double dataLength = parseIntLenient(property.get("dataLength"));
                putNumber(property, "maxLength", dataLength > widgetLength ? widgetLength : dataLength);
            }
            case "Date" -> {
                property.put("isDateType", Boolean.TRUE);
                entity.put("hasDates", Boolean.TRUE);
            }
            case "number" -> {
                // Every numeric value is right-aligned in tables; a float is additionally rendered
                // through its display pattern.
                property.put("isNumberType", Boolean.TRUE);
                String sqlType = strOr(property, "dataType", "").toUpperCase(Locale.ROOT);
                if ("DECIMAL".equals(sqlType) || "DOUBLE".equals(sqlType) || "FLOAT".equals(sqlType) || "REAL".equals(sqlType)) {
                    property.put("isFloatType", Boolean.TRUE);
                    String widgetPattern = str(property, "widgetPattern");
                    property.put("formatPattern", widgetPattern != null && !widgetPattern.trim()
                                                                                         .isEmpty() ? widgetPattern
                                                                                                 : DEFAULT_FLOAT_PATTERN);
                    entity.put("hasFloats", Boolean.TRUE);
                }
            }
            default -> {
                // no length or numeric handling for the remaining types
            }
        }
    }

    /**
     * Collects the properties a master layout renders in its object header - the first major
     * non-identity property becomes the title, the rest the subtitle list.
     *
     * @param property the property
     * @param entity the owning entity
     */
    private static void collectMasterProperties(Map<String, Object> property, Map<String, Object> entity) {
        String layoutType = str(entity, "layoutType");
        if (!("MANAGE_MASTER".equals(layoutType) || "LIST_MASTER".equals(layoutType))
                || !Boolean.TRUE.equals(property.get("widgetIsMajor"))) {
            return;
        }
        Map<String, Object> masterProperties = asMap(entity.get("masterProperties"));
        if (masterProperties == null) {
            masterProperties = new LinkedHashMap<>();
            masterProperties.put("title", null);
            masterProperties.put("properties", new ArrayList<>());
            entity.put("masterProperties", masterProperties);
        }
        if (Boolean.TRUE.equals(property.get("dataAutoIncrement"))) {
            return;
        }
        if (masterProperties.get("title") == null) {
            masterProperties.put("title", property);
        } else {
            ModelValues.asList(masterProperties.get("properties"))
                       .add(property);
        }
    }

    /**
     * Records every projection this property points at, so the dropdown and the "add new" link can
     * address the owning project instead of this one.
     *
     * @param property the property
     * @param entity the owning entity
     * @param entities every entity in the model
     */
    private static void collectReferencedProjections(Map<String, Object> property, Map<String, Object> entity,
            List<Map<String, Object>> entities) {
        String relationshipEntityName = str(property, "relationshipEntityName");
        for (Map<String, Object> candidate : entities) {
            if (relationshipEntityName == null || !relationshipEntityName.equals(str(candidate, "name"))) {
                continue;
            }
            String referencedModel = str(candidate, "projectionReferencedModel");
            ProjectionOwner owner = referencedModel == null ? null : ProjectionOwner.of(referencedModel);
            if (owner == null) {
                continue;
            }
            Map<String, Object> projection = new LinkedHashMap<>();
            projection.put("name", candidate.get("name"));
            projection.put("project", owner.project());
            projection.put("genFolderName", owner.genFolderName());
            ModelValues.asList(entity.get("referencedProjections"))
                       .add(projection);
        }
    }

    /**
     * Builds the lookup URLs and the identity metadata for a relation rendered as a dropdown. A
     * document status is a dropdown-backed foreign key too - it renders as a pill, but it still needs
     * the lookup to resolve the identifier to the status name.
     *
     * @param property the property
     * @param entity the owning entity
     * @param parameters the generation parameters
     */
    private static void resolveDropdown(Map<String, Object> property, Map<String, Object> entity, Map<String, Object> parameters) {
        String widgetType = str(property, "widgetType");
        if (!("DROPDOWN".equals(widgetType) || "DOCUMENT_STATUS".equals(widgetType))) {
            return;
        }
        entity.put("hasDropdowns", Boolean.TRUE);

        String targetProject = str(parameters, "projectName");
        String targetGenFolder = str(parameters, "genFolderName");
        String relationshipEntityName = str(property, "relationshipEntityName");
        for (Map<String, Object> projection : asMaps(entity.get("referencedProjections"))) {
            if (relationshipEntityName != null && relationshipEntityName.equals(str(projection, "name"))) {
                targetProject = str(projection, "project");
                targetGenFolder = str(projection, "genFolderName");
                break;
            }
        }

        if (!truthy(parameters, "javaRuntime")) {
            String perspective = str(property, "relationshipEntityPerspectiveName");
            property.put("widgetDropdownUrl", "/services/ts/" + targetProject + "/gen/" + targetGenFolder + "/api/" + perspective + "/"
                    + relationshipEntityName + "Service.ts");
            property.put("widgetDropdownControllerUrl", "/services/ts/" + targetProject + "/gen/" + targetGenFolder + "/api/" + perspective
                    + "/" + relationshipEntityName + "Controller.ts");
            return;
        }

        String javaGen = NamingHelper.sanitizeJavaIdentifier(targetGenFolder);
        String javaPerspective = NamingHelper.sanitizeJavaIdentifier(str(property, "relationshipEntityPerspectiveName"));
        String javaUrl = "/services/java/" + targetProject + "/gen/" + javaGen + "/api/" + javaPerspective + "/" + relationshipEntityName
                + "Controller";
        property.put("widgetDropdownUrl", javaUrl);
        property.put("widgetDropdownControllerUrl", javaUrl);
        String dataPackage = "gen." + javaGen + ".data." + javaPerspective + ".";

        // leafOnly: the generated validation counts the referenced node's children through the
        // target's own repository - client Java compiles registry-wide, so a cross-model import
        // resolves like any hand-written one.
        if (truthy(property, "widgetLeafOnly") && truthy(property, "widgetHierarchyProperty")) {
            property.put("leafOnlyRepositoryClass", dataPackage + relationshipEntityName + "Repository");
            entity.put("hasReferenceValidations", Boolean.TRUE);
        }
        property.put("targetRepositoryClass", dataPackage + relationshipEntityName + "Repository");
        if (truthy(property, "relationshipPersonal") && truthy(property, "relationshipIdentityProperty")) {
            entity.put("personalProperty", property.get("name"));
            entity.put("personalFkJavaClass", property.get("dataTypeJavaClass"));
            entity.put("personalIdentityProperty", property.get("relationshipIdentityProperty"));
            entity.put("personalIdentityLabel",
                    strOr(property, "relationshipIdentityLabel", str(property, "relationshipIdentityProperty")));
            entity.put("personalIdentityEntityClass", dataPackage + relationshipEntityName + "Entity");
            entity.put("personalIdentityRepositoryClass", dataPackage + relationshipEntityName + "Repository");
            entity.put("personalReadOnly", truthy(property, "relationshipPersonalReadOnly"));
        }
        if (truthy(property, "relationshipPartner") && truthy(property, "relationshipPartnerIdentityProperty")) {
            entity.put("partnerProperty", property.get("name"));
            entity.put("partnerFkJavaClass", property.get("dataTypeJavaClass"));
            entity.put("partnerIdentityProperty", property.get("relationshipPartnerIdentityProperty"));
            entity.put("partnerIdentityLabel",
                    strOr(property, "relationshipPartnerIdentityLabel", str(property, "relationshipPartnerIdentityProperty")));
            entity.put("partnerIdentityEntityClass", dataPackage + relationshipEntityName + "Entity");
            entity.put("partnerIdentityRepositoryClass", dataPackage + relationshipEntityName + "Repository");
        }
        // The target's own application, for the "add new" dialog. Its web assets live under the raw
        // folder name while the controllers live under the sanitized one, so this must be built from
        // the raw folder rather than by rewriting the controller URL.
        property.put("widgetDropdownAppUrl", "/services/web/" + targetProject + "/gen/" + targetGenFolder + "/index.html");
    }

    /**
     * Builds the option-source lookup URLs for a MULTISELECT property - a plain value column holding a
     * subset of a lookup entity's keys, whose widget offers that entity's rows. Deliberately a sibling
     * of {@link #resolveDropdown}, never a widened gate: a multiselect is not a relation, so none of
     * the relation-only concerns there (projections, personal/partner identity, the add-new dialog, the
     * target repository) apply, and the two differ on exactly the inputs the whole method keys on -
     * where the target's name and its perspective come from.
     *
     * <p>
     * An unresolvable options entity FAILS the generation rather than degrading: the widget's view
     * block is gated on the widget type while its option loading is gated on the owning entity carrying
     * any option source at all, so a target that resolves to nothing used to emit a Refresh button
     * calling a {@code loadOptions()} that was never generated - a dead widget with nothing anywhere to
     * say why (dirigible #6896).
     *
     * @param property the property
     * @param entity the owning entity
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void resolveMultiselect(Map<String, Object> property, Map<String, Object> entity, List<Map<String, Object>> entities,
            Map<String, Object> parameters) {
        if (!"MULTISELECT".equals(str(property, "widgetType"))) {
            return;
        }
        String owner = str(entity, "name") + "." + str(property, "name");
        String optionsEntity = str(property, "widgetOptionsEntityName");
        if (optionsEntity == null || optionsEntity.isEmpty()) {
            throw new IllegalArgumentException("Property [" + owner + "] is a multi-select but names no options entity"
                    + " - it must name the lookup entity whose rows it offers.");
        }
        String perspective = multiselectPerspective(findEntity(entities, optionsEntity));
        if (perspective == null) {
            throw new IllegalArgumentException("Property [" + owner + "] is a multi-select over [" + optionsEntity
                    + "], which is not an entity of this model that publishes a controller - its options could never load.");
        }
        entity.put("hasDropdowns", Boolean.TRUE);
        String targetProject = str(parameters, "projectName");
        String targetGenFolder = str(parameters, "genFolderName");
        if (!truthy(parameters, "javaRuntime")) {
            property.put("widgetDropdownUrl", "/services/ts/" + targetProject + "/gen/" + targetGenFolder + "/api/" + perspective + "/"
                    + optionsEntity + "Service.ts");
            property.put("widgetDropdownControllerUrl", "/services/ts/" + targetProject + "/gen/" + targetGenFolder + "/api/" + perspective
                    + "/" + optionsEntity + "Controller.ts");
            return;
        }
        String javaGen = NamingHelper.sanitizeJavaIdentifier(targetGenFolder);
        String javaPerspective = NamingHelper.sanitizeJavaIdentifier(perspective);
        String javaUrl =
                "/services/java/" + targetProject + "/gen/" + javaGen + "/api/" + javaPerspective + "/" + optionsEntity + "Controller";
        property.put("widgetDropdownUrl", javaUrl);
        property.put("widgetDropdownControllerUrl", javaUrl);
    }

    /**
     * The perspective a multi-select's options controller publishes under, or {@code null} when the
     * target cannot serve options at all - unknown to this model, or an entity with no perspective of
     * its own (a projection). The SETTING rewrite happens in ModelGenerator at render time - AFTER this
     * processor - so a settings target's raw perspectiveName would bake a URL the target never
     * publishes under.
     *
     * @param target the options entity, or null when it resolved to none
     * @return the perspective, or null
     */
    private static String multiselectPerspective(Map<String, Object> target) {
        if (target == null) {
            return null;
        }
        String perspective = "SETTING".equals(str(target, "type")) ? "Settings" : str(target, "perspectiveName");
        return perspective == null || perspective.isEmpty() ? null : perspective;
    }

    /**
     * Propagates a composition master's immutability to its direct children, so that the REST surface
     * forbids what the generated UI already withholds.
     *
     * <p>
     * A child declares no immutability of its own - the lock belongs to the document - yet its writes
     * synchronously recompute the master's aggregate columns. Without this, creating, editing or
     * deleting a line of a locked document succeeded over REST and silently rewrote the very totals the
     * lock protects, after the number was stamped, the snapshot taken and the ledger posted.
     *
     * <p>
     * Only the direct child is covered, which is the shape that writes through to the master. Engine
     * writers stay exempt by construction: they go through the repository rather than the controller,
     * exactly as the master's own guard already assumes. A child that declares
     * {@code locksWithMaster: false} keeps its user writes - the deliberately post-lock collection,
     * such as the payments settling an issued invoice.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    /**
     * Joins the two halves of date-based immutability (intent {@code immutableInPeriod:}) into the one
     * map the controller templates read.
     *
     * <p>
     * The guarded entity carries which register locks it and which of its own dates decides the window;
     * the register carries its bounds, its status property and the seed ids that mean closed. Only this
     * pass knows both, plus the generated package each entity lands in - the same reason a master's
     * inherited lock is resolved here rather than emitted whole.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void resolvePeriodLock(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        for (Map<String, Object> entity : entities) {
            Map<String, Object> register = findEntity(entities, str(entity, "periodLockEntity"));
            String dateProperty = str(entity, "periodLockDateProperty");
            if (register == null || dateProperty == null || dateProperty.isEmpty()) {
                continue;
            }
            Map<String, Object> date = findProperty(entity, dateProperty);
            String registerPerspective = NamingHelper.sanitizeJavaIdentifier(str(register, "perspectiveName"));
            String registerPackage = "gen." + str(parameters, "javaGenFolderName") + ".data." + registerPerspective + ".";
            Map<String, Object> periodLock = new LinkedHashMap<>();
            periodLock.put("dateProperty", dateProperty);
            periodLock.put("dateJavaClass", date == null ? "java.time.LocalDate" : str(date, "dataTypeJavaClass"));
            periodLock.put("entity", register.get("name"));
            periodLock.put("entityClass", registerPackage + str(register, "name") + "Entity");
            periodLock.put("repositoryClass", registerPackage + str(register, "name") + "Repository");
            periodLock.put("startProperty", str(register, "periodStartProperty"));
            periodLock.put("endProperty", str(register, "periodEndProperty"));
            periodLock.put("statusProperty", str(register, "periodStatusProperty"));
            periodLock.put("closedValues", str(register, "periodClosedValues"));
            entity.put("periodLock", periodLock);
        }
    }

    private static void inheritMasterLock(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        for (Map<String, Object> entity : entities) {
            if ("false".equals(str(entity, "locksWithMaster"))) {
                continue;
            }
            Map<String, Object> parentFk = findCompositionProperty(entity);
            if (parentFk == null) {
                continue;
            }
            Map<String, Object> parent = findEntity(entities, str(parentFk, "relationshipEntityName"));
            if (parent == null) {
                continue;
            }
            boolean always = truthy(parent, "immutableAlways");
            String statusProperty = str(parent, "immutableStatusProperty");
            boolean statusLock = always || (statusProperty != null && !statusProperty.isEmpty());
            Object parentPeriod = parent.get("periodLock");
            if (!statusLock && parentPeriod == null) {
                continue;
            }
            String parentPerspective = NamingHelper.sanitizeJavaIdentifier(str(parentFk, "relationshipEntityPerspectiveName"));
            String parentPackage = "gen." + str(parameters, "javaGenFolderName") + ".data." + parentPerspective + ".";
            Map<String, Object> masterLock = new LinkedHashMap<>();
            masterLock.put("fkProperty", parentFk.get("name"));
            masterLock.put("fkJavaClass", parentFk.get("dataTypeJavaClass"));
            masterLock.put("entity", parent.get("name"));
            masterLock.put("entityClass", parentPackage + str(parent, "name") + "Entity");
            masterLock.put("repositoryClass", parentPackage + str(parent, "name") + "Repository");
            masterLock.put("always", always);
            // Which halves are actually there: a master may be locked by its status, by its period, or
            // by both, and the child's guard emits only the checks its master declares.
            masterLock.put("statusLock", statusLock);
            masterLock.put("statusProperty", statusProperty);
            masterLock.put("statusValues", str(parent, "immutableStatusValues"));
            if (parentPeriod != null) {
                masterLock.put("period", parentPeriod);
            }
            entity.put("masterLock", masterLock);
        }
    }

    /**
     * Propagates the personal scope from a composition parent to its direct children - one hop only,
     * which is what the generated surfaces support. A deeper child simply has no personal surface.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void inheritPersonalScope(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        for (Map<String, Object> entity : entities) {
            if (entity.get("personalProperty") != null) {
                continue;
            }
            Map<String, Object> parentFk = findCompositionProperty(entity);
            if (parentFk == null) {
                continue;
            }
            Map<String, Object> parent = findEntity(entities, str(parentFk, "relationshipEntityName"));
            if (parent == null || parent.get("personalProperty") == null) {
                continue;
            }
            String parentPerspective = NamingHelper.sanitizeJavaIdentifier(str(parentFk, "relationshipEntityPerspectiveName"));
            String parentPackage = "gen." + str(parameters, "javaGenFolderName") + ".data." + parentPerspective + ".";
            Map<String, Object> personalParent = new LinkedHashMap<>();
            personalParent.put("fkProperty", parentFk.get("name"));
            personalParent.put("fkJavaClass", parentFk.get("dataTypeJavaClass"));
            personalParent.put("entity", parent.get("name"));
            personalParent.put("entityClass", parentPackage + str(parent, "name") + "Entity");
            personalParent.put("repositoryClass", parentPackage + str(parent, "name") + "Repository");
            personalParent.put("personalProperty", parent.get("personalProperty"));
            personalParent.put("personalFkJavaClass", parent.get("personalFkJavaClass"));
            entity.put("personalParent", personalParent);
            entity.put("personalReadOnly", truthy(parent, "personalReadOnly"));
            entity.put("personalIdentityProperty", parent.get("personalIdentityProperty"));
            entity.put("personalIdentityLabel", parent.get("personalIdentityLabel"));
            entity.put("personalIdentityEntityClass", parent.get("personalIdentityEntityClass"));
            entity.put("personalIdentityRepositoryClass", parent.get("personalIdentityRepositoryClass"));
        }
    }

    /**
     * The external-partner mirror of {@link #inheritPersonalScope(List, Map)}.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void inheritPartnerScope(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        for (Map<String, Object> entity : entities) {
            if (entity.get("partnerProperty") != null) {
                continue;
            }
            Map<String, Object> parentFk = findCompositionProperty(entity);
            if (parentFk == null) {
                continue;
            }
            Map<String, Object> parent = findEntity(entities, str(parentFk, "relationshipEntityName"));
            if (parent == null || parent.get("partnerProperty") == null) {
                continue;
            }
            String parentPerspective = NamingHelper.sanitizeJavaIdentifier(str(parentFk, "relationshipEntityPerspectiveName"));
            String parentPackage = "gen." + str(parameters, "javaGenFolderName") + ".data." + parentPerspective + ".";
            Map<String, Object> partnerParent = new LinkedHashMap<>();
            partnerParent.put("fkProperty", parentFk.get("name"));
            partnerParent.put("fkJavaClass", parentFk.get("dataTypeJavaClass"));
            partnerParent.put("entity", parent.get("name"));
            partnerParent.put("entityClass", parentPackage + str(parent, "name") + "Entity");
            partnerParent.put("repositoryClass", parentPackage + str(parent, "name") + "Repository");
            partnerParent.put("partnerProperty", parent.get("partnerProperty"));
            partnerParent.put("partnerFkJavaClass", parent.get("partnerFkJavaClass"));
            entity.put("partnerParent", partnerParent);
            entity.put("partnerIdentityProperty", parent.get("partnerIdentityProperty"));
            entity.put("partnerIdentityLabel", parent.get("partnerIdentityLabel"));
            entity.put("partnerIdentityEntityClass", parent.get("partnerIdentityEntityClass"));
            entity.put("partnerIdentityRepositoryClass", parent.get("partnerIdentityRepositoryClass"));
        }
    }

    /**
     * Collects the property names a scoped response must scrub.
     *
     * @param entities every entity in the model
     */
    private static void collectSensitiveProperties(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            if (entity.get("personalProperty") == null && entity.get("personalParent") == null && entity.get("partnerProperty") == null
                    && entity.get("partnerParent") == null) {
                continue;
            }
            List<Object> sensitive = new ArrayList<>();
            for (Map<String, Object> property : asMaps(entity.get("properties"))) {
                if (isTrue(property, "sensitiveProperty")) {
                    sensitive.add(property.get("name"));
                }
            }
            entity.put("sensitiveProperties", sensitive);
        }
    }

    /**
     * Flags the entities carrying a read-scoped property - the entity modeler's per-property read role,
     * or the intent's {@code visibleTo:} allow-list, which is emitted as the same attribute. The
     * generated pages ask the controller which of those fields the caller may not see only when there
     * is such a field, so an application that uses none of this pays nothing for it.
     *
     * @param entities every entity in the model
     */
    private static void collectRestrictedProperties(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            for (Map<String, Object> property : asMaps(entity.get("properties"))) {
                if (truthy(property, "roleRead")) {
                    entity.put("hasRestrictedFields", Boolean.TRUE);
                    break;
                }
            }
        }
    }

    /**
     * Collects, for each scoped entity, the children that inherit its scope - rendered on its form as
     * an embedded calendar or table panel.
     *
     * @param entities every entity in the model
     */
    private static void collectScopedChildren(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            if (entity.get("personalProperty") != null || entity.get("personalParent") != null) {
                entity.put("myChildren", scopedChildren(entities, entity, "personalParent", "MyController", true));
            }
        }
        for (Map<String, Object> entity : entities) {
            if (entity.get("partnerProperty") != null || entity.get("partnerParent") != null) {
                entity.put("partnerChildren", scopedChildren(entities, entity, "partnerParent", "PartnerController", false));
            }
        }
    }

    /**
     * Builds the child panels of one scoped entity.
     *
     * @param entities every entity in the model
     * @param parent the scoped entity
     * @param scopeKey the key naming the inherited scope
     * @param controllerSuffix the suffix of the scoped controller the panel talks to
     * @param withCalendar whether a child may render as a calendar panel
     * @return the panel descriptors
     */
    private static List<Object> scopedChildren(List<Map<String, Object>> entities, Map<String, Object> parent, String scopeKey,
            String controllerSuffix, boolean withCalendar) {
        List<Object> children = new ArrayList<>();
        String parentName = str(parent, "name");
        for (Map<String, Object> child : entities) {
            Map<String, Object> scope = asMap(child.get(scopeKey));
            if (scope == null || parentName == null || !parentName.equals(str(scope, "entity"))) {
                continue;
            }
            String fkProperty = str(scope, "fkProperty");
            Map<String, Object> panel = new LinkedHashMap<>();
            panel.put("name", child.get("name"));
            panel.put("label", strOr(child, "menuLabel", str(child, "name")));
            panel.put("fkProperty", fkProperty);
            panel.put("apiPath",
                    "/" + NamingHelper.sanitizeJavaIdentifier(str(child, "perspectiveName")) + "/" + str(child, "name") + controllerSuffix);
            if (withCalendar) {
                panel.put("calendar", isTrue(child, "detailCalendar") ? calendarPanel(child) : null);
            }
            // Whether this child has role-scoped columns at all: only then does the panel ask the
            // child's own scoped controller which of them the caller in front of it may not see.
            panel.put("restrictedFields", truthy(child, "hasRestrictedFields"));
            panel.put("columns", panelColumns(child, fkProperty));
            children.add(panel);
        }
        return children;
    }

    /**
     * Describes a child panel rendered as a calendar.
     *
     * @param child the child entity
     * @return the calendar descriptor
     */
    private static Map<String, Object> calendarPanel(Map<String, Object> child) {
        Map<String, Object> calendar = new LinkedHashMap<>();
        calendar.put("start", child.get("calendarStartProperty"));
        calendar.put("end", child.get("calendarEndProperty"));
        String titleProperty = str(child, "calendarTitleProperty");
        calendar.put("title", titleProperty);
        // A title naming a relation resolves to its referenced label on the panel.
        Map<String, Object> titleLookup = null;
        if (titleProperty != null) {
            for (Map<String, Object> property : asMaps(child.get("properties"))) {
                String widgetType = str(property, "widgetType");
                if (titleProperty.equals(str(property, "name"))
                        && ("DROPDOWN".equals(widgetType) || "DOCUMENT_STATUS".equals(widgetType))) {
                    titleLookup = new LinkedHashMap<>();
                    titleLookup.put("url", property.get("widgetDropdownControllerUrl"));
                    titleLookup.put("key", property.get("widgetDropDownKey"));
                    titleLookup.put("value", property.get("widgetDropDownValue"));
                    break;
                }
            }
        }
        calendar.put("titleLookup", titleLookup);
        calendar.put("view", strOr(child, "calendarInitialView", "month"));
        return calendar;
    }

    /**
     * Selects the columns a child panel's table shows.
     *
     * @param child the child entity
     * @param fkProperty the foreign key pointing back at the parent
     * @return the column descriptors
     */
    private static List<Object> panelColumns(Map<String, Object> child, String fkProperty) {
        List<Object> columns = new ArrayList<>();
        for (Map<String, Object> property : asMaps(child.get("properties"))) {
            String auditType = str(property, "auditType");
            String name = str(property, "name");
            // Note that widgetIsMajor is already a boolean by now, so the original's comparison
            // against the string "false" never excludes anything - kept as it is so the generated
            // panels do not change shape.
            boolean excluded = truthy(property, "sensitiveProperty") || Boolean.TRUE.equals(property.get("dataAutoIncrement"))
                    || (name != null && name.equals(fkProperty)) || "ProcessId".equals(name) || "ProcessIds".equals(name)
                    || (auditType != null && !"NONE".equals(auditType)) || "false".equals(property.get("widgetIsMajor"));
            if (excluded) {
                continue;
            }
            Map<String, Object> column = new LinkedHashMap<>();
            column.put("name", name);
            column.put("label", strOr(property, "widgetLabel", name));
            column.put("number", Boolean.TRUE.equals(property.get("isNumberType")));
            column.put("date", Boolean.TRUE.equals(property.get("isDateType")));
            columns.add(column);
        }
        return columns;
    }

    /**
     * Turns each entity's declared related-records registers into what the generated page renders: the
     * referencing entity's controller and application URLs, and one column descriptor per property it
     * shows.
     *
     * <p>
     * The declaration carries facts only - which entity, where it lives, its key, the foreign key back
     * here, the property metadata of its columns - because a model may be authored by hand or emitted
     * by a generator that must stay ignorant of the paths a template publishes. Every URL is therefore
     * built here, from the same coordinates a dropdown's lookup URL is built from, and a register whose
     * source lives in another project resolves against THAT project rather than this one.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void resolveRelatedRegisters(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        for (Map<String, Object> entity : entities) {
            List<Map<String, Object>> registers = asMaps(entity.get("relatedEntities"));
            for (Map<String, Object> register : registers) {
                resolveRelatedRegister(register, entities, parameters);
            }
        }
    }

    /**
     * Resolves one register: its owner project, the URLs its panel calls and opens, and its columns.
     *
     * @param register the register
     * @param entities every entity in the model, for resolving a column's projection owner
     * @param parameters the generation parameters
     */
    private static void resolveRelatedRegister(Map<String, Object> register, List<Map<String, Object>> entities,
            Map<String, Object> parameters) {
        ProjectionOwner owner = ProjectionOwner.of(str(register, "referencedModel"));
        String project = owner != null ? owner.project() : str(parameters, "projectName");
        String genFolder = owner != null ? owner.genFolderName() : str(parameters, "genFolderName");
        String entityName = str(register, "entity");
        register.put("apiPath", javaControllerUrl(project, genFolder, str(register, "perspectiveName"), entityName));
        // The source's own application, for opening a listed record in the shared record dialog. Web
        // assets live under the RAW folder name while the controllers live under the sanitized one, so
        // this is built from the raw folder rather than by rewriting the controller URL.
        register.put("appUrl", "/services/web/" + project + "/gen/" + genFolder + "/index.html");
        register.put("local", owner == null);
        List<Object> columns = new ArrayList<>();
        for (Map<String, Object> property : asMaps(register.get("properties"))) {
            columns.add(relatedColumn(property, entities, project, genFolder));
        }
        register.put("columns", columns);
        register.remove("properties");
    }

    /**
     * Describes one register column: its heading, how the cell renders (number / float pattern / date)
     * and, for a foreign key or a multi-select, where to fetch the referenced rows its label comes
     * from.
     *
     * @param property the source property's metadata, as the model declares it
     * @param entities every entity in the model, for resolving a projection owner
     * @param sourceProject the project owning the register's source entity
     * @param sourceGenFolder the generation folder owning the register's source entity
     * @return the column descriptor
     */
    private static Map<String, Object> relatedColumn(Map<String, Object> property, List<Map<String, Object>> entities, String sourceProject,
            String sourceGenFolder) {
        String name = str(property, "name");
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("name", name);
        column.put("label", strOr(property, "widgetLabel", NamingHelper.humanizeIdentifier(name)));
        if (property.get("dataName") != null) {
            column.put("dataName", property.get("dataName"));
        }
        ModelDataTypes.DataType dataType = ModelDataTypes.parse(str(property, "dataType"));
        String widgetType = str(property, "widgetType");
        boolean relation =
                ("DROPDOWN".equals(widgetType) || "DOCUMENT_STATUS".equals(widgetType)) && truthy(property, "relationshipEntityName");
        if (relation) {
            // The referenced rows resolve the foreign key to its label. The target may live in a third
            // project (a projection of the source's model), so its owner wins over the source's.
            ProjectionOwner target = ProjectionOwner.of(relatedColumnOwnerModel(property, entities));
            String project = target != null ? target.project() : sourceProject;
            String genFolder = target != null ? target.genFolderName() : sourceGenFolder;
            Map<String, Object> lookup = new LinkedHashMap<>();
            lookup.put("url", javaControllerUrl(project, genFolder, str(property, "relationshipEntityPerspectiveName"),
                    str(property, "relationshipEntityName")));
            lookup.put("key", strOr(property, "widgetDropDownKey", "Id"));
            lookup.put("text", strOr(property, "widgetDropDownValue", "Name"));
            column.put("lookup", lookup);
        } else if ("MULTISELECT".equals(widgetType) && truthy(property, "widgetOptionsEntityName")) {
            // A subset column holds a KEY LIST ("1,3"), so the panel resolves EACH key through the
            // options entity's rows and joins the labels - the same lookup shape as a foreign key,
            // routed by the explicit `multi` flag (never by sniffing the value for commas). A subset
            // cannot be cross-model, so the options entity belongs to the register SOURCE's project;
            // its perspective travels on the property, the only place a source owned by another model
            // can carry it.
            String optionsEntity = str(property, "widgetOptionsEntityName");
            String perspective =
                    strOr(property, "widgetOptionsEntityPerspectiveName", multiselectPerspective(findEntity(entities, optionsEntity)));
            if (perspective == null) {
                LOGGER.warn("Register column [{}] is a multi-select over [{}], whose perspective this model cannot resolve"
                        + " - the column renders the raw keys", name, optionsEntity);
            } else {
                Map<String, Object> lookup = new LinkedHashMap<>();
                lookup.put("url", javaControllerUrl(sourceProject, sourceGenFolder, perspective, optionsEntity));
                lookup.put("key", strOr(property, "widgetDropDownKey", "Id"));
                lookup.put("text", strOr(property, "widgetDropDownValue", "Name"));
                column.put("multi", Boolean.TRUE);
                column.put("lookup", lookup);
            }
        } else if ("Date".equals(dataType.typescript())) {
            column.put("date", Boolean.TRUE);
        } else if ("number".equals(dataType.typescript())) {
            column.put("number", Boolean.TRUE);
            String sqlType = strOr(property, "dataType", "").toUpperCase(Locale.ROOT);
            if ("DECIMAL".equals(sqlType) || "DOUBLE".equals(sqlType) || "FLOAT".equals(sqlType) || "REAL".equals(sqlType)) {
                column.put("float", Boolean.TRUE);
                column.put("pattern", strOr(property, "widgetPattern", DEFAULT_FLOAT_PATTERN));
            }
        }
        if (isTrue(property, "sensitiveProperty")) {
            // Marked, not dropped: a register renders on the power surfaces, where the owning entity's
            // own lists render the column too.
            column.put("sensitive", Boolean.TRUE);
        }
        return column;
    }

    /**
     * The model a register column's referenced entity is owned by: the one the declaration names (the
     * source's own cross-model reference), else the projection this model carries for it, else null for
     * a target owned by the register's own model.
     *
     * @param property the column's source property
     * @param entities every entity in the model
     * @return the owner's referenced-model path, or null
     */
    private static String relatedColumnOwnerModel(Map<String, Object> property, List<Map<String, Object>> entities) {
        String declared = str(property, "referencedModel");
        if (declared != null) {
            return declared;
        }
        String target = str(property, "relationshipEntityName");
        for (Map<String, Object> entity : entities) {
            if ("PROJECTION".equals(str(entity, "type")) && target != null && target.equals(str(entity, "name"))) {
                return str(entity, "projectionReferencedModel");
            }
        }
        return null;
    }

    /**
     * The URL of a generated Java controller. The package segments are sanitized Java identifiers while
     * the project stays as authored, exactly as the dropdown lookup URLs are built.
     *
     * @param project the owning project
     * @param genFolderName the owning generation folder
     * @param perspectiveName the entity's perspective
     * @param entityName the entity
     * @return the controller URL
     */
    private static String javaControllerUrl(String project, String genFolderName, String perspectiveName, String entityName) {
        return "/services/java/" + project + "/gen/" + NamingHelper.sanitizeJavaIdentifier(genFolderName) + "/api/"
                + NamingHelper.sanitizeJavaIdentifier(perspectiveName) + "/" + entityName + "Controller";
    }

    /**
     * Resolves each relation token of a computed label to the repository the generated name computation
     * loads through, dropping the parts whose foreign key does not resolve.
     *
     * @param entities every entity in the model
     */
    private static void resolveLabelParts(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            if (entity.get("labelParts") == null) {
                continue;
            }
            List<Object> kept = new ArrayList<>();
            for (Map<String, Object> part : asMaps(entity.get("labelParts"))) {
                if (!"relation".equals(str(part, "kind"))) {
                    kept.add(part);
                    continue;
                }
                Map<String, Object> foreignKey = findProperty(entity, str(part, "relation"));
                if (foreignKey == null || !truthy(foreignKey, "targetRepositoryClass")) {
                    continue;
                }
                part.put("repositoryClass", foreignKey.get("targetRepositoryClass"));
                kept.add(part);
            }
            entity.put("labelParts", kept);
            entity.put("hasLabel", Boolean.TRUE);
        }
    }

    /**
     * Resolves the lookup URLs a dependent widget needs at runtime. This runs as its own sweep so it
     * works regardless of property order - the trigger is always a dropdown, whose URL the property
     * pass has already built, but it may belong to another entity.
     *
     * @param entities every entity in the model
     */
    private static void resolveDependentWidgets(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            for (Map<String, Object> property : asMaps(entity.get("properties"))) {
                resolveDependsOn(property, entity, entities);
                resolveWidgetLiterals(property);
            }
            // A hierarchical entity guards its own tree edge against a cycle.
            if (entity.get("hierarchyProperty") != null) {
                entity.put("hasReferenceValidations", Boolean.TRUE);
            }
        }
    }

    /**
     * Resolves the trigger and classifier lookup URLs of one dependent widget.
     *
     * @param property the property
     * @param entity the owning entity
     * @param entities every entity in the model
     */
    private static void resolveDependsOn(Map<String, Object> property, Map<String, Object> entity, List<Map<String, Object>> entities) {
        String dependsOnProperty = str(property, "widgetDependsOnProperty");
        if (dependsOnProperty == null) {
            return;
        }
        // A header-mediated trigger belongs to the document header, not to this item, so its URL has
        // to be resolved on the header entity.
        Map<String, Object> triggerOwner =
                isTrue(property, "widgetDependsOnHeader") ? findEntity(entities, str(property, "widgetDependsOnHeaderEntity")) : entity;
        Map<String, Object> trigger = triggerOwner == null ? null : findProperty(triggerOwner, dependsOnProperty);
        // Every property carries the lookup URL key, empty unless it is a relation, so an empty one
        // must leave the dependent widget's key unset rather than set it to nothing.
        if (trigger != null && truthy(trigger, "widgetDropdownControllerUrl")) {
            property.put("widgetDependsOnControllerUrl", trigger.get("widgetDropdownControllerUrl"));
        }
        // A conditional value whose path hops through a relation needs that relation's URL to fetch
        // the classifier record: the relation is either on this entity or on the document header.
        String valueBy = str(property, "widgetDependsOnValueBy");
        if (valueBy == null) {
            return;
        }
        String[] segments = valueBy.split("\\.", -1);
        Map<String, Object> hopOwner = null;
        String hopProperty = null;
        if (isTrue(property, "widgetDependsOnValueByHeader") && segments.length == 3) {
            hopOwner = findEntity(entities, str(property, "widgetDependsOnValueByHeaderEntity"));
            hopProperty = segments[1];
        } else if (!isTrue(property, "widgetDependsOnValueByHeader") && segments.length == 2) {
            hopOwner = entity;
            hopProperty = segments[0];
        }
        if (hopOwner == null || hopProperty == null) {
            return;
        }
        Map<String, Object> hop = findProperty(hopOwner, hopProperty);
        if (hop != null && truthy(hop, "widgetDropdownControllerUrl")) {
            property.put("widgetDependsOnValueByUrl", hop.get("widgetDropdownControllerUrl"));
        }
    }

    /**
     * Pre-renders the property's regular expression and static option filter as ready literals, so the
     * templates can emit them verbatim. Escaping them here rather than in the templates is what keeps a
     * backslash intact through both a JavaScript string and a Java one.
     *
     * @param property the property
     */
    private static void resolveWidgetLiterals(Map<String, Object> property) {
        String widgetPattern = str(property, "widgetPattern");
        if (widgetPattern != null && !widgetPattern.isEmpty()) {
            property.put("widgetPatternJs", "'" + widgetPattern.replace("\\", "\\\\")
                                                               .replace("'", "\\'")
                    + "'");
            // The same expression as the body of a Java string literal, without the quotes: an
            // unescaped backslash would make the generated controller fail to compile, and the
            // client Java batch is all-or-nothing.
            property.put("widgetPatternJava", widgetPattern.replace("\\", "\\\\")
                                                           .replace("\"", "\\\""));
        }
        if (property.get("widgetOptionsFilterBy") != null && property.containsKey("widgetOptionsFilterValue")) {
            String raw = String.valueOf(property.get("widgetOptionsFilterValue"));
            property.put("widgetOptionsFilterValueJs", raw.matches("-?\\d+(\\.\\d+)?") ? raw
                    : "'" + raw.replace("\\", "\\\\")
                               .replace("'", "\\'")
                            + "'");
        }
    }

    /**
     * Collects the perspectives the model's entities belong to, with the views registered under each.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void collectPerspectives(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        Map<String, Object> perspectives = new LinkedHashMap<>();
        parameters.put("perspectives", perspectives);
        for (Map<String, Object> entity : entities) {
            String perspectiveName = str(entity, "perspectiveName");
            if (perspectiveName == null) {
                continue;
            }
            Map<String, Object> perspective = asMap(perspectives.get(perspectiveName));
            if (perspective == null) {
                perspective = new LinkedHashMap<>();
                perspective.put("views", new ArrayList<>());
                perspectives.put(perspectiveName, perspective);
            }
            perspective.put("name", perspectiveName);
            perspective.put("label", perspectiveName);
            // A key the entity does not carry is left out rather than set to null: the original assigns
            // an absent value, which drops the key from the serialized parameters entirely.
            carry(perspective, "header", entity, "perspectiveHeader");
            carry(perspective, "order", entity, "perspectiveOrder");
            carry(perspective, "navId", entity, "perspectiveNavId");
            carry(perspective, "icon", entity, "perspectiveIcon");
            carry(perspective, "role", entity, "perspectiveRole");
            ModelValues.asList(perspective.get("views"))
                       .add(entity.get("name"));
        }
    }

    /**
     * Collects the default read and write roles the model asks to be generated. A projection owns no
     * table, and a report or filter is read-only, so neither contributes a write role.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void collectRoles(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        List<Object> roles = new ArrayList<>();
        parameters.put("roles", roles);
        for (Map<String, Object> entity : entities) {
            if (!isTrue(entity, "generateDefaultRoles")) {
                continue;
            }
            String type = str(entity, "type");
            if ("PROJECTION".equals(type)) {
                continue;
            }
            Map<String, Object> rolePair = new LinkedHashMap<>();
            rolePair.put("entityName", entity.get("name"));
            if (truthy(entity, "roleRead")) {
                rolePair.put("roleRead", entity.get("roleRead"));
            }
            if (!"REPORT".equals(type) && !"FILTER".equals(type) && truthy(entity, "roleWrite")) {
                rolePair.put("roleWrite", entity.get("roleWrite"));
            }
            roles.add(rolePair);
        }
    }

    /**
     * Copies a value under a new key, leaving the key out when the source does not carry it.
     *
     * @param target the target node
     * @param targetKey the key to write
     * @param source the source node
     * @param sourceKey the key to read
     */
    private static void carry(Map<String, Object> target, String targetKey, Map<String, Object> source, String sourceKey) {
        if (source.containsKey(sourceKey)) {
            target.put(targetKey, source.get(sourceKey));
        } else {
            target.remove(targetKey);
        }
    }

    /**
     * Finds the entity's first composition property, the one pointing at its parent.
     *
     * @param entity the entity
     * @return the property, or null when the entity is not a composition child
     */
    private static Map<String, Object> findCompositionProperty(Map<String, Object> entity) {
        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            if ("COMPOSITION".equals(str(property, "relationshipType"))) {
                return property;
            }
        }
        return null;
    }

    /**
     * Finds an entity by name.
     *
     * @param entities every entity in the model
     * @param name the entity name, may be null
     * @return the entity, or null
     */
    private static Map<String, Object> findEntity(List<Map<String, Object>> entities, String name) {
        if (name == null) {
            return null;
        }
        for (Map<String, Object> entity : entities) {
            if (name.equals(str(entity, "name"))) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Finds a property of an entity by name.
     *
     * @param entity the entity
     * @param name the property name, may be null
     * @return the property, or null
     */
    private static Map<String, Object> findProperty(Map<String, Object> entity, String name) {
        if (name == null) {
            return null;
        }
        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            if (name.equals(str(property, "name"))) {
                return property;
            }
        }
        return null;
    }

    /**
     * Parses a leading integer the way the model's free-text length fields are read: an absent or empty
     * value counts as zero, a value with a trailing suffix keeps its leading digits, and a non-numeric
     * value yields not-a-number - which the scrub then drops.
     *
     * @param value the raw value
     * @return the parsed number, possibly {@link Double#NaN}
     */
    private static double parseIntLenient(Object value) {
        if (value instanceof Number number) {
            return Math.floor(number.doubleValue());
        }
        String text = value == null ? ""
                : value.toString()
                       .trim();
        if (text.isEmpty()) {
            return 0d;
        }
        int end = 0;
        if (end < text.length() && (text.charAt(end) == '+' || text.charAt(end) == '-')) {
            end++;
        }
        int digits = end;
        while (digits < text.length() && Character.isDigit(text.charAt(digits))) {
            digits++;
        }
        if (digits == end) {
            return Double.NaN;
        }
        return Double.parseDouble(text.substring(0, digits));
    }

    /**
     * The project and generation folder a projection's owner model lives in.
     *
     * @param project the owning project
     * @param genFolderName the owner's generation folder
     */
    private record ProjectionOwner(String project, String genFolderName) {

        /**
         * Reads the owner out of a referenced-model path. The last two segments are used rather than fixed
         * indices, so both the {@code /<project>/<model>.model} form and the older
         * {@code /<workspace>/<project>/<model>.model} one - still written by the entity editor and present
         * in every already-committed model - resolve identically.
         *
         * @param referencedModel the referenced model path
         * @return the owner, or null when the path is too short to name one
         */
        static ProjectionOwner of(String referencedModel) {
            if (referencedModel == null) {
                return null;
            }
            List<String> tokens = new ArrayList<>();
            for (String token : referencedModel.split("/", -1)) {
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
            if (tokens.size() < 2) {
                return null;
            }
            String file = tokens.get(tokens.size() - 1);
            int dot = file.indexOf('.');
            return new ProjectionOwner(tokens.get(tokens.size() - 2), dot >= 0 ? file.substring(0, dot) : file);
        }
    }

}
