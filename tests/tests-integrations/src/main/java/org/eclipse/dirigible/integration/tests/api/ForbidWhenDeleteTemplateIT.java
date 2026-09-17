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
 * A {@code checks: forbidWhen} refuses the DELETE of a row it guards, on every generated REST
 * surface (issue #7372).
 *
 * <p>
 * The construct refused a create and an update while its condition held, and its master guard hides
 * the child panel's Add, row edit AND row delete - so a rule reading "no line may change while the
 * quotation is sent" let the line be REMOVED, the largest of the three changes, through any caller
 * that is not the generated page. The affordance being hidden is what made it silent: a reviewer
 * clicking through the pages sees all three gone and concludes the rule holds.
 *
 * <p>
 * Rendered through the platform's own {@link VelocityGenerationEngine} rather than published,
 * because the personal and partner surfaces of a guarded child are not reachable from an intent
 * fixture (that would need a personal root that also carries the check), and because the gated
 * variant has no fleet author yet. {@code IntentEmissionCoverageIT} covers the power controller
 * end-to-end from a real intent.
 *
 * <p>
 * An unrendered Velocity branch is the silent-degradation case: a typo in a variable reference
 * emits itself literally and the generated Java only fails later, at compile time in a user's
 * project - hence the assertion that nothing of the form <code>${...}</code> survives.
 */
class ForbidWhenDeleteTemplateIT {

    private static final String BASE = "/META-INF/dirigible/template-application-rest-java/api/";
    private static final List<String> CONTROLLERS =
            List.of("EntityController.java.template", "EntityMyController.java.template", "EntityPartnerController.java.template");

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    /**
     * The guard reads the STORED row - a delete carries no payload - loads the parent hop the condition
     * tests by FK, and answers with the authored message.
     */
    @Test
    void everyGeneratedSurfaceRefusesTheDeleteOfAGuardedRow() throws Exception {
        for (String template : CONTROLLERS) {
            String rendered = render(template, context(ungated()));

            assertTrue(rendered.contains("private static void requireDeletable(InvoiceItemEntity entity) {"),
                    template + " must emit the delete guard: " + rendered);
            assertTrue(rendered.contains("new gen.sales.data.invoices.InvoiceRepository().findById(hop0Fk)"),
                    template + " must load the hop the condition reads: " + rendered);
            assertTrue(rendered.contains("java.util.Objects.equals((hop0 == null ? null : hop0.Status), 2)"),
                    template + " must emit the authored condition: " + rendered);
            assertTrue(rendered.contains("HttpStatus.BAD_REQUEST, \"Cannot remove a line from a posted entry\""),
                    template + " must answer with the authored message: " + rendered);
            assertNoUnresolvedReferences(rendered);
        }
    }

    /** The power surface consults the stored row it reads by id; the scoped ones the row they hold. */
    @Test
    void theGuardIsCalledFromTheDeleteVerbOfEachSurface() throws Exception {
        assertTrue(
                render("EntityController.java.template", context(ungated())).contains(
                        "repository.findOne(id).ifPresent(stored -> requireDeletable(stored));"),
                "the power controller's delete must consult the stored row");
        for (String template : List.of("EntityMyController.java.template", "EntityPartnerController.java.template")) {
            assertTrue(render(template, context(ungated())).contains("requireDeletable(existing);"),
                    template + " must consult the row its scope check already loaded");
        }
    }

    /**
     * A GATED forbidWhen is the repository's business for a create and an update, but a delete is
     * nobody's transition - so the gate rides along on this verb rather than routing the rule away from
     * it, rendered as the status condition it declares.
     */
    @Test
    void aGatedForbidWhenCarriesItsGateIntoTheDeleteGuard() throws Exception {
        for (String template : CONTROLLERS) {
            String rendered = render(template, context(gated()));

            assertTrue(rendered.contains("if (entity.Status != null && entity.Status == 3) {"),
                    template + " must gate the guard on the authored status: " + rendered);
            assertTrue(rendered.contains("HttpStatus.BAD_REQUEST, \"Cannot remove a confirmed line\""),
                    template + " must answer with the authored message: " + rendered);
            assertNoUnresolvedReferences(rendered);
        }
    }

    /** An entity carrying no forbidWhen renders exactly as before - no method, no call. */
    @Test
    void anUnguardedEntityEmitsNoDeleteGuardAtAll() throws Exception {
        for (String template : CONTROLLERS) {
            Map<String, Object> context = context(null);
            String rendered = render(template, context);

            assertFalse(rendered.contains("requireDeletable"), template + " must emit nothing where no check is authored: " + rendered);
        }
    }

    /**
     * A message quoting a field name - which the DSL's own examples suggest - must reach the guard
     * through its escaped twin, or the literal it is written into ends early and the whole module's
     * generated Java fails to compile (#7241).
     */
    @Test
    void theAuthoredMessageTravelsAsAJavaLiteral() throws Exception {
        Map<String, Object> check = ungated();
        check.put("message", "A \"line\" of a posted entry cannot be removed");
        check.put("messageJavaLiteral", "A \\\"line\\\" of a posted entry cannot be removed");

        for (String template : CONTROLLERS) {
            String rendered = render(template, context(check));

            assertTrue(rendered.contains("\"A \\\"line\\\" of a posted entry cannot be removed\""),
                    template + " must write the escaped twin into the Java literal: " + rendered);
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
            if (line.contains("requireDeletable") || line.contains("hop0")) {
                assertFalse(line.contains("${"), "an unresolved template reference survived into the guard: " + line);
            }
        }
    }

    /** An UNGATED forbidWhen reading the composition master one hop away, as the processor joins it. */
    private static Map<String, Object> ungated() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("kind", "forbidWhen");
        check.put("message", "Cannot remove a line from a posted entry");
        check.put("messageJavaLiteral", "Cannot remove a line from a posted entry");
        check.put("guardJavaExpression", "java.util.Objects.equals((hop0 == null ? null : hop0.Status), 2)");
        check.put("pathLoads", List.of(hop()));
        return check;
    }

    /** The same rule with a {@code status:} gate - the routing that sends the write half elsewhere. */
    private static Map<String, Object> gated() {
        Map<String, Object> check = ungated();
        check.put("message", "Cannot remove a confirmed line");
        check.put("messageJavaLiteral", "Cannot remove a confirmed line");
        check.put("status", "3");
        check.put("statusProperty", "Status");
        return check;
    }

    private static Map<String, Object> hop() {
        Map<String, Object> load = new LinkedHashMap<>();
        load.put("local", "hop0");
        load.put("sourceExpression", "entity.Invoice");
        load.put("entityClass", "gen.sales.data.invoices.InvoiceEntity");
        load.put("repositoryClass", "gen.sales.data.invoices.InvoiceRepository");
        return load;
    }

    private static Map<String, Object> context(Map<String, Object> deleteCheck) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "InvoiceItem");
        parameters.put("projectName", "sales");
        parameters.put("perspectiveName", "Invoices");
        parameters.put("javaGenFolderName", "sales");
        parameters.put("javaPerspectiveName", "invoices");
        parameters.put("properties", List.of(primaryKey(), amount(), status()));
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
        parameters.put("deleteChecks", deleteCheck == null ? new ArrayList<>() : List.of(deleteCheck));
        return parameters;
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

    private static Map<String, Object> status() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Status");
        property.put("dataName", "INVOICEITEM_STATUS");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }
}
