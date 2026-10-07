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
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMap;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.defaultRole;
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
            resolveDocumentItemsScope(entities);
            inheritPartnerScope(entities, parameters);
            collectSensitiveProperties(entities);
            collectScopedChildren(entities);
            resolveLabelParts(entities);
            resolveRelatedRegisters(entities, parameters);
            resolveRollupGuards(entities);
            resolveAgreeGuards(entities, parameters);
            resolveTargetAgreementRules(entities);
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
        splitChecks(entity, parameters);
        resolveUniqueConstraintLiterals(entity, parameters);
        resolveLifecycleStatusNames(entity);
        resolveDataOrder(entity);
        resolveOrderBy(entity);

        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            processProperty(property, entity, entities, parameters);
        }
        resolveVisibleWhen(entity);
        resolveListProperties(entity);
    }

    /**
     * Derives {@code listProperties}, the sequence the list templates iterate their columns in. An
     * entity with a {@code listOrder} (intent {@code list:}, dirigible #7614) lists the named
     * properties first, in that order, then the rest in control order - the rest are not major, so no
     * list shows them. Without one it is the properties themselves, so the list follows the control
     * order as before. Either way the elements are the same property maps the form iterates.
     *
     * @param entity the entity
     */
    private static void resolveListProperties(Map<String, Object> entity) {
        List<Map<String, Object>> properties = asMaps(entity.get("properties"));
        String listOrder = str(entity, "listOrder");
        if (listOrder == null || listOrder.isBlank()) {
            entity.put("listProperties", properties);
            return;
        }
        List<Map<String, Object>> ordered = new ArrayList<>(properties.size());
        Set<Map<String, Object>> placed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (String wanted : listOrder.split(",")) {
            for (Map<String, Object> property : properties) {
                if (!placed.contains(property) && wanted.trim()
                                                        .equalsIgnoreCase(str(property, "name"))) {
                    ordered.add(property);
                    placed.add(property);
                    break;
                }
            }
        }
        for (Map<String, Object> property : properties) {
            if (!placed.contains(property)) {
                ordered.add(property);
            }
        }
        entity.put("listProperties", ordered);
    }

    /**
     * Renders the {@code visibleWhen} conditions (dirigible #7502) into the JavaScript the views
     * evaluate: on a property, the expression its form / document input's {@code x-show} folds in
     * ({@code visibleWhenJs}); on a composition child, the term list its detail registration hands the
     * shared panel ({@code visibleWhenTermsJs}), read against the master record. A condition that does
     * not render leaves no key, so the templates emit no gate.
     *
     * @param entity the entity
     */
    private static void resolveVisibleWhen(Map<String, Object> entity) {
        for (Map<String, Object> property : asMaps(entity.get("properties"))) {
            String expression = VisibleWhenLiterals.formExpression(str(property, "visibleWhen"), entity);
            if (expression != null) {
                property.put("visibleWhenJs", expression);
            }
        }
        String terms = VisibleWhenLiterals.termsLiteral(str(entity, "visibleWhen"));
        if (terms != null) {
            entity.put("visibleWhenTermsJs", terms);
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
     * repository's document-level block. A {@code forbidWhen} additionally lands in the delete list,
     * whatever its gate - the one check kind that is about the write happening at all rather than about
     * the values it carries (#7372) - unless its {@code verbs} scope leaves the delete out (#7710).
     *
     * @param entity the entity
     */
    private static void splitChecks(Map<String, Object> entity, Map<String, Object> parameters) {
        List<Map<String, Object>> checks = asMaps(entity.get("checks"));
        if (checks.isEmpty()) {
            return;
        }
        List<Object> rowChecks = new ArrayList<>();
        List<Object> guardChecks = new ArrayList<>();
        List<Object> documentChecks = new ArrayList<>();
        List<Object> forbidWhenGuards = new ArrayList<>();
        List<Object> deleteChecks = new ArrayList<>();
        List<Object> warningChecks = new ArrayList<>();
        int index = -1;
        for (Map<String, Object> check : checks) {
            index++;
            String kind = str(check, "kind");
            resolveMessageLiteral(check);
            resolveMessageCatalog(check, ModelTranslations.checkMessageKey(entity, check, index), parameters);
            resolveCheckJavaExpressions(check);
            resolveCheckPathLoads(check, parameters);
            if ("warn".equals(str(check, "severity"))) {
                // The soft tier (#7466), whatever its kind: the repository collects what the write would
                // warn about and the controllers ask the person before persisting. A warning refuses
                // nothing, so it reaches neither validate(), nor the delete verb, nor the transition.
                String code = str(check, "code");
                check.put("codeJavaLiteral", JavaLiterals.escape(code == null ? str(entity, "name") + "." + kind : code));
                warningChecks.add(check);
            } else if ("exactlyOne".equals(kind) || "agree".equals(kind)) {
                // Both hold from the first save and take no gate: one relates the row's own fields, the
                // other the two records a junction row links (#7409).
                rowChecks.add(check);
            } else if ("compare".equals(kind)) {
                // A comparison is row-level unless it names the status it is enforced at - the same
                // routing requiredWhen has: without a gate it holds on every user write, with one it is
                // the repository's, so "days > 0 before SUBMITTED" does not forbid the draft (#7338).
                (str(check, "status") == null || str(check, "status").isEmpty() ? rowChecks : documentChecks).add(check);
            } else if ("guard".equals(kind)) {
                guardChecks.add(check);
            } else if ("requiredWhen".equals(kind) || "forbidWhen".equals(kind)) {
                // A forbidWhen may scope itself to the verbs it is about (#7710) - absent, all three. A
                // scope naming no create/update leaves the writes alone and reaches only the delete.
                List<Object> verbs = check.get("verbs") instanceof List<?> scoped ? new ArrayList<>(scoped) : null;
                // A conditionally required or forbidden value is row-level unless it names the status it
                // is enforced at: without a gate it must hold on every user write, with one it is the
                // repository's business, like every other gated check.
                if (verbs == null || verbs.contains("create") || verbs.contains("update")) {
                    (str(check, "status") == null || str(check, "status").isEmpty() ? rowChecks : documentChecks).add(check);
                }
                if ("forbidWhen".equals(kind)) {
                    // The UI half (#7275): a forbidWhen that reads the composition master carries a
                    // descriptor the detail-register template emits, so the master-detail panel hides
                    // the child's Add/edit/delete affordance while the condition holds - only the ones
                    // its verbs cover, when it is scoped (#7710).
                    List<Map<String, Object>> masterGuard = asMaps(check.get("masterGuard"));
                    if (!masterGuard.isEmpty()) {
                        Map<String, Object> guard = new LinkedHashMap<>();
                        guard.put("terms", masterGuard);
                        if (verbs != null) {
                            guard.put("verbs", verbs);
                        }
                        forbidWhenGuards.add(guard);
                    }
                    // ...and the third affordance that panel hides is the row's DELETE, which the server
                    // half did not cover (#7372): a rule reading "no line may change while the quotation
                    // is sent" refused the create and the update and let the line be REMOVED - the
                    // largest of the three changes - because forbidWhen is built on requiredWhen, which
                    // is about the CONTENT of a write and so has no delete semantics to inherit. Every
                    // forbidWhen - gated or not - therefore reaches the three controllers' delete verb as
                    // well, and the gate rides along with it rather than routing it elsewhere: a delete
                    // is nobody's transition (no process step deletes a record, and `whenDeleted:` only
                    // REACTS to one), so there is no repository-side write for a gated check to sit on.
                    // Keeping it out of the repository is also what leaves the composition cascade alone:
                    // a master sweeping its own children away goes through their repositories, and
                    // whether THAT delete is allowed is `whenMasterDeleted:`'s question, not a child
                    // check's. A rule scoped away from the delete (#7710) - "no new allocation onto a PAID
                    // invoice", which must still let a wrong allocation be removed - stays out of it.
                    if (verbs == null || verbs.contains("delete")) {
                        deleteChecks.add(check);
                    }
                }
            } else {
                documentChecks.add(check);
            }
        }
        entity.put("rowChecks", rowChecks);
        entity.put("guardChecks", guardChecks);
        entity.put("documentChecks", documentChecks);
        entity.put("forbidWhenGuards", forbidWhenGuards);
        entity.put("deleteChecks", deleteChecks);
        entity.put("warningChecks", warningChecks);
    }

    /**
     * Derives the escaped twin of an authored message, for the templates that write it into a Java
     * string literal.
     *
     * <p>
     * A check's, a guard's or a unique key's message is prose an author writes - and the very messages
     * the DSL's own examples suggest quote a field name ({@code A "due" date is never before the
     * invoice date}). Interpolated verbatim, that quote ends the literal it is written into and fails
     * the compile of every generated class of the module, not just the one carrying the message (#7241,
     * the sibling of #7154). The raw value is left in place for the surfaces that render it as text;
     * only the Java sites read the twin.
     *
     * <p>
     * A holder carrying no message is left untouched rather than given an empty twin, as the default
     * value literal is: the key's absence is what a template reads.
     *
     * @param holder the check or unique constraint
     */
    private static void resolveMessageLiteral(Map<String, Object> holder) {
        String message = str(holder, "message");
        if (message != null) {
            holder.put("messageJavaLiteral", JavaLiterals.escape(message));
        }
    }

    /**
     * Derives the arguments a generated refusal or warning hands {@code CheckMessages} (issue #7611):
     * the message's fully qualified catalog key ({@code <project>:<catalog prefix>.checks.<key>} - the
     * same key the generated en-US catalog writes the message under and a language catalog translates)
     * and the default-language text. The generated code resolves them for the request's language when
     * the check fires, so one declaration reaches every reader in their own language.
     *
     * <p>
     * {@code messageArgsJava} is the two arguments joined, ready to be written into the call; a
     * template appends the placeholder values (`{count}`, `{match}`) after it.
     *
     * @param check the check
     * @param messageKey the check's catalog key
     * @param parameters the generation parameters (project name and model file path)
     */
    private static void resolveMessageCatalog(Map<String, Object> check, String messageKey, Map<String, Object> parameters) {
        String message = str(check, "message");
        if (message == null) {
            return;
        }
        String catalogKey = ModelTranslations.catalogKey(parameters, ModelTranslations.CHECKS_CATALOG + "." + messageKey);
        check.put("messageCatalogKey", catalogKey);
        check.put("messageCatalogKeyJavaLiteral", JavaLiterals.escape(catalogKey));
        check.put("messageArgsJava", "\"" + JavaLiterals.escape(catalogKey) + "\", \"" + JavaLiterals.escape(message) + "\"");
    }

    /**
     * Derives the Java twins of a check's NEUTRAL halves - the literal a {@code compare} tests against
     * and the condition a {@code requiredWhen} / {@code forbidWhen} is gated by (issue #7405).
     *
     * <p>
     * The model carries both as data: {@code value} is the reading of the authored literal,
     * {@code when} the typed terms of the condition. The generator resolved and refused them while the
     * author was generating; turning them into Java is this layer's business, exactly as a property's
     * {@code dataDefaultValue} becomes a {@code dataDefaultValueJavaLiteral} here rather than in the
     * model. That split is what lets the same check reach a non-Java template - and what keeps a
     * {@code java.math.BigDecimal} out of an artefact an author opens in the modeler.
     *
     * <p>
     * A half that does not render is left absent rather than empty, as the default value literal is:
     * the key's presence is what the templates read.
     *
     * @param check the check
     */
    private static void resolveCheckJavaExpressions(Map<String, Object> check) {
        Object value = check.get("value");
        if (value instanceof Map<?, ?> reading) {
            String expression = JavaLiterals.compareLiteralExpression(asStringKeyed(reading));
            if (expression != null) {
                check.put("literalJavaExpression", expression);
            }
        }
        List<Map<String, Object>> when = asMaps(check.get("when"));
        if (!when.isEmpty()) {
            String expression = JavaLiterals.conditionExpression(when);
            if (expression != null) {
                check.put("guardJavaExpression", expression);
            }
        }
        // The condition over the document's LINES (#7560), read off the `item` local of the loop the
        // repository renders around it.
        List<Map<String, Object>> whenAnyItem = asMaps(check.get("whenAnyItem"));
        if (!whenAnyItem.isEmpty()) {
            String expression = JavaLiterals.conditionExpression(whenAnyItem);
            if (expression != null) {
                check.put("anyItemJavaExpression", expression);
            }
        }
    }

    /**
     * A nested model map under the key type the renderers take - a {@code .model} is JSON, so its keys
     * are strings whatever the deserialiser's wildcard says.
     *
     * @param map the nested map
     * @return the same map, string-keyed
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringKeyed(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    /**
     * Derives the escaped twins of a unique key's authored name and message, both of which the REST
     * controllers write into Java string literals when they translate a constraint violation.
     *
     * @param entity the entity
     * @param parameters the generation parameters
     */
    private static void resolveUniqueConstraintLiterals(Map<String, Object> entity, Map<String, Object> parameters) {
        for (Map<String, Object> constraint : asMaps(entity.get("uniqueConstraints"))) {
            resolveMessageLiteral(constraint);
            // The message's translation key (#7611), so a collision is answered in the reader's language.
            String uniqueKey = ModelTranslations.uniqueMessageKey(entity, constraint);
            if (uniqueKey != null) {
                constraint.put("messageCatalogKeyJavaLiteral",
                        JavaLiterals.escape(ModelTranslations.catalogKey(parameters, ModelTranslations.CHECKS_CATALOG + "." + uniqueKey)));
            }
            String name = str(constraint, "name");
            if (name != null) {
                constraint.put("nameJavaLiteral", JavaLiterals.escape(name));
            }
        }
    }

    /**
     * Normalizes the seeded status names a lifecycle refusal quotes into the escaped entries the
     * generated repository builds its lookup map from.
     *
     * <p>
     * The names used to travel as one {@code id=name,} join that the template split back apart at
     * class-init time (#7295). A status name is authored prose: a comma in one shifted every following
     * entry, and a quote or a backslash ended the Java literal the join was written into and failed the
     * compile of the whole generated module. The model now carries the pairs structurally; a
     * {@code .model} written before that still carries the join and is read back here, so nothing
     * regenerates differently for a name that never held a separator.
     *
     * @param entity the entity
     */
    private static void resolveLifecycleStatusNames(Map<String, Object> entity) {
        List<Object> entries = new ArrayList<>();
        for (Map<String, Object> declared : asMaps(entity.get("lifecycleStatusNameList"))) {
            addLifecycleStatusName(entries, str(declared, "id"), str(declared, "name"));
        }
        if (entries.isEmpty()) {
            String joined = str(entity, "lifecycleStatusNames");
            if (joined != null && !joined.isEmpty()) {
                for (String seeded : joined.split(",")) {
                    int separator = seeded.indexOf('=');
                    if (separator > 0) {
                        addLifecycleStatusName(entries, seeded.substring(0, separator), seeded.substring(separator + 1));
                    }
                }
            }
        }
        if (!entries.isEmpty()) {
            entity.put("lifecycleStatusNameEntries", entries);
        }
    }

    /**
     * Adds one seeded status name, escaped for the Java literal the generated repository writes it
     * into.
     *
     * @param entries the entries collected so far
     * @param id the status id
     * @param name the seeded name
     */
    private static void addLifecycleStatusName(List<Object> entries, String id, String name) {
        if (id == null || id.isEmpty() || name == null || name.isEmpty()) {
            return;
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("idJavaLiteral", JavaLiterals.escape(id));
        entry.put("nameJavaLiteral", JavaLiterals.escape(name));
        entries.add(entry);
    }

    /**
     * Resolves a check's declared path hops to the generated classes that load them - the reader of a
     * {@code Relation.field} value must fetch the related record before it can read the field.
     *
     * <p>
     * A cross-model hop resolves against the owner model's generation folder, as every other
     * cross-model reference does; this is the pass that knows the generation folder at all, which is
     * why the intent generator emits the hop's coordinates and not a class name.
     *
     * @param check the check
     * @param parameters the generation parameters
     */
    private static void resolveCheckPathLoads(Map<String, Object> check, Map<String, Object> parameters) {
        List<Map<String, Object>> hops = asMaps(check.get("pathLoads"));
        if (hops.isEmpty()) {
            return;
        }
        List<Object> loads = new ArrayList<>();
        for (Map<String, Object> hop : hops) {
            String genFolder = truthy(hop, "crossModel") ? NamingHelper.sanitizeJavaIdentifier(str(hop, "targetModel"))
                    : str(parameters, "javaGenFolderName");
            String qualified =
                    "gen." + genFolder + ".data." + NamingHelper.sanitizeJavaIdentifier(str(hop, "perspective")) + "." + str(hop, "entity");
            Map<String, Object> load = new LinkedHashMap<>();
            load.put("local", hop.get("local"));
            load.put("sourceExpression", hop.get("sourceExpression"));
            load.put("entityClass", qualified + "Entity");
            load.put("repositoryClass", qualified + "Repository");
            loads.add(load);
        }
        check.put("pathLoads", loads);
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
     * Derives {@code orderByHql}, the default row order of the generated list endpoint - the intent's
     * {@code orderBy:} (dirigible #7727), or, for a hand-modeled entity, the per-property
     * {@code dataOrderBy} the entity editor writes. Both say the same thing, so they resolve to one
     * clause: {@code e.Number asc, e.Date desc}, the list and search queries append it verbatim. An
     * entity declaring neither leaves no key, so those queries are emitted exactly as before and the
     * rows come in whatever order the database returns them.
     *
     * @param entity the entity
     */
    private static void resolveOrderBy(Map<String, Object> entity) {
        List<String> terms = new ArrayList<>();
        String declared = str(entity, "orderBy");
        if (declared != null && !declared.isBlank()) {
            for (String term : declared.split(",")) {
                String[] parts = term.trim()
                                     .split("\\s+");
                if (parts.length > 0 && !parts[0].isBlank()) {
                    terms.add("e." + parts[0] + " " + (parts.length > 1 && "desc".equalsIgnoreCase(parts[1]) ? "desc" : "asc"));
                }
            }
        } else {
            for (Map<String, Object> property : asMaps(entity.get("properties"))) {
                String direction = str(property, "dataOrderBy");
                if (direction != null && !direction.isBlank()) {
                    terms.add("e." + str(property, "name") + " " + ("DESC".equalsIgnoreCase(direction.trim()) ? "desc" : "asc"));
                }
            }
        }
        if (terms.isEmpty()) {
            return;
        }
        entity.put("orderByHql", String.join(", ", terms));
        // ...and the same order as a Criteria chain, for the scoped surfaces, which list through one.
        StringBuilder chain = new StringBuilder();
        for (String term : terms) {
            String property = term.substring("e.".length(), term.lastIndexOf(' '));
            chain.append(term.endsWith(" desc") ? ".orderByDesc(\"" : ".orderByAsc(\"")
                 .append(property)
                 .append("\")");
        }
        entity.put("orderByCriteria", chain.toString());
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
        // The authored label reaches the Harmonia templates inside single-quoted JS string literals
        // and Alpine T() call arguments - interpolated verbatim, an apostrophe in it ("Owner's copy")
        // closes the literal early and breaks the whole generated page (dirigible #7294, the #7207
        // class). Escaped once here, the same way widgetPatternJs is, so every template can write
        // '${property.widgetLabelJs}' instead of '${property.widgetLabel}' without re-deriving it.
        property.put("widgetLabelJs", JsLiterals.escape(str(property, "widgetLabel")));
        // A picker rule naming its message's catalog key (#7611) gets the fully qualified key the
        // shared runtime's T() resolves - the namespace is this project and the catalog prefix this
        // model file's, neither of which the model itself may embed.
        String pickable = str(property, "widgetPickable");
        if (pickable != null && pickable.contains("\"messageKey\"")) {
            Map<String, Object> rule = ModelJson.parseObject(pickable);
            if (rule.get("messageKey") instanceof String messageKey) {
                rule.put("messageCatalogKey",
                        ModelTranslations.catalogKey(parameters, ModelTranslations.CHECKS_CATALOG + "." + messageKey));
                property.put("widgetPickable", JavaScriptJson.compact(rule));
            }
        }
        // The authored description is the property's @Documentation argument in the generated entity -
        // a Java string literal, so a quote in it ({@code Customer's "trade" name}) would end that
        // literal and fail the compile of the whole generated module (#7295). The raw value stays for
        // the form's own description paragraph, which is HTML text.
        String description = str(property, "description");
        if (description != null) {
            property.put("descriptionJavaLiteral", JavaLiterals.escape(description));
        }
        // The series a document number is allocated from: authored free text ("Sales Invoice"), written
        // into the allocator call as a literal by both the repository and the numbering delegate.
        String numberSeries = str(property, "numberSeries");
        if (numberSeries != null) {
            property.put("numberSeriesJavaLiteral", JavaLiterals.escape(numberSeries));
        }

        String name = str(property, "name");
        if ("ProcessId".equals(name)) {
            entity.put("hasProcess", Boolean.TRUE);
        }
        // Bookkeeping with nothing to say to a reader stays out of every generated form, list and
        // details block: the per-process stamps (ProcessIds, kept by name so a model written before the
        // flag existed still hides them) and whatever the model flags itself, such as the displaced
        // status a capacity roll-up remembers.
        property.put("isHiddenProperty", isTrue(property, "isHiddenProperty") || "ProcessIds".equals(name));
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

        resolveDefaultValueLiterals(property, entity);

        resolveWidgetLengths(property, entity, dataType);
        // After the widget flags: the seed is emitted in the shape the draft holds, and a numeric
        // column is what decides between a real number and a string.
        resolveDefaultValueJsLiteral(property);
        property.put("inputRule", strOr(property, "widgetPattern", ""));
        collectMasterProperties(property, entity);
        collectReferencedProjections(property, entity, entities);
        resolveDropdown(property, entity, parameters);
        resolveMultiselect(property, entity, entities, parameters);
    }

    /**
     * Derives the authored default as the literals the generated artefacts write it into.
     *
     * <p>
     * The value is a piece of authored text that ends up inside a Java string literal (the repository
     * assigning the default) and inside a JSON string (the {@code .schema} declaring the column
     * DEFAULT). Interpolated verbatim by a template, a value carrying a quote or a backslash ended the
     * literal it was being written into and took the whole artefact with it - a failed compile of the
     * generated module (#7154), an unparseable schema for which the synchronizer then created no table
     * at all (#7206). Resolving the literals here keeps the worst case at one mis-valued field.
     *
     * <p>
     * The JSON literal is the value as authored, because the schema's DEFAULT reaches the DDL verbatim
     * by design - a malformed default is the author's broken SQL, and only escaped so that it cannot
     * break anything but its own column. The Java expression is of the property's own type and exists
     * only where a literal can stand in for the default at all.
     *
     * <p>
     * A key with no value is left absent rather than null: a template reads the key's presence as "this
     * property has a default".
     *
     * @param property the property
     * @param entity the owning entity, to name the property in a refusal
     */
    private static void resolveDefaultValueLiterals(Map<String, Object> property, Map<String, Object> entity) {
        String defaultValue = str(property, "dataDefaultValue");
        if (defaultValue == null || defaultValue.isEmpty()) {
            return;
        }
        String nowShape = nowDefaultShape(property, defaultValue);
        if (nowShape != null) {
            // `defaultValue: now` (#7603) is the moment of the create, in the field's own shape - filled
            // by the repository on save (and by the generated form on open), never handed to the column:
            // a DEFAULT of `now` is not SQL, and no database DEFAULT produces the month or week shape.
            property.put("dataDefaultNowShape", nowShape);
            property.put("dataDefaultValueJavaLiteral", JavaExpressions.expression(Map.of("kind", "now", "shape", nowShape)));
            return;
        }
        property.put("dataDefaultValueJsonLiteral", JsonLiterals.stringLiteral(defaultValue));

        // The generated key is the database's to assign, so its default is never applied in Java - the
        // schema, which only declares what was authored, keeps carrying it.
        if (Boolean.TRUE.equals(property.get("dataPrimaryKey")) || Boolean.TRUE.equals(property.get("dataAutoIncrement"))) {
            return;
        }
        String expression = JavaLiterals.defaultValueExpression(str(property, "dataTypeJavaClass"), defaultValue,
                str(entity, "name") + "." + str(property, "name"));
        if (expression != null) {
            property.put("dataDefaultValueJavaLiteral", expression);
        }
    }

    /**
     * The shape a {@code now} default is the current moment in - {@code date}, {@code timestamp},
     * {@code month} or {@code week}, the set {@code JavaExpressions} and the shell's
     * {@code App.utils.todayAs} render - or null when the default is not the token, or the property
     * holds no moment, in which case it stays an ordinary literal. A month and a week are stored as
     * text, so their widget tells them apart from a string column.
     *
     * @param property the property
     * @param defaultValue its authored default
     * @return the shape, or null
     */
    private static String nowDefaultShape(Map<String, Object> property, String defaultValue) {
        if (!"now".equals(defaultValue.trim())) {
            return null;
        }
        String widget = strOr(property, "widgetType", "");
        if ("MONTH".equals(widget)) {
            return "month";
        }
        if ("WEEK".equals(widget)) {
            return "week";
        }
        return switch (strOr(property, "dataType", "").toUpperCase(Locale.ROOT)) {
            case "DATE" -> "date";
            case "DATETIME", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE" -> "timestamp";
            default -> null;
        };
    }

    /**
     * Derives the authored default as the JavaScript value the item dialog seeds a new line with.
     *
     * <p>
     * The column already carries this value as a DB DEFAULT and the repository applies it on create
     * (#7104); seeding the dialog is what makes it visible and editable before the row is posted. The
     * seed is resolved here rather than assembled in the template, so an authored value carrying an
     * apostrophe or a backslash is escaped instead of ending the literal it is written into and making
     * the whole generated register a syntax error - which fails the page, not the one field (#7207).
     *
     * <p>
     * A key with no expression is left absent rather than null: a template reads the key's presence as
     * "this property has a default to seed".
     *
     * @param property the property
     */
    private static void resolveDefaultValueJsLiteral(Map<String, Object> property) {
        if (property.get("dataDefaultNowShape") != null) {
            return; // today is computed by the page when the form or the line dialog opens
        }
        String expression = JsLiterals.defaultValueExpression(str(property, "widgetType"),
                Boolean.TRUE.equals(property.get("isNumberType")), str(property, "dataDefaultValue"));
        if (expression != null) {
            property.put("dataDefaultValueJsLiteral", expression);
        }
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
        // whenTargetDeleted (#7547): the target's repository cannot know which entities reference it -
        // another model may - so this entity's repository contributes the rule the target's delete
        // applies, keyed by the target's generated entity class, which is what that delete names itself
        // by. Same-model and cross-model alike: this is the package the target is generated into.
        // `keep` (#7634) contributes nothing: the reference outlives its target.
        if (truthy(property, "whenTargetDeleted") && !"keep".equals(String.valueOf(property.get("whenTargetDeleted")))) {
            property.put("targetEntityClass", dataPackage + relationshipEntityName + "Entity");
            entity.put("hasTargetDeleteRules", Boolean.TRUE);
        }
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
    /**
     * Renders what a capacity guard needs beyond its coordinates (issue #7542): the row filter, as the
     * {@code Criteria} chain the re-sum appends and the Java test the row being written has to pass,
     * and the authored refusal message.
     *
     * <p>
     * The filter is applied TWICE, and that is the whole point of rendering both halves from the one
     * declaration: the sum re-read from the store must exclude the rows the author retired, and so must
     * the amount of the row in hand - otherwise cancelling an allocation would be refused by the very
     * ceiling the cancellation frees. The guard is skipped outright when the incoming row does not
     * count, since a row outside the filter changes no sum there is anything to refuse.
     *
     * <p>
     * It is done here rather than in the model generator because the comparison has to be written in
     * the property's own Java shape, and this is the pass that has resolved it - a boxed
     * {@code Objects.equals} against a bare literal is the {@code #7237} class of guard that reads as
     * authored and is never true.
     *
     * @param entities the entities
     */
    private static void resolveRollupGuards(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            for (Map<String, Object> guard : asMaps(entity.get("rollupGuards"))) {
                List<Map<String, Object>> filter = asMaps(guard.get("filter"));
                if (!filter.isEmpty()) {
                    guard.put("filterChain", JavaLiterals.criteriaChain(filter));
                    guard.put("incomingMatch", incomingRowMatch(entity, filter));
                }
                String message = str(guard, "message");
                if (message != null && !message.isEmpty()) {
                    guard.put("messageExpression", guardMessageExpression(message, guard));
                }
                if (truthy(guard, "guardStatusProperty")) {
                    // The targeted write path has to run the gated guards too - a workflow setter is how
                    // a document reaches the gate status - and Velocity cannot ask a list whether any of
                    // its entries carries a key, so the answer is computed once here.
                    entity.put("hasGatedRollupGuards", "true");
                }
            }
        }
    }

    /**
     * The row being written, tested against the roll-up's filter in Java - the clauses ANDed, each in
     * the shape its own property carries.
     *
     * @param entity the child entity
     * @param filter the filter clauses
     * @return the Java boolean expression
     */
    private static String incomingRowMatch(Map<String, Object> entity, List<Map<String, Object>> filter) {
        StringBuilder match = new StringBuilder();
        for (Map<String, Object> clause : filter) {
            String property = str(clause, "property");
            String operator = str(clause, "op");
            String value = JavaLiterals.valueExpression(clause.get("value"));
            if (property == null || value == null) {
                continue;
            }
            Map<String, Object> declared = findProperty(entity, property);
            String javaClass = declared == null ? null : str(declared, "dataTypeJavaClass");
            String read = "entity." + property;
            String equal;
            if ("Boolean".equals(javaClass) || "String".equals(javaClass)) {
                equal = "java.util.Objects.equals(" + read + ", " + value + ")";
            } else {
                // A number is compared BY VALUE: the literal's own width says nothing about the
                // column's, and a boxed equality across two of them is silently never true.
                equal = "(" + read + " != null && " + read + ".longValue() == " + value + "L)";
            }
            if (match.length() > 0) {
                match.append(" && ");
            }
            match.append("ne".equals(operator) ? "!" + equal : equal);
        }
        return match.toString();
    }

    /**
     * The authored refusal message as a Java expression, with the four figures the guard has in hand
     * spliced in where the author placed them.
     *
     * @param message the authored message
     * @param guard the guard
     * @return the Java string expression
     */
    private static String guardMessageExpression(String message, Map<String, Object> guard) {
        String capacity = "guardParent." + str(guard, "capacityField");
        Map<String, String> figures = new LinkedHashMap<>();
        figures.put("{capacity}", capacity);
        figures.put("{sum}", "guardConsumed");
        figures.put("{requested}", "guardIncoming");
        figures.put("{remaining}", capacity + ".subtract(guardConsumed)");
        StringBuilder expression = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        int index = 0;
        while (index < message.length()) {
            String matched = null;
            for (String token : figures.keySet()) {
                if (message.startsWith(token, index)) {
                    matched = token;
                    break;
                }
            }
            if (matched == null) {
                literal.append(message.charAt(index));
                index++;
                continue;
            }
            appendLiteral(expression, literal);
            expression.append(expression.length() == 0 ? "" : " + ")
                      .append(figures.get(matched));
            index += matched.length();
        }
        appendLiteral(expression, literal);
        return expression.length() == 0 ? "\"\"" : expression.toString();
    }

    private static void appendLiteral(StringBuilder expression, StringBuilder literal) {
        if (literal.length() == 0) {
            return;
        }
        if (expression.length() > 0) {
            expression.append(" + ");
        }
        expression.append('"')
                  .append(JavaLiterals.escape(literal.toString()))
                  .append('"');
        literal.setLength(0);
    }

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

    /**
     * Resolves the lock a composition child inherits from its masters. A master's lock reaches down the
     * WHOLE composition chain, not one hop (#7550): a payslip line is as frozen as its payslip while
     * the payroll run above both is posted, even though the payslip declares no lock of its own. So the
     * child carries its direct master as {@code masterLock} (with that master's own lock, which may be
     * none) plus {@code masterLock.ancestors}: every further master up to the farthest one that locks,
     * each with the FK leading to it from the level below and its own lock. The climb stops at a master
     * declaring {@code locksWithMaster: false} - it outlives the lock above it, and so does everything
     * below it.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
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
            List<Map<String, Object>> ancestors = lockingAncestors(entities, entity, parent, parameters);
            if (!locks(parent) && ancestors.isEmpty()) {
                continue;
            }
            Map<String, Object> masterLock = lockLevel(parentFk, parent, parameters);
            masterLock.put("ancestors", ancestors);
            entity.put("masterLock", masterLock);
        }
    }

    /**
     * The masters above {@code parent}, nearest first, up to the farthest one that locks - the
     * intermediate ones are kept, being the hops a write walks to reach it. Empty when nothing above
     * {@code parent} locks.
     */
    private static List<Map<String, Object>> lockingAncestors(List<Map<String, Object>> entities, Map<String, Object> entity,
            Map<String, Object> parent, Map<String, Object> parameters) {
        List<Map<String, Object>> chain = new ArrayList<>();
        int lockingDepth = 0;
        Set<String> visited = new HashSet<>(List.of(str(entity, "name"), str(parent, "name")));
        Map<String, Object> level = parent;
        while (!"false".equals(str(level, "locksWithMaster"))) {
            Map<String, Object> fk = findCompositionProperty(level);
            Map<String, Object> master = fk == null ? null : findEntity(entities, str(fk, "relationshipEntityName"));
            if (master == null || !visited.add(str(master, "name"))) {
                break;
            }
            chain.add(lockLevel(fk, master, parameters));
            if (locks(master)) {
                lockingDepth = chain.size();
            }
            level = master;
        }
        return new ArrayList<>(chain.subList(0, lockingDepth));
    }

    /**
     * One level of an inherited lock: the master reached through {@code fk}, where its generated entity
     * and repository live, and the master's own lock - append-only, a status scope, a period.
     */
    private static Map<String, Object> lockLevel(Map<String, Object> fk, Map<String, Object> master, Map<String, Object> parameters) {
        String perspective = NamingHelper.sanitizeJavaIdentifier(str(fk, "relationshipEntityPerspectiveName"));
        String masterPackage = "gen." + str(parameters, "javaGenFolderName") + ".data." + perspective + ".";
        Map<String, Object> level = new LinkedHashMap<>();
        level.put("fkProperty", fk.get("name"));
        level.put("fkJavaClass", fk.get("dataTypeJavaClass"));
        level.put("entity", master.get("name"));
        level.put("entityClass", masterPackage + str(master, "name") + "Entity");
        level.put("repositoryClass", masterPackage + str(master, "name") + "Repository");
        level.put("always", truthy(master, "immutableAlways"));
        level.put("statusProperty", str(master, "immutableStatusProperty"));
        level.put("statusValues", str(master, "immutableStatusValues"));
        if (master.get("periodLock") != null) {
            level.put("period", master.get("periodLock"));
        }
        return level;
    }

    /** Whether the entity declares a lock of its own: append-only, a status scope or a period. */
    private static boolean locks(Map<String, Object> entity) {
        String statusProperty = str(entity, "immutableStatusProperty");
        return truthy(entity, "immutableAlways") || (statusProperty != null && !statusProperty.isEmpty())
                || entity.get("periodLock") != null;
    }

    /**
     * Resolves the cross-model parents of each junction's refusing {@code checks: agree} (#7701) into
     * the {@code targetAgreementRules} its generated repository contributes as a
     * {@code TargetAgreementRule}: keyed by the parent's generated entity class - what the parent's
     * repository names itself by when it asks - next to the junction's foreign key, the parent's
     * relied-on property and the check's message as a Java literal. The entity class is the twin of the
     * repository the dropdown resolution already placed on the foreign key, so a parent owned by
     * another model resolves into that model's package. A foreign key that resolved no repository is
     * dropped rather than emitted as a broken reference.
     *
     * @param entities every entity in the model
     */
    private static void resolveTargetAgreementRules(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            List<Map<String, Object>> rules = new ArrayList<>();
            for (Map<String, Object> check : asMaps(entity.get("checks"))) {
                if (!"agree".equals(str(check, "kind")) || "warn".equals(str(check, "severity"))) {
                    continue;
                }
                for (Map<String, Object> parent : asMaps(check.get("crossModelParents"))) {
                    String repositoryClass = null;
                    for (Map<String, Object> property : asMaps(entity.get("properties"))) {
                        if (str(parent, "fkProperty") != null && str(parent, "fkProperty").equals(str(property, "name"))) {
                            repositoryClass = str(property, "targetRepositoryClass");
                        }
                    }
                    if (repositoryClass == null || !repositoryClass.endsWith("Repository")) {
                        continue;
                    }
                    Map<String, Object> rule = new LinkedHashMap<>();
                    rule.put("targetEntityClass",
                            repositoryClass.substring(0, repositoryClass.length() - "Repository".length()) + "Entity");
                    rule.put("fkProperty", str(parent, "fkProperty"));
                    rule.put("property", str(parent, "property"));
                    rule.put("message", str(check, "message"));
                    resolveMessageLiteral(rule);
                    rules.add(rule);
                }
            }
            if (!rules.isEmpty()) {
                entity.put("targetAgreementRules", rules);
                entity.put("hasTargetAgreementRules", Boolean.TRUE);
            }
        }
    }

    /**
     * Resolves each entity's {@code agreeGuards} (the parent side of a junction's
     * {@code checks: agree}, #7589) into the junction's generated repository, so the DAO can ask
     * whether a junction row still references the record before it lets the agreed property change -
     * same-model only, exactly like {@link #inheritMasterLock} resolves a composition parent's
     * coordinates. The message gets its Java-literal twin here, as a check's does.
     *
     * @param entities every entity in the model
     * @param parameters the generation parameters
     */
    private static void resolveAgreeGuards(List<Map<String, Object>> entities, Map<String, Object> parameters) {
        for (Map<String, Object> entity : entities) {
            List<Map<String, Object>> guards = asMaps(entity.get("agreeGuards"));
            if (guards.isEmpty()) {
                continue;
            }
            List<Map<String, Object>> resolved = new ArrayList<>();
            for (Map<String, Object> guard : guards) {
                Map<String, Object> referencing = findEntity(entities, str(guard, "referencingEntity"));
                if (referencing == null) {
                    continue; // the junction was not generated - drop rather than emit a broken reference
                }
                String referencingPerspective = NamingHelper.sanitizeJavaIdentifier(str(referencing, "perspectiveName"));
                guard.put("repositoryClass", "gen." + str(parameters, "javaGenFolderName") + ".data." + referencingPerspective + "."
                        + str(referencing, "name") + "Repository");
                resolveMessageLiteral(guard);
                resolved.add(guard);
            }
            entity.put("agreeGuards", resolved);
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
            // The scope comes from the parent; the writes need not. A child whose composition edge
            // declares personalReadOnly (intent #7340) is see-only on the personal surface even though
            // the parent it inherits the scope from is writable - the shape of a user-authored header
            // whose lines only an engine writes.
            entity.put("personalReadOnly", truthy(parent, "personalReadOnly") || truthy(parentFk, "relationshipPersonalReadOnly"));
            entity.put("personalIdentityProperty", parent.get("personalIdentityProperty"));
            entity.put("personalIdentityLabel", parent.get("personalIdentityLabel"));
            entity.put("personalIdentityEntityClass", parent.get("personalIdentityEntityClass"));
            entity.put("personalIdentityRepositoryClass", parent.get("personalIdentityRepositoryClass"));
        }
    }

    /**
     * Whether a document master's inline ITEMS panel is see-only on the personal surface - the master's
     * own see-only flag, or the items child's own opt-out (intent #7340: {@code personalReadOnly} on
     * the child's composition edge). The items panel lives on the MASTER's page, so the flag the
     * document template gates Add / Fill Month / row delete / item save on has to be the master's, and
     * a child that refuses the writes with 403 must not be offered them.
     *
     * <p>
     * Derived here rather than emitted into the model, so a hand-authored {@code .edm} carrying the
     * child's attribute gets the same page.
     *
     * @param entities every entity in the model
     */
    private static void resolveDocumentItemsScope(List<Map<String, Object>> entities) {
        for (Map<String, Object> entity : entities) {
            String itemsEntity = str(entity, "documentItemsEntity");
            if (itemsEntity == null || itemsEntity.isEmpty()) {
                continue;
            }
            Map<String, Object> items = findEntity(entities, itemsEntity);
            entity.put("documentItemsReadOnly", truthy(entity, "personalReadOnly") || (items != null && truthy(items, "personalReadOnly")));
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
                entity.put("myChildren", scopedChildren(entities, entity, "personalParent", "MyController", true, "personalReadOnly"));
            }
        }
        for (Map<String, Object> entity : entities) {
            if (entity.get("partnerProperty") != null || entity.get("partnerParent") != null) {
                entity.put("partnerChildren", scopedChildren(entities, entity, "partnerParent", "PartnerController", false, null));
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
     * @param readOnlyKey the child attribute marking the panel see-only, or null when the scope has no
     *        such opt-out
     * @return the panel descriptors
     */
    private static List<Object> scopedChildren(List<Map<String, Object>> entities, Map<String, Object> parent, String scopeKey,
            String controllerSuffix, boolean withCalendar, String readOnlyKey) {
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
            // A see-only child (intent personalReadOnly) refuses the panel's Add with 403, so the panel
            // must not offer it.
            panel.put("readOnly", readOnlyKey != null && truthy(child, readOnlyKey));
            // A status-gated panel (intent `visibleWhen:`, #7502): the terms the scoped page reads
            // against the record it holds before showing the panel. Absent = always shown.
            if (child.get("visibleWhenTermsJs") != null) {
                panel.put("visibleWhen", child.get("visibleWhenTermsJs"));
            }
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
            for (Object part : kept) {
                resolveLabelPartLiterals(asMap(part));
            }
            entity.put("labelParts", kept);
            entity.put("hasLabel", Boolean.TRUE);
        }
    }

    /**
     * Derives the escaped twins of a label part's authored text, for the generated name computation
     * that writes them into Java string literals.
     *
     * <p>
     * A label pattern is prose an author writes around the fields it interpolates - a quote in a
     * literal segment, or in a format, is interpolated verbatim into the {@code computeName} body,
     * where it ends the literal it is written into and fails the compile of every generated class of
     * the module (#7295, the #7241 class). The raw value stays for the surfaces that render it as text;
     * only the Java site reads the twin.
     *
     * @param part the label part
     */
    private static void resolveLabelPartLiterals(Map<String, Object> part) {
        if (part == null) {
            return;
        }
        String text = str(part, "text");
        if (text != null) {
            part.put("textJavaLiteral", JavaLiterals.escape(text));
        }
        String format = str(part, "format");
        if (format != null) {
            part.put("formatJavaLiteral", JavaLiterals.escape(format));
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
                if (resolveNormalizeLiterals(property)) {
                    entity.put("hasNormalize", Boolean.TRUE);
                }
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
            property.put("widgetPatternJs", "'" + JsLiterals.escape(widgetPattern) + "'");
            // The same expression as the body of a Java string literal, without the quotes: an
            // unescaped backslash would make the generated controller fail to compile, and the
            // client Java batch is all-or-nothing.
            property.put("widgetPatternJava", JavaLiterals.escape(widgetPattern));
        }
        if (property.get("widgetOptionsFilterBy") != null && property.containsKey("widgetOptionsFilterValue")) {
            String raw = String.valueOf(property.get("widgetOptionsFilterValue"));
            property.put("widgetOptionsFilterValueJs", raw.matches("-?\\d+(\\.\\d+)?") ? raw : "'" + JsLiterals.escape(raw) + "'");
        }
    }

    /**
     * Pre-renders a property's {@code normalize:} (#7726) for the templates: the characters it strips
     * as the body of a Java string literal ({@code normalizeStripJava}), and the whole step as a
     * JavaScript function ({@code normalizeJs}) the generated form applies on blur - the same
     * transforms, in the same order, as {@code org.eclipse.dirigible.sdk.db.Normalize} on the server.
     * The function is written so it can sit verbatim inside a double-quoted HTML attribute and inside a
     * JavaScript file: every character of the strip set outside a plain safe set is a {@code \\uXXXX}
     * escape.
     *
     * @param property the property
     * @return whether the property normalizes anything
     */
    private static boolean resolveNormalizeLiterals(Map<String, Object> property) {
        boolean trim = "true".equals(str(property, "normalizeTrim"));
        Object stripValue = property.get("normalizeStrip");
        String strip = stripValue == null ? null : stripValue.toString();
        String caseFold = str(property, "normalizeCase");
        boolean hasStrip = strip != null && !strip.isEmpty();
        boolean hasCase = "upper".equals(caseFold) || "lower".equals(caseFold);
        if (!trim && !hasStrip && !hasCase) {
            return false;
        }
        StringBuilder js = new StringBuilder("((v) => typeof v === 'string' ? v");
        if (trim) {
            js.append(".trim()");
        }
        if (hasStrip) {
            property.put("normalizeStripJava", JavaLiterals.escape(strip));
            js.append(".split('').filter((c) => !'")
              .append(attributeSafeJs(strip))
              .append("'.includes(c)).join('')");
        }
        if (hasCase) {
            js.append("upper".equals(caseFold) ? ".toUpperCase()" : ".toLowerCase()");
        }
        js.append(" : v)");
        property.put("normalizeJs", js.toString());
        return true;
    }

    /**
     * The body of a single-quoted JavaScript string that is also safe inside a double-quoted HTML
     * attribute: letters, digits and a few punctuation marks verbatim, everything else as
     * {@code \\uXXXX}.
     */
    private static String attributeSafeJs(String raw) {
        StringBuilder out = new StringBuilder(raw.length() * 2);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) && c < 128 || " -_.,:;()+/*=!?@%".indexOf(c) >= 0) {
                out.append(c);
            } else {
                out.append(String.format("\\u%04x", (int) c));
            }
        }
        return out.toString();
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
     * Collects the default read and write roles the model asks to be generated - each gate's own role,
     * its first (see {@link ModelValues#defaultRole}). A projection owns no table, and a report or
     * filter is read-only, so neither contributes a write role.
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
            String roleRead = defaultRole(entity, "roleRead");
            if (roleRead != null) {
                rolePair.put("roleRead", roleRead);
            }
            String roleWrite = defaultRole(entity, "roleWrite");
            if (!"REPORT".equals(type) && !"FILTER".equals(type) && roleWrite != null) {
                rolePair.put("roleWrite", roleWrite);
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
