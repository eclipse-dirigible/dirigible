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
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * Renders the three generated REST controllers through the platform's
 * {@link VelocityGenerationEngine} to cover BOTH shapes of the system-owned status guard (dirigible
 * #7339, widened by #7553).
 *
 * <p>
 * A status a process step or a capacity roll-up COMPUTES is owned wholesale - an omitted value is
 * preserved AND a differing one refused. A status only a {@code transitions:} button writes is
 * PRESERVED but never refused: the button writes one declared seed id rather than computing the
 * column, and both stronger readings take a working feature away - claiming the whole column
 * strands every status no button targets ({@code IntentEmissionCoverageIT}'s Entry, whose one
 * Cancel button left its ordinary PUT to the POSTED gate refused), while refusing just the targeted
 * values collides with the {@code checks:} design, whose gated rules are enforced on the plain
 * update path against a gate status that is normally exactly what a button writes (the same
 * fixture's Doc). Which hand moves are legal is {@code lifecycle:}'s question.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context and uses
 * its own engine instance.
 */
class WorkflowStatusScopeControllerTemplateIT {

    private static final String BASE = "/META-INF/dirigible/template-application-rest-java/api/";

    private static final List<String> CONTROLLERS =
            List.of("EntityController.java.template", "EntityMyController.java.template", "EntityPartnerController.java.template");

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    /**
     * A process- or roll-up-owned status: the whole column is the writer's, so any value is refused.
     */
    @Test
    void aComputedStatusIsRefusedWholesaleOnEverySurface() throws Exception {
        for (String template : CONTROLLERS) {
            String rendered = render(template, context());

            assertTrue(rendered.contains("WORKFLOW_STATUS_REFUSAL"), template + " must emit the refusal");
            assertTrue(rendered.contains("requireWorkflowStatusOnCreate(entity);"), template + " must guard the create verb");
            assertTrue(rendered.contains("throw new ResponseStatusException(HttpStatus.CONFLICT, WORKFLOW_STATUS_REFUSAL);"),
                    template + " must refuse a differing value");
            assertTrue(rendered.contains("entity.Status = stored.Status;"), template + " must preserve an omitted status");
            assertNoUnresolvedReferences(rendered);
        }
    }

    /**
     * A transitions-only status: the omitted-value data loss is fixed (the stored status is kept), but
     * nothing is refused - a gated `checks:` rule is enforced on the plain update path and its gate
     * status is normally exactly what a button writes, so refusing here would remove that feature.
     */
    @Test
    void aTransitionOwnedStatusIsOnlyPreservedOnEverySurface() throws Exception {
        for (String template : CONTROLLERS) {
            Map<String, Object> context = context();
            context.put("workflowStatusPreserveOnly", "true");
            String rendered = render(template, context);

            // The half that IS the defect: an omitted status is taken from the stored row, not nulled.
            assertTrue(rendered.contains("entity.Status = stored.Status;"), template + " must preserve an omitted status");
            // ...and nothing else of the guard survives, so an ordinary edit still reaches the
            // repository, where the checks: gate and lifecycle: refuse what they are there to refuse.
            assertFalse(rendered.contains("WORKFLOW_STATUS_REFUSAL"), template + " must not refuse a transition-owned status: " + template);
            assertFalse(rendered.contains("requireWorkflowStatusOnCreate"), template + " must not guard the create verb");
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
            if (line.contains("WorkflowStatus") || line.contains("WORKFLOW_STATUS_REFUSAL")
                    || line.contains("requireWorkflowStatusOnCreate")) {
                assertFalse(line.contains("${"), "an unresolved template reference survived into the guard: " + line);
            }
        }
    }

    private static Map<String, Object> context() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Invoice");
        parameters.put("projectName", "sales");
        parameters.put("perspectiveName", "Invoices");
        parameters.put("javaGenFolderName", "sales");
        parameters.put("javaPerspectiveName", "invoices");
        parameters.put("properties", List.of(primaryKey(), status()));
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
        parameters.put("workflowStatusProperty", "Status");
        parameters.put("workflowStatusInitial", "1");
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "INVOICE_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> status() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Status");
        property.put("dataName", "INVOICE_STATUS");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }
}
