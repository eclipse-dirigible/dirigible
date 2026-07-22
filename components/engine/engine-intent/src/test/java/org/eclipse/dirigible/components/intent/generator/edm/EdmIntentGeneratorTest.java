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
        assertEquals("false", country.get("generateDefaultRoles"));
        assertNull(country.get("roleRead"));

        Map<String, Object> customer = entityByName(entities, "Customer");
        assertEquals("true", customer.get("generateDefaultRoles"));
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
    void crossModelProjectionCellIsMarkedProjectionInTheEdmDiagram() {
        IntentModel parsed = IntentParser.parse(readResource("/billing/customers.intent"));
        String edm = EdmIntentGenerator.buildEdmXmlForTest(parsed, "customers");

        // The mxGraph diagram <Entity> cell for a cross-model reference must carry
        // entityType="PROJECTION" (+ the reference attrs) so the EDM editor renders it as a projection,
        // not as a plain owned PRIMARY entity box.
        int cell = edm.indexOf("<Entity name=\"Country\"");
        assertTrue(cell >= 0, "the Country projection cell must be present in the mxGraph diagram");
        int end = edm.indexOf("/>", cell);
        String countryCell = edm.substring(cell, end);
        assertTrue(countryCell.contains("entityType=\"PROJECTION\""),
                "the cross-model Country cell must be entityType=PROJECTION, was: " + countryCell);
        assertTrue(countryCell.contains("projectionReferencedEntity=\"Country\""), "the projection cell must carry its referenced entity");

        // A locally-owned entity stays a plain cell (no entityType=PROJECTION).
        int owned = edm.indexOf("<Entity name=\"Customer\"");
        String customerCell = edm.substring(owned, edm.indexOf("/>", owned));
        assertTrue(!customerCell.contains("entityType=\"PROJECTION\""), "an owned entity must not be a projection cell");
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
        assertEquals("true", invoice.get("hasPrint"), "a document master gets a .print template, so it is flagged for a Print action");
        assertNull(entityByName(entities, "SalesInvoiceItem").get("hasPrint"), "a line-items child is not a document master - no Print");
        assertEquals("Sales Invoice", invoice.get("documentLabel"), "the document header label is the humanized master name");
        assertEquals("Sales Invoice Items", invoice.get("documentItemsLabel"), "the items label is the humanized + pluralized child name");
        assertEquals("true", propertyByName(invoice, "Total").get("aggregate"), "a field marked aggregate carries the footer render hint");
        assertEquals("true", propertyByName(invoice, "Net").get("aggregate"));
        assertNull(propertyByName(invoice, "Date").get("aggregate"), "a non-aggregate field must not carry the hint");
        // The items child itself stays a normal detail (its inline table + controller come from there).
        assertEquals("MANAGE_DETAILS", entityByName(entities, "SalesInvoiceItem").get("layoutType"));
    }

    @Test
    void attachmentChildInjectsFileMetadataAndIsMarked() {
        String yaml = """
                name: docs
                entities:
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: CompanyAttachment
                    function: Attachment
                    fields:
                      - { name: category, type: string, length: 50 }
                    relations:
                      - { name: Company, kind: manyToOne, to: Company, composition: true, required: true }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "docs");
        List<Map<String, Object>> entities = entities(model);
        Map<String, Object> att = entityByName(entities, "CompanyAttachment");

        // Marked for the generated controller (upload/download verbs) + the Harmonia Attachments panel;
        // it stays a composition detail of its master (the master-detail wiring is unchanged).
        assertEquals("true", att.get("attachmentEntity"));
        assertEquals("MANAGE_DETAILS", att.get("layoutType"));

        // The standard file-metadata columns are injected (upload-set -> read-only); FileName is the
        // row's display title (shown on the list), StoragePath is the internal CMS reference.
        assertEquals("VARCHAR", propertyByName(att, "FileName").get("dataType"));
        assertEquals("true", propertyByName(att, "FileName").get("isReadOnlyProperty"));
        assertEquals("true", propertyByName(att, "FileName").get("widgetIsMajor"));
        assertEquals("BIGINT", propertyByName(att, "FileSize").get("dataType"));
        assertEquals("false", propertyByName(att, "StoragePath").get("widgetIsMajor"));
        assertNotNull(propertyByName(att, "ContentType"));
        assertNotNull(propertyByName(att, "Uuid"));
        // Implicitly audited - who uploaded when.
        assertEquals("CREATED_AT", propertyByName(att, "CreatedAt").get("auditType"));
        // Author-declared domain field preserved alongside the injected ones.
        assertNotNull(propertyByName(att, "Category"));

        // The author declares no primary key on an attachment child, so a generated integer Id is
        // synthesized (auto-increment) - otherwise the generated entity/controller would have no PK.
        Map<String, Object> id = propertyByName(att, "Id");
        assertEquals("true", id.get("dataPrimaryKey"));
        assertEquals("INTEGER", id.get("dataType"));
        assertEquals("true", id.get("dataAutoIncrement"));

        // A plain (non-attachment) entity carries no marker.
        assertNull(entityByName(entities, "Company").get("attachmentEntity"));
        // An attachment is editable (not read-only).
        assertNull(att.get("attachmentReadOnly"));
    }

    @Test
    void snapshotChildIsReadOnlyWithVersionAndFileMetadata() {
        String yaml = """
                name: docs
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                  - name: SalesInvoiceCopy
                    function: Snapshot
                    relations:
                      - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "docs");
        List<Map<String, Object>> entities = entities(model);
        Map<String, Object> snap = entityByName(entities, "SalesInvoiceCopy");

        // Marked as a file child AND read-only (copies are generated server-side, never uploaded/deleted).
        assertEquals("true", snap.get("attachmentEntity"));
        assertEquals("true", snap.get("attachmentReadOnly"));
        assertEquals("MANAGE_DETAILS", snap.get("layoutType"));

        // Same injected file metadata as an attachment...
        assertEquals("VARCHAR", propertyByName(snap, "FileName").get("dataType"));
        assertEquals("true", propertyByName(snap, "FileName").get("isReadOnlyProperty"));
        assertNotNull(propertyByName(snap, "StoragePath"));
        assertNotNull(propertyByName(snap, "Uuid"));
        // ...plus the synthesized generated Id...
        assertEquals("true", propertyByName(snap, "Id").get("dataPrimaryKey"));
        // ...plus a read-only, major Version carrying the DOCUMENT_VERSION widget.
        Map<String, Object> version = propertyByName(snap, "Version");
        assertEquals("INTEGER", version.get("dataType"));
        assertEquals("DOCUMENT_VERSION", version.get("widgetType"));
        assertEquals("true", version.get("isReadOnlyProperty"));
        assertEquals("true", version.get("widgetIsMajor"));
        // Implicitly audited.
        assertEquals("CREATED_AT", propertyByName(snap, "CreatedAt").get("auditType"));
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

    @Test
    void documentMasterWithACalendarViewStillCarriesThePrintFlag() {
        // A document (header-items) master whose UI is overridden to a range calendar: the layout
        // becomes MANAGE_CALENDAR (the document page is replaced by the calendar + the shared manage
        // form for edit), but it still gets a .print template + feeder - so it must keep hasPrint, which
        // is what lets that reused manage form show a Print button.
        String yaml = """
                name: leave
                entities:
                  - name: LeaveRequest
                    view: range
                    calendar: { start: fromDate, end: toDate }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: fromDate, type: date }
                      - { name: toDate, type: date }
                  - name: LeaveRequestItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: day, type: date }
                    relations:
                      - { name: LeaveRequest, kind: manyToOne, to: LeaveRequest, composition: true, required: true }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "leave");
        List<Map<String, Object>> entities = entities(model);
        Map<String, Object> request = entityByName(entities, "LeaveRequest");
        assertEquals("MANAGE_CALENDAR", request.get("layoutType"), "view: range overrides the document layout with the calendar");
        assertEquals("true", request.get("hasPrint"),
                "a document master keeps its Print flag even when the calendar view replaces the document page");
    }

    @Test
    void monthAndWeekMapToVarcharWithTheirPickerWidgets() {
        String yaml = """
                name: planning
                entities:
                  - name: Plan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: month }
                      - { name: sprint, type: week }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "planning");
        Map<String, Object> plan = entityByName(entities(model), "Plan");

        // Both are stored as VARCHAR (indistinguishable at the JDBC level), so the picker widget is
        // chosen from the logical type - the crux of the feature.
        Map<String, Object> period = propertyByName(plan, "Period");
        assertEquals("VARCHAR", period.get("dataType"));
        assertEquals("MONTH", period.get("widgetType"));
        assertEquals("7", period.get("dataLength"), "a month column is sized for YYYY-MM");

        Map<String, Object> sprint = propertyByName(plan, "Sprint");
        assertEquals("VARCHAR", sprint.get("dataType"));
        assertEquals("WEEK", sprint.get("widgetType"));
        assertEquals("8", sprint.get("dataLength"), "a week column is sized for YYYY-Www");
    }

    @Test
    void immutableAlwaysEmitsTheAppendOnlyAttribute() {
        String yaml = """
                name: ledger
                entities:
                  - name: InvoiceSnapshot
                    immutable: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: payload, type: text }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "ledger");
        Map<String, Object> entry = entityByName(entities(model), "InvoiceSnapshot");
        assertEquals("true", entry.get("immutableAlways"));
        assertNull(entry.get("immutableStatusProperty"));
    }

    @Test
    void immutableWhenEmitsStatusGuardAttributes() {
        String yaml = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    immutableWhen: "Status == 2 || Status == 3"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "ledger");
        Map<String, Object> entry = entityByName(entities(model), "JournalEntry");
        assertEquals("Status", entry.get("immutableStatusProperty"));
        assertEquals("2,3", entry.get("immutableStatusValues"));
    }

    @Test
    void securedByDefaultEmitsGenerateDefaultRolesAndRoleNames() {
        String yaml = """
                name: library
                entities:
                  - name: Genre
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Book
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: title, type: string, required: true }
                    relations:
                      - { name: Genre, kind: manyToOne, to: Genre }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "library");
        List<Map<String, Object>> entities = entities(model);

        Map<String, Object> book = entityByName(entities, "Book");
        assertEquals("true", book.get("generateDefaultRoles"));
        assertEquals("library.Book.BookReadOnly", book.get("roleRead"));
        assertEquals("library.Book.BookFullAccess", book.get("roleWrite"));

        Map<String, Object> genre = entityByName(entities, "Genre");
        assertEquals("true", genre.get("generateDefaultRoles"));
        assertEquals("Settings", genre.get("perspectiveName"));
        assertEquals("library.Genre.GenreReadOnly", genre.get("roleRead"));
        assertEquals("library.Genre.GenreFullAccess", genre.get("roleWrite"));
    }

    @Test
    void hierarchyEmitsTreeAndLeafOnlyAttributes() {
        String yaml = """
                name: ledger
                entities:
                  - name: Account
                    hierarchy: Parent
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, required: true, length: 10 }
                    relations:
                      - { name: Parent, kind: manyToOne, to: Account }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                    relations:
                      - { name: Account, kind: manyToOne, to: Account, required: true, leafOnly: true }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "ledger");
        List<Map<String, Object>> entities = entities(model);
        // The tree edge on the entity itself: the self-relation's FK property, PascalCased.
        assertEquals("Parent", entityByName(entities, "Account").get("hierarchyProperty"));
        // The referencing FK carries the restriction plus the TARGET's tree-edge property, so the
        // picker can compute depth/leaves and the validation can count children.
        Map<String, Object> account = propertyByName(entityByName(entities, "JournalEntryItem"), "Account");
        assertEquals("true", account.get("widgetLeafOnly"));
        assertEquals("Parent", account.get("widgetHierarchyProperty"));
        // The self-FK itself carries neither.
        assertNull(propertyByName(entityByName(entities, "Account"), "Parent").get("widgetLeafOnly"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checksEmitTemplateReadyMaps() {
        String yaml = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    checks:
                      - { kind: itemsSumEqual, over: [debit, credit], status: 2, message: "Must balance" }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  - name: JournalEntryItem
                    checks:
                      - { kind: exactlyOne, fields: [debit, credit], message: "Debit or credit" }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "ledger");
        List<Map<String, Object>> entities = entities(model);
        List<Map<String, Object>> entryChecks = (List<Map<String, Object>>) entityByName(entities, "JournalEntry").get("checks");
        assertEquals(1, entryChecks.size());
        Map<String, Object> sumCheck = entryChecks.get(0);
        // Everything the DAO template needs, precomputed: items entity + back-FK + gate + fields.
        assertEquals("JournalEntryItem", sumCheck.get("itemsEntity"));
        assertEquals("JournalEntry", sumCheck.get("itemsFk"));
        assertEquals("Status", sumCheck.get("statusProperty"));
        assertEquals("2", sumCheck.get("status"));
        assertEquals("Debit", sumCheck.get("overA"));
        assertEquals("Credit", sumCheck.get("overB"));
        List<Map<String, Object>> itemChecks = (List<Map<String, Object>>) entityByName(entities, "JournalEntryItem").get("checks");
        assertEquals(List.of("Debit", "Credit"), itemChecks.get(0)
                                                           .get("fields"));
    }

    @Test
    void whereEmitsStaticOptionFilterAttributes() {
        String yaml = """
                name: shop
                uses:
                  - { model: uoms }
                entities:
                  - name: ProductType
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: Type, kind: manyToOne, to: ProductType }
                  - name: StockLine
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: Product, kind: manyToOne, to: Product, where: { Type: 1 } }
                      - { name: UoM, kind: manyToOne, to: UoM, model: uoms, where: { code: KG } }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "shop");
        List<Map<String, Object>> entities = entities(model);

        Map<String, Object> stockLine = entityByName(entities, "StockLine");
        // Same-model: the chooser narrows to Type = 1; a YAML integer renders without a trailing .0.
        Map<String, Object> product = propertyByName(stockLine, "Product");
        assertEquals("Type", product.get("widgetOptionsFilterBy"));
        assertEquals("1", product.get("widgetOptionsFilterValue"));
        // Cross-model (convention fallback in tests): the authored key is PascalCased, value verbatim.
        Map<String, Object> uom = propertyByName(stockLine, "UoM");
        assertEquals("Code", uom.get("widgetOptionsFilterBy"));
        assertEquals("KG", uom.get("widgetOptionsFilterValue"));
        // An unfiltered relation carries neither attribute.
        Map<String, Object> type = propertyByName(entityByName(entities, "Product"), "Type");
        assertNull(type.get("widgetOptionsFilterBy"));
    }

    @Test
    void multilingualEntityAndLanguagesFlowIntoTheModel() {
        String yaml = """
                name: uoms
                languages: [en, bg]
                entities:
                  - name: UoM
                    kind: setting
                    multilingual: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 100 }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "uoms");
        List<Map<String, Object>> entities = entities(model);

        assertEquals("true", entityByName(entities, "UoM").get("multilingual"),
                "a multilingual entity should carry the EDM multilingual attribute");
        assertNull(entityByName(entities, "Product").get("multilingual"), "a regular entity must not carry the attribute");

        @SuppressWarnings("unchecked")
        List<String> languages = (List<String>) ((Map<String, Object>) model.get("model")).get("languages");
        assertEquals(List.of("en", "bg"), languages, "the intent's languages should land on the .model root");
    }

    @Test
    void dashboardWidgetsFlowIntoTheModelRoot() {
        String yaml = """
                name: sales
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                reports:
                  - name: OverdueInvoices
                    source: Invoice
                    widget: { kind: count, label: Overdue Invoices }
                widgets:
                  - name: SystemHealth
                    url: /services/js/sales/custom/health.js
                  - name: SalesFunnel
                    kind: page
                    url: /services/web/sales/custom/funnel/index.html
                    label: Sales Funnel
                    icon: chart-column
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "sales");
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) model.get("model");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> widgets = (List<Map<String, Object>>) root.get("widgets");
        assertEquals(2, widgets.size(), "both custom widgets should land on the .model root");
        Map<String, Object> health = widgets.get(0);
        assertEquals("kpi", health.get("kind"), "kind should default to kpi");
        assertEquals("System Health", health.get("label"), "label should default to the humanized name");
        assertEquals("widgetSystemHealth", health.get("tId"));
        assertEquals("gauge", health.get("icon"), "icon should default to gauge");
        Map<String, Object> funnel = widgets.get(1);
        assertEquals("page", funnel.get("kind"));
        assertEquals("/services/web/sales/custom/funnel/index.html", funnel.get("url"));
    }

    @Test
    void reportWidgetAloneAddsNoCustomWidgetsArray() {
        String yaml = """
                name: sales
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                reports:
                  - name: OverdueInvoices
                    source: Invoice
                    widget: { kind: count }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) EdmIntentGenerator.buildModelJsonForTest(parsed, "sales")
                                                                           .get("model");
        assertNull(root.get("widgets"), "no custom widgets - the .model root must not carry an empty array");
    }

    @Test
    @SuppressWarnings("unchecked")
    void explicitOrderInterleavesFieldsAndRelations() {
        String yaml = """
                name: sales
                entities:
                  - name: Header
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Line
                    order: [Id, Header, Product, Name]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: Header, kind: manyToOne, to: Header, composition: true, required: true }
                      - { name: Product, kind: manyToOne, to: Product }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "sales");
        Map<String, Object> line = entityByName(entities(model), "Line");
        List<String> names = ((List<Map<String, Object>>) line.get("properties")).stream()
                                                                                 .map(p -> String.valueOf(p.get("name")))
                                                                                 .toList();
        // The four listed properties come first in the given order (relations interleaved, no longer
        // pushed last); the unlisted Quantity keeps its default position and is appended after.
        assertEquals(List.of("Id", "Header", "Product", "Name", "Quantity"), names,
                "properties should follow the explicit order, with unlisted ones appended");
    }

    private static Map<String, Object> buildFromResource(String resource, String intentName) {
        IntentModel parsed = IntentParser.parse(readResource(resource));
        return EdmIntentGenerator.buildModelJsonForTest(parsed, intentName);
    }

    @SuppressWarnings("unchecked")
    @Test
    void dependentCalendarChildKeepsTheDetailLayoutAndCarriesTheCalendarMeta() {
        String yaml = """
                name: work
                entities:
                  - name: Timesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: month, type: date }
                    relations:
                      - { name: days, kind: oneToMany, to: DayAllocation }
                  - name: DayAllocation
                    view: calendar
                    calendar: { start: day, title: note }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: day, type: date }
                      - { name: note, type: string }
                    relations:
                      - { name: Timesheet, kind: manyToOne, to: Timesheet, composition: true, required: true }
                  - name: Meeting
                    view: calendar
                    calendar: { start: at }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: at, type: timestamp }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "work");
        List<Map<String, Object>> entities = entities(model);

        // A COMPOSITION CHILD with view: calendar stays a detail of its master (registry, filtered
        // controller, form pages) - the calendar is HOW the master renders its panel.
        Map<String, Object> child = entityByName(entities, "DayAllocation");
        assertEquals("MANAGE_DETAILS", child.get("layoutType"));
        assertEquals("true", child.get("detailCalendar"));
        assertEquals("Day", child.get("calendarStartProperty"));
        assertEquals("Note", child.get("calendarTitleProperty"));

        // A PRIMARY calendar entity keeps the standalone calendar page as before.
        Map<String, Object> primary = entityByName(entities, "Meeting");
        assertEquals("MANAGE_CALENDAR", primary.get("layoutType"));
        assertEquals(null, primary.get("detailCalendar"));
    }

    @Test
    void chatDocumentMasterCarriesTheChatLayoutAndResolvedMessageProperties() {
        String yaml = """
                name: services
                entities:
                  - name: Case
                    function: Document
                    documentItemsLayout: chat
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: subject, type: string }
                  - name: CaseMessage
                    function: DocumentItem
                    audit: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: body, type: text, messageBody: true }
                      - { name: internal, type: boolean, messageInternal: true }
                    relations:
                      - { name: Case, kind: manyToOne, to: Case, composition: true, required: true }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "services");
        Map<String, Object> master = entityByName(entities(model), "Case");

        assertEquals("MANAGE_DOCUMENT", master.get("layoutType"));
        assertEquals("CaseMessage", master.get("documentItemsEntity"));
        assertEquals("chat", master.get("documentItemsLayout"));
        assertEquals("Body", master.get("chatBodyProperty"));
        assertEquals("Internal", master.get("chatInternalProperty"));
    }

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

    @Test
    void identityPersonalAndSensitiveFlowIntoTheModel() {
        String yaml = """
                name: hr
                entities:
                  - name: Employee
                    identity: email
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 200 }
                      - { name: email, type: string, required: true, unique: true, length: 320 }
                  - name: VacationRequest
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string, length: 400 }
                      - { name: dailyRate, type: decimal, sensitive: true }
                    relations:
                      - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "hr");
        List<Map<String, Object>> entities = entities(model);
        // The mapping entity advertises which property identifies the current user - consumers
        // (incl. cross-model, via TargetInfo) read it off the model.
        assertEquals("Email", entityByName(entities, "Employee").get("identityProperty"));
        // The owner FK carries the personal marker plus the target's identity property, which the
        // generated personal controller matches against the logged-in username.
        Map<String, Object> owner = propertyByName(entityByName(entities, "VacationRequest"), "Employee");
        assertEquals("true", owner.get("relationshipPersonal"));
        assertEquals("Email", owner.get("relationshipIdentityProperty"));
        // The confidential field is flagged for the personal-surface scrub; a plain one is not.
        assertEquals("true", propertyByName(entityByName(entities, "VacationRequest"), "DailyRate").get("sensitiveProperty"));
        assertNull(propertyByName(entityByName(entities, "VacationRequest"), "Note").get("sensitiveProperty"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void labelSynthesizesTheStoredNameAndTheTemplateParts() {
        String yaml = """
                name: sales
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 200 }
                  - name: SalesInvoice
                    label: "{number} - {date|yyyy MMMM} - {Customer.name}"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, length: 40 }
                      - { name: date, type: date, required: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: SalesInvoice }
                """;
        // (relation target kept same-model for the unit scope)
        yaml = yaml.replace("to: SalesInvoice", "to: Customer");
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "sales");
        List<Map<String, Object>> entities = entities(model);
        Map<String, Object> invoice = entityByName(entities, "SalesInvoice");
        // The synthesized stored Name: read-only, on the list, 512 chars.
        Map<String, Object> nameProperty = propertyByName(invoice, "Name");
        assertEquals("SALES_INVOICE_NAME", nameProperty.get("dataName"));
        assertEquals("true", nameProperty.get("isReadOnlyProperty"));
        // The template-ready parts: field, formatted field, one-hop relation, literals between.
        List<Map<String, Object>> parts = (List<Map<String, Object>>) invoice.get("labelParts");
        assertEquals("field", parts.get(0)
                                   .get("kind"));
        assertEquals("Number", parts.get(0)
                                    .get("property"));
        assertEquals("yyyy MMMM", parts.get(2)
                                       .get("format"));
        Map<String, Object> relationPart = parts.get(4);
        assertEquals("relation", relationPart.get("kind"));
        assertEquals("Customer", relationPart.get("relation"));
        assertEquals("Name", relationPart.get("property"));
        // A dropdown pointing at a label-bearing entity resolves to its generated Name.
        assertEquals("Name", propertyByName(invoice, "Customer").get("widgetDropDownValue")
                                                                .toString()
                                                                .replace("Name", "Name"));
    }

    @Test
    void partnerRelationEmitsThePartnerOwnerAttributesFromTheTargetIdentity() {
        String yaml = """
                name: services
                entities:
                  - name: Customer
                    identity: email
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: name,  type: string, required: true, length: 200 }
                      - { name: email, type: string, required: true, unique: true, length: 320 }
                  - name: Case
                    fields:
                      - { name: id,      type: integer, primaryKey: true, generated: true }
                      - { name: subject, type: string, length: 200 }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, required: true, partner: true }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "services");
        Map<String, Object> fk = propertyByName(entityByName(entities(model), "Case"), "Customer");

        assertEquals("true", fk.get("relationshipPartner"));
        assertEquals("Email", fk.get("relationshipPartnerIdentityProperty"));
        assertEquals("Name", fk.get("relationshipPartnerIdentityLabel"));
    }

    @Test
    void extendsMarksExtensionEntityWithBaseReferenceAndNoPerspective() {
        String yaml = """
                name: kf-mod-employees-bg
                uses:
                  - { model: kf-mod-employees }
                entities:
                  - name: EmployeeBg
                    extends: { model: kf-mod-employees, entity: Employee }
                    fields:
                      - { name: egn, type: string, length: 10 }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "kf-mod-employees-bg");
        Map<String, Object> ext = entityByName(entities(model), "EmployeeBg");

        // Marked EXTENSION with the base reference the model-to-code merge keys on; owns no UI.
        assertEquals("EXTENSION", ext.get("type"));
        assertEquals("kf-mod-employees", ext.get("extensionReferencedModel"));
        assertEquals("Employee", ext.get("extensionReferencedEntity"));
        assertEquals("", ext.get("layoutType"));
        assertEquals("", ext.get("perspectiveName"));
        // Its contributed field is emitted as a normal property (folded into the base table later).
        assertEquals("VARCHAR", propertyByName(ext, "Egn").get("dataType"));
    }

    @Test
    void extensionFieldCarriesPlacementHint() {
        String yaml = """
                name: employees-bg
                uses:
                  - { model: employees }
                entities:
                  - name: EmployeeBg
                    extends: { model: employees, entity: Employee }
                    fields:
                      - { name: egn,  type: string, length: 10, after: lastName }
                      - { name: tin,  type: string, length: 15, before: birthDate }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "employees-bg");
        Map<String, Object> ext = entityByName(entities(model), "EmployeeBg");
        // The placement hint is PascalCased to match the base's .model property name; the merge
        // (generateUtils) splices the column at that position in the base's property order.
        assertEquals("LastName", propertyByName(ext, "Egn").get("extensionAfter"));
        assertEquals("BirthDate", propertyByName(ext, "Tin").get("extensionBefore"));
    }
}
