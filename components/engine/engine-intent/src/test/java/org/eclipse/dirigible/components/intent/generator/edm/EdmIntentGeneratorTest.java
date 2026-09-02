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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals("/countries/countries.model", country.get("projectionReferencedModel"),
                "the projection path must name only the owner project + model - no workspace segment (#6423)");
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

        // uuid carries the unique constraint and is platform-generated on create (no custom action);
        // the four audit columns are present.
        assertEquals("true", propertyByName(customer, "Uuid").get("dataUnique"));
        assertEquals("true", propertyByName(customer, "Uuid").get("generatedUuid"));
        assertEquals("CREATED_AT", propertyByName(customer, "CreatedAt").get("auditType"));
        assertEquals("UPDATED_BY", propertyByName(customer, "UpdatedBy").get("auditType"));

        // The entity's navigation group flows to perspectiveNavId (the shared-shell groupId).
        assertEquals("master-data", customer.get("perspectiveNavId"));
    }

    @Test
    void numberFieldEmitsStampMarkers() {
        String yaml = """
                name: billing
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, number: { series: SalesInvoice, stampOn: issue } }
                  - name: Proforma
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, number: { series: Proforma, stampOn: create } }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "billing");
        List<Map<String, Object>> entities = entities(model);

        // stampOn: issue -> a UUID placeholder on create (reusing the uuid auto-fill) + the series markers.
        Map<String, Object> siNumber = propertyByName(entityByName(entities, "SalesInvoice"), "Number");
        assertEquals("issue", siNumber.get("numberStampOn"));
        assertEquals("SalesInvoice", siNumber.get("numberSeries"));
        assertEquals("true", siNumber.get("generatedUuid"));
        assertNull(siNumber.get("numberStampOnCreate"));

        // stampOn: create -> the real number is stamped on insert (numberStampOnCreate), no placeholder.
        Map<String, Object> pfNumber = propertyByName(entityByName(entities, "Proforma"), "Number");
        assertEquals("create", pfNumber.get("numberStampOn"));
        assertEquals("true", pfNumber.get("numberStampOnCreate"));
        assertNull(pfNumber.get("generatedUuid"));
        // The model carries the series REFERENCE and (optionally) the partition - never the shape. The
        // prefix and width live in the .numbers artefact and the tenant's settings, so one application
        // serves jurisdictions with different conventions without being regenerated.
        assertEquals("Proforma", pfNumber.get("numberSeries"));
        assertNull(pfNumber.get("numberFormat"), "the model must not carry a number format");
        assertNull(pfNumber.get("numberScope"), "the model must not carry a scope/token list");
    }

    @SuppressWarnings("unchecked")
    @Test
    void processUserTasksEmitTheirLabelCatalogMap() {
        String yaml = """
                name: orders
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                processes:
                  - name: OrderApproval
                    trigger: { onCreate: Order }
                    steps:
                      - name: managerReview
                        kind: userTask
                        args: { assignee: manager, form: ApproveOrder }
                forms:
                  - name: ApproveOrder
                    forEntity: Order
                    fields: [total]
                    actions: [approve]
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "orders");
        Map<String, Object> body = (Map<String, Object>) model.get("model");
        // Keyed by the authored step name (the customActionLabels convention); the value is the
        // humanized runtime task name, so the reverse map baked into config.js lets a view resolve
        // an inbox task's name to <project>:<model>-model.processes.<step name> by exact match.
        Map<String, String> labels = (Map<String, String>) body.get("processTaskLabels");
        assertNotNull(labels, "a process with user tasks must put processTaskLabels on the .model root");
        assertEquals("Manager Review", labels.get("managerReview"));
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

        // The enclosing <mxCell> must carry style="projection" - the EDM editor colors an entity purely
        // from the cell style on load, so without it the projection renders as the default blue (#6333).
        assertTrue(mxCellTagFor(edm, "Country").contains("style=\"projection\""),
                "the PROJECTION cell must carry style=projection so the modeler colors it purple");

        // A locally-owned entity stays a plain cell (no entityType=PROJECTION) styled as the default blue.
        int owned = edm.indexOf("<Entity name=\"Customer\"");
        String customerCell = edm.substring(owned, edm.indexOf("/>", owned));
        assertTrue(!customerCell.contains("entityType=\"PROJECTION\""), "an owned entity must not be a projection cell");
        assertTrue(mxCellTagFor(edm, "Customer").contains("style=\"entity\""), "an owned PRIMARY entity must keep style=entity");
    }

    @Test
    void crossModelRelationDrawsAnEdgeToTheProjection() {
        IntentModel parsed = IntentParser.parse(readResource("/billing/customers.intent"));
        String edm = EdmIntentGenerator.buildEdmXmlForTest(parsed, "customers");

        // The EDM modeler draws the diagram purely from the <mxGraphModel> edges. A cross-model FK must
        // emit the same owner-FK -> projection-PK edge a same-model relation does, or the projection box
        // renders as a disconnected island with no arrow to its owning entity (#6335). Customer.Country
        // is a manyToOne into the countries model, so the FK property cell is ent_Customer_p_Country and
        // the edge targets the Country projection's primary-key cell (ent_Country_p_*).
        int edgeIdx = edm.indexOf("id=\"edge_Customer_Country\"");
        assertTrue(edgeIdx >= 0, "a diagram edge from the Customer FK to the Country projection must exist");
        String edge = edm.substring(edm.lastIndexOf("<mxCell", edgeIdx), edm.indexOf('>', edgeIdx) + 1);
        assertTrue(edge.contains("edge=\"1\""), "the cross-model link must be an mxGraph edge, was: " + edge);
        assertTrue(edge.contains("source=\"ent_Customer_p_Country\""),
                "the edge must start at the Customer Country FK property cell, was: " + edge);
        assertTrue(edge.contains("target=\"ent_Country_p_"), "the edge must point at the Country projection's PK cell, was: " + edge);
    }

    @Test
    void notNullPropertiesCarryDataNotNullSoTheModelerKeepsTheConstraint() {
        IntentModel parsed = IntentParser.parse(readResource("/billing/customers.intent"));
        String edm = EdmIntentGenerator.buildEdmXmlForTest(parsed, "customers");

        // The EDM editor's "Not null" checkbox binds to dataNotNull; a NOT NULL column must carry
        // dataNotNull="true" in its mxGraph <Property> cell exactly as a hand-modeled .edm does, or the
        // checkbox loads unchecked and a re-save drops the constraint via dataNullable="true" (#6332).
        // A required field and the primary key are both NOT NULL.
        assertTrue(propertyCell(edm, "Name").contains("dataNotNull=\"true\""),
                "a required field must carry dataNotNull=\"true\", was: " + propertyCell(edm, "Name"));
        assertTrue(propertyCell(edm, "Id").contains("dataNotNull=\"true\""),
                "a primary key must carry dataNotNull=\"true\", was: " + propertyCell(edm, "Id"));
        // A nullable field must NOT carry it, so its checkbox stays unchecked and it round-trips nullable.
        assertTrue(!propertyCell(edm, "Phone").contains("dataNotNull"),
                "a nullable field must not carry dataNotNull, was: " + propertyCell(edm, "Phone"));
    }

    private static String propertyCell(String edm, String propertyName) {
        int idx = edm.indexOf("<Property name=\"" + propertyName + "\"");
        assertTrue(idx >= 0, "the mxGraph <Property> cell for [" + propertyName + "] must be present");
        return edm.substring(idx, edm.indexOf("/>", idx));
    }

    @Test
    void entityCellsCarryTheModelerStylePerType() {
        // The EDM editor colors each entity solely from the mxCell style attribute (reconciled from
        // entityType only when the entity dialog re-saves, never on load). So an intent-generated .edm must
        // emit the same per-type style a hand-modeled one carries, or every entity loads as the default
        // blue "entity" style regardless of type (#6333). sales-invoices exercises all four types.
        IntentModel parsed = IntentParser.parse(readResource("/billing/sales-invoices.intent"));
        String edm = EdmIntentGenerator.buildEdmXmlForTest(parsed, "sales-invoices");

        assertTrue(mxCellTagFor(edm, "SalesInvoice").contains("style=\"entity\""), "a PRIMARY entity is styled entity (blue)");
        assertTrue(mxCellTagFor(edm, "SalesInvoiceItem").contains("style=\"dependent\""),
                "a composition-child DEPENDENT entity is styled dependent (darker blue)");
        assertTrue(mxCellTagFor(edm, "PaymentMethod").contains("style=\"setting\""), "a SETTING entity is styled setting (grey)");
        assertTrue(mxCellTagFor(edm, "CustomerPayment").contains("style=\"projection\""),
                "a cross-model PROJECTION entity is styled projection (purple)");

        // A PROJECTION entity's property rows carry the dedicated projectionproperty style, matching the
        // editor's own serialization; the other three types keep the default (unstyled) child cells.
        assertTrue(edm.contains("style=\"projectionproperty\""),
                "a PROJECTION entity's property cells must carry style=projectionproperty");
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
        assertEquals("/customer-payments/customer-payments.model", paymentProjection.get("projectionReferencedModel"));

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
    void relationMajorFalseEmitsWidgetIsMajorFalseAndDefaultsTrue() {
        String yaml = """
                name: shop
                uses:
                  - { model: products }
                entities:
                  - name: Category
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: Category, kind: manyToOne, to: Category }                             # default -> list column
                      - { name: Note, kind: manyToOne, to: Category, major: false }                   # local, off the list
                      - { name: Product, kind: manyToOne, to: Product, model: products, major: false } # cross-model, off the list
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "shop");
        Map<String, Object> item = entityByName(entities(model), "OrderItem");

        assertEquals("true", propertyByName(item, "Category").get("widgetIsMajor"), "a relation is a list column by default");
        assertEquals("false", propertyByName(item, "Note").get("widgetIsMajor"), "major:false keeps a local relation off the list table");
        assertEquals("false", propertyByName(item, "Product").get("widgetIsMajor"),
                "major:false keeps a cross-model relation off the list table");
    }

    /**
     * A to-one relation may derive its FK server-side, the counterpart of the field-level calculated
     * action: {@code init:} covers a FIXED default (a literal seed id) but never a DERIVED one - a
     * document's currency read off its company's base currency. The property must carry the same three
     * keys a calculated field emits, on BOTH the same-model and the cross-model relation builder,
     * because the DAO template's shared property loop is what turns them into
     * {@code entity.<Relation> = Beans.get(<class>.class).calculate(entity);}. A relation with no
     * action must stay untouched - the marker is what makes the template emit the call at all.
     */
    @Test
    void relationCalculatedActionEmitsTheServerSideCallOutOnBothRelationBuilders() {
        String yaml =
                """
                        name: shop
                        uses:
                          - { model: currencies }
                        entities:
                          - name: Company
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: name, type: string }
                          - name: Order
                            imports: |
                              import custom.shop.OrderCurrencyAction;
                              import custom.shop.OrderCompanyAction;
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: name, type: string }
                            relations:
                              # cross-model: the FK is derived from another record on create
                              - { name: Currency, kind: manyToOne, to: Currency, model: currencies, calculatedActionOnCreate: OrderCurrencyAction }
                              # same-model, and the update slot as well
                              - { name: Company, kind: manyToOne, to: Company, calculatedActionOnCreate: OrderCompanyAction, calculatedActionOnUpdate: OrderCompanyAction }
                              - { name: Alternate, kind: manyToOne, to: Company }
                        """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "shop");
        Map<String, Object> order = entityByName(entities(model), "Order");

        Map<String, Object> currency = propertyByName(order, "Currency");
        assertEquals("true", currency.get("isCalculatedProperty"), "the cross-model FK must be marked calculated");
        assertEquals("OrderCurrencyAction", currency.get("calculatedActionOnCreate"));
        assertNull(currency.get("calculatedActionOnUpdate"), "only the create slot was authored");
        assertEquals("INTEGER", currency.get("dataType"), "it stays an ordinary FK property - that is what the template assigns");

        Map<String, Object> company = propertyByName(order, "Company");
        assertEquals("true", company.get("isCalculatedProperty"), "the same-model FK must be marked calculated too");
        assertEquals("OrderCompanyAction", company.get("calculatedActionOnCreate"));
        assertEquals("OrderCompanyAction", company.get("calculatedActionOnUpdate"));

        Map<String, Object> alternate = propertyByName(order, "Alternate");
        assertNull(alternate.get("isCalculatedProperty"), "a relation with no action must not be marked calculated");
        assertNull(alternate.get("calculatedActionOnCreate"));
    }

    @Test
    void documentMasterWithACalendarViewKeepsTheDocumentLayoutAndAddsTheCalendar() {
        // #6547: a calendar/range view is an ADDITIONAL page - it must NOT replace the layout. A document
        // (header-items) master browsed on a calendar keeps MANAGE_DOCUMENT (so create/edit open the
        // document editor with its line items, Print and inline process tasks) and merely carries
        // calendarView + the calendar* metadata for the extra page.
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
        assertEquals("MANAGE_DOCUMENT", request.get("layoutType"), "view: range no longer overrides the document layout");
        assertEquals("LeaveRequestItem", request.get("documentItemsEntity"));
        assertEquals("true", request.get("calendarView"), "the calendar rides alongside the layout as an additional page");
        assertEquals("FromDate", request.get("calendarStartProperty"));
        assertEquals("ToDate", request.get("calendarEndProperty"));
        assertEquals("true", request.get("calendarRange"));
        assertEquals("true", request.get("hasPrint"), "a document master keeps its Print flag");
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

    /**
     * A text field is a wide VARCHAR, not a CLOB: the generated entity declares the same length, so the
     * entity layer's Hibernate mapping agrees with the column instead of rewriting it to its own
     * default (which silently made every text column a varchar(255)). Its widget still comes from the
     * logical type, since the column type no longer tells them apart.
     */
    @Test
    void textFieldIsAWideVarcharWithATextareaWidget() {
        String yaml = """
                name: ledger
                entities:
                  - name: Note
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: body, type: text }
                      - { name: excerpt, type: text, length: 500 }
                      - { name: title, type: string }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "ledger");
        Map<String, Object> note = entityByName(entities(model), "Note");

        Map<String, Object> body = propertyByName(note, "Body");
        assertEquals("VARCHAR", body.get("dataType"));
        assertEquals("4000", body.get("dataLength"));
        assertEquals("TEXTAREA", body.get("widgetType"));

        // An authored length still wins over the default.
        assertEquals("500", propertyByName(note, "Excerpt").get("dataLength"));

        // A plain string keeps its own default and widget.
        Map<String, Object> title = propertyByName(note, "Title");
        assertEquals("100", title.get("dataLength"));
        assertEquals("TEXTBOX", title.get("widgetType"));
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
    void periodLockEmitsBothHalvesOnTheEntitiesThatDeclareThem() {
        String yaml = """
                name: ledger
                entities:
                  - name: PeriodStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: AccountingPeriod
                    period: { start: startDate, end: endDate, closedWhen: "Status == CLOSED" }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: startDate, type: date }
                      - { name: endDate, type: date }
                    relations:
                      - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: 1 }
                  - name: JournalEntry
                    immutableInPeriod: { period: AccountingPeriod, date: entryDate }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: entryDate, type: date }
                seeds:
                  - name: period-statuses
                    entity: PeriodStatus
                    rows:
                      - { id: 1, name: OPEN }
                      - { id: 2, name: CLOSED }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "ledger");
        // The register contributes its own shape...
        Map<String, Object> period = entityByName(entities(model), "AccountingPeriod");
        assertEquals("StartDate", period.get("periodStartProperty"));
        assertEquals("EndDate", period.get("periodEndProperty"));
        assertEquals("Status", period.get("periodStatusProperty"));
        assertEquals("2", period.get("periodClosedValues"));
        // ...the guarded entity only which register locks it and which of its dates decides.
        Map<String, Object> entry = entityByName(entities(model), "JournalEntry");
        assertEquals("AccountingPeriod", entry.get("periodLockEntity"));
        assertEquals("EntryDate", entry.get("periodLockDateProperty"));
        assertNull(entry.get("periodStartProperty"));
        assertNull(period.get("periodLockEntity"));
    }

    @Test
    void lifecycleEmitsTheStateMachineTheRepositoryEnforces() {
        String yaml = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                    lifecycle:
                      edges:
                        - { from: DRAFT,  to: [POSTED, CANCELLED] }
                        - { from: POSTED, to: [VOIDED] }
                seeds:
                  - name: entry-statuses
                    entity: EntryStatus
                    rows:
                      - { id: 1, name: DRAFT }
                      - { id: 2, name: POSTED }
                      - { id: 3, name: CANCELLED }
                      - { id: 4, name: VOIDED }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "ledger");
        Map<String, Object> entry = entityByName(entities(model), "JournalEntry");
        assertEquals("Status", entry.get("lifecycleStatusProperty"));
        assertEquals("1>2,1>3,2>4", entry.get("lifecycleEdges"));
        // The seeded names ride along so a rejection reads "cannot move from POSTED to DRAFT".
        assertEquals("1=DRAFT,2=POSTED,3=CANCELLED,4=VOIDED", entry.get("lifecycleStatusNames"));
        // With a declared start, a record cannot be CREATED mid-lifecycle either.
        assertEquals("1", entry.get("lifecycleInitialStatus"));
        // An entity without a lifecycle carries none of it.
        assertNull(entityByName(entities(model), "EntryStatus").get("lifecycleEdges"));
    }

    @Test
    void declaredPhasesReachTheModelSoTheRepositoryCanAnnounceThem() {
        String yaml = """
                name: inventory
                entities:
                  - name: StockMovement
                    phases: [costed, invoiced]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: costValue, type: decimal }
                  - name: Warehouse
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "inventory");

        assertEquals("costed,invoiced", entityByName(entities(model), "StockMovement").get("phases"));
        // An entity that announces nothing carries nothing - a model that says nothing generates
        // byte-identically to one written before the axis existed.
        assertNull(entityByName(entities(model), "Warehouse").get("phases"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardCheckEmitsKeyedAggregateGuard() {
        String yaml = """
                name: inventory
                entities:
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Store
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: StockMovement
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: Product, kind: manyToOne, to: Product }
                      - { name: Store, kind: manyToOne, to: Store }
                    checks:
                      - kind: guard
                        aggregate: onHand
                        minimum: 0
                        message: Insufficient stock
                        enabledBy: INVENTORY_BLOCK_NEGATIVE_STOCK
                  - name: ProductAvailability
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: onHand, type: decimal }
                    relations:
                      - { name: Product, kind: manyToOne, to: Product }
                      - { name: Store, kind: manyToOne, to: Store }
                aggregates:
                  - name: onHand
                    of: StockMovement
                    op: sum
                    sum: quantity
                    by: [Product, Store]
                    into: ProductAvailability
                    field: onHand
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "inventory");
        Map<String, Object> movement = entityByName(entities(model), "StockMovement");
        List<Map<String, Object>> checks = (List<Map<String, Object>>) movement.get("checks");
        assertEquals(1, checks.size());
        Map<String, Object> guard = checks.get(0);
        assertEquals("guard", guard.get("kind"));
        assertEquals("Quantity", guard.get("sumField"));
        assertEquals("Id", guard.get("pk"));
        assertEquals("0", guard.get("minimum"));
        assertEquals("Insufficient stock", guard.get("message"));
        assertEquals("INVENTORY_BLOCK_NEGATIVE_STOCK", guard.get("enabledBy"));
        List<Map<String, String>> keys = (List<Map<String, String>>) guard.get("keys");
        assertEquals(2, keys.size());
        assertEquals("Product", keys.get(0)
                                    .get("key"));
        assertEquals("Store", keys.get(1)
                                  .get("key"));
        // No authored outcome = block: the write fails. The two non-blocking outcomes are asserted below.
        assertEquals("block", guard.get("outcome"));

        // The aggregate's SOURCE entity carries its grouping keys + pk, so the DAO can detect that a
        // key moved and let the aggregate repair the tuple the row left (there is no event for it).
        List<Map<String, String>> sourceKeys = (List<Map<String, String>>) movement.get("groupingKeys");
        assertEquals(2, sourceKeys.size(), "the aggregate source must carry every grouping key");
        assertEquals("Product", sourceKeys.get(0)
                                          .get("key"));
        assertEquals("Store", sourceKeys.get(1)
                                        .get("key"));
        assertEquals("Id", movement.get("groupingSourcePk"));
        // The TARGET is not a source, so it carries no rekey metadata.
        assertNull(entityByName(entities(model), "ProductAvailability").get("groupingKeys"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rollupChildCarriesItsParentFkAsAGroupingKey() {
        String yaml = """
                name: orders
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: orderCount, type: integer }
                    relations:
                      - { name: orders, kind: oneToMany, to: Order }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                rollups:
                  - { name: orderCount, entity: Order, via: Customer, field: orderCount, op: count }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "orders");
        // Re-parenting a roll-up child moves its rows between parents exactly as an aggregate key does,
        // so the child must carry the `via` FK too - the DAO compares it and publishes "-rekeyed", which
        // is the only signal the parent the child moved AWAY from ever gets (#6819).
        List<Map<String, String>> keys = (List<Map<String, String>>) entityByName(entities(model), "Order").get("groupingKeys");
        assertEquals(1, keys.size(), "a roll-up child must track its parent FK");
        assertEquals("Customer", keys.get(0)
                                     .get("key"));
        assertEquals("Id", entityByName(entities(model), "Order").get("groupingSourcePk"));
        // The PARENT of a roll-up is not grouped by anything - only the child moves.
        assertNull(entityByName(entities(model), "Customer").get("groupingKeys"));
    }

    /**
     * A settlement matches invoices by columns of the PAYMENT row, so a corrected match value (the
     * payment re-filed under another Customer) moves the whole allocation between counterparties. The
     * payment must track those columns as grouping keys - the DAO's "-rekeyed" publish is the only
     * signal the settlement's re-key handler ever gets.
     */
    @Test
    @SuppressWarnings("unchecked")
    void aSettlementsMatchColumnsAreThePaymentsGroupingKeys() {
        String yaml = """
                name: settle
                entities:
                  - name: Invoice
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: date,  type: date }
                      - { name: total, type: decimal, precision: 18, scale: 2 }
                      - { name: paid,  type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: Payment
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: date,   type: date }
                      - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: Customer
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string,  required: true, length: 100 }
                  - name: InvoicePayment
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                      - { name: Payment, kind: manyToOne, to: Payment, required: true }
                settlements:
                  - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                      amount: amount, total: total, paid: paid, pot: amount, order: date,
                      match: [Customer] }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "settle");
        List<Map<String, String>> keys = (List<Map<String, String>>) entityByName(entities(model), "Payment").get("groupingKeys");
        assertEquals(1, keys.size(), "the payment must track its settlement match column");
        assertEquals("Customer", keys.get(0)
                                     .get("key"));
        // The invoice side is matched by the RECORD's own column and never moves an allocation by
        // itself - only the payment's match value re-targets the settlement.
        assertNull(entityByName(entities(model), "Invoice").get("groupingKeys"));
    }

    /**
     * The two non-blocking guard outcomes. Both PERSIST the row and mark it instead of failing the
     * write: {@code task} stamps a boolean marker that the entity's process decision branches on (the
     * credit-limit shape - the order is accepted, then parked on a hold step), {@code reject} forces
     * the EntityStatus FK (the leave-request shape - the request is filed, already rejected).
     */
    @SuppressWarnings("unchecked")
    @Test
    void guardOutcomeTaskAndRejectEmitTheirMarkerAndStatusWrite() {
        String yaml = """
                name: sales
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: OrderStatus
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 40 }
                  - name: Order
                    fields:
                      - { name: id,           type: integer, primaryKey: true, generated: true }
                      - { name: amount,       type: decimal }
                      - { name: withinCredit, type: boolean }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                      - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
                    checks:
                      - kind: guard
                        aggregate: exposure
                        minimum: 0
                        outcome: task
                        marker: withinCredit
                        message: Over the credit limit
                  - name: Reservation
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: days, type: decimal }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                      - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
                    checks:
                      - kind: guard
                        aggregate: remaining
                        minimum: 0
                        outcome: reject
                        setStatus: 3
                        message: No allowance left
                  - name: Exposure
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: Allowance
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: remaining, type: decimal }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                aggregates:
                  - name: exposure
                    of: Order
                    op: sum
                    sum: amount
                    by: [Customer]
                    into: Exposure
                    field: total
                  - name: remaining
                    of: Reservation
                    op: sum
                    sum: days
                    by: [Customer]
                    into: Allowance
                    field: remaining
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "sales");

        Map<String, Object> taskGuard = ((List<Map<String, Object>>) entityByName(entities(model), "Order").get("checks")).get(0);
        assertEquals("task", taskGuard.get("outcome"));
        assertEquals("WithinCredit", taskGuard.get("marker"));
        assertNull(taskGuard.get("statusProperty"), "a task outcome must not carry a status write");

        Map<String, Object> rejectGuard = ((List<Map<String, Object>>) entityByName(entities(model), "Reservation").get("checks")).get(0);
        assertEquals("reject", rejectGuard.get("outcome"));
        assertEquals("Status", rejectGuard.get("statusProperty"));
        assertEquals("3", rejectGuard.get("statusValue"));
        assertNull(rejectGuard.get("marker"), "a reject outcome must not carry a marker");
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
    void historyEntityCarriesTheModelAttribute() {
        String yaml = """
                name: legal
                entities:
                  - name: Contract
                    audit: true
                    history: true
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                  - name: Note
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: text, type: string }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        List<Map<String, Object>> entities = entities(EdmIntentGenerator.buildModelJsonForTest(parsed, "legal"));

        assertEquals("true", entityByName(entities, "Contract").get("history"),
                "a historized entity should carry the EDM history attribute - the schema, DAO and UI templates all key on it");
        assertNull(entityByName(entities, "Note").get("history"), "an entity that did not ask for a history must not carry the attribute");
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

        // A PRIMARY calendar entity gets the standalone calendar page as an ADDITIONAL page (#6547): its
        // own layout stays (here MANAGE - no composition children of its own), so the create/edit form
        // and the table browse page it brings are all still generated.
        Map<String, Object> primary = entityByName(entities, "Meeting");
        assertEquals("MANAGE", primary.get("layoutType"));
        assertEquals("true", primary.get("calendarView"));
        assertEquals(null, primary.get("detailCalendar"));
    }

    @Test
    void documentMasterWithASlotsViewKeepsTheDocumentLayoutAndAddsThePicker() {
        // #6547, same rule as the calendar: a slot picker is how a booking is CREATED, the document page
        // is how it is worked with afterwards - so the picker rides alongside the layout instead of
        // replacing it, and slot-click lands on the document editor.
        String yaml = """
                name: clinic
                entities:
                  - name: Appointment
                    function: Document
                    view: slots
                    slots: { start: startsAt, open: "09:00", close: "17:00", step: 15 }
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: startsAt, type: timestamp, required: true }
                  - name: AppointmentItem
                    function: DocumentItem
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 100 }
                    relations:
                      - { name: Appointment, kind: manyToOne, to: Appointment, composition: true, required: true }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "clinic");
        Map<String, Object> appointment = entityByName(entities(model), "Appointment");
        assertEquals("MANAGE_DOCUMENT", appointment.get("layoutType"), "view: slots no longer overrides the document layout");
        assertEquals("true", appointment.get("slotsView"), "the picker rides alongside the layout as an additional page");
        assertEquals("StartsAt", appointment.get("slotStartProperty"));
        assertEquals("09:00", appointment.get("slotOpen"));
        assertEquals("17:00", appointment.get("slotClose"));
        assertEquals("15", appointment.get("slotStep"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void documentWhoseItemsChildIsACalendarRendersTheItemsPaneAsACalendar() {
        // #6482: the line-items child declaring `view: calendar` used to emit its calendar metadata and
        // then be filtered out of the document's secondary panels, so nothing rendered. The master now
        // carries documentItemsLayout: calendar, which is what makes the items PANE the calendar.
        String yaml = """
                name: booking
                entities:
                  - name: Booking
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: reference, type: string }
                  - name: BookingItem
                    view: calendar
                    calendar: { start: day, title: note, initialView: month }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: day, type: date, required: true }
                      - { name: note, type: string }
                    relations:
                      - { name: Booking, kind: manyToOne, to: Booking, composition: true, required: true }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "booking");
        List<Map<String, Object>> entities = entities(model);

        Map<String, Object> master = entityByName(entities, "Booking");
        assertEquals("MANAGE_DOCUMENT", master.get("layoutType"));
        assertEquals("BookingItem", master.get("documentItemsEntity"));
        assertEquals("calendar", master.get("documentItemsLayout"), "the items pane renders as a calendar");

        // The child stays an ordinary detail: its registration is what carries the calendar's own
        // configuration (start / title / view) to the document page at runtime.
        Map<String, Object> child = entityByName(entities, "BookingItem");
        assertEquals("MANAGE_DETAILS", child.get("layoutType"));
        assertEquals("true", child.get("detailCalendar"));
        assertEquals("Day", child.get("calendarStartProperty"));
        assertEquals("Note", child.get("calendarTitleProperty"));
        assertEquals("month", child.get("calendarInitialView"));
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

    @Test
    // `related:` emits a read-only register carrying the source's key, its back-reference and the
    // metadata its columns render from - and no URL, which belongs to the generation parameters.
    void relatedEmitsTheReverseOfTheIncomingAssociation() {
        String yaml = """
                name: timesheets
                entities:
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: string }
                    related:
                      - entity: EmployeeTimesheet
                        label: Employee Timesheets
                  - name: EmployeeTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: totalHours, type: decimal }
                      - { name: comment, type: string, major: false }
                    relations:
                      - { name: projectTimesheet, kind: manyToOne, to: ProjectTimesheet, required: true }
                      - { name: employee, kind: manyToOne, to: Employee }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "timesheets");
        Map<String, Object> project = entityByName(entities(model), "ProjectTimesheet");
        List<Map<String, Object>> registers = (List<Map<String, Object>>) project.get("relatedEntities");
        assertEquals(1, registers.size());
        Map<String, Object> register = registers.get(0);
        assertEquals("EmployeeTimesheet", register.get("entity"));
        assertEquals("Employee Timesheets", register.get("label"));
        assertEquals("EmployeeTimesheet", register.get("perspectiveName"));
        assertEquals("TIMESHEETS_EMPLOYEE_TIMESHEET", register.get("dataName"));
        assertEquals("Id", register.get("primaryKey"));
        // The single relation pointing back here needs no via:, and it is what the register filters by.
        assertEquals("ProjectTimesheet", register.get("fkProperty"));
        // A same-model source is resolved from this model, so nothing is read from another one.
        assertNull(register.get("referencedModel"));

        // The default columns are the source's own list columns: not its generated identifier, not the
        // foreign key back to the record the register already belongs to, not a `major: false` field.
        List<Map<String, Object>> columns = (List<Map<String, Object>>) register.get("properties");
        List<String> names = columns.stream()
                                    .map(c -> String.valueOf(c.get("name")))
                                    .toList();
        assertEquals(List.of("Number", "TotalHours", "Employee"), names);
        // A relation column carries the lookup facts its label resolves through - the URL itself is
        // built by the generation parameters, never here.
        Map<String, Object> employee = columns.get(2);
        assertEquals("Employee", employee.get("relationshipEntityName"));
        assertEquals("Employee", employee.get("relationshipEntityPerspectiveName"));
        assertEquals("Id", employee.get("widgetDropDownKey"));
        assertNull(employee.get("apiPath"), "a register entry carries facts, never a template-owned URL");
    }

    @Test
    // `show:` picks the register's columns and their order; an unnamed register is titled by the
    // pluralized entity name.
    void relatedShowSelectsAndOrdersTheColumns() {
        String yaml = """
                name: sales
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    related:
                      - entity: Invoice
                        show: [total, number]
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: total, type: decimal }
                    relations:
                      - { name: customer, kind: manyToOne, to: Customer }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "sales");
        Map<String, Object> register =
                ((List<Map<String, Object>>) entityByName(entities(model), "Customer").get("relatedEntities")).get(0);
        assertEquals("Invoices", register.get("label"), "an unnamed register is titled by the pluralized entity");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) register.get("properties");
        assertEquals(List.of("Total", "Number"), columns.stream()
                                                        .map(c -> String.valueOf(c.get("name")))
                                                        .toList());
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

    /**
     * The opening {@code <mxCell ...>} tag that wraps the {@code <Entity name="<name>">} value cell in
     * the emitted EDM diagram - the tag carrying the {@code style} attribute the modeler colors the
     * entity by.
     */
    private static String mxCellTagFor(String edm, String name) {
        int entityIdx = edm.indexOf("<Entity name=\"" + name + "\"");
        assertTrue(entityIdx >= 0, "the diagram cell for entity [" + name + "] must be present");
        int cellStart = edm.lastIndexOf("<mxCell", entityIdx);
        return edm.substring(cellStart, edm.indexOf('>', cellStart) + 1);
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

    /**
     * {@code visibleTo:} is emitted as the model's own per-property read AND write roles - the pair the
     * generated controllers already enforce - so the allow-list reaches the runtime through the same
     * attributes a hand-modeled EDM would carry. Both sides get the same comma-separated list: a caller
     * who may not see the value must not be able to set it either.
     */
    @Test
    void visibleToBecomesThePropertyReadAndWriteRoles() {
        String yaml = """
                name: hr
                permissions:
                  - { role: Payroll }
                  - { role: Administrator }
                entities:
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 200 }
                      - { name: dailyRate, type: decimal, visibleTo: [Payroll, Administrator] }
                """;
        Map<String, Object> employee =
                entityByName(entities(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "hr")), "Employee");

        Map<String, Object> rate = propertyByName(employee, "DailyRate");
        assertEquals("Payroll,Administrator", rate.get("roleRead"));
        assertEquals("Payroll,Administrator", rate.get("roleWrite"));
        assertNull(propertyByName(employee, "Name").get("roleRead"));
        assertNull(propertyByName(employee, "Name").get("roleWrite"));
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
                name: employees-bg
                uses:
                  - { model: employees }
                entities:
                  - name: EmployeeBg
                    extends: { model: employees, entity: Employee }
                    fields:
                      - { name: egn, type: string, length: 10 }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "employees-bg");
        Map<String, Object> ext = entityByName(entities(model), "EmployeeBg");

        // Marked EXTENSION with the base reference the model-to-code merge keys on; owns no UI.
        assertEquals("EXTENSION", ext.get("type"));
        assertEquals("employees", ext.get("extensionReferencedModel"));
        assertEquals("Employee", ext.get("extensionReferencedEntity"));
        assertEquals("", ext.get("layoutType"));
        assertEquals("", ext.get("perspectiveName"));
        // Its contributed field is emitted as a normal property (folded into the base table later).
        assertEquals("VARCHAR", propertyByName(ext, "Egn").get("dataType"));
    }

    /**
     * The conditional valueFrom form (#6358): the classifier by-path, the header-start coordinates, the
     * hop entity (whose record is fetched), the cases map as JSON with PascalCased properties, and the
     * no-match default - with NO plain widgetDependsOnValueFrom emitted.
     */
    @org.junit.jupiter.api.Test
    void conditionalDependsOnEmitsClassifierAttributes() {
        String yaml = """
                name: shop
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: priceLevel, type: integer }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: wholesalePrice, type: decimal }
                      - { name: retailPrice, type: decimal }
                  - name: SalesOrder
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: date, type: date }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: SalesOrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - name: price
                        type: decimal
                        dependsOn:
                          relation: Product
                          valueFrom:
                            by: SalesOrder.Customer.priceLevel
                            cases: { 1: wholesalePrice, 2: retailPrice }
                            default: retailPrice
                    relations:
                      - { name: SalesOrder, kind: manyToOne, to: SalesOrder, composition: true, required: true }
                      - { name: Product, kind: manyToOne, to: Product }
                    """;
        java.util.Map<String, Object> model =
                EdmIntentGenerator.buildModelJsonForTest(org.eclipse.dirigible.components.intent.parser.IntentParser.parse(yaml), "shop");
        java.util.Map<String, Object> price = propertyByName(entityByName(entities(model), "SalesOrderItem"), "Price");
        assertEquals("Product", price.get("widgetDependsOnProperty"));
        assertEquals("SalesOrder.Customer.PriceLevel", price.get("widgetDependsOnValueBy"));
        assertEquals("true", price.get("widgetDependsOnValueByHeader"));
        assertEquals("SalesOrder", price.get("widgetDependsOnValueByHeaderEntity"));
        assertEquals("Customer", price.get("widgetDependsOnValueByEntity"));
        assertEquals("{\"1\":\"WholesalePrice\",\"2\":\"RetailPrice\"}", price.get("widgetDependsOnValueCases"));
        assertEquals("RetailPrice", price.get("widgetDependsOnValueDefault"));
        assertNull(price.get("widgetDependsOnValueFrom"), "the conditional form replaces the fixed valueFrom");
    }

    /**
     * Reproducibility (#6423): generated model files are committed, so the projection reference must
     * depend only on the sources - generating the same intent in a differently-named IDE workspace must
     * not rewrite it.
     */
    @org.junit.jupiter.api.Test
    void projectionPathIsIndependentOfTheGeneratingWorkspace() {
        String yaml = """
                name: invoices
                uses:
                  - { model: companies, project: companies }
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Company, kind: manyToOne, to: Company, model: companies }
                    """;
        org.eclipse.dirigible.components.intent.model.IntentModel intent =
                org.eclipse.dirigible.components.intent.parser.IntentParser.parse(yaml);
        java.util.Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(intent, "invoices");
        assertEquals("/companies/companies.model", entityByName(entities(model), "Company").get("projectionReferencedModel"),
                "the path must carry no environment detail - only the owner project and its model");
    }

    /** A field's input-format regex (#6336) becomes the property's widgetPattern, XML-escaped. */
    @org.junit.jupiter.api.Test
    void fieldPatternEmitsWidgetPattern() {
        String yaml = """
                name: banking
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: iban, type: string, length: 34, pattern: '^[A-Z]{2}<[0-9]{2}$' }
                      - { name: label, type: string }
                    """;
        java.util.Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(
                org.eclipse.dirigible.components.intent.parser.IntentParser.parse(yaml), "banking");
        java.util.Map<String, Object> account = entityByName(entities(model), "Account");
        assertEquals("^[A-Z]{2}<[0-9]{2}$", propertyByName(account, "Iban").get("widgetPattern"));
        assertNull(propertyByName(account, "Label").get("widgetPattern"), "a field without a pattern must not carry the attribute");
    }

    /**
     * The header-mediated trigger (#6358): the trigger property is resolved on the DOCUMENT header, so
     * the runtime watches the header form rather than a sibling of the row.
     */
    @org.junit.jupiter.api.Test
    void headerMediatedDependsOnEmitsHeaderTriggerAttributes() {
        String yaml = """
                name: shop
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: standardDiscount, type: decimal }
                  - name: SalesOrder
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: date, type: date }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: SalesOrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: discount, type: decimal, dependsOn: { relation: SalesOrder.Customer, valueFrom: standardDiscount } }
                    relations:
                      - { name: SalesOrder, kind: manyToOne, to: SalesOrder, composition: true, required: true }
                    """;
        java.util.Map<String, Object> model =
                EdmIntentGenerator.buildModelJsonForTest(org.eclipse.dirigible.components.intent.parser.IntentParser.parse(yaml), "shop");
        java.util.Map<String, Object> discount = propertyByName(entityByName(entities(model), "SalesOrderItem"), "Discount");
        // The trigger is the HEADER's Customer relation, not anything on the row.
        assertEquals("Customer", discount.get("widgetDependsOnProperty"));
        assertEquals("Customer", discount.get("widgetDependsOnEntity"));
        assertEquals("true", discount.get("widgetDependsOnHeader"));
        assertEquals("SalesOrder", discount.get("widgetDependsOnHeaderEntity"));
        assertEquals("StandardDiscount", discount.get("widgetDependsOnValueFrom"));
        assertNull(discount.get("widgetDependsOnFilterBy"), "a field has no option list to filter");
    }

    /**
     * `format: email` (#6463) selects the EMAIL widget - the only way to reach a type="email" control
     * from the DSL - and supplies the canonical regex through the SAME widgetPattern attribute a raw
     * pattern uses, so the server-side 400 and the client-side feedback come from one path, not two.
     */
    @org.junit.jupiter.api.Test
    void fieldFormatEmailEmitsTheEmailWidgetAndTheCanonicalPattern() {
        String yaml = """
                name: crm
                entities:
                  - name: Contact
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string, length: 320, format: email }
                      - { name: note, type: string }
                    """;
        java.util.Map<String, Object> model =
                EdmIntentGenerator.buildModelJsonForTest(org.eclipse.dirigible.components.intent.parser.IntentParser.parse(yaml), "crm");
        java.util.Map<String, Object> contact = entityByName(entities(model), "Contact");
        java.util.Map<String, Object> email = propertyByName(contact, "Email");
        assertEquals("EMAIL", email.get("widgetType"));
        String pattern = (String) email.get("widgetPattern");
        assertNotNull(pattern, "format: email must supply the canonical regex via widgetPattern");
        // The emitted regex must be valid where it is USED: a Java regex in the generated controller's
        // matches(), and an HTML pattern in the form input. Compiling it here covers the Java half.
        java.util.regex.Pattern compiled = java.util.regex.Pattern.compile(pattern);
        assertTrue(compiled.matcher("a.b@example.com")
                           .matches(),
                "a normal address must match: " + pattern);
        assertTrue(!compiled.matcher("not-an-email")
                            .matches(),
                "a bare word must not match: " + pattern);
        assertTrue(!compiled.matcher("a b@example.com")
                            .matches(),
                "whitespace must not match: " + pattern);
        assertTrue(!compiled.matcher("a@b")
                            .matches(),
                "a dotless domain must not match: " + pattern);
        // An untouched string field stays a plain textbox with no pattern.
        assertEquals("TEXTBOX", propertyByName(contact, "Note").get("widgetType"));
        assertNull(propertyByName(contact, "Note").get("widgetPattern"));
    }

    /**
     * A child collection that does not freeze with its master carries the marker the detail
     * registration reads; every other entity keeps byte-identical output (the attribute is emitted only
     * when declared false).
     */
    @Test
    void locksWithMasterFalseMarksTheChildThatOutlivesItsMastersLock() {
        String yaml = """
                name: sales
                entities:
                  - name: Invoice
                    function: Document
                    immutableWhen: "Status == 3"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - name: InvoiceStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 50 }
                  - name: InvoiceItem
                    function: DocumentItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                  - name: InvoiceAllocation
                    locksWithMaster: false
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                """;
        IntentModel parsed = IntentParser.parse(yaml);
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(parsed, "sales");
        List<Map<String, Object>> entities = entities(model);

        assertEquals("false", entityByName(entities, "InvoiceAllocation").get("locksWithMaster"));
        // The document's own line items keep freezing with it - that is what immutability is for.
        assertNull(entityByName(entities, "InvoiceItem").get("locksWithMaster"));
        assertNull(entityByName(entities, "Invoice").get("locksWithMaster"));
    }

    /**
     * A trigger-target entity carries TWO process columns, and they answer different questions.
     *
     * <p>
     * {@code ProcessId} is the most recent instance - what the UI correlates the record's actionable
     * tasks on, and all there was. Reading it as "has this process already run for this record" made
     * every process indistinguishable from every other, so a record an earlier flow had stamped
     * silently skipped its follow-up - the composition an {@code onTransition} trigger invites. The
     * per-process answer, and the instance each process was started with (what a wait or an abort has
     * to correlate on), lives in {@code ProcessIds}.
     */
    @SuppressWarnings("unchecked")
    @Test
    void aTriggerTargetCarriesThePerProcessStampBesideProcessId() {
        String yaml = """
                name: fines
                entities:
                  - name: Fine
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                  - name: Driver
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                processes:
                  - name: Identify
                    trigger: { onCreate: Fine }
                    steps:
                      - { name: done, kind: end }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "fines");
        List<Map<String, Object>> entities = entities(model);

        Map<String, Object> fine = entityByName(entities, "Fine");
        Map<String, Object> processId = propertyByName(fine, "ProcessId");
        Map<String, Object> stamps = propertyByName(fine, "ProcessIds");
        assertNotNull(processId, "the trigger target keeps its ProcessId back-reference");
        assertNotNull(stamps, "the trigger target must also carry the per-process stamps");
        assertEquals("VARCHAR", stamps.get("dataType"));
        assertEquals("FINE_PROCESS_IDS", stamps.get("dataName"));
        assertEquals("true", stamps.get("dataNullable"));
        // Several ids, so wider than the single one - and system-managed the same way: never editable,
        // never a major column, and excluded from the generated forms and lists.
        assertEquals("1000", stamps.get("dataLength"));
        assertEquals("true", stamps.get("isReadOnlyProperty"));
        assertEquals("false", stamps.get("widgetIsMajor"));
        assertEquals("NONE", stamps.get("auditType"));

        // Only the entity a process starts on: nothing else pays for the column.
        Map<String, Object> driver = entityByName(entities, "Driver");
        List<String> driverProperties = ((List<Map<String, Object>>) driver.get("properties")).stream()
                                                                                              .map(p -> String.valueOf(p.get("name")))
                                                                                              .toList();
        assertFalse(driverProperties.contains("ProcessIds"), "an entity no process triggers on must not carry the stamps");
        assertFalse(driverProperties.contains("ProcessId"));
    }

    /**
     * A capacity roll-up that drives a status (#7016) needs somewhere to keep the status it displaced,
     * so a sum back at zero can restore it: a hidden, read-only integer column on the PARENT, named
     * after the status relation. The child and the nomenclature carry nothing.
     */
    @SuppressWarnings("unchecked")
    @Test
    void aStatusRollupParentCarriesTheDisplacedStatusColumn() {
        String yaml = """
                name: billing
                entities:
                  - name: Bill
                    fields:
                      - { name: id,      type: integer, primaryKey: true, generated: true }
                      - { name: total,   type: decimal, precision: 18, scale: 2 }
                      - { name: paid,    type: decimal, precision: 18, scale: 2 }
                      - { name: balance, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Status, kind: manyToOne, to: BillStatus }
                  - name: BillStatus
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string,  required: true, length: 50 }
                  - name: BillPayment
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                    relations:
                      - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }
                rollups:
                  - { name: billPaid, entity: BillPayment, via: Bill, field: paid, op: sum, of: amount,
                      capacity: total, balance: balance, status: Status, statusWhenFull: 2, statusWhenPartial: 1 }
                """;
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "billing");
        List<Map<String, Object>> entities = entities(model);

        Map<String, Object> displaced = propertyByName(entityByName(entities, "Bill"), "DisplacedStatus");
        assertNotNull(displaced, "the parent of a status roll-up must remember the status the roll-up displaced");
        assertEquals("INTEGER", displaced.get("dataType"));
        assertEquals("BILL_DISPLACED_STATUS", displaced.get("dataName"));
        assertEquals("true", displaced.get("dataNullable"));
        // Read-only so a full-row form save preserves it, never a major column, and hidden outright: it is
        // bookkeeping only the roll-up handler reads.
        assertEquals("true", displaced.get("isReadOnlyProperty"));
        assertEquals("true", displaced.get("isHiddenProperty"));
        assertEquals("false", displaced.get("widgetIsMajor"));

        for (String other : List.of("BillPayment", "BillStatus")) {
            List<String> names = ((List<Map<String, Object>>) entityByName(entities, other).get("properties")).stream()
                                                                                                              .map(p -> String.valueOf(
                                                                                                                      p.get("name")))
                                                                                                              .toList();
            assertFalse(names.contains("DisplacedStatus"), other + " is not the parent of the roll-up");
        }
    }
}
