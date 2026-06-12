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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.intent.domain.Intent;
import org.eclipse.dirigible.components.intent.service.IntentService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the engine-intent module: drops a comprehensive Orders {@code app.intent}
 * into the registry, triggers reconciliation, and asserts the full intent -> model-file pipeline.
 *
 * <p>
 * The intent declares five entities (Country / Customer / Product / Order / OrderItem) with
 * relations in both directions (including Customer -> Country reference data shaped after
 * {@code codbex/codbex-countries}), an OrderApproval process with every step kind (userTask /
 * decision / serviceTask / end), two forms bound to entities, a report, three permission roles, and
 * a seed block that preloads three rows into {@code COUNTRY} a-la
 * {@code codbex/codbex-countries-data}. Every {@code IntentModel} field defined today is exercised.
 * The test asserts:
 * <ul>
 * <li>the {@link Intent} JPA artefact is persisted via {@link IntentService}</li>
 * <li>the {@code /services/ide/intent/*} REST endpoints list / fetch / source / regenerate the
 * project</li>
 * <li>{@code EdmIntentGenerator} produces a {@code gen/orders.edm} + {@code gen/orders.model} pair
 * containing every entity (Country included), every property, and every relation (Customer ->
 * Country and Order -> Customer are both wired as references rather than free-text strings)</li>
 * <li>{@code BpmnIntentGenerator} produces a {@code gen/OrderApproval.bpmn} with the right BPMN
 * elements for each step kind and the conditioned outgoing flow on the decision</li>
 * <li>{@code FormIntentGenerator} produces a {@code gen/<form>.form} per form with controls typed
 * from the bound entity's fields and action buttons</li>
 * <li>{@code ReportIntentGenerator} parses {@code aggregate(field)} measure expressions into
 * columns with the right aggregate</li>
 * <li>{@code PermissionIntentGenerator} emits the deduped {@code .roles} file</li>
 * <li>{@code CsvimIntentGenerator} emits {@code gen/countries.csvim} + {@code gen/countries.csv}
 * with the rows in the entity's declared field order</li>
 * </ul>
 *
 * <p>
 * HTTP-only - no Selenide, no Chrome. Runs fast enough to be part of the default IT suite.
 */
class IntentEngineIT extends IntegrationTest {

    private static final String PROJECT = "orders";
    private static final String INTENT_LOCATION = "/" + PROJECT + "/app.intent";
    private static final String REGISTRY_INTENT = IRepositoryStructure.PATH_REGISTRY_PUBLIC + INTENT_LOCATION;
    private static final String REGISTRY_GEN = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/gen";

    private static final String INTENT_YAML = """
            name: orders
            description: Order management with approval workflow
            version: 1

            entities:
              - name: Country
                description: ISO 3166-1 country reference data (shape borrowed from codbex/codbex-countries)
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: name,    type: string,  required: true, length: 100 }
                  - { name: code2,   type: string,  length: 2 }
                  - { name: code3,   type: string,  length: 3 }
                  - { name: numeric, type: string,  length: 3 }

              - name: Customer
                description: Buyer account (Partner-style profile, see codbex/codbex-partners)
                fields:
                  - { name: id,     type: uuid,    primaryKey: true, generated: true }
                  - { name: name,   type: string,  required: true, length: 200 }
                  - { name: email,  type: string,  length: 200 }
                  - { name: active, type: boolean, defaultValue: "true" }
                relations:
                  - { name: country, kind: manyToOne, to: Country }
                  - { name: orders,  kind: oneToMany, to: Order }

              - name: Product
                description: Product catalog entry
                fields:
                  - { name: id,          type: uuid,    primaryKey: true, generated: true }
                  - { name: name,        type: string,  required: true, length: 200 }
                  - { name: description, type: text }
                  - { name: price,       type: decimal, required: true }
                  - { name: inStock,     type: boolean }

              - name: Order
                description: Customer order
                fields:
                  - { name: id,        type: uuid,    primaryKey: true, generated: true }
                  - { name: orderDate, type: date,    required: true }
                  - { name: status,    type: string,  length: 32 }
                  - { name: total,     type: decimal }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer, required: true }
                  - { name: items,    kind: oneToMany, to: OrderItem }

              - name: OrderItem
                description: Line item in an order
                fields:
                  - { name: id,        type: uuid,    primaryKey: true, generated: true }
                  - { name: quantity,  type: integer, required: true }
                  - { name: lineTotal, type: decimal }
                relations:
                  - { name: order,   kind: manyToOne, to: Order,   required: true }
                  - { name: product, kind: manyToOne, to: Product, required: true }

            processes:
              - name: OrderApproval
                description: Manager approval for orders above 10000
                trigger: { onCreate: Order }
                steps:
                  - name: managerReview
                    kind: userTask
                    args: { assignee: order-manager, form: ApproveOrder }
                  - name: bigOrder
                    kind: decision
                    args: { if: "amount > 10000", then: cfoReview }
                  - name: cfoReview
                    kind: userTask
                    args: { assignee: cfo, form: ApproveOrder }
                  - name: notifyCustomer
                    kind: serviceTask
                    args: { call: "custom/notify-customer.ts" }
                  - name: done
                    kind: end

            forms:
              - name: ApproveOrder
                forEntity: Order
                description: Approve or reject an order
                fields: [orderDate, status, total]
                actions: [approve, reject]

              - name: NewCustomer
                forEntity: Customer
                description: Create a new customer
                fields: [name, email, country, active]
                actions: [save, cancel]

            reports:
              - name: OrdersByCustomer
                source: Order
                dimensions: [customer.country]
                measures: ["count(*)", "sum(total)"]

            permissions:
              - { role: Sales,         description: Sales staff,    can: [Customer:read, Customer:write, Order:read, Order:create] }
              - { role: Manager,       description: Sales manager,  can: [Order:approve, Order:read] }
              - { role: Administrator, description: System admin,   can: [Customer:write, Product:write, Order:write] }

            seeds:
              - name: countries
                entity: Country
                description: Sample ISO 3166-1 rows (shape borrowed from codbex/codbex-countries-data)
                rows:
                  - { id: 1, name: Afghanistan, code2: AF, code3: AFG, numeric: "004" }
                  - { id: 2, name: Albania,     code2: AL, code3: ALB, numeric: "008" }
                  - { id: 3, name: Algeria,     code2: DZ, code3: DZA, numeric: "012" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private IntentService intentService;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void full_intent_pipeline_generates_all_model_files() {
        repository.createResource(REGISTRY_INTENT, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        synchronizationProcessor.forceProcessSynchronizers();

        assertIntentIsPersisted();
        assertEdmAndModelGenerated();
        assertBpmnGenerated();
        assertFormGenerated();
        assertReportGenerated();
        assertRolesGenerated();
        assertSeedsGenerated();
        assertRestEndpoints();
    }

    @Test
    void intent_removal_cleans_artefact() {
        repository.createResource(REGISTRY_INTENT, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        synchronizationProcessor.forceProcessSynchronizers();
        assertTrue(intentService.findByLocation(INTENT_LOCATION)
                                .stream()
                                .findFirst()
                                .isPresent(),
                "intent should be persisted after publish");

        repository.removeResource(REGISTRY_INTENT);
        synchronizationProcessor.forceProcessSynchronizers();

        assertTrue(intentService.findByLocation(INTENT_LOCATION)
                                .isEmpty(),
                "intent should be cleaned up after the .intent file is removed and the synchronizer runs");
    }

    private void assertIntentIsPersisted() {
        List<Intent> matches = intentService.findByLocation(INTENT_LOCATION);
        assertTrue(!matches.isEmpty(), "intent should be persisted at location " + INTENT_LOCATION);
        Intent intent = matches.get(0);
        assertNotNull(intent.getContent(), "intent payload should be persisted");
        assertTrue(intent.getContent()
                         .contains("entities:"),
                "intent content should contain the YAML body, not just an empty record");
    }

    private void assertEdmAndModelGenerated() {
        IResource edm = repository.getResource(REGISTRY_GEN + "/orders.edm");
        assertTrue(edm.exists(), "gen/orders.edm should be generated");
        String edmXml = new String(edm.getContent(), StandardCharsets.UTF_8);
        for (String entityName : List.of("Country", "Customer", "Product", "Order", "OrderItem")) {
            assertTrue(edmXml.contains("name=\"" + entityName + "\""), "EDM should declare entity [" + entityName + "]");
        }
        assertTrue(edmXml.contains("dataPrimaryKey=\"true\""), "EDM should mark at least one property as primary key");
        assertTrue(edmXml.contains("widgetType=\"NUMBER\""), "EDM should map decimal/integer fields to a NUMBER widget");
        assertTrue(edmXml.contains("widgetType=\"DATE\""), "EDM should map the date field to a DATE widget");
        assertTrue(edmXml.contains("widgetType=\"CHECKBOX\""), "EDM should map the boolean field to a CHECKBOX widget");
        assertTrue(edmXml.contains("widgetType=\"TEXTAREA\""), "EDM should map the text field to a TEXTAREA widget");
        assertTrue(edmXml.contains("type=\"PRIMARY\""), "EDM should declare at least one PRIMARY entity");
        assertTrue(edmXml.contains("type=\"DEPENDENT\""),
                "EDM should mark Customer/Order/OrderItem as DEPENDENT through the manyToOne edges");
        assertTrue(edmXml.contains("referenced=\"Country\""), "EDM should carry the Customer->Country reference relation");
        assertTrue(edmXml.contains("referenced=\"Customer\""), "EDM should carry the Order->Customer relation");
        assertTrue(edmXml.contains("referenced=\"Order\""), "EDM should carry the OrderItem->Order relation");
        assertTrue(edmXml.contains("referenced=\"Product\""), "EDM should carry the OrderItem->Product relation");
        assertTrue(edmXml.contains("dataName=\"CUSTOMER_COUNTRY\""),
                "Customer->Country FK should materialize as a CUSTOMER_COUNTRY column on Customer");

        IResource modelJson = repository.getResource(REGISTRY_GEN + "/orders.model");
        assertTrue(modelJson.exists(), "gen/orders.model should be generated");
        String modelBody = new String(modelJson.getContent(), StandardCharsets.UTF_8);
        assertTrue(modelBody.contains("\"entities\""), "model JSON should have an entities array");
        assertTrue(modelBody.contains("\"OrderItem\""), "model JSON should mention OrderItem");
    }

    private void assertBpmnGenerated() {
        IResource bpmn = repository.getResource(REGISTRY_GEN + "/OrderApproval.bpmn");
        assertTrue(bpmn.exists(), "gen/OrderApproval.bpmn should be generated");
        String body = new String(bpmn.getContent(), StandardCharsets.UTF_8);
        assertTrue(body.contains("<startEvent id=\"start\""), "BPMN should declare a start event");
        assertTrue(body.contains("<endEvent id=\"end\""), "BPMN should declare an end event");
        assertTrue(body.contains("<userTask id=\"managerReview\""), "BPMN should map managerReview to a userTask");
        assertTrue(body.contains("flowable:candidateGroups=\"order-manager\""), "BPMN should carry the userTask candidateGroups");
        assertTrue(body.contains("flowable:formKey=\"ApproveOrder\""), "BPMN should carry the userTask form key as the bare form name");
        assertTrue(body.contains("<exclusiveGateway id=\"bigOrder\""), "BPMN should map the decision step to an exclusiveGateway");
        assertTrue(body.contains("<serviceTask id=\"notifyCustomer\""), "BPMN should map serviceTask to a serviceTask element");
        assertTrue(body.contains("delegateExpression=\"${JSTask}\""), "BPMN should use the Flowable delegate expression for service tasks");
        assertTrue(body.contains("custom/notify-customer.ts"), "BPMN should reference the handler the intent declared verbatim");
        assertTrue(body.contains("conditionExpression"), "BPMN should emit a conditionExpression on the conditioned outgoing flow");
        assertTrue(body.contains("${amount > 10000}"), "BPMN should embed the decision's if expression");
    }

    private void assertFormGenerated() {
        IResource form = repository.getResource(REGISTRY_GEN + "/ApproveOrder.form");
        assertTrue(form.exists(), "gen/ApproveOrder.form should be generated");
        String body = new String(form.getContent(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"controlId\":\"header\""), "form should start with a header control");
        assertTrue(body.contains("\"label\":\"Order Date\""), "form should humanize the orderDate field name to 'Order Date'");
        assertTrue(body.contains("\"controlId\":\"input-date\""), "form should pick input-date for the orderDate field");
        assertTrue(body.contains("\"controlId\":\"input-number\""), "form should pick input-number for the total decimal field");
        assertTrue(body.contains("\"type\":\"positive\""), "form should mark the approve button as positive");
        assertTrue(body.contains("\"type\":\"negative\""), "form should mark the reject button as negative");
        assertTrue(body.contains("onApproveClicked"), "form code should declare the approve handler stub");

        IResource customerForm = repository.getResource(REGISTRY_GEN + "/NewCustomer.form");
        assertTrue(customerForm.exists(), "gen/NewCustomer.form should be generated");
        String customerBody = new String(customerForm.getContent(), StandardCharsets.UTF_8);
        assertTrue(customerBody.contains("\"controlId\":\"input-checkbox\""),
                "customer form should map active:boolean to an input-checkbox");
    }

    private void assertReportGenerated() {
        IResource report = repository.getResource(REGISTRY_GEN + "/OrdersByCustomer.report");
        assertTrue(report.exists(), "gen/OrdersByCustomer.report should be generated");
        String body = new String(report.getContent(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"alias\":\"OrdersByCustomer\""), "report should carry the intent name as alias");
        assertTrue(body.contains("\"baseTable\":\"ORDER\""), "report should map the source entity name to its upper-snake table name");
        assertTrue(body.contains("\"aggregate\":\"NONE\""), "dimensions should be emitted with aggregate NONE");
        assertTrue(body.contains("\"aggregate\":\"COUNT\""), "count(*) should be parsed into an aggregate COUNT column");
        assertTrue(body.contains("\"aggregate\":\"SUM\""), "sum(total) should be parsed into an aggregate SUM column");
        assertTrue(body.contains("\"name\":\"customer.country\""),
                "dotted dimension paths should be preserved verbatim in the column name");
        assertTrue(body.contains("\"name\":\"total\""), "sum(total) measure should resolve to a column whose name is 'total'");
    }

    private void assertSeedsGenerated() {
        IResource csvim = repository.getResource(REGISTRY_GEN + "/countries.csvim");
        assertTrue(csvim.exists(), "gen/countries.csvim should be generated");
        String csvimBody = new String(csvim.getContent(), StandardCharsets.UTF_8);
        assertTrue(csvimBody.contains("\"table\":\"COUNTRY\""), "csvim should declare the COUNTRY table");
        assertTrue(csvimBody.contains("\"schema\":\"PUBLIC\""), "csvim should default the schema to PUBLIC");
        assertTrue(csvimBody.contains("\"file\":\"/countries.csv\""), "csvim should point at the sibling csv");
        assertTrue(csvimBody.contains("\"header\":true"), "csvim should declare a header row");
        assertTrue(csvimBody.contains("\"useHeaderNames\":true"), "csvim should use header names for column mapping");

        IResource csv = repository.getResource(REGISTRY_GEN + "/countries.csv");
        assertTrue(csv.exists(), "gen/countries.csv should be generated");
        String csvBody = new String(csv.getContent(), StandardCharsets.UTF_8);
        assertTrue(csvBody.startsWith("COUNTRY_ID,COUNTRY_NAME,COUNTRY_CODE2,COUNTRY_CODE3,COUNTRY_NUMERIC"),
                "csv header should carry the upper-snake column names in entity-field order");
        assertTrue(csvBody.contains("1,Afghanistan,AF,AFG,004"), "csv should include the Afghanistan row");
        assertTrue(csvBody.contains("2,Albania,AL,ALB,008"), "csv should include the Albania row");
        assertTrue(csvBody.contains("3,Algeria,DZ,DZA,012"), "csv should include the Algeria row");
    }

    private void assertRolesGenerated() {
        IResource roles = repository.getResource(REGISTRY_GEN + "/orders.roles");
        assertTrue(roles.exists(), "gen/orders.roles should be generated");
        String body = new String(roles.getContent(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"name\":\"Sales\""), "Sales role should be present");
        assertTrue(body.contains("\"name\":\"Manager\""), "Manager role should be present");
        assertTrue(body.contains("\"name\":\"Administrator\""), "Administrator role should be present");
        assertTrue(body.contains("\"description\":\"Sales staff\""), "Role descriptions should be carried through");
    }

    private void assertRestEndpoints() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/ide/intent/projects")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasItem(PROJECT)));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/ide/intent/projects/" + PROJECT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("entities", hasSize(greaterThanOrEqualTo(5)))
                                                 .body("entities.name", hasItem("Country"))
                                                 .body("entities.name", hasItem("Customer"))
                                                 .body("entities.name", hasItem("OrderItem"))
                                                 .body("processes", hasSize(1))
                                                 .body("processes[0].name", equalTo("OrderApproval"))
                                                 .body("processes[0].steps", hasSize(5))
                                                 .body("forms", hasSize(2))
                                                 .body("reports", hasSize(1))
                                                 .body("permissions", hasSize(3))
                                                 .body("seeds", hasSize(1))
                                                 .body("seeds[0].entity", equalTo("Country"))
                                                 .body("seeds[0].rows", hasSize(3)));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/ide/intent/projects/" + PROJECT + "/source")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("name: orders"))
                                                 .body(containsString("OrderApproval")));

        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/intent/projects/" + PROJECT + "/regenerate")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("project", equalTo(PROJECT))
                                                 .body("status", equalTo("regenerated")));
    }

    @AfterEach
    void removeIntentFromRegistry() {
        if (repository.hasResource(REGISTRY_INTENT)) {
            repository.removeResource(REGISTRY_INTENT);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }
}
