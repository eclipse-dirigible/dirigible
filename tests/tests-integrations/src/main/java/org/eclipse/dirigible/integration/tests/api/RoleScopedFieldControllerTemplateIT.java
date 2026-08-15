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
 * {@link VelocityGenerationEngine} to cover the role-scoped-field branches (intent
 * {@code visibleTo:}, emitted as the model's per-property {@code roleRead}/{@code roleWrite}).
 *
 * <p>
 * The point of the feature is that the scoping is enforced where the data leaves the server, not in
 * the UI: the value is stripped from every response, ignored on every write, kept out of the change
 * trail, and the caller's own list of withheld fields is what the generated pages hide. That must
 * hold on ALL THREE surfaces - a personal or partner caller owns the record, which is not the same
 * as holding the role that may read every column of it.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context and uses
 * its OWN engine instance, exactly like {@code ChildLockControllerTemplateIT}, whose fixture shape
 * this mirrors.
 */
class RoleScopedFieldControllerTemplateIT {

    private static final String BASE = "/META-INF/dirigible/template-application-rest-java/api/";

    private static final List<String> SCOPED_SURFACES =
            List.of("EntityMyController.java.template", "EntityPartnerController.java.template");

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void thePowerControllerStripsTheFieldFromReadsAndIgnoresItOnWrites() throws Exception {
        String rendered = render("EntityController.java.template", context());

        assertTrue(rendered.contains("if (!isInAnyRole(\"Payroll,Administrator\")) {"),
                "the redaction must test the whole allow-list: " + rendered);
        assertTrue(rendered.contains("entity.DailyRate = null;"), "a caller outside the roles must read a null");
        assertTrue(rendered.contains("if (isInAnyRole(\"Payroll,Administrator\")) {\n            target.DailyRate = input.DailyRate;"),
                "an update must copy the field only for a caller inside the roles: " + rendered);
        assertTrue(rendered.contains("target.Amount = input.Amount;"), "an unrestricted property stays writable for everyone");
        assertNoUnresolvedReferences(rendered);
    }

    /** Any of the listed roles is enough - the allow-list is an OR, not a conjunction. */
    @Test
    void holdingAnyOneOfTheListedRolesIsEnough() throws Exception {
        String rendered = render("EntityController.java.template", context());

        assertTrue(rendered.contains("for (String role : roles.split(\",\"))"), "the helper must walk the comma-separated list");
        assertTrue(rendered.contains("if (!name.isEmpty() && UserFacade.isInRole(name)) {\n                return true;"),
                "the first role the caller holds must satisfy the check: " + rendered);
    }

    /**
     * What the generated pages gate on. The endpoint answers for the CALLER in front of it, so the
     * server stays the only place the roles are interpreted - the browser never learns a role name.
     */
    @Test
    void everySurfaceTellsTheCallerWhichFieldsItWithholds() throws Exception {
        for (String template : templates()) {
            String rendered = render(template, context());

            assertTrue(rendered.contains("@Get(\"/restricted\")"), template + " must expose the field-visibility pre-check");
            assertTrue(rendered.contains("restricted.add(\"DailyRate\");"), template + " must name the withheld property");
            assertFalse(rendered.contains("restricted.add(\"Amount\");"), template + " must not list an unrestricted property");
            assertNoUnresolvedReferences(rendered);
        }
    }

    /**
     * The change trail records the before/after of every tracked property, so it would hand out exactly
     * what the record's own response withholds.
     */
    @Test
    void theChangeTrailDropsTheEntriesOfAWithheldField() throws Exception {
        Map<String, Object> context = context();
        context.put("history", "true");
        String rendered = render("EntityController.java.template", context);

        assertTrue(rendered.contains("entries.removeIf(entry -> hidden.contains(String.valueOf(entry.get(\"Property\"))));"),
                "the history must be filtered by the caller's withheld properties: " + rendered);
        assertNoUnresolvedReferences(rendered);
    }

    /**
     * Owning the record - or being the partner it belongs to - is not a role, so the scoped surfaces
     * apply the same allow-list as the power one.
     */
    @Test
    void thePersonalAndPartnerSurfacesScrubAndRefuseTheSameField() throws Exception {
        for (String template : SCOPED_SURFACES) {
            String rendered = render(template, context());

            assertTrue(rendered.contains("if (!isInAnyRole(\"Payroll,Administrator\")) {\n            entity.DailyRate = null;"),
                    template + " must strip the field from its responses: " + rendered);
            assertTrue(rendered.contains("entity.DailyRate = existing.DailyRate;"),
                    template + " must keep the stored value when the caller may not write it: " + rendered);
            assertTrue(rendered.contains("User.isInRole(name)"), template + " must resolve roles through the client SDK");
            assertNoUnresolvedReferences(rendered);
        }
    }

    /** An entity with no role-scoped property must come out exactly as it always did. */
    @Test
    void anEntityWithoutRoleScopedPropertiesGetsNoneOfIt() throws Exception {
        Map<String, Object> context = context();
        context.put("properties", List.of(primaryKey(), amount()));

        for (String template : templates()) {
            String rendered = render(template, context);

            assertFalse(rendered.contains("/restricted"), template + " must not expose the pre-check with nothing to withhold");
            assertFalse(rendered.contains("isInAnyRole"), template + " must not carry the role helper with nothing to check");
        }
    }

    private static List<String> templates() {
        List<String> templates = new ArrayList<>();
        templates.add("EntityController.java.template");
        templates.addAll(SCOPED_SURFACES);
        return templates;
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

    /** Asserts the emitted guards resolved every reference - an unresolved one renders literally. */
    private static void assertNoUnresolvedReferences(String rendered) {
        for (String line : rendered.split("\n")) {
            if (line.contains("isInAnyRole") || line.contains("restricted.add") || line.contains("DailyRate")) {
                assertFalse(line.contains("${"), "an unresolved template reference survived into the guard: " + line);
            }
        }
    }

    private static Map<String, Object> context() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Payslip");
        parameters.put("projectName", "hr");
        parameters.put("perspectiveName", "People");
        parameters.put("javaGenFolderName", "hr");
        parameters.put("javaPerspectiveName", "people");
        parameters.put("tablePrefix", "HR_");
        parameters.put("dataName", "PAYSLIP");
        parameters.put("properties", List.of(primaryKey(), amount(), dailyRate()));
        parameters.put("sensitiveProperties", new ArrayList<>());
        parameters.put("personalProperty", "Person");
        parameters.put("personalFkJavaClass", "Integer");
        parameters.put("personalIdentityProperty", "Email");
        parameters.put("personalIdentityLabel", "Name");
        parameters.put("personalIdentityRepositoryClass", "gen.hr.data.people.PersonRepository");
        parameters.put("partnerProperty", "Customer");
        parameters.put("partnerFkJavaClass", "Integer");
        parameters.put("partnerIdentityProperty", "Email");
        parameters.put("partnerIdentityLabel", "Name");
        parameters.put("partnerIdentityRepositoryClass", "gen.hr.data.people.CustomerRepository");
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "PAYSLIP_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> amount() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Amount");
        property.put("dataName", "PAYSLIP_AMOUNT");
        property.put("dataType", "DECIMAL");
        property.put("dataTypeJavaClass", "java.math.BigDecimal");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }

    /** The role-scoped property: intent {@code visibleTo: [Payroll, Administrator]}. */
    private static Map<String, Object> dailyRate() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "DailyRate");
        property.put("dataName", "PAYSLIP_DAILY_RATE");
        property.put("dataType", "DECIMAL");
        property.put("dataTypeJavaClass", "java.math.BigDecimal");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("roleRead", "Payroll,Administrator");
        property.put("roleWrite", "Payroll,Administrator");
        return property;
    }
}
