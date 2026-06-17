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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: name,        type: string,  required: true, length: 200 }
                  - { name: active,      type: boolean, defaultValue: "true" }
                  - { name: creditLimit, type: decimal }
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
                    args: { assignee: manager, form: ApproveOrder }
                  - name: bigOrder
                    kind: decision
                    args: { if: "customer.creditLimit > 10000", then: cfoReview, else: notifyCustomer }
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
                                                                 "OrdersByCustomer.report", "orders.roles", "orders.glue",
                                                                 "countries.csvim", "countries.csv"))
                                                 .body("scrubbed", hasSize(0))
                                                 // The model-to-code plan the editor replays: one entry per generated model with a
                                                 // recipe in .settings, naming the template + parameters.
                                                 .body("codeGenerations.path",
                                                         hasItems("orders.model", "orders.glue", "ApproveOrder.form",
                                                                 "OrdersByCustomer.report"))
                                                 .body("codeGenerations.find { it.path == 'orders.model' }.templateId",
                                                         equalTo("template-application-angular-java/template/template.js"))
                                                 .body("codeGenerations.find { it.path == 'orders.model' }.parameters.dataSource",
                                                         equalTo("DefaultDB")));

        assertEdmAndModel();
        assertBpmn();
        assertForm();
        assertReport();
        assertRoles();
        assertSeeds();
        assertGlue();
        assertSettings();
    }

    @Test
    void glue_template_generates_the_trigger_and_resolver_handlers() {
        // Generate the models from the intent (orders.glue carries the triggers + resolvers)...
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // ...then run the glue-code template against the .glue file through the real generation service -
        // this exercises the generateUtils.js "triggers" + "resolvers" collection cases end to end.
        generateFromModel("template-application-events-java/template/template.js", "orders.glue");

        String handler = contentOf("gen/events/OrderApprovalTrigger.java");
        assertTrue(handler.contains("class OrderApprovalTrigger"),
                "the glue template should generate a handler class named after the process");
        assertTrue(handler.contains("@Listener(name = \"intent-test-Order-Order\""),
                "the handler should bind to the entity's event topic <project>-<perspective>-<entity>");
        assertTrue(handler.contains("Process.start(\"OrderApproval\""), "the handler should start the process");
        assertTrue(handler.contains("import gen.orders.data.order.OrderRepository"),
                "the handler should import the generated typed repository from its real (lowercased) Java package");
        // Must deserialize via the java.time-aware SDK helper, not a bare Gson (which throws
        // InaccessibleObjectException on LocalDate fields under JDK 17+).
        assertTrue(handler.contains("Json.parse(message,"), "the handler should parse the event with the SDK Json helper");
        assertFalse(handler.contains("new Gson()"), "the handler must not use a bare Gson (fails on java.time fields)");

        // The decision resolver (customer.creditLimit) is a JavaDelegate that loads Customer and sets
        // the variable the rewritten condition tests.
        String resolver = contentOf("gen/events/ResolveCustomerCreditLimit.java");
        assertTrue(resolver.contains("class ResolveCustomerCreditLimit implements JavaDelegate"),
                "the resolver should be a Flowable JavaDelegate");
        assertTrue(resolver.contains("import gen.orders.data.customer.CustomerRepository"),
                "the resolver should load the target entity from its real (lowercased) Java package");
        assertTrue(resolver.contains("execution.getVariable(\"Customer\")"),
                "the resolver should read the FK id from the process variables");
        assertTrue(resolver.contains("execution.setVariable(\"customer_creditLimit\""), "the resolver should set the resolved variable");
        assertTrue(resolver.contains("entity.CreditLimit"), "the resolver should read the target field");
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
        // The create-event publish must serialize via the java.time-aware SDK helper, not a bare Gson.
        String repository = contentOf("gen/orders/data/order/OrderRepository.java");
        assertTrue(repository.contains("Json.stringify(saved)"), "the repository should publish the event with the SDK Json helper");
        assertFalse(repository.contains("new Gson()"), "the repository must not use a bare Gson (fails on java.time fields)");
        // Generating the glue template must clean only gen/events, not gen/<modelName> - so the
        // full-stack output survives (the reported bug was the events generation wiping gen/orders).
        generateFromModel("template-application-events-java/template/template.js", "orders.glue");
        assertTrue(resource("gen/orders/data/order/OrderRepository.java").exists(),
                "generating the glue template must not delete the full-stack gen/orders output");
        assertTrue(resource("gen/events/OrderApprovalTrigger.java").exists(), "the glue template should still produce gen/events");
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

    private void writeProjectFile(String fileName, String content) {
        String path = PROJECT_PATH + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
                "relationshipName (the FK constraint name) should be <owner>_<target> like the Dirigible .model");
        assertTrue(edmXml.contains("relationshipEntityName=\"Order\"") && edmXml.contains("relationshipEntityPerspectiveName=\"Order\""),
                "FK property must carry relationshipEntityName + relationshipEntityPerspectiveName - the dropdown data-service URL is built from them");
        assertTrue(edmXml.contains("relationshipType=\"ASSOCIATION\"") && edmXml.contains("relationshipCardinality=\"n_1\""),
                "a non-composition manyToOne (e.g. Order->Customer) should be an ASSOCIATION with n_1 cardinality");
        assertTrue(edmXml.contains("name=\"Id\""), "property names should be PascalCase (Dirigible convention): id -> Id");
        assertTrue(edmXml.contains("auditType=\"NONE\""), "properties should carry auditType=\"NONE\" like Dirigible EDMs");
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
        // Process glue (triggers, resolvers) is NOT in the EDM model - it lives in the .glue file.
        assertFalse(modelBody.contains("\"triggers\""),
                "the EDM model must not carry process-glue metadata - that lives in the .glue file");
    }

    private void assertGlue() {
        assertTrue(resource("orders.glue").exists(), "the .glue file should be generated");
        String glue = contentOf("orders.glue");
        // Triggers: one per onCreate process trigger.
        assertTrue(
                glue.contains("\"triggers\"") && glue.contains("\"process\": \"OrderApproval\"") && glue.contains("\"entity\": \"Order\""),
                "glue should carry the trigger for the OrderApproval process on Order");
        // Resolvers: one per relation.field referenced in a decision.
        assertTrue(glue.contains("\"resolvers\"") && glue.contains("\"handler\": \"ResolveCustomerCreditLimit\""),
                "glue should carry the customer.creditLimit decision resolver");
        assertTrue(
                glue.contains("\"fkProperty\": \"Customer\"") && glue.contains("\"targetEntity\": \"Customer\"")
                        && glue.contains("\"targetField\": \"CreditLimit\"") && glue.contains("\"variable\": \"customer_creditLimit\""),
                "the resolver should carry the FK property, target entity/field and the resolved variable");
    }

    private void assertSettings() {
        assertTrue(resource("orders.settings").exists(), "the .settings file should be scaffolded");
        String settings = contentOf("orders.settings");
        assertTrue(settings.contains("\"generation\"") && settings.contains("template-application-angular-java"),
                "settings should carry the model generation recipe");
        assertTrue(settings.contains("\"glue\"") && settings.contains("template-application-events-java"),
                "settings should carry the glue generation recipe");
        assertTrue(
                settings.contains("\"overrides\"") && settings.contains("\"OrderApproval\"")
                        && settings.contains("\"ResolveCustomerCreditLimit\"") && settings.contains("\"ApproveOrder\""),
                "settings should enumerate the trigger / resolver / form overrides");
        assertTrue(settings.contains("\"candidateGroupsExtra\"") && settings.contains("ADMINISTRATOR"),
                "settings should default candidateGroupsExtra to ADMINISTRATOR");
    }

    @Test
    void settings_overrides_skip_generation_and_are_preserved() {
        writeIntent(INTENT_YAML);
        // First Generate scaffolds orders.settings (everything generate:true) and emits the form.
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        assertTrue(resource("ApproveOrder.form").exists());

        // The developer opts out of the form and the trigger (uses hand-written ones).
        writeProjectFile("orders.settings", """
                {
                  "overrides": {
                    "forms":    { "ApproveOrder":  { "generate": false } },
                    "triggers": { "OrderApproval": { "generate": false } }
                  }
                }
                """);

        // Regenerate: the opted-out form is no longer emitted (and is scrubbed); the glue has no trigger.
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("written", not(hasItem("ApproveOrder.form")))
                                                 .body("scrubbed", hasItem("ApproveOrder.form")));
        assertFalse(resource("ApproveOrder.form").exists(), "an opted-out form must not be generated");
        String glue = contentOf("orders.glue");
        assertFalse(glue.contains("\"entity\""), "an opted-out trigger must not appear in the glue (no trigger entries)");
        // The resolver was not opted out, so it survives.
        assertTrue(glue.contains("\"handler\": \"ResolveCustomerCreditLimit\""), "a non-opted-out resolver should still be generated");
        // The developer's settings file is preserved verbatim, not overwritten by the scaffold.
        assertTrue(contentOf("orders.settings").contains("\"generate\": false"),
                "the edited settings must be preserved across regeneration");
    }

    private void assertBpmn() {
        String body = contentOf("OrderApproval.bpmn");
        assertTrue(body.contains("<userTask id=\"managerReview\""), "BPMN should map managerReview to a userTask");
        // The assignee "manager" resolves to the declared role "Manager" (case-insensitive); the
        // settings' candidateGroupsExtra (ADMINISTRATOR by default) is appended so an admin can claim.
        assertTrue(body.contains("flowable:candidateGroups=\"Manager,ADMINISTRATOR\""),
                "the userTask candidateGroups must be the resolved role plus the settings' extra groups");
        assertFalse(body.contains("flowable:candidateGroups=\"manager\""), "the candidate group must not keep the raw lower-case assignee");
        // The form key must be the served form-page URL so the Inbox "Open Form" navigates to the page
        // (a bare name resolves relative to the inbox and 404s).
        assertTrue(body.contains("flowable:formKey=\"/services/web/intent-test/gen/ApproveOrder/forms/ApproveOrder/index.html\""),
                "the userTask formKey must be the generated form page URL");
        assertTrue(body.contains("<exclusiveGateway id=\"bigOrder\""), "BPMN should map the decision step to an exclusiveGateway");
        assertTrue(body.contains("delegateExpression=\"${JSTask}\""), "BPMN should use the Flowable delegate expression for service tasks");
        // The JS handler path is qualified with the project so the JSTask delegate (which resolves from
        // the registry root) finds the project's custom/ handler.
        assertTrue(body.contains("<![CDATA[intent-test/custom/notify-customer.ts]]>"),
                "the service-task handler path should be project-qualified");
        assertTrue(body.contains("id=\"flow_bigOrder_then\" sourceRef=\"bigOrder\" targetRef=\"cfoReview\""),
                "the conditioned flow should target the `then` step");
        assertTrue(body.contains("id=\"flow_bigOrder_default\" sourceRef=\"bigOrder\" targetRef=\"notifyCustomer\""),
                "the gateway default flow should target the `else` step so small orders skip CFO review");
        // The decision references customer.creditLimit, so a JavaTask resolver is inserted before the
        // gateway and the condition is rewritten to test the resolved variable.
        assertTrue(
                body.contains("<serviceTask id=\"ResolveCustomerCreditLimit\"")
                        && body.contains("flowable:delegateExpression=\"${JavaTask}\""),
                "a JavaTask resolver service task should precede the decision");
        assertTrue(body.contains("gen.events.ResolveCustomerCreditLimit"), "the resolver task should point at the generated handler FQN");
        assertTrue(
                body.contains("sourceRef=\"managerReview\" targetRef=\"ResolveCustomerCreditLimit\"")
                        && body.contains("sourceRef=\"ResolveCustomerCreditLimit\" targetRef=\"bigOrder\""),
                "the resolver should sit on the linear flow right before the decision");
        assertTrue(body.contains("${customer_creditLimit > 10000}"),
                "the decision condition should be rewritten to test the resolved variable");
        assertFalse(body.contains("customer.creditLimit"), "the raw relation.field path must not leak into the BPMN condition");

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
        assertTrue(body.contains("onApproveClicked"), "form code should declare the approve handler");
        // The action handler must complete the BPM task, not be a no-op stub. (The .form code is
        // HTML-escaped by Gson - ' becomes \\u0027, = becomes \\u003d - so match escape-free substrings;
        // the form-builder un-escapes the code when it injects it into the controller.)
        assertTrue(body.contains("__completeTask("), "the action buttons should complete the task");
        assertTrue(body.contains("/services/bpm/bpm-processes/tasks/") && body.contains("COMPLETE"),
                "the form should complete the task via the platform BPM task API");
        assertFalse(body.contains("TODO: wire"), "the action handlers must no longer be TODO stubs");
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
