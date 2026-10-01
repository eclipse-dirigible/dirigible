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
 * cover the {@code whenTargetDeleted: restrict} guard (dirigible #7547) - the branch a published
 * app fixture would otherwise need a whole second entity pair to reach.
 *
 * <p>
 * Same reasoning and same shape as {@code ChildLockControllerTemplateIT}: rendering needs nothing
 * from a running instance, so this boots no application context and uses its own engine instance.
 * An unrendered Velocity branch is the failure mode this guards against - a typo in a variable
 * reference emits itself literally as {@code ${...}} and only fails later, in a user's project, at
 * compile time.
 */
class DeleteRestrictorRepositoryTemplateIT {

    private static final String TEMPLATE = "/META-INF/dirigible/template-application-dao-java/data/Repository.java.template";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void deleteAndDeleteByIdBothRefuseWhileAReferenceExists() throws Exception {
        String rendered = render(context(List.of(restrictor())));

        assertTrue(rendered.contains("import org.eclipse.dirigible.sdk.db.DeleteRestrictionException;"),
                "the guard's exception must be imported");
        assertTrue(rendered.contains("private void requireNotReferenced(Object id)"), "the guard method must be emitted");
        assertTrue(rendered.contains("new gen.expenses.data.expenses.ExpenseRepository()"),
                "the guard must construct the REFERENCING entity's repository directly: " + rendered);
        assertTrue(rendered.contains(".eq(\"Category\", id))"), "the guard must query by the referencing entity's own FK property");
        assertTrue(rendered.contains("throw new DeleteRestrictionException("), "a non-empty match must refuse the delete");
        // The issue asks the refusal to name the referencing entity AND its count.
        assertTrue(rendered.contains(".size();"), "the guard must count the referencing rows, not just test emptiness");
        assertTrue(rendered.contains("is referenced by \" + ExpenseReferencing"), "the refusal must carry the count: " + rendered);
        assertTrue(rendered.contains("Expense record(s) and cannot be deleted"),
                "the refusal must name the referencing entity by its label: " + rendered);

        // Called at the very top of BOTH verbs, before any composition cascade.
        int deleteBody = rendered.indexOf("public void delete(ExpenseCategoryEntity entity) {");
        int deleteByIdBody = rendered.indexOf("public void deleteById(Object id) {");
        assertTrue(deleteBody >= 0 && deleteByIdBody >= 0, "both delete verbs must be present: " + rendered);
        assertTrue(rendered.substring(deleteBody, rendered.indexOf('\n', deleteBody + 60))
                           .contains("requireNotReferenced")
                || rendered.indexOf("requireNotReferenced(entity.Id);") > deleteBody, "delete(entity) must call the guard first");
        assertTrue(rendered.indexOf("requireNotReferenced(id);") > deleteByIdBody, "deleteById(id) must call the guard first");

        assertNoUnresolvedReferences(rendered);
    }

    @Test
    void noRestrictorsEmitsNeitherTheGuardNorItsImport() throws Exception {
        String rendered = render(context(List.of()));

        assertFalse(rendered.contains("DeleteRestrictionException"), "an entity nobody restricts must carry none of this machinery");
        assertFalse(rendered.contains("requireNotReferenced"), "an entity nobody restricts must carry none of this machinery");
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

    /** Asserts every reference the guard touches resolved - an unresolved one renders literally. */
    private static void assertNoUnresolvedReferences(String rendered) {
        for (String line : rendered.split("\n")) {
            if (line.contains("requireNotReferenced") || line.contains("DeleteRestrictionException") || line.contains("Repository()")) {
                assertFalse(line.contains("${") || line.contains("$restrictor") || line.contains("$deleteRestrictors"),
                        "an unresolved template reference survived into the guard: " + line);
            }
        }
    }

    private static Map<String, Object> restrictor() {
        Map<String, Object> restrictor = new LinkedHashMap<>();
        restrictor.put("referencingEntity", "Expense");
        restrictor.put("fkProperty", "Category");
        restrictor.put("entityClass", "gen.expenses.data.expenses.ExpenseEntity");
        restrictor.put("repositoryClass", "gen.expenses.data.expenses.ExpenseRepository");
        restrictor.put("referencingLabel", "Expense");
        return restrictor;
    }

    /** The minimal DAO template context for an entity with no compositions, checks, or history. */
    private static Map<String, Object> context(List<Object> deleteRestrictors) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("name", "ExpenseCategory");
        parameters.put("entityLabel", "Expense Category");
        parameters.put("projectName", "expenses");
        parameters.put("perspectiveName", "Expenses");
        parameters.put("javaGenFolderName", "expenses");
        parameters.put("javaPerspectiveName", "expenses");
        parameters.put("pkPropertyName", "Id");
        parameters.put("dataSource", "DefaultDB");
        parameters.put("properties", List.of(primaryKey(), name()));
        parameters.put("preservedOnUpdate", new ArrayList<>());
        parameters.put("reportedOnUpdate", new ArrayList<>());
        parameters.put("deleteRestrictors", deleteRestrictors);
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "EXPENSE_CATEGORY_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> name() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Name");
        property.put("dataName", "EXPENSE_CATEGORY_NAME");
        property.put("dataType", "VARCHAR");
        property.put("dataTypeJavaClass", "String");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }
}
