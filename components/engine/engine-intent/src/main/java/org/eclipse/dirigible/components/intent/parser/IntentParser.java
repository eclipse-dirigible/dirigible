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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static final Set<String> FIELD_TYPES =
            Set.of("string", "text", "integer", "int", "long", "decimal", "double", "boolean", "date", "timestamp", "uuid");
    /**
     * Primary keys must be an integer type - the codbex model convention is integer identifiers
     * (auto-increment), and a non-integer auto-increment column is invalid SQL on most databases.
     */
    private static final Set<String> INTEGER_PK_TYPES = Set.of("integer", "int", "long");
    private static final Set<String> RELATION_KINDS = Set.of("oneToMany", "manyToOne", "oneToOne", "manyToMany");
    private static final Set<String> STEP_KINDS = Set.of("userTask", "serviceTask", "decision", "script", "end");

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
        String json = GSON.toJson(tree);
        IntentModel model = GSON.fromJson(json, IntentModel.class);
        if (model == null) {
            return new IntentModel();
        }
        validate(model);
        return model;
    }

    /**
     * Run all structural checks. Collects every issue before throwing so authors get one complete error
     * message rather than playing whack-a-mole.
     */
    private static void validate(IntentModel model) {
        List<String> issues = new ArrayList<>();
        Set<String> entityNames = validateEntities(model, issues);
        validateProcesses(model, entityNames, issues);
        validateForms(model, entityNames, issues);
        validateReports(model, entityNames, issues);
        validateSeeds(model, entityNames, issues);
        if (!issues.isEmpty()) {
            throw new IntentValidationException(issues);
        }
    }

    private static Set<String> validateEntities(IntentModel model, List<String> issues) {
        Set<String> entityNames = new HashSet<>();
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
            }
            if (idCount > 1) {
                issues.add("entity [" + name + "] declares " + idCount + " primary-key fields - exactly one is allowed");
            }
        }
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() == null) {
                continue;
            }
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
                if (relation.isComposition() && !"manyToOne".equals(relation.getKind()) && !"oneToOne".equals(relation.getKind())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName()
                            + "] is marked composition but only a manyToOne/oneToOne relation can be a composition");
                }
                if (relation.getTo() == null || relation.getTo()
                                                        .isBlank()) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] has no target");
                } else if (!entityNames.contains(relation.getTo())) {
                    issues.add("entity [" + entity.getName() + "] relation [" + relation.getName() + "] points to unknown entity ["
                            + relation.getTo() + "]");
                }
            }
        }
        return entityNames;
    }

    private static void validateProcesses(IntentModel model, Set<String> entityNames, List<String> issues) {
        Set<String> processNames = new HashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            if (process.getName() == null || process.getName()
                                                    .isBlank()) {
                issues.add("process has no name");
                continue;
            }
            if (!processNames.add(process.getName())) {
                issues.add("duplicate process [" + process.getName() + "]");
            }
            Object onCreate = process.getTrigger()
                                     .get("onCreate");
            if (onCreate != null && !entityNames.contains(onCreate.toString())) {
                issues.add("process [" + process.getName() + "] trigger onCreate references unknown entity [" + onCreate + "]");
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
        }
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
        if (!"end".equalsIgnoreCase(target) && !stepNames.contains(target)) {
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
            if (form.getForEntity() != null && !form.getForEntity()
                                                    .isBlank()
                    && !entityNames.contains(form.getForEntity())) {
                issues.add("form [" + form.getName() + "] references unknown entity [" + form.getForEntity() + "]");
            }
        }
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
        }
    }

    private static void validateSeeds(IntentModel model, Set<String> entityNames, List<String> issues) {
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
            if (seed.getRows()
                    .isEmpty()) {
                issues.add("seed [" + seed.getName() + "] has no rows");
            }
        }
    }
}
