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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * Renders the three generated REST controllers through the platform's
 * {@link VelocityGenerationEngine} to cover the inherited-lock branches a published application
 * cannot reach on its own.
 *
 * <p>
 * A composition child of an immutable master must refuse user writes on EVERY generated surface -
 * the power controller, and the partner / personal ones, where a customer or the record's own owner
 * would otherwise rewrite a locked document's totals through their own endpoint. Only the power
 * controller's branch is exercised end-to-end by {@code IntentEmissionCoverageIT}: reaching the
 * other two from an intent needs a personal (or partner) root that also locks, and the personal
 * fixture there deliberately tests scoping on an unlocked one.
 *
 * <p>
 * An unrendered Velocity branch is the silent-degradation case this suite exists for: a typo in a
 * variable reference emits itself literally and the generated Java only fails later, in a user's
 * project, at compile time. So these assertions also check that nothing of the form
 * <code>${...}</code> survives into the emitted guard.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context and uses
 * its OWN engine instance rather than the shared bean - the same class, configured the same way,
 * with no chance of leaving anything behind for the tests that publish real projects.
 */
class ChildLockControllerTemplateIT {

    private static final String BASE = "/META-INF/dirigible/template-application-rest-java/api/";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void thePowerControllerGuardsEveryWriteVerbAgainstTheMastersLock() throws Exception {
        String rendered = render("EntityController.java.template", context(statusLock()));

        assertTrue(rendered.contains("gen.sales.data.invoices.InvoiceRepository masterRepository"),
                "the guard must inject the MASTER's repository: " + rendered);
        assertTrue(rendered.contains("requireMasterMutable(entity.Invoice);"), "create must consult the payload's master");
        assertTrue(rendered.contains("repository.findOne(id).ifPresent(stored -> requireMasterMutable(stored.Invoice));"),
                "update and delete must consult the STORED master");
        assertTrue(rendered.contains("\"2,3\".split(\",\")"), "the guard must carry the master's immutable status ids");
        assertTrue(rendered.contains("HttpStatus.CONFLICT"), "a write against a locked master must be a 409");
        assertNoUnresolvedReferences(rendered);
    }

    @Test
    void anAppendOnlyMasterLocksItsChildrenUnconditionally() throws Exception {
        String rendered = render("EntityController.java.template", context(appendOnlyLock()));

        assertTrue(rendered.contains("append-only Invoice"), "an append-only master must say so in the refusal");
        assertFalse(rendered.contains("master.Status"), "an append-only master has no status to consult: " + rendered);
        assertNoUnresolvedReferences(rendered);
    }

    /**
     * The personal and partner controllers already hold their composition parent's repository when the
     * scope is inherited through it, so the guard reuses that field rather than injecting the same
     * repository twice.
     */
    @Test
    void thePersonalAndPartnerControllersReuseTheParentRepositoryTheyAlreadyHold() throws Exception {
        for (String template : List.of("EntityMyController.java.template", "EntityPartnerController.java.template")) {
            Map<String, Object> context = context(statusLock());
            context.put("personalParent", parent());
            context.put("partnerParent", parent());
            String rendered = render(template, context);

            assertTrue(rendered.contains("parentRepository.findOne(masterId)"),
                    template + " must reuse the parent repository: " + rendered);
            assertFalse(rendered.contains("masterRepository"), template + " must not inject a second repository of the same type");
            assertTrue(rendered.contains("requireMasterMutable(existing.Invoice);"),
                    template + " must guard its update and delete against the stored master");
            assertNoUnresolvedReferences(rendered);
        }
    }

    /**
     * With no scope inherited through the parent (the child carries its own owner), the same
     * controllers have no parent repository to reuse and must inject the master's.
     */
    @Test
    void thePersonalAndPartnerControllersInjectTheMasterRepositoryWhenTheyHoldNoParent() throws Exception {
        for (String template : List.of("EntityMyController.java.template", "EntityPartnerController.java.template")) {
            String rendered = render(template, context(statusLock()));

            assertTrue(rendered.contains("gen.sales.data.invoices.InvoiceRepository masterRepository"),
                    template + " must inject the master's repository: " + rendered);
            assertTrue(rendered.contains("masterRepository.findOne(masterId)"), template + " must consult the master through it");
            assertNoUnresolvedReferences(rendered);
        }
    }

    private String render(String templateName, Map<String, Object> parameters) throws Exception {
        String location = BASE + templateName;
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** Asserts the emitted guard resolved every reference - an unresolved one renders literally. */
    private static void assertNoUnresolvedReferences(String rendered) {
        for (String line : rendered.split("\n")) {
            if (line.contains("MasterMutable") || line.contains("masterRepository") || line.contains("parentRepository.findOne")) {
                assertFalse(line.contains("${"), "an unresolved template reference survived into the guard: " + line);
            }
        }
    }

    private static Map<String, Object> context(Map<String, Object> masterLock) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "InvoiceItem");
        parameters.put("projectName", "sales");
        parameters.put("perspectiveName", "Invoices");
        parameters.put("javaGenFolderName", "sales");
        parameters.put("javaPerspectiveName", "invoices");
        parameters.put("properties", List.of(primaryKey(), amount()));
        parameters.put("sensitiveProperties", new ArrayList<>());
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
        parameters.put("masterLock", masterLock);
        return parameters;
    }

    private static Map<String, Object> statusLock() {
        Map<String, Object> masterLock = baseLock();
        masterLock.put("always", Boolean.FALSE);
        masterLock.put("statusProperty", "Status");
        masterLock.put("statusValues", "2,3");
        return masterLock;
    }

    private static Map<String, Object> appendOnlyLock() {
        Map<String, Object> masterLock = baseLock();
        masterLock.put("always", Boolean.TRUE);
        return masterLock;
    }

    private static Map<String, Object> baseLock() {
        Map<String, Object> masterLock = new LinkedHashMap<>();
        masterLock.put("fkProperty", "Invoice");
        masterLock.put("fkJavaClass", "Integer");
        masterLock.put("entity", "Invoice");
        masterLock.put("entityClass", "gen.sales.data.invoices.InvoiceEntity");
        masterLock.put("repositoryClass", "gen.sales.data.invoices.InvoiceRepository");
        return masterLock;
    }

    private static Map<String, Object> parent() {
        Map<String, Object> parent = new LinkedHashMap<>();
        parent.put("fkProperty", "Invoice");
        parent.put("fkJavaClass", "Integer");
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
        property.put("dataName", "INVOICEITEM_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> amount() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Amount");
        property.put("dataName", "INVOICEITEM_AMOUNT");
        property.put("dataType", "DECIMAL");
        property.put("dataTypeJavaClass", "java.math.BigDecimal");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }
}
