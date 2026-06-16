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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the intent editor services: {@code POST /services/ide/intent/parse} (the
 * editor's live diagram + validation feed) and {@code POST /services/ide/intent/generate} (model
 * files generated into the developer's workspace project, with the stale-output scrub).
 *
 * <p>
 * The intent is an authoring artifact like the {@code .edm} - there is no synchronizer and nothing
 * here touches the registry. Generation is exercised against the {@code admin} user's default
 * workspace, exactly as the editor's Generate button does it. HTTP-only - no Selenide, no Chrome,
 * no synchronization cycles.
 */
class IntentEngineIT extends IntegrationTest {

    private static final String PROJECT = "intent-test";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String PARSE_URL = "/services/ide/intent/parse";
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";

    private static final String INTENT_YAML = """
            name: orders
            description: Order management with approval workflow
            version: 1

            entities:
              - name: Country
                description: ISO 3166-1 country reference data
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: name,    type: string,  required: true, length: 100 }
                  - { name: code2,   type: string,  length: 2 }

              - name: Customer
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string,  required: true, length: 200 }
                  - { name: active, type: boolean, defaultValue: "true" }
                relations:
                  - { name: country, kind: manyToOne, to: Country }
                  - { name: orders,  kind: oneToMany, to: Order }

              - name: Order
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: orderDate, type: date,    required: true }
                  - { name: total,     type: decimal }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
                  - { name: items,    kind: oneToMany, to: OrderItem }

              - name: OrderItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: integer, required: true }
                relations:
                  - { name: order, kind: manyToOne, to: Order, composition: true }

            processes:
              - name: OrderApproval
                trigger: { onCreate: Order }
                steps:
                  - name: managerReview
                    kind: userTask
                    args: { assignee: order-manager, form: ApproveOrder }
                  - name: bigOrder
                    kind: decision
                    args: { if: "amount > 10000", then: cfoReview, else: notifyCustomer }
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
                fields: [orderDate, total]
                actions: [approve, reject]

            reports:
              - name: OrdersByCustomer
                source: Order
                dimensions: [customer]
                measures: ["count(*)", "sum(total)"]
              - name: BigOrderItems
                source: OrderItem
                description: Order items with quantity over one, with their order date
                dimensions: [order.orderDate, quantity]
                filter: "quantity > 1"

            permissions:
              - { role: Sales,   description: Sales staff,   can: [Customer:read, Order:create] }
              - { role: Manager, description: Sales manager, can: [Order:approve] }

            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 1, name: Afghanistan, code2: AF }
                  - { id: 2, name: Albania,     code2: AL }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void parse_returns_the_full_model() {
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(INTENT_YAML)
                                                 .when()
                                                 .post(PARSE_URL)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("name", equalTo("orders"))
                                                 .body("entities", hasSize(4))
                                                 .body("entities.name", hasItems("Country", "Customer", "Order", "OrderItem"))
                                                 .body("processes", hasSize(1))
                                                 .body("processes[0].steps", hasSize(5))
                                                 .body("forms", hasSize(1))
                                                 .body("reports", hasSize(2))
                                                 .body("permissions", hasSize(2))
                                                 .body("seeds[0].rows", hasSize(2)));
    }

    @Test
    void parse_reports_every_validation_issue_at_once() {
        String broken = """
                name: broken
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true }
                    relations:
                      - { name: country, kind: manyToOne, to: Nowhere }
                processes:
                  - name: Flow
                    steps:
                      - name: decide
                        kind: decision
                        args: { if: "x > 1", then: missingStep }
                """;
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(broken)
                                                 .when()
                                                 .post(PARSE_URL)
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues", hasSize(2))
                                                 .body("issues", hasItems(
                                                         "entity [Customer] relation [country] points to unknown entity [Nowhere]",
                                                         "process [Flow] decision [decide] `then` references unknown step [missingStep]")));
    }

    @Test
    void parse_rejects_a_trigger_to_an_unknown_entity() {
        String yaml = """
                name: badtrigger
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                processes:
                  - name: Approve
                    trigger: { onCreate: Nowhere }
                    steps:
                      - { name: done, kind: end }
                """;
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(yaml)
                                                 .when()
                                                 .post(PARSE_URL)
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues", hasItem(
                                                         "process [Approve] trigger onCreate references unknown entity [Nowhere]")));
    }

    @Test
    void parse_rejects_a_non_integer_primary_key() {
        String yaml = """
                name: badpk
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: uuid, primaryKey: true, generated: true }
                """;
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(yaml)
                                                 .when()
                                                 .post(PARSE_URL)
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues", hasItem(
                                                         "entity [Customer] primary-key field [id] must be an integer type (integer/int/long) - identifiers are integer by convention, got [uuid]")));
    }

    @Test
    void generate_writes_all_model_files_into_the_workspace_project() {
        writeIntent(INTENT_YAML);

        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("project", equalTo(PROJECT))
                                                 .body("written",
                                                         hasItems("orders.edm", "orders.model", "OrderApproval.bpmn", "ApproveOrder.form",
                                                                 "OrdersByCustomer.report", "orders.roles", "countries.csvim",
                                                                 "countries.csv"))
                                                 .body("scrubbed", hasSize(0)));

        assertEdmAndModel();
        assertBpmn();
        assertForm();
        assertReport();
        assertRoles();
        assertSeeds();
    }

    @Test
    void events_template_generates_the_process_trigger_handler() {
        // Generate the models from the intent (orders.model carries the triggers collection)...
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // ...then run the events template against that model through the real generation service -
        // this exercises the generateUtils.js "triggers" collection case end to end.
        generateFromModel("template-application-events-java/template/template.js", "orders.model");

        String handler = contentOf("gen/events/OrderApprovalTrigger.java");
        assertTrue(handler.contains("class OrderApprovalTrigger"),
                "the events template should generate a handler class named after the process");
        assertTrue(handler.contains("@Listener(name = \"intent-test-Order-Order\""),
                "the handler should bind to the entity's event topic <project>-<perspective>-<entity>");
        assertTrue(handler.contains("Process.start(\"OrderApproval\""), "the handler should start the process");
        assertTrue(handler.contains("import gen.orders.data.order.OrderRepository"),
                "the handler should import the generated typed repository from its real (lowercased) Java package");
    }

    @Test
    void generating_the_events_template_preserves_the_full_stack_gen_output() {
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // Full-stack (DAO) generation writes the repository under gen/orders.
        generateFromModel("template-application-dao-java/template/template.js", "orders.model");
        assertTrue(resource("gen/orders/data/order/OrderRepository.java").exists(),
                "the DAO template should generate the repository under gen/orders");
        // Generating the events template must clean only gen/events, not gen/<modelName> - so the
        // full-stack output survives (the reported bug was the events generation wiping gen/orders).
        generateFromModel("template-application-events-java/template/template.js", "orders.model");
        assertTrue(resource("gen/orders/data/order/OrderRepository.java").exists(),
                "generating the events template must not delete the full-stack gen/orders output");
        assertTrue(resource("gen/events/OrderApprovalTrigger.java").exists(), "the events template should still produce gen/events");
    }

    @Test
    void regeneration_scrubs_stale_model_files() {
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        assertTrue(resource("countries.csvim").exists(), "seed output should exist after the first generation");

        writeIntent(INTENT_YAML.substring(0, INTENT_YAML.indexOf("seeds:")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("scrubbed", hasItems("countries.csvim", "countries.csv")));

        assertTrue(!resource("countries.csvim").exists(), "removing the seed block should scrub the stale .csvim");
        assertTrue(!resource("countries.csv").exists(), "removing the seed block should scrub the stale .csv");
        assertTrue(resource("orders.edm").exists(), "still-declared slices must survive the scrub");
        assertTrue(resource("app.intent").exists(), "the scrub must never touch the intent source itself");
    }

    @Test
    void generate_rejects_invalid_intents_with_the_issue_list() {
        writeIntent("entities:\n  - name: A\n    fields:\n      - { name: x, type: nosuchtype }\n");
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues", hasItem("entity [A] field [x] has unknown type [nosuchtype]")));
    }

    private void writeIntent(String yaml) {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private IResource resource(String fileName) {
        return repository.getResource(PROJECT_PATH + "/" + fileName);
    }

    private String contentOf(String fileName) {
        return new String(resource(fileName).getContent(), StandardCharsets.UTF_8);
    }

    /** Run a language template against a generated model through the real generation service. */
    private void generateFromModel(String templateModule, String modelFile) {
        String payload = "{\"template\":\"" + templateModule + "\",\"parameters\":{}}";
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(payload)
                                                 .when()
                                                 .post("/services/js/service-generate/generate.mjs/model/" + WORKSPACE + "/" + PROJECT
                                                         + "?path=" + modelFile)
                                                 .then()
                                                 .statusCode(201));
    }

    private void assertEdmAndModel() {
        assertTrue(resource("orders.edm").exists(), "orders.edm should be generated");
        String edmXml = contentOf("orders.edm");
        for (String entityName : new String[] {"Country", "Customer", "Order", "OrderItem"}) {
            assertTrue(edmXml.contains("name=\"" + entityName + "\""), "EDM should declare entity [" + entityName + "]");
        }
        assertTrue(edmXml.contains("dataName=\"ORDERS_ORDER\""),
                "EDM dataName should be intent-prefixed (ORDERS_ORDER) to avoid reserved words and cross-project clashes");
        assertTrue(edmXml.contains("type=\"DEPENDENT\""),
                "EDM should mark OrderItem as DEPENDENT through its composition manyToOne to Order");
        assertTrue(edmXml.contains("relationshipName=\"OrderItem_Order\""),
                "relationshipName (the FK constraint name) should be <owner>_<target> like the codbex .model");
        assertTrue(edmXml.contains("relationshipEntityName=\"Order\"") && edmXml.contains("relationshipEntityPerspectiveName=\"Order\""),
                "FK property must carry relationshipEntityName + relationshipEntityPerspectiveName - the dropdown data-service URL is built from them");
        assertTrue(edmXml.contains("relationshipType=\"ASSOCIATION\"") && edmXml.contains("relationshipCardinality=\"n_1\""),
                "a non-composition manyToOne (e.g. Order->Customer) should be an ASSOCIATION with n_1 cardinality");
        assertTrue(edmXml.contains("name=\"Id\""), "property names should be PascalCase (Dirigible/codbex convention): id -> Id");
        assertTrue(edmXml.contains("auditType=\"NONE\""), "properties should carry auditType=\"NONE\" like codbex EDMs");
        assertTrue(edmXml.contains("isRequiredProperty=\"true\""),
                "a required field/FK should carry isRequiredProperty - the REST controller's required validation keys on it");
        assertTrue(edmXml.contains("widgetDropDownKey=\"Id\""),
                "dropdown key should be the target entity's actual PK property name, PascalCased (Id)");
        assertTrue(edmXml.contains("referenced=\"Customer\""), "EDM should carry the Order->Customer relation");
        assertTrue(edmXml.contains("dataName=\"CUSTOMER_COUNTRY\""),
                "Customer->Country FK should materialize as a CUSTOMER_COUNTRY column on Customer");
        // OrderApproval has trigger { onCreate: Order }, so Order gains a ProcessId back-reference.
        assertTrue(edmXml.contains("name=\"ProcessId\"") && edmXml.contains("dataName=\"ORDER_PROCESS_ID\""),
                "an entity a process starts on create should get a ProcessId back-reference property");

        // The EDM editor renders the canvas ONLY from mxGraphModel - without it the editor opens
        // empty. Assert the diagram block, an entity vertex, and a relation edge are present.
        assertTrue(edmXml.contains("<mxGraphModel>"), "EDM must carry an mxGraphModel diagram or the editor renders an empty canvas");
        assertTrue(edmXml.contains("style=\"entity\""), "mxGraphModel must contain entity vertices");
        assertTrue(edmXml.contains("<Entity") && edmXml.contains("type=\"Entity\""),
                "mxGraphModel entity cells must carry an Entity value");
        assertTrue(edmXml.contains("edge=\"1\""), "mxGraphModel must wire the foreign-key relations as edges");

        assertTrue(resource("orders.model").exists(), "orders.model should be generated");
        String modelBody = contentOf("orders.model");
        assertTrue(modelBody.contains("\"entities\""), "model JSON should have an entities array");
        assertTrue(modelBody.contains("\"perspectives\""), "model JSON should carry the perspectives array like editor-written files");
        assertTrue(modelBody.contains("\"navigations\""), "model JSON should carry the navigations array like editor-written files");
        // The trigger collection the events template iterates: one entry per onCreate process trigger.
        assertTrue(
                modelBody.contains("\"triggers\"") && modelBody.contains("\"process\": \"OrderApproval\"")
                        && modelBody.contains("\"entity\": \"Order\""),
                "model JSON should carry a triggers collection for the events template");
    }

    private void assertBpmn() {
        String body = contentOf("OrderApproval.bpmn");
        assertTrue(body.contains("<userTask id=\"managerReview\""), "BPMN should map managerReview to a userTask");
        assertTrue(body.contains("flowable:candidateGroups=\"order-manager\""), "BPMN should carry the userTask candidateGroups");
        assertTrue(body.contains("<exclusiveGateway id=\"bigOrder\""), "BPMN should map the decision step to an exclusiveGateway");
        assertTrue(body.contains("delegateExpression=\"${JSTask}\""), "BPMN should use the Flowable delegate expression for service tasks");
        assertTrue(body.contains("id=\"flow_bigOrder_then\" sourceRef=\"bigOrder\" targetRef=\"cfoReview\""),
                "the conditioned flow should target the `then` step");
        assertTrue(body.contains("id=\"flow_bigOrder_default\" sourceRef=\"bigOrder\" targetRef=\"notifyCustomer\""),
                "the gateway default flow should target the `else` step so small orders skip CFO review");
        assertTrue(body.contains("${amount > 10000}"), "BPMN should embed the decision's if expression");

        // The Flowable/Oryx modeler renders the canvas ONLY from the diagram interchange - without it
        // the editor opens empty. Assert the diagram block, a node shape, and a flow edge are present.
        assertTrue(body.contains("<bpmndi:BPMNDiagram"), "BPMN must carry a bpmndi diagram or the editor renders an empty canvas");
        assertTrue(body.contains("<bpmndi:BPMNShape bpmnElement=\"managerReview\""), "the diagram must place a shape for each node");
        assertTrue(body.contains("<bpmndi:BPMNEdge bpmnElement=\"flow_bigOrder_then\""), "the diagram must draw an edge for each flow");
        assertTrue(body.contains("<omgdc:Bounds"), "shapes must carry layout bounds");
    }

    private void assertForm() {
        String body = contentOf("ApproveOrder.form");
        assertTrue(body.contains("\"controlId\": \"header\""), "form should start with a header control");
        assertTrue(body.contains("\"controlId\": \"input-date\""), "form should pick input-date for the orderDate field");
        assertTrue(body.contains("\"controlId\": \"input-number\""), "form should pick input-number for the total decimal field");
        assertTrue(body.contains("\"model\": \"OrderDate\""),
                "form field model should bind to the PascalCase entity property (orderDate -> OrderDate)");
        assertTrue(body.contains("\"type\": \"positive\""), "form should mark the approve button as positive");
        assertTrue(body.contains("onApproveClicked"), "form code should declare the approve handler stub");
    }

    private void assertReport() {
        String body = contentOf("OrdersByCustomer.report");
        assertTrue(body.contains("\"name\": \"OrdersByCustomer\""), "report should carry its declared name");
        assertTrue(body.contains("\"alias\": \"Order\""), "report alias should be the source entity");
        assertTrue(body.contains("\"table\": \"ORDERS_ORDER\""),
                "report table should be the same intent-prefixed table name the EDM declares as dataName");
        assertTrue(body.contains("\"aggregate\": \"COUNT\""), "count(*) should be parsed into an aggregate COUNT column");
        assertTrue(body.contains("\"aggregate\": \"SUM\""), "sum(total) should be parsed into an aggregate SUM column");
        // The query is materialised SQL (not left empty): SELECT ... FROM <table> as <alias> ... GROUP BY.
        assertTrue(body.contains("SELECT ") && body.contains("FROM ORDERS_ORDER as Order") && body.contains("GROUP BY"),
                "report query should be a materialised SQL statement with GROUP BY, not empty");
        assertTrue(body.contains("SUM(Order.ORDER_TOTAL)"), "sum(total) should aggregate the qualified ORDER_TOTAL column");
        assertTrue(body.contains("\"roleRead\":"), "report should carry default-role read security");
        // A bare to-one relation dimension (customer) joins the related table and shows its name field,
        // grouping by the name - not the raw FK id.
        assertTrue(body.contains("INNER JOIN ORDERS_CUSTOMER as Customer ON Order.ORDER_CUSTOMER = Customer.CUSTOMER_ID"),
                "a bare relation dimension (customer) should INNER JOIN the related entity");
        assertTrue(body.contains("SELECT Customer.CUSTOMER_NAME as") && body.contains("GROUP BY Customer.CUSTOMER_NAME"),
                "the bare relation dimension should select + group by the related entity's name, not its FK id");

        // A relation.field dimension joins the related table; the filter becomes a qualified WHERE.
        String joined = contentOf("BigOrderItems.report");
        assertTrue(joined.contains("INNER JOIN ORDERS_ORDER as Order ON OrderItem.ORDER_ITEM_ORDER = Order.ORDER_ID"),
                "a relation.field dimension (order.orderDate) should INNER JOIN the related entity on its FK");
        assertTrue(joined.contains("WHERE OrderItem.ORDER_ITEM_QUANTITY > 1"),
                "the intent filter should become a WHERE with the field rewritten to its qualified column");
    }

    private void assertRoles() {
        String body = contentOf("orders.roles");
        assertTrue(body.contains("\"name\": \"Sales\""), "Sales role should be present");
        assertTrue(body.contains("\"name\": \"Manager\""), "Manager role should be present");
        assertTrue(body.contains("\"description\": \"Sales staff\""), "Role descriptions should be carried through");
    }

    private void assertSeeds() {
        String csvimBody = contentOf("countries.csvim");
        assertTrue(csvimBody.contains("\"table\": \"ORDERS_COUNTRY\""),
                "csvim should target the same intent-prefixed table name the EDM declares as dataName");
        assertTrue(csvimBody.contains("\"file\": \"/" + PROJECT + "/countries.csv\""),
                "csvim file path must be project-qualified - CsvimProcessor resolves it against /registry/public");

        String csvBody = contentOf("countries.csv");
        assertTrue(csvBody.startsWith("COUNTRY_ID,COUNTRY_NAME,COUNTRY_CODE2"),
                "csv header should carry the upper-snake column names in entity-field order");
        assertTrue(csvBody.contains("1,Afghanistan,AF"), "csv should include the Afghanistan row with an integral id");
    }

    @AfterEach
    void removeProject() {
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
