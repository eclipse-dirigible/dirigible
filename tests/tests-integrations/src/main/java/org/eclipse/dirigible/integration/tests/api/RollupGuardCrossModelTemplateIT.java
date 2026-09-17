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
 * The capacity guard of a roll-up whose PARENT is owned by another model, rendered through the
 * platform's {@link VelocityGenerationEngine} (dirigible #7410).
 *
 * <p>
 * The guard lives in the CHILD's repository, and on this direction the child is local - so the
 * check is generated here whichever model owns the parent. What differs is how the parent is
 * addressed: by its fully-qualified generated type out of the owner's gen folder, never through an
 * import, which could collide with a local entity of the same name. A local parent must keep
 * rendering exactly as it did, import and all.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context - the shape
 * {@code PersonalSurfaceCreateValidationTemplateIT} established.
 */
class RollupGuardCrossModelTemplateIT {

    private static final String DAO_BASE = "/META-INF/dirigible/template-application-dao-java/data/";
    private static final String OWNER_TYPE = "gen.customer_payments.data.customerpayment.CustomerPayment";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void aForeignParentIsAddressedByItsFullyQualifiedGeneratedType() throws Exception {
        String rendered = render(context(guard("customer_payments")));

        assertTrue(rendered.contains(OWNER_TYPE + "Entity guardParent = new " + OWNER_TYPE + "Repository().findById("),
                "the guard must load the parent out of the owner's gen folder: " + rendered);
        assertTrue(rendered.contains("guardParent.Amount != null"),
                "the guard must read the foreign parent's capacity column: " + rendered);
        assertTrue(rendered.contains("throw new ValidationException(\"CustomerPayment capacity exceeded"),
                "the refusal must name the parent: " + rendered);
    }

    @Test
    void aForeignParentIsNeverImported() throws Exception {
        String rendered = render(context(guard("customer_payments")));

        assertFalse(rendered.contains("import gen.sales_invoices.data.customerpayment.CustomerPaymentEntity;"),
                "a foreign parent must not be imported from THIS project's gen folder - it does not live there: " + rendered);
        assertFalse(rendered.contains("import " + OWNER_TYPE + "Entity;"),
                "the foreign parent is addressed inline, so nothing this DAO imports can collide with it: " + rendered);
    }

    /** The guard runs on both write paths, or a create or an edit could overdraw unchecked. */
    @Test
    void theForeignParentGuardRunsOnCreateAndOnUpdate() throws Exception {
        String rendered = render(context(guard("customer_payments")));

        int first = rendered.indexOf(OWNER_TYPE + "Entity guardParent");
        int second = rendered.indexOf(OWNER_TYPE + "Entity guardParent", first + 1);
        assertTrue(first >= 0 && second > first, "the guard must be emitted for both the create and the update path: " + rendered);
    }

    @Test
    void aLocalParentKeepsItsImportAndItsPlainName() throws Exception {
        String rendered = render(context(guard("")));

        assertTrue(rendered.contains("import gen.sales_invoices.data.customerpayment.CustomerPaymentEntity;"),
                "a local parent is still imported from this project's gen folder: " + rendered);
        assertTrue(rendered.contains("CustomerPaymentEntity guardParent = new CustomerPaymentRepository().findById("),
                "a local parent is still addressed by its plain name: " + rendered);
        assertFalse(rendered.contains(OWNER_TYPE), "a local parent must carry no owner-model package: " + rendered);
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

    /**
     * The guard the EDM generator stamps on the link entity. An empty gen folder is the local parent -
     * the value {@code EdmIntentGenerator} writes when {@code via} carries no {@code model:}.
     */
    private static Map<String, Object> guard(String parentGenFolder) {
        Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("parentEntity", "CustomerPayment");
        guard.put("parentPerspective", "customerpayment");
        guard.put("parentGenFolder", parentGenFolder);
        guard.put("fkProperty", "CustomerPayment");
        guard.put("capacityField", "Amount");
        guard.put("ofField", "Amount");
        guard.put("childIdField", "Id");
        return guard;
    }

    /** The allocation link entity: local rows, a pot that may be owned by another module. */
    private static Map<String, Object> context(Map<String, Object> rollupGuard) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "SalesInvoiceCustomerPayment");
        parameters.put("projectName", "sales-invoices");
        parameters.put("perspectiveName", "SalesInvoice");
        parameters.put("javaGenFolderName", "sales_invoices");
        parameters.put("javaPerspectiveName", "salesinvoice");
        parameters.put("tablePrefix", "SALES_INVOICES_");
        parameters.put("dataName", "SALES_INVOICE_CUSTOMER_PAYMENT");
        parameters.put("pkPropertyName", "Id");
        parameters.put("properties", List.of(primaryKey(), amount()));
        parameters.put("sensitiveProperties", List.of());
        parameters.put("rollupGuard", rollupGuard);
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "SALES_INVOICE_CUSTOMER_PAYMENT_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> amount() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Amount");
        property.put("dataName", "SALES_INVOICE_CUSTOMER_PAYMENT_AMOUNT");
        property.put("dataType", "DECIMAL");
        property.put("dataTypeJavaClass", "java.math.BigDecimal");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("dataNotNull", Boolean.FALSE);
        return property;
    }
}
