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
import static org.mockito.Mockito.atLeastOnce;
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

    /** The written {@code .access} document, or null when the pass wrote none. */
    private static String access(boolean optIn) {
        IntentModel model = IntentParser.parse(YAML);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "sales", repository);
        context.setSettings(settings(optIn));

        new PermissionIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/sales.access")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static IntentSettings settings(boolean generateAccess) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("access", Map.of("generate", generateAccess));
        return IntentSettings.parse(new com.google.gson.Gson().toJson(document));
    }
}
