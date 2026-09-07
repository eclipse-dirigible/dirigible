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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;

/**
 * A task form showing a field of a CROSS-MODEL to-one (dirigible #7093): the Send form of a sales
 * invoice shows the recipient address, so the clerk sees an empty one before pressing Send. The
 * customer is owned by another model, and the same one-hop path already resolves cross-model as a
 * {@code notify} recipient - so the resolver behind the form control resolves it the same way, from
 * the owner model's {@code .model}, and imports the OWNER's generated Entity/Repository.
 */
class GlueFormCrossModelHopTest {

    private static final String YAML = """
            name: sales-invoices
            uses:
              - { model: customers }
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, model: customers, required: true }
            processes:
              - name: Send
                trigger: { onCreate: SalesInvoice }
                steps:
                  - { name: send, kind: userTask, args: { assignee: clerk, form: SendSalesInvoice } }
                  - { name: done, kind: end }
            forms:
              - name: SendSalesInvoice
                forEntity: SalesInvoice
                fields: [number, Customer, Customer.email]
                actions: [send]
            """;

    /** The owner model as the customers project generated it. */
    private static final String OWNER_MODEL = """
            {
              "model": {
                "entities": [
                  {
                    "name": "Customer",
                    "perspectiveName": "Customer",
                    "dataName": "CUSTOMERS_CUSTOMER",
                    "properties": [
                      { "name": "Id", "dataName": "ID", "dataType": "INTEGER", "dataPrimaryKey": "true" },
                      { "name": "Name", "dataName": "NAME", "dataType": "VARCHAR" },
                      { "name": "Email", "dataName": "EMAIL", "dataType": "VARCHAR" }
                    ]
                  }
                ]
              }
            }
            """;

    /** The reported case verbatim: the path used to be refused at parse time. */
    @Test
    void aCrossModelRelationFieldIsAcceptedOnAForm() {
        IntentParser.parse(YAML);
    }

    @Test
    void theResolverImportsTheOwnerModelsPackage() {
        IntentGenerationContext context = contextWithOwnerModel(IntentParser.parse(YAML));
        List<Map<String, Object>> resolvers = GlueIntentGenerator.buildResolversForTest(context.getModel(), context);

        assertEquals(1, resolvers.size(), "the form's one-hop path must drive one resolver, got: " + resolvers);
        Map<String, Object> resolver = resolvers.get(0);
        assertEquals("ResolveCustomerEmail", resolver.get("handler"));
        assertEquals("Customer_email", resolver.get("variable"));
        assertEquals("Customer", resolver.get("fkProperty"));
        assertEquals("Email", resolver.get("targetField"));
        // The owner model's own perspective, not a guess - a wrong package fails the whole client-Java
        // batch.
        assertEquals("Customer", resolver.get("targetPerspective"));
        assertEquals(Boolean.TRUE, resolver.get("crossModel"));
        assertEquals("customers", resolver.get("targetModel"));
        assertEquals("customers", resolver.get("targetProject"));
        assertEquals("intValue", resolver.get("targetIdAccessor"));
    }

    @Test
    void aLocalRelationFieldCarriesNoCrossModelCoordinates() {
        String local = """
                name: library
                entities:
                  - name: Book
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal }
                  - name: Loan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: book, kind: manyToOne, to: Book }
                processes:
                  - name: Approve
                    trigger: { onCreate: Loan }
                    steps:
                      - { name: approve, kind: userTask, args: { assignee: librarian, form: ApproveLoan } }
                      - { name: done, kind: end }
                forms:
                  - name: ApproveLoan
                    forEntity: Loan
                    fields: [book.price]
                    actions: [approve]
                """;
        Map<String, Object> resolver = GlueIntentGenerator.buildResolversForTest(IntentParser.parse(local), null)
                                                          .get(0);
        assertEquals(Boolean.FALSE, resolver.get("crossModel"));
        assertEquals("", resolver.get("targetModel"), "a local target must leave the model empty so the local gen folder is used");
        assertEquals("", resolver.get("targetProject"));
    }

    /**
     * A field the owner model does not declare fails LOUDLY: skipping the resolver would leave the BPMN
     * with a service task pointing at a handler nothing generated, and the form control bound to a
     * variable nothing ever sets.
     */
    @Test
    void aFieldTheOwnerModelDoesNotDeclareIsReported() {
        IntentGenerationContext context = contextWithOwnerModel(IntentParser.parse(YAML.replace("Customer.email", "Customer.mobile")));
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> GlueIntentGenerator.buildResolversForTest(context.getModel(), context));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("Mobile") && issue.contains("Customer")),
                "the unknown property and its owner must both be named, got: " + ex.getIssues());
    }

    /** A context whose repository serves {@link #OWNER_MODEL} as the sibling project's model. */
    private static IntentGenerationContext contextWithOwnerModel(IntentModel model) {
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(missing.exists()).thenReturn(false);
        IResource owner = mock(IResource.class);
        when(owner.exists()).thenReturn(true);
        when(owner.getContent()).thenReturn(OWNER_MODEL.getBytes(StandardCharsets.UTF_8));
        when(repository.getResource(anyString())).thenReturn(missing);
        when(repository.getResource("/users/admin/workspace/customers/customers.model")).thenReturn(owner);
        return TestContexts.context(model, repository, "/users/admin/workspace/sales-invoices", "app");
    }
}
