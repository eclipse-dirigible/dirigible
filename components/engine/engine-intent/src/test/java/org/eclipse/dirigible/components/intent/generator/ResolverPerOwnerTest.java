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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Two task forms on two entities showing the same one-hop field each get a resolver of their own
 * (issue #8331). The resolver loads the trigger entity's record by the process's id variable, so a
 * class named after the path alone was one file both processes wrote, the last generated won, and
 * the other process's form looked its id up as the wrong entity.
 */
class ResolverPerOwnerTest {

    private static final String YAML =
            """
                    name: billing
                    entities:
                      - name: Customer
                        fields:
                          - { name: id,          type: integer, primaryKey: true, generated: true }
                          - { name: creditLimit, type: decimal }
                      - name: SalesInvoice
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string, length: 32 }
                        relations:
                          - { name: Customer, kind: manyToOne, to: Customer }
                      - name: DebitNote
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string, length: 32 }
                        relations:
                          - { name: Customer, kind: manyToOne, to: Customer }
                    processes:
                      - name: SalesInvoiceApproval
                        trigger: { onCreate: SalesInvoice }
                        steps:
                          - { name: creditHold, kind: userTask, args: { assignee: clerk, form: CreditHoldSalesInvoice } }
                          - { name: done,       kind: end }
                      - name: SalesInvoiceReminder
                        trigger: { onCreate: SalesInvoice }
                        steps:
                          - { name: remind, kind: userTask, args: { assignee: clerk, form: CreditHoldSalesInvoice } }
                          - { name: done,   kind: end }
                      - name: DebitNoteApproval
                        trigger: { onCreate: DebitNote }
                        steps:
                          - { name: creditHold, kind: userTask, args: { assignee: clerk, form: CreditHoldDebitNote } }
                          - { name: done,       kind: end }
                    forms:
                      - { name: CreditHoldSalesInvoice, forEntity: SalesInvoice, fields: [number, Customer, Customer.creditLimit], actions: [proceed] }
                      - { name: CreditHoldDebitNote,    forEntity: DebitNote,    fields: [number, Customer, Customer.creditLimit], actions: [proceed] }
                    """;

    @Test
    void eachOwnerEntityGetsItsOwnResolverClass() {
        List<Map<String, Object>> resolvers = GlueIntentGenerator.buildResolversForTest(IntentParser.parse(YAML), null);

        Map<String, String> ownerByProcess = resolvers.stream()
                                                      .collect(Collectors.toMap(r -> (String) r.get("process"),
                                                              r -> r.get("handler") + "/" + r.get("ownerEntity")));
        assertEquals(
                Map.of("SalesInvoiceApproval", "ResolveSalesInvoiceCustomerCreditLimit/SalesInvoice", "SalesInvoiceReminder",
                        "ResolveSalesInvoiceCustomerCreditLimit/SalesInvoice", "DebitNoteApproval",
                        "ResolveDebitNoteCustomerCreditLimit/DebitNote"),
                ownerByProcess, "a resolver is named after the entity whose record it loads: " + resolvers);
        // Both entities' forms bind the same variable name, which is safe: a variable lives in its own
        // process instance. The class is what the processes shared.
        resolvers.forEach(resolver -> assertEquals("Customer_creditLimit", resolver.get("variable")));
    }

    @Test
    void eachProcessRunsItsOwnOwnersResolver() {
        Map<String, String> bpmn = bpmn(IntentParser.parse(YAML));

        String invoice = bpmn.get("SalesInvoiceApproval");
        assertTrue(invoice.contains("resolveSalesInvoiceCustomerCreditLimit"), invoice);
        assertFalse(invoice.contains("DebitNote"), "the invoice process must not run the debit note's resolver:\n" + invoice);

        String debitNote = bpmn.get("DebitNoteApproval");
        assertTrue(debitNote.contains("resolveDebitNoteCustomerCreditLimit"), debitNote);
        assertFalse(debitNote.contains("SalesInvoice"), "the debit note process must not run the invoice's resolver:\n" + debitNote);
    }

    @Test
    void theSettingsScaffoldOffersOneOptOutPerResolverClass() {
        String settings = IntentSettings.scaffold(IntentParser.parse(YAML))
                                        .toJson();

        assertTrue(settings.contains("\"ResolveSalesInvoiceCustomerCreditLimit\"")
                && settings.contains("\"ResolveDebitNoteCustomerCreditLimit\""), settings);
        assertFalse(settings.contains("\"ResolveCustomerCreditLimit\""), settings);
    }

    /** Every process BPMN the generator writes, keyed by process name. */
    private static Map<String, String> bpmn(IntentModel model) {
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", repository);
        context.setSettings(IntentSettings.scaffold(model));

        new BpmnIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        Map<String, String> byProcess = new HashMap<>();
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            String path = paths.getAllValues()
                               .get(i);
            if (path.endsWith(".bpmn")) {
                byProcess.put(path.substring(path.lastIndexOf('/') + 1, path.length() - ".bpmn".length()),
                        new String(contents.getAllValues()
                                           .get(i),
                                StandardCharsets.UTF_8));
            }
        }
        return byProcess;
    }
}
