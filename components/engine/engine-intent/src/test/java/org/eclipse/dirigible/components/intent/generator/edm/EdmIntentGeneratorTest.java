/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the cross-model + faithfulness output of {@link EdmIntentGenerator} against the Billing
 * example {@code .intent} files: a PROJECTION entity per cross-model target, an integer FK +
 * dropdown on the consuming side, no perspective leakage from projections, and the unique /
 * calculated / audit attributes. Drives the convention-fallback path (no repository); the
 * owner-model-reading path and the generated tables / controllers are covered by the integration
 * test.
 */
class EdmIntentGeneratorTest {

    @SuppressWarnings("unchecked")
    @Test
    void customerEmitsCrossModelProjectionsAndForeignKeys() {
        Map<String, Object> model = buildFromResource("/billing/customers.intent", "customers");
        List<Map<String, Object>> entities = entities(model);

        Map<String, Object> country = entityByName(entities, "Country");
        assertNotNull(country, "a Country projection entity must be emitted");
        assertEquals("PROJECTION", country.get("type"));
        assertEquals("/workspace/countries/countries.model", country.get("projectionReferencedModel"));
        assertEquals("Country", country.get("projectionReferencedEntity"));
        // A projection must stay out of this app's navigation - no perspective.
        assertEquals("", country.get("perspectiveName"));

        Map<String, Object> customer = entityByName(entities, "Customer");
        Map<String, Object> countryFk = propertyByName(customer, "Country");
        assertEquals("INTEGER", countryFk.get("dataType"));
        assertEquals("DROPDOWN", countryFk.get("widgetType"));
        assertEquals("ASSOCIATION", countryFk.get("relationshipType"));
        assertEquals("Country", countryFk.get("relationshipEntityName"));
        assertEquals("Id", countryFk.get("widgetDropDownKey"));
        assertEquals("Name", countryFk.get("widgetDropDownValue"));

        // No perspective should be generated for a projection target.
        List<Map<String, Object>> perspectives = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("perspectives");
        assertTrue(perspectives.stream()
                               .noneMatch(p -> "Country".equals(p.get("name"))),
                "projection targets must not create perspectives");

        // uuid carries the unique constraint; the four audit columns are present.
        assertEquals("true", propertyByName(customer, "Uuid").get("dataUnique"));
        assertEquals("CREATED_AT", propertyByName(customer, "CreatedAt").get("auditType"));
        assertEquals("UPDATED_BY", propertyByName(customer, "UpdatedBy").get("auditType"));

        // The entity's navigation group flows to perspectiveNavId (the shared-shell groupId).
        assertEquals("master-data", customer.get("perspectiveNavId"));
    }

    @Test
    void salesInvoiceModelsCrossModelNToMAndCalculatedNumber() {
        Map<String, Object> model = buildFromResource("/billing/sales-invoices.intent", "sales-invoices");
        List<Map<String, Object>> entities = entities(model);

        // The n:m intermediate is a DEPENDENT of SalesInvoice (local composition) and references
        // CustomerPayment cross-model with an Amount bridge field.
        Map<String, Object> link = entityByName(entities, "SalesInvoiceCustomerPayment");
        assertEquals("DEPENDENT", link.get("type"));
        Map<String, Object> paymentFk = propertyByName(link, "CustomerPayment");
        assertEquals("INTEGER", paymentFk.get("dataType"));
        assertEquals("DROPDOWN", paymentFk.get("widgetType"));
        assertEquals("CustomerPayment", paymentFk.get("relationshipEntityName"));
        assertNotNull(propertyByName(link, "Amount"), "the n:m bridge Amount field must be present");

        Map<String, Object> paymentProjection = entityByName(entities, "CustomerPayment");
        assertEquals("PROJECTION", paymentProjection.get("type"));
        assertEquals("/workspace/customer-payments/customer-payments.model", paymentProjection.get("projectionReferencedModel"));

        // The invoice number is a calculated property assigned on create.
        Map<String, Object> invoice = entityByName(entities, "SalesInvoice");
        Map<String, Object> number = propertyByName(invoice, "Number");
        assertEquals("true", number.get("isCalculatedProperty"));
        assertEquals("java.util.UUID.randomUUID().toString()", number.get("calculatedPropertyExpressionCreate"));

        // The local composition leg stays a normal relation (not cross-model), so the intermediate is a
        // detail of SalesInvoice and SalesInvoice itself owns a real table (PRIMARY).
        assertEquals("PRIMARY", invoice.get("type"));
        // Settings owned by this model are NOT projections (they generate their own tables here).
        assertNull(entityByName(entities, "PaymentMethod").get("projectionReferencedModel"));

        // SalesInvoice owns a composition child whose name ends in "Item" -> it renders with the document
        // (header-items) layout and names its line-items entity; the totals fields carry the aggregate
        // render hint (shown in the footer, not the header form).
        assertEquals("MANAGE_DOCUMENT", invoice.get("layoutType"), "a master with an *Item composition child uses the document layout");
        assertEquals("SalesInvoiceItem", invoice.get("documentItemsEntity"), "the document names its line-items entity");
        assertEquals("Sales Invoice", invoice.get("documentLabel"), "the document header label is the humanized master name");
        assertEquals("Sales Invoice Items", invoice.get("documentItemsLabel"), "the items label is the humanized + pluralized child name");
        assertEquals("true", propertyByName(invoice, "Total").get("aggregate"), "a field marked aggregate carries the footer render hint");
        assertEquals("true", propertyByName(invoice, "Net").get("aggregate"));
        assertNull(propertyByName(invoice, "Date").get("aggregate"), "a non-aggregate field must not carry the hint");
        // The items child itself stays a normal detail (its inline table + controller come from there).
        assertEquals("MANAGE_DETAILS", entityByName(entities, "SalesInvoiceItem").get("layoutType"));
    }

