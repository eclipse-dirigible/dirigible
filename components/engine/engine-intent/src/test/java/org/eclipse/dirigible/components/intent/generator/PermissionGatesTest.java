/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.permission.PermissionIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The intent's {@code permissions[].can:} tokens are what the generated application's gates are
 * bound from (dirigible #6760). Before this, {@code permissions[].role} became {@code .roles} while
 * the controller checked a convention-derived name no intent construct mentioned, so granting an
 * authored role granted nothing at all - silently.
 */
class PermissionGatesTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: items, kind: oneToMany, to: OrderItem }
              - name: OrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: integer }
                relations:
                  - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            permissions:
              - { role: SalesAdmin,  can: [Order:read, Order:create, Order:delete] }
              - { role: SalesViewer, can: [Order:read] }
            """;

    @Test
    void aReportGrantBecomesItsReadGate() {
        String yaml = YAML + """
                reports:
                  - name: OrderTotals
                    source: Order
                    dimensions: [id]
                    measures: ["sum(total)"]
                """;
        IntentModel model = IntentParser.parse(yaml.replace("can: [Order:read]", "can: [Order:read, OrderTotals:read]"));
        PermissionSupport.Gates gates = PermissionSupport.gates(model);
        assertEquals("SalesViewer", gates.readRoles("OrderTotals"));
        assertNull(gates.writeRoles("OrderTotals"));
    }

    @Test
    void anUndeclaredResourceIsReportedAsAnIssue() {
        IntentModel model = IntentParser.parse(YAML.replace("can: [Order:read]", "can: [Ordr:read]"));
        PermissionSupport.Gates gates = PermissionSupport.gates(model);
        assertTrue(gates.issues()
                        .stream()
                        .anyMatch(issue -> issue.contains("[Ordr]")),
                "a typo'd resource gates nothing and must not pass unremarked: " + gates.issues());
        assertFalse(gates.covers("Ordr"));
    }

    @Test
    void aBusinessActionWithNoGeneratedGateIsReportedAsAnAdvisory() {
        IntentModel model = IntentParser.parse(YAML.replace("can: [Order:read]", "can: [Order:read, Order:approve]"));
        PermissionSupport.Gates gates = PermissionSupport.gates(model);
        assertTrue(gates.advisories()
                        .stream()
                        .anyMatch(advisory -> advisory.contains("[approve]")),
                "an action the binary gate cannot express must be named, not dropped: " + gates.advisories());
        assertEquals("SalesAdmin,SalesViewer", gates.readRoles("Order"), "the mappable tokens of the same grant still bind");
    }

    @Test
    void aMalformedTokenIsRefusedAtParse() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML.replace("can: [Order:read]", "can: [Order]")));
        assertTrue(String.join("\n", failure.getIssues())
                         .contains("Resource:action"),
                failure.getIssues()
                       .toString());
    }

    @Test
    void theAccessArtefactIsOptInAndCoversThePublishedPaths() {
        assertNull(access(false), "the access artefact must not appear until the project's settings ask for it");
        String access = access(true);
        assertTrue(access.contains("/services/java/proj/gen/sales/api/order/OrderController/**"), access);
        assertTrue(access.contains("/services/web/proj/gen/sales/views/Order/Order-*.html"), access);
        assertTrue(access.contains("/services/web/proj/gen/sales/js/components/pages/Order/Order*.js"), access);
        // The child is reachable to the master's roles, under the master's perspective.
        assertTrue(access.contains("/services/java/proj/gen/sales/api/order/OrderItemController/**"), access);
        // Method stays * on purpose: the generated controllers read through POST .../search, so a
        // "POST means write" split would lock a read-only role out of every list.
        assertTrue(access.contains("\"method\": \"*\""), access);
        assertTrue(access.contains("\"SalesViewer\""), access);
        // An entity no token names keeps no constraint - it is gated exactly as before.
        assertFalse(access.contains("CustomerController"), access);
    }

    /**
     * A report's page is generated into the same sanitized folder as its controller, so that is the
     * path its constraint must cover - one under the report's case-preserved name gated nothing on a
     * case-sensitive filesystem (dirigible #7889).
     */
    @Test
    void aReportsConstraintsCoverItsControllerAndPageInOneFolder() {
        String yaml = YAML + """
                reports:
                  - name: OrderTotals
                    source: Order
                    dimensions: [id]
                    measures: ["sum(total)"]
                """;
        String access = written(yaml.replace("can: [Order:read]", "can: [Order:read, OrderTotals:read]"), settings(true), "/sales.access");
        assertTrue(access.contains("/services/java/proj/gen/ordertotals/api/reports/OrderTotalsController/**"), access);
        assertTrue(access.contains("/services/web/proj/gen/ordertotals/reports/OrderTotals/**"), access);
        assertFalse(access.contains("/gen/OrderTotals/"), access);
    }

    /**
     * The roles {@code .settings} {@code access.extraRoles} appends to every gate are declared in
     * {@code <intent>.roles} - also when the intent authors no {@code permissions:} at all, which is
     * the multitenant case the setting exists for (dirigible #7652).
     */
    @Test
    void theDeploymentsExtraRolesAreDeclaredWithoutAnyAuthoredPermission() {
        String yaml = YAML.substring(0, YAML.indexOf("permissions:"));
        String roles = written(yaml, settings(Map.of("extraRoles", List.of("Owner", "User"), "extraRolesReadOnly", List.of("Auditor"))),
                "/sales.roles");
        assertTrue(roles.contains("\"name\": \"Owner\""), roles);
        assertTrue(roles.contains("\"name\": \"User\""), roles);
        assertTrue(roles.contains("\"name\": \"Auditor\""), roles);
    }

    @Test
    void anExtraRoleAnAuthoredPermissionDeclaresIsDeclaredOnce() {
        String roles = written(YAML, settings(Map.of("extraRoles", List.of("SalesViewer", "Owner"))), "/sales.roles");
        assertEquals(1, roles.split("\"SalesViewer\"", -1).length - 1, roles);
        assertTrue(roles.contains("\"name\": \"Owner\""), roles);
    }

    /**
     * Every role the intent authors is a role for the application's people, so a tenant's Owner may
     * give it in Settings → Users (dirigible #8728): the {@code .roles} marks the {@code permissions:}
     * roles and the extra roles {@code assignable}.
     */
    @Test
    void everyAuthoredRoleIsAssignable() {
        String roles =
                written(YAML, settings(Map.of("extraRoles", List.of("Owner"), "extraRolesReadOnly", List.of("Auditor"))), "/sales.roles");
        Map<String, Boolean> assignable = new LinkedHashMap<>();
        for (JsonElement element : JsonParser.parseString(roles)
                                             .getAsJsonArray()) {
            JsonObject role = element.getAsJsonObject();
            assignable.put(role.get("name")
                               .getAsString(),
                    role.has("assignable") && role.get("assignable")
                                                  .getAsBoolean());
        }
        assertEquals(Map.of("SalesAdmin", true, "SalesViewer", true, "Owner", true, "Auditor", true), assignable, roles);
    }

    @Test
    void noRolesArtefactWithoutPermissionsOrExtraRoles() {
        String yaml = YAML.substring(0, YAML.indexOf("permissions:"));
        assertNull(written(yaml, settings(Map.of()), "/sales.roles"));
    }

    @Test
    void theAccessConstraintsAdmitTheDeploymentsExtraRoles() {
        String access = written(YAML, settings(Map.of("generate", true, "extraRoles", List.of("Owner"))), "/sales.access");
        // No authored grant names Owner, so it can only be the appended extra.
        assertTrue(access.contains("\"Owner\""), "a constraint must not refuse a caller the controller behind it admits: " + access);
    }

    @Test
    void anExclusiveGrantKeepsTheExtraRolesOffEveryGateItNames() {
        String yaml =
                YAML.replace("{ role: SalesViewer, can: [Order:read] }", "{ role: SalesViewer, can: [Order:read, OrderTotals:read] }") + """
                          - { role: Payroll, can: [Customer:write, OrderTotals:read], exclusive: true }
                        reports:
                          - name: OrderTotals
                            source: Order
                            dimensions: [id]
                            measures: ["sum(total)"]
                        """;
        PermissionSupport.Gates gates = PermissionSupport.gates(IntentParser.parse(yaml),
                settings(Map.of("extraRoles", List.of("Owner"), "extraRolesReadOnly", List.of("Auditor"))));

        assertEquals("Payroll", gates.writeGate("sales", "Customer", "Customer"));
        assertEquals("Payroll", gates.readGate("sales", "Customer", "Customer"));
        assertEquals("SalesViewer,Payroll", gates.appendRead("OrderTotals", gates.readRoles("OrderTotals")));
        assertEquals("SalesAdmin,Owner", gates.writeGate("sales", "Order", "Order"), "a resource no exclusive grant names keeps them");
        String access = written(yaml, settings(Map.of("generate", true, "extraRoles", List.of("Owner"))), "/sales.access");
        for (var constraint : com.google.gson.JsonParser.parseString(access)
                                                        .getAsJsonObject()
                                                        .getAsJsonArray("constraints")) {
            String path = constraint.getAsJsonObject()
                                    .get("path")
                                    .getAsString();
            if (path.contains("/Customer")) {
                assertEquals("[\"Payroll\"]", constraint.getAsJsonObject()
                                                        .get("roles")
                                                        .toString(),
                        path);
            }
        }
    }

    /**
     * An {@code additive: true} entry is bound apart from the others (issue #8852): the resource it
     * names is not covered - its convention roles stay - and the role is added by the gate methods.
     */
    @Test
    void anAdditiveGrantLeavesTheResourceUncoveredAndIsAddedToItsGates() {
        PermissionSupport.Gates gates =
                PermissionSupport.gates(IntentParser.parse(ADDITIVE), settings(Map.of("extraRoles", List.of("Owner"))));

        assertFalse(gates.covers("Customer"), "an additive token must not displace the convention roles");
        assertNull(gates.readRoles("Customer"));
        assertTrue(gates.reaches("Customer", "Customer"));
        assertEquals("sales.Customer.CustomerReadOnly,sales.Customer.CustomerFullAccess,Clerk,Owner",
                gates.readGate("sales", "Customer", "Customer"));
        assertEquals("sales.Customer.CustomerFullAccess,Owner", gates.writeGate("sales", "Customer", "Customer"));
        assertTrue(gates.issues()
                        .isEmpty(),
                gates.issues()
                     .toString());
    }

    @Test
    void anAdditiveGrantOnAReportKeepsItsConventionReadRole() {
        PermissionSupport.Gates gates =
                PermissionSupport.gates(IntentParser.parse(ADDITIVE), settings(Map.of("extraRoles", List.of("Owner"))));

        assertNull(gates.readRoles("OrderTotals"), "the report's convention role must stay declared");
        assertEquals("sales.Report.OrderTotalsReadOnly,Clerk,Owner", gates.readGate("OrderTotals", "sales.Report.OrderTotalsReadOnly"));
        // Beside a non-additive grant the convention role is replaced, as it always was.
        PermissionSupport.Gates mixed =
                PermissionSupport.gates(
                        IntentParser.parse(ADDITIVE.replace("{ role: SalesViewer, can: [Order:read] }",
                                "{ role: SalesViewer, can: [Order:read, OrderTotals:read] }")),
                        settings(Map.of("extraRoles", List.of("Owner"))));
        assertEquals("SalesViewer,Clerk,Owner", mixed.readGate("OrderTotals", "sales.Report.OrderTotalsReadOnly"));
    }

    /**
     * The intent's {@code .roles} gains the additive role only: the convention roles it keeps are
     * declared by the template layer, without the {@code assignable} mark, so they are not offered to a
     * tenant's people - the point of issue #8852.
     */
    @Test
    void anAdditiveGrantDeclaresItsRoleAndNoConventionRole() {
        String roles = written(ADDITIVE, settings(Map.of("extraRoles", List.of("Owner"))), "/sales.roles");
        List<String> names = new java.util.ArrayList<>();
        for (JsonElement element : JsonParser.parseString(roles)
                                             .getAsJsonArray()) {
            JsonObject role = element.getAsJsonObject();
            names.add(role.get("name")
                          .getAsString());
            assertTrue(role.get("assignable")
                           .getAsBoolean(),
                    roles);
        }
        assertEquals(List.of("SalesAdmin", "SalesViewer", "Clerk", "Owner"), names, roles);
    }

    @Test
    void theAccessConstraintsOfAnAdditiveResourceAdmitItsConventionRolesToo() {
        String access = written(ADDITIVE, settings(Map.of("generate", true, "extraRoles", List.of("Owner"))), "/sales.access");
        Map<String, String> rolesByPath = new LinkedHashMap<>();
        for (JsonElement constraint : JsonParser.parseString(access)
                                                .getAsJsonObject()
                                                .getAsJsonArray("constraints")) {
            rolesByPath.put(constraint.getAsJsonObject()
                                      .get("path")
                                      .getAsString(),
                    constraint.getAsJsonObject()
                              .get("roles")
                              .toString());
        }
        // The constraint has no write gate to fall back on, so a FullAccess holder must be named.
        assertEquals("[\"proj.Customer.CustomerReadOnly\",\"proj.Customer.CustomerFullAccess\",\"Clerk\",\"Owner\"]",
                rolesByPath.get("/services/java/proj/gen/sales/api/customer/CustomerController/**"), access);
        assertEquals("[\"proj.Report.OrderTotalsReadOnly\",\"Clerk\",\"Owner\"]",
                rolesByPath.get("/services/java/proj/gen/ordertotals/api/reports/OrderTotalsController/**"), access);
        assertEquals("[\"SalesAdmin\",\"SalesViewer\",\"Owner\"]",
                rolesByPath.get("/services/java/proj/gen/sales/api/order/OrderController/**"), "an authored gate is constrained as before");
    }

    /** {@link #YAML} with a report and an additive role on an entity and on the report. */
    private static final String ADDITIVE = YAML + """
              - { role: Clerk, can: [Customer:read, OrderTotals:read], additive: true }
            reports:
              - name: OrderTotals
                source: Order
                dimensions: [id]
                measures: ["sum(total)"]
            """;

    /** The written {@code .access} document, or null when the pass wrote none. */
    private static String access(boolean optIn) {
        return written(YAML, settings(optIn), "/sales.access");
    }

    /** The content the permission generator wrote to the path ending in the suffix, or null. */
    private static String written(String yaml, IntentSettings settings, String suffix) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "sales", repository);
        context.setSettings(settings);

        new PermissionIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeast(0)).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith(suffix)) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static IntentSettings settings(boolean generateAccess) {
        return settings(Map.of("generate", generateAccess));
    }

    private static IntentSettings settings(Map<String, Object> access) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("access", access);
        return IntentSettings.parse(new com.google.gson.Gson().toJson(document));
    }
}
