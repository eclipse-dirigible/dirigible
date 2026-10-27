/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * Renders the generated DAO repository through the platform's {@link VelocityGenerationEngine} to
 * cover all three {@code whenTargetDeleted} rules a referencing repository contributes (dirigible
 * #7547) - the branches a published fixture would otherwise need an entity per rule to reach.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context and uses
 * its own engine instance. An unrendered Velocity branch is the failure mode this guards against -
 * a typo in a variable reference emits itself literally as {@code ${...}} and only fails later, in
 * a user's project, at compile time.
 */
class TargetDeleteRuleRepositoryTemplateIT {

    private static final String TEMPLATE = "/META-INF/dirigible/template-application-dao-java/data/Repository.java.template";

    private static final String CATEGORY = "gen.expenses.data.expenses.ExpenseCategoryEntity";
    private static final String EMPLOYEE = "gen.hr.data.employees.EmployeeEntity";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void eachRuleIsContributedToTheTargetsDelete() throws Exception {
        String rendered = render(context(true));

        assertTrue(
                rendered.contains("public class ExpenseClaimRepository extends JavaRepository<ExpenseClaimEntity>"
                        + " implements org.eclipse.dirigible.sdk.db.TargetDeleteRule {"),
                "the repository must contribute the rules: " + rendered);

        String count = method(rendered, "public int countRestricting(String targetEntity, Object targetId) {");
        assertTrue(count.contains("\"" + CATEGORY + "\".equals(targetEntity)") && count.contains(".eq(\"Category\", targetId)"),
                "restrict counts its references: " + count);
        assertTrue(count.contains("restricting += count("), "the references are counted in the database, never loaded: " + count);
        assertFalse(count.contains("Employee") || count.contains("Reviewer"), "only a restricting relation counts: " + count);

        String release = method(rendered, "public void release(String targetEntity, Object targetId) {");
        assertTrue(
                release.contains(".eq(\"Reviewer\", targetId)") && release.contains("updateProperty(referencing.Id, \"Reviewer\", null);"),
                "nullify clears the foreign key through the targeted write: " + release);
        assertTrue(release.contains(".eq(\"Employee\", targetId)") && release.contains("delete(referencing);"),
                "cascade deletes the referencing record through this repository: " + release);
        assertFalse(release.contains("Category"), "a restricting relation releases nothing: " + release);

        assertTrue(rendered.contains("return \"Expense Claim\";"), "the refusal names the referencing records by their label");
        for (String line : rendered.split("\n")) {
            assertFalse(line.contains("${") || line.contains("$property"), "an unresolved template reference survived: " + line);
        }
    }

    @Test
    void anEntityWithNoRulesContributesNothing() throws Exception {
        String rendered = render(context(false));

        assertFalse(rendered.contains("TargetDeleteRule"), "an entity referencing nothing must carry none of this machinery");
        assertFalse(rendered.contains("countRestricting") || rendered.contains("public void release("),
                "an entity referencing nothing must carry none of this machinery");
    }

    /**
     * dirigible #7986: a relation carrying a target key renders the existence check, called on create
     * with nothing stored to compare against and on both update paths against the stored row.
     */
    @Test
    void aReferencedRowIsCheckedBeforeTheWrite() throws Exception {
        Map<String, Object> parameters = context(false);
        Map<String, Object> category = relation("Category", "restrict", CATEGORY);
        category.put("relationshipEntityName", "ExpenseCategory");
        category.put("targetRepositoryClass", "gen.expenses.data.expenses.ExpenseCategoryRepository");
        category.put("targetKeyProperty", "Id");
        @SuppressWarnings("unchecked")
        List<Object> properties = (List<Object>) parameters.get("properties");
        properties.add(category);
        parameters.put("hasReferenceTargets", Boolean.TRUE);

        String rendered = render(parameters);

        assertTrue(rendered.contains("import org.eclipse.dirigible.sdk.db.ValidationException;"), "the refusal type is imported");
        assertTrue(rendered.contains("requireReferencedRowsExist(entity, true);"), "create checks every relation: " + rendered);
        assertEquals(2, rendered.split("requireReferencedRowsExist\\(entity, false\\);", -1).length - 1,
                "update and updateWithoutEvent check the changed relations: " + rendered);
        String check = method(rendered, "private void requireReferencedRowsExist(ExpenseClaimEntity entity, boolean creating) {");
        assertTrue(check.contains("new gen.expenses.data.expenses.ExpenseCategoryRepository().count(")
                && check.contains(".eq(\"Id\", entity.Category)"), "the target is counted by its key, never loaded: " + check);
        assertTrue(check.contains("\"ExpenseClaim.Category: there is no ExpenseCategory with Id \""),
                "the refusal names the relation: " + check);
        for (String line : rendered.split("\n")) {
            assertFalse(line.contains("${") || line.contains("$property") || line.contains("$creating"),
                    "an unresolved template reference survived: " + line);
        }
    }

    private String render(Map<String, Object> parameters) throws Exception {
        String template;
        try (InputStream in = getClass().getResourceAsStream(TEMPLATE)) {
            assertNotNull(in, "template resource not found on classpath: " + TEMPLATE);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, TEMPLATE, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** The body of the method declared by {@code signature}, up to its closing brace. */
    private static String method(String rendered, String signature) {
        int start = rendered.indexOf(signature);
        assertTrue(start >= 0, "missing " + signature + " in: " + rendered);
        return rendered.substring(start, rendered.indexOf("\n    }\n", start));
    }

    /** The minimal DAO template context for an entity with no compositions, checks, or history. */
    private static Map<String, Object> context(boolean withRules) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("name", "ExpenseClaim");
        parameters.put("entityLabel", "Expense Claim");
        parameters.put("projectName", "expenses");
        parameters.put("perspectiveName", "Expenses");
        parameters.put("javaGenFolderName", "expenses");
        parameters.put("javaPerspectiveName", "expenses");
        parameters.put("pkPropertyName", "Id");
        parameters.put("dataSource", "DefaultDB");
        List<Object> properties = new ArrayList<>(List.of(primaryKey()));
        if (withRules) {
            properties.add(relation("Category", "restrict", CATEGORY));
            properties.add(relation("Employee", "cascade", EMPLOYEE));
            properties.add(relation("Reviewer", "nullify", EMPLOYEE));
            parameters.put("hasTargetDeleteRules", Boolean.TRUE);
        }
        parameters.put("properties", properties);
        parameters.put("preservedOnUpdate", new ArrayList<>());
        parameters.put("reportedOnUpdate", new ArrayList<>());
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "EXPENSE_CLAIM_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> relation(String name, String rule, String targetEntityClass) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", "EXPENSE_CLAIM_" + name.toUpperCase());
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("whenTargetDeleted", rule);
        property.put("targetEntityClass", targetEntityClass);
        return property;
    }
}
