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
 * The REFERENCE checks of the self-service surfaces, rendered through the platform's
 * {@link VelocityGenerationEngine} (dirigible #7153).
 *
 * <p>
 * A {@code leafOnly:} relation must point at a leaf and a {@code hierarchy:} parent must not close
 * a cycle. Both rules lived only on the power controller, so a personal or partner POST/PUT that
 * broke either of them committed silently - and with referential integrity deliberately
 * business-layer-only, nothing downstream refused the row. #7110 aligned the two surfaces on VALUE
 * validation; this pins the reference half of the same alignment.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context - the
 * fixture shape of {@code PersonalSurfaceCreateValidationTemplateIT}.
 */
class SelfServiceReferenceValidationTemplateIT {

    private static final String REST_BASE = "/META-INF/dirigible/template-application-rest-java/api/";

    private static final String POWER_SURFACE = "EntityController.java.template";

    private static final List<String> SELF_SERVICE_SURFACES =
            List.of("EntityMyController.java.template", "EntityPartnerController.java.template");

    private static final String LEAF_REFUSAL = "The 'Account' must reference a leaf Account - the selected one has children";

    private static final String CYCLE_REFUSAL = "The 'Parent' reference would create a cycle in the Booking hierarchy";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    /** The refusals are the power surface's, verbatim: one rule, whichever door the caller used. */
    @Test
    void aSelfServiceSurfaceRefusesANonLeafAndACycleExactlyAsThePowerSurfaceDoes() throws Exception {
        String power = render(POWER_SURFACE);
        assertTrue(power.contains(LEAF_REFUSAL), "the power surface's own leafOnly refusal changed: " + power);
        assertTrue(power.contains(CYCLE_REFUSAL), "the power surface's own cycle refusal changed: " + power);

        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(template);

            assertTrue(rendered.contains(LEAF_REFUSAL), template + " must refuse a non-leaf leafOnly reference: " + rendered);
            assertTrue(rendered.contains(CYCLE_REFUSAL), template + " must refuse a hierarchy cycle: " + rendered);
        }
    }

    /**
     * The walk up the tree is bounded - a hierarchy already broken by an older write must refuse the
     * request, not spin.
     */
    @Test
    void theCycleWalkIsBoundedOnTheSelfServiceSurfacesToo() throws Exception {
        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(template);

            assertTrue(rendered.contains("The Booking hierarchy is implausibly deep - aborting"),
                    template + " must bound the parent walk: " + rendered);
        }
    }

    /**
     * Called after the value validation, on the payload the server has finished deciding, and before
     * the write - a refused reference must never reach the insert or the update.
     */
    @Test
    void theReferenceChecksRunAfterTheValueChecksAndBeforeTheWrite() throws Exception {
        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(template);

            int validate = rendered.indexOf("        validate(entity);");
            int references = rendered.indexOf("        validateReferences(entity);");
            int save = rendered.indexOf("repository.save(entity)");
            assertTrue(validate >= 0 && references > validate, template + " must check references after the values: " + rendered);
            assertTrue(save > references, template + " must check references before the insert: " + rendered);

            int update = rendered.indexOf("repository.update(entity)");
            assertTrue(rendered.lastIndexOf("        validateReferences(entity);") < update,
                    template + " must check references before the update too: " + rendered);
        }
    }

    /** An entity with neither rule carries no dead validator and no call to one. */
    @Test
    void anEntityWithoutHierarchyRulesGetsNeitherTheMethodNorTheCall() throws Exception {
        Map<String, Object> parameters = context();
        parameters.remove("hasReferenceValidations");
        parameters.remove("hierarchyProperty");
        parameters.put("properties", List.of(primaryKey(), employee()));

        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(template, parameters);

            assertTrue(!rendered.contains("validateReferences"), template + " must not emit an unused validator: " + rendered);
        }
    }

    /** A see-only personal surface refuses every write already: it carries no validator to call. */
    @Test
    void aSeeOnlyPersonalSurfaceCarriesNoValidatorAtAll() throws Exception {
        Map<String, Object> parameters = context();
        parameters.put("personalReadOnly", Boolean.TRUE);

        String rendered = render("EntityMyController.java.template", parameters);

        assertTrue(!rendered.contains("validateReferences"), "a read-only personal surface must not emit a validator: " + rendered);
    }

    private String render(String templateName) throws Exception {
        return render(templateName, context());
    }

    private String render(String templateName, Map<String, Object> parameters) throws Exception {
        String location = REST_BASE + templateName;
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** A personally owned document that is itself a tree AND points at a leafOnly relation. */
    private static Map<String, Object> context() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Booking");
        parameters.put("projectName", "bookings");
        parameters.put("perspectiveName", "Booking");
        parameters.put("javaGenFolderName", "bookings");
        parameters.put("javaPerspectiveName", "booking");
        parameters.put("tablePrefix", "BOOKINGS_");
        parameters.put("dataName", "BOOKING");
        parameters.put("pkPropertyName", "Id");
        parameters.put("properties", List.of(primaryKey(), employee(), account(), parent()));
        parameters.put("sensitiveProperties", List.of());
        parameters.put("hierarchyProperty", "Parent");
        parameters.put("hasReferenceValidations", Boolean.TRUE);
        parameters.put("personalProperty", "Employee");
        parameters.put("personalFkJavaClass", "Integer");
        parameters.put("personalIdentityProperty", "Email");
        parameters.put("personalIdentityLabel", "Name");
        parameters.put("personalIdentityRepositoryClass", "gen.bookings.data.booking.EmployeeRepository");
        parameters.put("partnerProperty", "Employee");
        parameters.put("partnerFkJavaClass", "Integer");
        parameters.put("partnerIdentityProperty", "Email");
        parameters.put("partnerIdentityLabel", "Name");
        parameters.put("partnerIdentityRepositoryClass", "gen.bookings.data.booking.EmployeeRepository");
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "BOOKING_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }

    /** The identity relation the personal / partner surfaces scope every row by. */
    private static Map<String, Object> employee() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Employee");
        property.put("dataName", "BOOKING_EMPLOYEE");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("relationshipEntityName", "Employee");
        return property;
    }

    /** intent `leafOnly: true` on a relation into a hierarchical Account. */
    private static Map<String, Object> account() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Account");
        property.put("dataName", "BOOKING_ACCOUNT");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("relationshipEntityName", "Account");
        property.put("widgetLeafOnly", "true");
        property.put("widgetHierarchyProperty", "Parent");
        property.put("leafOnlyRepositoryClass", "gen.bookings.data.booking.AccountRepository");
        return property;
    }

    /** intent `hierarchy: Parent` - the entity's own tree edge. */
    private static Map<String, Object> parent() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Parent");
        property.put("dataName", "BOOKING_PARENT");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("relationshipEntityName", "Booking");
        return property;
    }
}