    @Test
    void dependsOnEmitsWidgetAttributesWithPrimaryKeyDefaults() {
        String yaml = """
                name: shop
                uses:
                  - { model: uoms }
                entities:
                  - name: Country
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: City
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: price, type: decimal }
                    relations:
                      - { name: UoM, kind: manyToOne, to: UoM, model: uoms }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal, dependsOn: { relation: Product, valueFrom: price } }
                    relations:
                      - { name: Product, kind: manyToOne, to: Product }
                      - { name: UoM, kind: manyToOne, to: UoM, model: uoms, dependsOn: { relation: Product, valueFrom: UoM } }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country }
                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country, filterBy: Country } }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "shop");
        List<Map<String, Object>> entities = entities(model);

        // Cascade: City filtered by the selected Country; valueFrom defaults to the trigger's PK.
        Map<String, Object> city = propertyByName(entityByName(entities, "Customer"), "City");
        assertEquals("Country", city.get("widgetDependsOnProperty"));
        assertEquals("Country", city.get("widgetDependsOnEntity"));
        assertEquals("Id", city.get("widgetDependsOnValueFrom"), "valueFrom defaults to the trigger target's primary key");
        assertEquals("Country", city.get("widgetDependsOnFilterBy"));

        Map<String, Object> orderItem = entityByName(entities, "OrderItem");
        // Scalar auto-populate: price copied from the chosen Product; a field carries no filterBy.
        Map<String, Object> price = propertyByName(orderItem, "Price");
        assertEquals("Product", price.get("widgetDependsOnProperty"));
        assertEquals("Product", price.get("widgetDependsOnEntity"));
        assertEquals("Price", price.get("widgetDependsOnValueFrom"));
        assertNull(price.get("widgetDependsOnFilterBy"), "a scalar field has no option list to filter");

        // Narrow-to-referenced on a cross-model dependent: filterBy defaults to its own target's PK.
        Map<String, Object> uom = propertyByName(orderItem, "UoM");
        assertEquals("Product", uom.get("widgetDependsOnProperty"));
        assertEquals("UoM", uom.get("widgetDependsOnValueFrom"));
        assertEquals("Id", uom.get("widgetDependsOnFilterBy"), "filterBy defaults to the dependent's own target primary key");

        // An independent property carries none of the attributes.
        Map<String, Object> countryFk = propertyByName(entityByName(entities, "Customer"), "Country");
        assertNull(countryFk.get("widgetDependsOnProperty"));
    }

    private static Map<String, Object> buildFromResource(String resource, String intentName) {
        IntentModel parsed = IntentParser.parse(readResource(resource));
        return EdmIntentGenerator.buildModelJsonForTest(parsed, intentName);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entities(Map<String, Object> modelJson) {
        return (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
    }

    private static Map<String, Object> entityByName(List<Map<String, Object>> entities, String name) {
        return entities.stream()
                       .filter(e -> name.equals(e.get("name")))
                       .findFirst()
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertyByName(Map<String, Object> entity, String name) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
        return properties.stream()
                         .filter(p -> name.equals(p.get("name")))
                         .findFirst()
                         .orElseThrow(() -> new AssertionError("property [" + name + "] not found on entity [" + entity.get("name") + "]"));
    }

    private static String readResource(String resource) {
        try (InputStream in = EdmIntentGeneratorTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing test resource " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("failed to read " + resource, e);
        }
    }
}
