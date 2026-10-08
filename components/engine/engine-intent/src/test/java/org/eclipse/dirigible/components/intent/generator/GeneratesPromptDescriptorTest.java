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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.generates.GeneratesIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The action descriptor of a prompted create-from (issues #6685, #7748): the source default each
 * input carries, and - for a target that is no detail of the button's view - the controls
 * themselves.
 */
class GeneratesPromptDescriptorTest {

    private static final String LOCAL = """
            name: sales
            entities:
              - name: PaymentMethod
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: CustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, required: true, label: Amount paid }
                  - { name: date, type: date }
                relations:
                  - { name: Method, kind: manyToOne, to: PaymentMethod }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: balance, type: decimal }
              - name: SalesInvoicePayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment }
            generates:
              - name: record-payment
                from: SalesInvoice
                to: CustomerPayment
                prompt:
                  - { field: amount, required: true, default: balance }
                  - { field: Method }
                  - { field: date }
                link: { entity: SalesInvoicePayment, map: { amount: amount } }
            """;

    @Test
    void aStandaloneTargetCarriesItsControlsAndTheDefault() {
        String descriptor = descriptor(LOCAL, Map.of());
        assertTrue(descriptor.contains("\"name\": \"Amount\",\n      \"required\": true,\n      \"default\": \"Balance\""), descriptor);
        assertTrue(descriptor.contains("\"promptColumns\""), descriptor);
        assertTrue(descriptor.contains("\"label\": \"Amount paid\",\n      \"widget\": \"NUMBER\""), descriptor);
        assertTrue(descriptor.contains("\"widget\": \"DATE\""), descriptor);
        assertTrue(descriptor.contains("\"url\": \"/services/java/proj/gen/sales/api/paymentmethod/PaymentMethodController\""), descriptor);
        assertTrue(descriptor.contains("\"text\": \"Name\""), descriptor);
    }

    /** A detail of the button's view keeps rendering from its own detail registration. */
    @Test
    void aDetailOfTheViewCarriesNoControls() {
        String yaml = LOCAL.replace("- { name: Method, kind: manyToOne, to: PaymentMethod }",
                "- { name: Method, kind: manyToOne, to: PaymentMethod }\n      - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice,"
                        + " composition: true }");
        String descriptor = descriptor(yaml, Map.of());
        assertFalse(descriptor.contains("\"promptColumns\""), descriptor);
    }

    /**
     * A cross-model target is typed from its owner's model, and a relation of it into a THIRD model - a
     * payment's method lives in the payment-methods model - reads its options from that model.
     */
    @Test
    void aCrossModelTargetReadsItsControlsFromTheOwnerAndAThirdModel() {
        String yaml = """
                name: sales
                uses:
                  - { model: customer-payments }
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: balance, type: decimal }
                  - name: SalesInvoicePayment
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                      - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, model: customer-payments }
                generates:
                  - name: record-payment
                    from: SalesInvoice
                    to: CustomerPayment
                    uses: customer-payments
                    prompt:
                      - { field: amount, required: true, default: balance }
                      - { field: Method }
                    link: { entity: SalesInvoicePayment, map: { amount: amount } }
                """;
        String owner = """
                {"model": {"entities": [
                  {"name": "CustomerPayment", "perspectiveName": "CustomerPayment", "properties": [
                    {"name": "Id", "dataType": "INTEGER", "dataPrimaryKey": "true"},
                    {"name": "Amount", "dataType": "DECIMAL"},
                    {"name": "Method", "dataType": "INTEGER", "widgetType": "DROPDOWN", "relationshipEntityName": "PaymentMethod"}]},
                  {"name": "PaymentMethod", "type": "PROJECTION", "projectionReferencedModel": "/payment-methods/payment-methods.model",
                   "perspectiveName": "", "properties": [{"name": "Id", "dataType": "INTEGER", "dataPrimaryKey": "true"}]}]}}
                """;
        String third = """
                {"model": {"entities": [
                  {"name": "PaymentMethod", "perspectiveName": "Settings", "properties": [
                    {"name": "Id", "dataType": "INTEGER", "dataPrimaryKey": "true"},
                    {"name": "Name", "dataType": "VARCHAR"}]}]}}
                """;
        String descriptor = descriptor(yaml, Map.of("/registry/public/customer-payments/customer-payments.model", owner,
                "/registry/public/payment-methods/payment-methods.model", third));
        assertTrue(descriptor.contains("\"widget\": \"NUMBER\""), descriptor);
        assertTrue(
                descriptor.contains("\"url\": \"/services/java/payment-methods/gen/payment_methods/api/settings/PaymentMethodController\""),
                descriptor);
        assertTrue(descriptor.contains("\"default\": \"Balance\""), descriptor);
    }

    private static String descriptor(String yaml, Map<String, String> models) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        models.forEach((path, content) -> {
            IResource resource = mock(IResource.class);
            when(resource.exists()).thenReturn(true);
            when(resource.getContent()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
            when(repository.getResource(path)).thenReturn(resource);
        });
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", repository);
        context.setSettings(IntentSettings.scaffold(model));

        new GeneratesIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/record-payment-generate-action.js")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the action descriptor was not written; wrote " + paths.getAllValues());
    }
}
