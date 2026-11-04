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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * A {@code requiredWhen} over the document's LINES (dirigible #7560), rendered through the
 * platform's {@link VelocityGenerationEngine}: the repository's gate reads the lines, tests each
 * against the authored condition and refuses an empty header value when any line matches - the
 * legal ground a zero-rated line calls for.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context.
 */
class RequiredWhenAnyItemRepositoryTemplateIT {

    private static final String DAO_BASE = "/META-INF/dirigible/template-application-dao-java/data/";
    private static final String ANY_ITEM =
            "((item == null ? null : item.VatRate) != null && new java.math.BigDecimal(String.valueOf((item == null ? null : item.VatRate))).compareTo(new java.math.BigDecimal(\"0\")) == 0)";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void theGateReadsTheLinesAndRequiresTheValueWhenAnyMatches() throws Exception {
        String rendered = render(context(check(null)));

        assertTrue(rendered.contains("if (entity.Status != null && entity.Status == 2) {"), "gated on the authored status: " + rendered);
        assertTrue(rendered.contains("for (InvoiceLineEntity item : new InvoiceLineRepository().findAll("),
                "the lines must be read at the gate: " + rendered);
        assertTrue(rendered.contains("Criteria.create().eq(\"Invoice\", entity.Id)"), "the document's own lines: " + rendered);
        assertTrue(rendered.contains("if (" + ANY_ITEM + ") {"), "each line tested against the authored condition: " + rendered);
        assertTrue(rendered.contains(
                "if ((anyItem1) && (requiredValue == null || org.eclipse.dirigible.sdk.db.Normalize.isBlank(String.valueOf(requiredValue)))) {"),
                "the value is required when any line matched: " + rendered);
        assertTrue(rendered.contains("import org.eclipse.dirigible.components.data.store.java.repository.Criteria;"),
                "the items query needs Criteria: " + rendered);
    }

    /** A record-local condition alongside it is ANDed - both must hold for the value to be required. */
    @Test
    void aRecordConditionAndTheLineConditionAreBothRequired() throws Exception {
        String rendered = render(context(check("java.util.Objects.equals(entity.Kind, \"export\")")));

        assertTrue(rendered.contains("if (((java.util.Objects.equals(entity.Kind, \"export\")) && anyItem1) && (requiredValue == null"),
                "the two conditions are ANDed: " + rendered);
    }

    /** A requiredWhen without a line condition renders exactly as it did before #7560. */
    @Test
    void aRequiredWhenWithoutALineConditionReadsNoLines() throws Exception {
        Map<String, Object> check = check("java.util.Objects.equals(entity.Kind, \"export\")");
        check.remove("anyItemJavaExpression");
        check.remove("itemsEntity");
        check.remove("itemsFk");
        String rendered = render(context(check));

        assertFalse(rendered.contains("InvoiceLineRepository"), "no lines are read: " + rendered);
        assertTrue(rendered.contains("if ((java.util.Objects.equals(entity.Kind, \"export\")) && (requiredValue == null"),
                "the record-local condition alone: " + rendered);
    }

    private String render(Map<String, Object> parameters) throws Exception {
        String location = DAO_BASE + "Repository.java.template";
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** The gated check as ModelParameterProcessor hands it to the template. */
    private static Map<String, Object> check(String guardJavaExpression) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("kind", "requiredWhen");
        check.put("status", "2");
        check.put("statusProperty", "Status");
        check.put("valueExpression", "entity.VatGround");
        check.put("label", "VatGround");
        check.put("messageArgsJava", "\"sales:sales-model.checks.Invoice_requiredWhen_0\", \"A zero-rated line needs its legal ground\"");
        check.put("itemsEntity", "InvoiceLine");
        check.put("itemsFk", "Invoice");
        check.put("anyItemJavaExpression", ANY_ITEM);
        if (guardJavaExpression != null) {
            check.put("guardJavaExpression", guardJavaExpression);
        }
        return check;
    }

    private static Map<String, Object> context(Map<String, Object> check) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Invoice");
        parameters.put("projectName", "billing");
        parameters.put("perspectiveName", "Invoice");
        parameters.put("javaGenFolderName", "billing");
        parameters.put("javaPerspectiveName", "invoice");
        parameters.put("tablePrefix", "BILLING_");
        parameters.put("dataName", "INVOICE");
        parameters.put("pkPropertyName", "Id");
        parameters.put("properties",
                List.of(primaryKey(), property("Status", "INTEGER", "Integer"), property("VatGround", "VARCHAR", "String")));
        parameters.put("sensitiveProperties", List.of());
        parameters.put("documentChecks", List.of(check));
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = property("Id", "INTEGER", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> property(String name, String dataType, String javaClass) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", "INVOICE_" + name.toUpperCase());
        property.put("dataType", dataType);
        property.put("dataTypeJavaClass", javaClass);
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("dataNotNull", Boolean.FALSE);
        return property;
    }
}
