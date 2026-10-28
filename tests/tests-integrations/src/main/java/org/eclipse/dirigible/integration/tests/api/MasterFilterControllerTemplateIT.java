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
 * The master filter of a generated child list is declared with the FK's own Java class (dirigible
 * #8237). Declared as a {@code String} and bound as-is into the JPQL against the integer FK, a typo
 * such as {@code ?Invoice=abc} failed inside Hibernate and was answered 500 with its coercion
 * message; typed, the controller invoker refuses it with the same 400 an unparsable {@code $limit}
 * gets. The personal and partner lists of a child carry their parent filter the same way. Rendered
 * through the platform's {@link VelocityGenerationEngine}, like
 * {@code ListPagingControllerTemplateIT}; {@code IntentReversalIT} runs the power controller's
 * typed filter against a published application.
 */
class MasterFilterControllerTemplateIT {

    private static final String REST_BASE = "/META-INF/dirigible/template-application-rest-java/api/";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void theChildListAndUploadBindTheMasterFilterWithTheForeignKeysJavaClass() throws Exception {
        Map<String, Object> context = context();
        context.put("layoutType", "MANAGE_DETAILS");
        context.put("masterEntity", "Invoice");
        context.put("masterEntityId", "Invoice");
        context.put("masterEntityIdJavaClass", "Long");
        // the upload endpoint of an attachment entity binds the same master filter
        context.put("attachmentEntity", Boolean.TRUE);

        String rendered = render("EntityController.java.template", context);

        assertTrue(rendered.contains("@QueryParam(\"Invoice\") Long Invoice) {"),
                "the list must declare the master filter with the FK's Java class: " + rendered);
        assertTrue(rendered.contains("upload(@QueryParam(\"Invoice\") Long Invoice) {"),
                "the upload must declare the master filter with the FK's Java class: " + rendered);
        assertFalse(rendered.contains("@QueryParam(\"Invoice\") String Invoice"), "no master filter may be bound untyped: " + rendered);
        assertFalse(rendered.contains("@QueryParam(\"Invoice\") Integer Invoice"), "no master filter may hard-code Integer: " + rendered);
    }

    @Test
    void theSelfServiceListsBindTheParentFilterWithTheForeignKeysJavaClass() throws Exception {
        for (String template : List.of("EntityMyController.java.template", "EntityPartnerController.java.template")) {
            Map<String, Object> context = context();
            context.put("personalParent", parent());
            context.put("partnerParent", parent());

            String rendered = render(template, context);

            assertTrue(rendered.contains("getAll(@QueryParam(\"Invoice\") Long Invoice,"),
                    template + " must declare the parent filter with the FK's Java class: " + rendered);
            assertTrue(rendered.contains("private void requireMyParent(Long parentId) {"),
                    template + " must prove the parent by its typed id: " + rendered);
            assertTrue(rendered.contains("parentRepository.findOne(parentId)"),
                    template + " must look the parent up by the typed id: " + rendered);
            assertFalse(rendered.contains("Long.valueOf(parentId)"),
                    template + " must not parse the id it already holds typed: " + rendered);
            assertFalse(rendered.contains("String.valueOf(entity.Invoice)"),
                    template + " must pass the payload's FK as it is: " + rendered);
        }
    }

    private String render(String templateName, Map<String, Object> parameters) throws Exception {
        String location = REST_BASE + templateName;
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new String(velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
    }

    private static Map<String, Object> context() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "InvoiceItem");
        parameters.put("projectName", "sales");
        parameters.put("perspectiveName", "Invoices");
        parameters.put("javaGenFolderName", "sales");
        parameters.put("javaPerspectiveName", "invoices");
        parameters.put("tablePrefix", "SALES_");
        parameters.put("dataName", "INVOICE_ITEM");
        parameters.put("pkPropertyName", "Id");
        parameters.put("properties", List.of(primaryKey()));
        parameters.put("sensitiveProperties", List.of());
        parameters.put("rowChecks", List.of());
        parameters.put("personalProperty", "Person");
        parameters.put("personalFkJavaClass", "Integer");
        parameters.put("personalIdentityProperty", "Email");
        parameters.put("personalIdentityLabel", "Name");
        parameters.put("personalIdentityRepositoryClass", "gen.sales.data.people.PersonRepository");
        parameters.put("partnerProperty", "Customer");
        parameters.put("partnerFkJavaClass", "Integer");
        parameters.put("partnerIdentityProperty", "Email");
        parameters.put("partnerIdentityLabel", "Name");
        parameters.put("partnerIdentityRepositoryClass", "gen.sales.data.people.CustomerRepository");
        return parameters;
    }

    /** The composition parent as {@code ModelParameterProcessor} joins it, keyed by a BIGINT id. */
    private static Map<String, Object> parent() {
        Map<String, Object> parent = new LinkedHashMap<>();
        parent.put("fkProperty", "Invoice");
        parent.put("fkJavaClass", "Long");
        parent.put("entity", "Invoice");
        parent.put("entityClass", "gen.sales.data.invoices.InvoiceEntity");
        parent.put("repositoryClass", "gen.sales.data.invoices.InvoiceRepository");
        parent.put("personalProperty", "Person");
        parent.put("personalFkJavaClass", "Integer");
        parent.put("partnerProperty", "Customer");
        parent.put("partnerFkJavaClass", "Integer");
        return parent;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "INVOICE_ITEM_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }
}
