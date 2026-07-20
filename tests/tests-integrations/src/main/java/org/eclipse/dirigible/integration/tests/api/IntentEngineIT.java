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
    private static final String AGENT_URL = "/services/ide/intent/agent";

    private static final String INTENT_YAML = """
            name: orders
            description: Order management with approval workflow
            version: 1
            # Data languages the app offers: the Harmonia Region & Language setting lists them and the
            # multilingual entities translate by the chosen one.
            languages: [en, bg]

            entities:
              - name: Country
                kind: setting
                multilingual: true
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
                  - { name: orderCount,  type: integer }
                relations:
                  - { name: country, kind: manyToOne, to: Country }
                  - { name: orders,  kind: oneToMany, to: Order }

              - name: Order
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: orderDate, type: date,    required: true }
                  - { name: total,     type: decimal }
                  # Depends-On auto-populate: copied from the chosen customer's creditLimit.
                  - { name: creditSnapshot, type: decimal, dependsOn: { relation: customer, valueFrom: creditLimit } }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
                  # Depends-On cascade: narrowed to the chosen customer's country (filterBy defaults to the PK).
                  - { name: country,  kind: manyToOne, to: Country, dependsOn: { relation: customer, valueFrom: country } }
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
                  - name: cfoDecision
                    kind: decision
                    args: { if: "action == 'approve'", then: notifyCustomer, else: done }
                  - name: notifyCustomer
                    kind: serviceTask
                  - name: done
                    kind: end

            forms:
              - name: ApproveOrder
                forEntity: Order
                description: Approve or reject an order
                # customer.creditLimit is also used by the bigOrder decision (so its resolver moves
                # earlier, before this form); customer.name is referenced only here (form-only resolver).
                fields: [orderDate, total, customer.creditLimit, customer.name]
                actions: [approve, reject]

            reports:
              - name: OrdersByCustomer
                source: Order
                dimensions: [customer]
                measures: ["count(*)", "sum(total)"]
              # month(field) buckets a date dimension into a sortable YYYYMM integer. The widget
              # turns the report into a dashboard KPI: one aggregate cell, the month pinned to now.
              - name: OrdersByMonth
                source: Order
                dimensions: ["month(orderDate)"]
                measures: ["count(*)", "sum(total)"]
                widget:
                  value: "sum(total)"
                  at: { "month(orderDate)": now }
                  label: Revenue (this month)
                  icon: banknote
              - name: BigOrderItems
                source: OrderItem
                description: Order items with quantity over one, with their order date
                dimensions: [order.orderDate, quantity]
                filter: "quantity > 1"
                widget: { kind: count, label: Big Order Items, icon: alert-triangle }
              # kind: balance - the accounting shape: opening/period/closing debit+credit totals per
              # dimension between the runtime fromDate/toDate parameters (declared on the .report).
              - name: OrderBalance
                kind: balance
                source: Order
                date: orderDate
                debit: total
                credit: creditSnapshot
                dimensions: [customer]

            # Custom dashboard widgets - developer-supplied content: a REST KPI (the url returns
            # {value, description?}) and an embedded page tile.
            widgets:
              - name: SystemHealth
                kind: kpi
                url: /services/js/orders/custom/health.js
                label: System Health
                icon: activity
              - name: SalesFunnel
                kind: page
                url: /services/web/orders/custom/funnel.html

            permissions:
              - { role: Sales,   description: Sales staff,   can: [Customer:read, Order:create] }
              - { role: Manager, description: Sales manager, can: [Order:approve] }

            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 1, name: Afghanistan, code2: AF }
                  - { id: 2, name: Albania,     code2: AL }
              # Translations for the multilingual Country - land in ORDERS_COUNTRY_LANG.
              - name: countries-bg
                entity: Country
                language: bg
                rows:
                  - { id: 1, name: "Афганистан" }
                  - { id: 2, name: "Албания" }
              # Large data sets stay OUT of the intent: an authored CSV in a subfolder, referenced
              # by path - only the .csvim is generated.
              - name: countries-extra
                entity: Country
                file: data/countries-extra.csv

            notifications:
              - name: orderUpdated
                event: { onUpdate: Order }
                to: ops@example.com
                subject: "Order {id} for {customer.name}, total {total}"
                body: "The order changed."

            schedules:
              - name: staleOrders
                cron: "0 0 9 * * ?"
                entity: Order
                where:
                  - { field: orderDate, op: lt, value: CURRENT_DATE }
                notify:
                  to: ops@example.com
                  subject: "Stale order {id} for {customer.name}"
                  body: "This order is stale."

            integrations:
              - name: pushOrderToWarehouse
                event: { onCreate: Order }
                method: POST
                url: "@config:WAREHOUSE_URL"

            inbound:
              - name: ingestOrder
                path: /ingest
                create: Order

            rollups:
              - name: customerOrderCount
                entity: Order
                via: customer
                field: orderCount
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
                                                 .body("processes[0].steps", hasSize(6))
                                                 .body("forms", hasSize(1))
                                                 .body("reports", hasSize(4))
                                                 .body("permissions", hasSize(2))
                                                 .body("seeds[0].rows", hasSize(2)));
    }

    @Test
    void agent_reports_when_not_configured() {
        // No DIRIGIBLE_INTENT_AI_API_KEY is set in the test environment, so the assistant is disabled
        // and the endpoint must say so (412) rather than attempting an upstream call. Network-free.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"yaml\":\"name: demo\",\"message\":\"add a field\",\"history\":[]}")
                                                 .when()
                                                 .post(AGENT_URL)
                                                 .then()
                                                 .statusCode(412));
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
    void parse_rejects_a_trigger_business_key_that_is_not_a_field() {
        String yaml = """
                name: orders
                entities:
                  - name: SalesOrder
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                processes:
                  - name: Approve
                    trigger: { onCreate: SalesOrder, businessKey: nope }
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
                                                         "process [Approve] trigger businessKey [nope] is not a field of [SalesOrder]")));
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
                                                                 "OrdersByCustomer.report", "OrderBalance.report", "orders.roles",
                                                                 "orders.glue", "countries.csvim", "countries.csv",
                                                                 "doc/Templates/Order/Print/en/standard.print", "orders.test"))
                                                 .body("scrubbed", hasSize(0))
                                                 // The model-to-code plan the editor replays: one entry per generated model with a
                                                 // recipe in .settings, naming the template + parameters.
                                                 .body("codeGenerations.path",
                                                         hasItems("orders.model", "orders.glue", "ApproveOrder.form",
                                                                 "OrdersByCustomer.report"))
                                                 .body("codeGenerations.find { it.path == 'orders.model' }.templateId",
                                                         equalTo("template-application-ui-harmonia-java/template/template.js"))
                                                 .body("codeGenerations.find { it.path == 'orders.model' }.parameters.dataSource",
                                                         equalTo("DefaultDB"))
                                                 // The report is generated server-side as Java (DAO + REST controller) with a Harmonia UI.
                                                 .body("codeGenerations.find { it.path == 'OrdersByCustomer.report' }.templateId", equalTo(
                                                         "template-application-ui-harmonia-java/template/template-report-file.js")));

        assertEdmAndModel();
        assertBpmn();
        assertForm();
        assertReport();
        assertRoles();
        assertSeeds();
        assertGlue();
        assertSettings();
        assertAppTestManifest();
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
        assertTrue(handler.contains("implements MessageHandler"), "the trigger should be a self-describing MessageHandler");
        assertTrue(handler.contains("return \"intent-test-Order-Order\""),
                "the handler should bind to the entity's event topic <project>-<perspective>-<entity> via destination()");
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
        // Clear-D: the process context holds only the trigger entity's id, so the resolver loads the
        // OWNER (Order) by that id and reads the FK off it, rather than reading the FK from a start-time
        // process variable.
        assertTrue(resolver.contains("execution.getVariable(\"Id\")"),
                "the resolver should read the trigger entity's id from the process variables (id-only context)");
        assertTrue(resolver.contains("owner.Customer"), "the resolver should read the FK off the owner entity it loaded by id");
        assertTrue(resolver.contains("execution.setVariable(\"customer_creditLimit\""), "the resolver should set the resolved variable");
        assertTrue(resolver.contains("entity.CreditLimit"), "the resolver should read the target field");

        // The form-only relation.field (customer.name on the ApproveOrder form) produces its own resolver
        // even though no decision references it - the user-task form is a resolver trigger in its own
        // right.
        String formResolver = contentOf("gen/events/ResolveCustomerName.java");
        assertTrue(formResolver.contains("class ResolveCustomerName implements JavaDelegate"),
                "a relation.field referenced only by a user-task form should still generate a resolver");
        assertTrue(formResolver.contains("execution.setVariable(\"customer_name\"") && formResolver.contains("entity.Name"),
                "the form resolver should publish the related field as the customer_name variable the form control binds to");

        // The notification (onUpdate: Order) is a self-describing @Component MessageHandler that sends mail
        // when an Order is updated -
        // exercises the generateUtils.js "notifications" collection case end to end.
        String notification = contentOf("gen/events/OrderUpdatedNotification.java");
        assertTrue(notification.contains("class OrderUpdatedNotification implements MessageHandler"),
                "the notification should be a message-handling listener (PascalCased class name)");
        assertTrue(notification.contains("@Component") && notification.contains("return \"intent-test-Order-Order-updated\""),
                "an onUpdate notification (self-describing @Component MessageHandler) should bind the entity's -updated topic via destination()");
        assertTrue(notification.contains("Mail.send("), "the notification should send via the SDK Mail API");
        assertTrue(notification.contains("String to = \"ops@example.com\""), "a literal recipient should be emitted as a literal");
        assertTrue(notification.contains("import gen.orders.data.order.OrderEntity"),
                "the notification should import the event entity from its real (lowercased) Java package");
        // The subject references a one-hop relation.field ({customer.name}), so the listener loads the
        // related Customer by FK id and the subject reads its field - the same one-hop mechanism as the
        // decision resolvers.
        assertTrue(
                notification.contains("import gen.orders.data.customer.CustomerEntity")
                        && notification.contains("import gen.orders.data.customer.CustomerRepository"),
                "the notification should import the related entity + repository it loads");
        assertTrue(
                notification.contains(
                        "CustomerEntity customer = entity.Customer == null ? null : new CustomerRepository().findById(entity.Customer)"),
                "the listener should load the one-hop related entity by FK id");
        assertTrue(notification.contains("\"Order \" + entity.Id + \" for \" + (customer == null ? null : customer.Name)"),
                "the subject should interpolate the relation.field against the loaded related entity");

        // The schedule is a self-describing @Component JobHandler (cron()) that queries via a typed
        // Criteria and notifies per row.
        String job = contentOf("gen/events/StaleOrdersJob.java");
        assertTrue(
                job.contains("@Component") && job.contains("class StaleOrdersJob implements JobHandler")
                        && job.contains("return \"0 0 9 * * ?\""),
                "the schedule should generate a self-describing @Component JobHandler whose cron() returns the expression");
        assertTrue(job.contains("new OrderRepository().findAll(Criteria.create().lt(\"OrderDate\", java.time.LocalDate.now()))"),
                "the job should query the entity with a typed Criteria built from the where clause");
        assertTrue(job.contains("for (OrderEntity entity : rows)"), "the job should iterate the matching rows");
        assertTrue(
                job.contains(
                        "CustomerEntity customer = entity.Customer == null ? null : new CustomerRepository().findById(entity.Customer)"),
                "the per-row notify should load the one-hop related entity");
        assertTrue(job.contains("Mail.send("), "the job should notify per row via the SDK Mail API");

        // The integration is a self-describing @Component MessageHandler that forwards the entity JSON to
        // an external endpoint.
        String integration = contentOf("gen/events/PushOrderToWarehouseIntegration.java");
        assertTrue(integration.contains("class PushOrderToWarehouseIntegration implements MessageHandler"),
                "the integration should be a message-handling listener");
        assertTrue(integration.contains("@Component") && integration.contains("return \"intent-test-Order-Order\""),
                "an onCreate integration (self-describing @Component MessageHandler) should bind the entity's base topic via destination()");
        assertTrue(integration.contains("String url = Configurations.get(\"WAREHOUSE_URL\")"),
                "an @config: URL should resolve through the configuration at run time");
        assertTrue(
                integration.contains("HttpClient.post(url, Json.stringify(options))")
                        && integration.contains("options.put(\"text\", message)"),
                "a POST integration should forward the entity JSON as the request body");

        // The inbound webhook is a @Controller that ingests a posted JSON payload as the entity.
        String webhook = contentOf("gen/events/IngestOrderWebhook.java");
        assertTrue(webhook.contains("@Controller") && webhook.contains("class IngestOrderWebhook"),
                "the inbound webhook should be a @Controller");
        assertTrue(webhook.contains("@Post(\"/ingest\")"), "the webhook should expose the declared path");
        assertTrue(
                webhook.contains("OrderEntity entity = Json.parse(body, OrderEntity.class)")
                        && webhook.contains("new OrderRepository().save(entity)"),
                "the webhook should deserialize the payload and save it through the repository");

        // Rollups: two self-describing @Component MessageHandlers (child create/delete) that recompute the
        // parent counter via Criteria.
        // Together with the assertions above, this proves the full declarative-glue catalog - triggers,
        // resolvers, notifications, schedules, integrations, inbound webhooks and rollups - is generated
        // from a single app.intent.
        String rollupCreate = contentOf("gen/events/OrderCustomerRollupOnCreate.java");
        assertTrue(
                rollupCreate.contains("@Component") && rollupCreate.contains("return \"intent-test-Order-Order\"")
                        && rollupCreate.contains("new OrderRepository().findAll(Criteria.create().eq(\"Customer\", entity.Customer))")
                        && rollupCreate.contains("int count = rows.size();") && rollupCreate.contains("parent.OrderCount = count"),
                "the rollup create-listener should recompute the parent count via Criteria");
        assertTrue(contentOf("gen/events/OrderCustomerRollupOnDelete.java").contains("intent-test-Order-Order-deleted"),
                "the rollup delete-listener should bind the child's -deleted topic");

        // The print feeder (Order is a document master via the OrderItem composition child): a @Controller
        // that loads the document + its related graph through the repositories and returns the nested
        // { document, items } payload the .print template binds - exercises the generateUtils.js
        // "printFeeders" collection case end to end. This class IS the audit of what a print receives.
        assertTrue(contentOf("orders.glue").contains("\"printFeeders\""), "the glue should carry a printFeeders collection");
        String feeder = contentOf("gen/events/OrderPrintFeeder.java");
        assertTrue(feeder.contains("@Controller") && feeder.contains("class OrderPrintFeeder") && feeder.contains("@Get(\"/{id}\")"),
                "the feeder should be a @Controller exposing GET /{id}");
        assertTrue(feeder.contains("new gen.orders.data.order.OrderRepository().findById(id)"),
                "the feeder should load the document master through its generated repository");
        assertTrue(feeder.contains("document.put(\"Total\", root.Total)"), "the feeder should project the master's own fields");
        // A same-model relation (customer) is materialised as a nested object with __label so a bare
        // {{document.Customer}} still renders the label while {{document.Customer.<Field>}} descends.
        assertTrue(
                feeder.contains("new gen.orders.data.customer.CustomerRepository().findById(root.Customer)")
                        && feeder.contains("customerMap.put(\"__label\""),
                "the feeder should load a to-one relation and carry its label under __label");
        assertTrue(feeder.contains("document.put(\"Customer\", customerMap)"), "the relation node should be attached to the document map");
        assertTrue(feeder.contains("new gen.orders.data.order.OrderItemRepository().findAll(Criteria.create().eq(\"Order\", id))"),
                "the feeder should load the line items by the composition FK");
        assertTrue(feeder.contains("return Json.stringify(payload)"), "the feeder should return the { document, items } payload as JSON");
    }

    @Test
    void set_field_glue_sets_entity_status_on_approve_reject_branches() {
        // A MemberApproval process whose approve/reject decision routes to two setField service tasks:
        // approve -> status ACTIVE, reject -> status REJECTED. `next: done` on the activate branch makes
        // both branches converge on `done` instead of activate falling through into reject. The form
        // completes the task with the chosen action as a process variable the decision tests.
        String yaml = """
                name: members
                entities:
                  - name: Member
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: name,   type: string,  required: true, length: 100 }
                      - { name: status, type: string,  length: 20, defaultValue: "PENDING" }
                processes:
                  - name: MemberApproval
                    trigger: { onCreate: Member }
                    steps:
                      - { name: librarianReview, kind: userTask, args: { assignee: librarian, form: ApproveMember } }
                      - name: approved
                        kind: decision
                        args: { if: "action == 'approve'", then: activate, else: reject }
                      - { name: activate, kind: serviceTask, args: { setField: status, value: ACTIVE,   next: done } }
                      - { name: reject,   kind: serviceTask, args: { setField: status, value: REJECTED } }
                      - { name: done, kind: end }
                forms:
                  - { name: ApproveMember, forEntity: Member, fields: [name, status], actions: [approve, reject] }
                permissions:
                  - { role: Librarian, can: [Member:approve] }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));

        // BPMN: each setField serviceTask binds the generated JavaDelegate (NOT a custom/ stub), the
        // decision routes to both branches, and `next: done` makes activate skip past reject to the end.
        String bpmn = contentOf("MemberApproval.bpmn");
        assertTrue(bpmn.contains("<serviceTask id=\"activate\"") && bpmn.contains("gen.events.MemberApprovalActivate"),
                "the activate setField step should bind the generated JavaDelegate handler");
        assertTrue(bpmn.contains("<serviceTask id=\"reject\"") && bpmn.contains("gen.events.MemberApprovalReject"),
                "the reject setField step should bind its own generated handler");
        assertFalse(bpmn.contains("custom.Activate") || bpmn.contains("custom.Reject"),
                "a setField service task must not scaffold a custom/ stub");
        assertTrue(bpmn.contains("id=\"flow_approved_then\" sourceRef=\"approved\" targetRef=\"activate\""),
                "approve branch should route to the activate setter");
        assertTrue(bpmn.contains("id=\"flow_approved_default\" sourceRef=\"approved\" targetRef=\"reject\""),
                "reject branch should be the gateway default");
        assertTrue(bpmn.contains("sourceRef=\"activate\" targetRef=\"end\"") && bpmn.contains("sourceRef=\"reject\" targetRef=\"end\""),
                "both branches should converge on the end via `next` (activate must not fall through into reject)");
        assertTrue(bpmn.contains("${action == 'approve'}"), "the decision should test the form action variable");

        // Glue: a `setters` collection, one entry per setField step, carrying the field + literal value.
        String glue = contentOf("members.glue");
        assertTrue(glue.contains("\"setters\""), "the glue should carry a setters collection");
        assertTrue(
                glue.contains("\"className\": \"MemberApprovalActivate\"") && glue.contains("\"field\": \"Status\"")
                        && glue.contains("\"value\": \"ACTIVE\"") && glue.contains("\"keyProperty\": \"Id\""),
                "the activate setter should set the PascalCase field to its literal, loading by the PK property");
        assertTrue(glue.contains("\"className\": \"MemberApprovalReject\"") && glue.contains("\"value\": \"REJECTED\""),
                "the reject setter should carry its own value");

        // Run the glue-code template: each setter becomes a JavaDelegate that persists the field via
        // the TARGETED single-column updateProperty (only that column is in the UPDATE statement, so a
        // concurrent write to another column cannot be reverted), WITHOUT re-publishing an update event.
        generateFromModel("template-application-events-java/template/template.js", "members.glue");
        String activate = contentOf("gen/events/MemberApprovalActivate.java");
        assertTrue(activate.contains("class MemberApprovalActivate implements JavaDelegate"),
                "the setter should be generated as a Flowable JavaDelegate");
        assertTrue(activate.contains("import gen.members.data.member.MemberEntity") && activate.contains("execution.getVariable(\"Id\")"),
                "the setter should import the entity from its real Java package and read the PK process variable");
        assertTrue(activate.contains("repository.updateProperty(((Number) key).intValue(), \"Status\", \"ACTIVE\")"),
                "the setter should persist the field via the targeted single-column updateProperty");
        assertFalse(activate.contains("updateWithoutEvent"),
                "the setter must NOT full-row merge (updateWithoutEvent) - that reverts concurrent writes to other columns");
        assertTrue(contentOf("gen/events/MemberApprovalReject.java").contains("\"Status\", \"REJECTED\""),
                "the reject setter should persist the rejected status via the targeted write");
        // The transition IS observable: the setter publishes the dedicated -transitioned topic (the
        // status-reached channel for posting glue / integrations), which reactions never listen on -
        // so onUpdate reactions still do not re-fire, but a consumer can bind the transition. The
        // topic prefix is the PROJECT name (matching the DAO's create/-updated topics), not the
        // intent model name - here the IT's workspace project.
        // The publish is deferred to the END of the synchronous BPMN chain (after-commit): a service
        // task following the setter (a number-generation delegate) runs in the same Flowable command,
        // and the async consumer re-loads the source on receive - it must observe those writes.
        assertTrue(
                activate.contains("Process.executeAfterCommit(")
                        && activate.contains("Producer.sendToTopic(\"" + PROJECT + "-Member-Member-transitioned\", transitioned)"),
                "the setter should publish the -transitioned topic after the BPMN chain commits");
    }

    @Test
    void delegate_service_task_binds_a_client_java_delegate_via_flowable_class_with_injected_fields() {
        // A serviceTask with a `delegate` names an author-provided client JavaDelegate FQN. Unlike
        // setField/setRelationField (bound to a generated gen.events delegate through ${JavaTask}) or a
        // bare serviceTask (bound to a scaffolded custom.<Step> stub), a delegate is bound via
        // flowable:class so Flowable injects the declared `fields` into it. The delegate lives in the
        // document's OWN project (it manages the entity through its generated repository).
        String yaml = """
                name: invoicing
                entities:
                  - name: Invoice
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, length: 100 }
                processes:
                  - name: IssueInvoice
                    trigger: { onCreate: Invoice }
                    steps:
                      - { name: review, kind: userTask, args: { assignee: clerk, form: ReviewInvoice } }
                      - name: generateNumber
                        kind: serviceTask
                        args:
                          delegate: custom.invoicing.DocumentNumberGeneratorDelegate
                          fields: { type: "Sales Invoice" }
                          next: done
                      - { name: done, kind: end }
                forms:
                  - { name: ReviewInvoice, forEntity: Invoice, fields: [number], actions: [submit] }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));

        // BPMN: the delegate step binds flowable:class (NOT ${JavaTask}) and injects each field.
        String bpmn = contentOf("IssueInvoice.bpmn");
        assertTrue(
                bpmn.contains("<serviceTask id=\"generateNumber\" name=\"Generate Number\"")
                        && bpmn.contains("flowable:class=\"custom.invoicing.DocumentNumberGeneratorDelegate\""),
                "a delegate service task should bind flowable:class to the author-named client delegate");
        assertFalse(bpmn.contains("id=\"generateNumber\"") && bpmn.contains("flowable:delegateExpression=\"${JavaTask}\""),
                "a delegate service task must not fall back to the ${JavaTask} dispatcher");
        assertTrue(bpmn.contains("<flowable:field name=\"type\">") && bpmn.contains("<![CDATA[Sales Invoice]]>"),
                "each delegate field should be emitted as an injectable flowable:field");
        assertFalse(bpmn.contains("custom.GenerateNumber"), "a delegate service task must not scaffold a custom/ stub");
    }

    @Test
    void field_major_false_is_kept_off_the_list_via_widget_is_major() {
        // `major: false` on a field maps to the model's widgetIsMajor="false" so the entity list table
        // omits that column (the field is still shown in forms + the details pane). Default is true.
        String yaml = """
                name: catalog
                entities:
                  - name: Product
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: name,  type: string,  required: true, length: 100 }
                      - { name: notes, type: text,    major: false }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        String model = contentOf("catalog.model");
        assertTrue(model.contains("\"widgetIsMajor\": \"false\""),
                "a field with major: false should emit widgetIsMajor=false so it is excluded from the list table");
        assertTrue(model.contains("\"widgetIsMajor\": \"true\""), "fields default to major (widgetIsMajor=true)");
    }

    @Test
    void scheduled_generation_emits_a_create_from_job_not_a_mail_notification() {
        // A schedule with a `generate` action (scheduled record generation) runs the create-from mapping
        // per matching row on the cron tick: it builds a fresh target through the target's repository (so
        // numbering / status init / calculated fields fire), rather than sending mail. The queried row is
        // the source, so map/defaults render against the job's loop variable "entity".
        String yaml = """
                name: hr
                entities:
                  - name: Employee
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: name,   type: string }
                      - { name: status, type: string }
                  - name: EmployeeTimesheet
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: period, type: date }
                    relations:
                      - { name: Employee, kind: manyToOne, to: Employee }
                schedules:
                  - name: monthly-timesheets
                    cron: "0 0 1 1 * ?"
                    entity: Employee
                    where:
                      - { field: status, op: eq, value: ACTIVE }
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                      defaults:
                        Period: now
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "hr.glue");

        String job = contentOf("gen/events/MonthlyTimesheetsJob.java");
        assertTrue(job.contains("class MonthlyTimesheetsJob implements JobHandler"),
                "a hyphenated schedule name should still yield a valid Java class (pascalIdentifier)");
        assertTrue(job.contains("new EmployeeRepository().findAll("), "the job should query the source rows");
        assertTrue(job.contains(".eq(\"Status\", \"ACTIVE\")"), "the where filter should render as a typed criteria");
        assertTrue(job.contains("for (EmployeeEntity entity : rows)"), "the job should loop the matching rows");
        assertTrue(job.contains(".EmployeeTimesheetEntity target ="), "the job should build a fresh target per row");
        assertTrue(job.contains("target.Employee = entity.Id;"), "map copies the row's field onto the target property");
        assertTrue(job.contains("target.Period = java.time.LocalDate.now();"), "a `now` default renders as today's date");
        assertTrue(job.contains(".EmployeeTimesheetRepository().save(target);"),
                "the target is saved through its generated repository so create-time logic fires");
        assertFalse(job.contains("Mail.send"), "a generate schedule must not emit the notify (mail) path");
    }

    @Test
    void process_trigger_on_update_with_a_guard_generates_a_suffixed_guarded_listener() {
        String yaml = """
                name: shipping
                entities:
                  - name: Shipment
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: status, type: string }
                processes:
                  - name: Deliver
                    trigger: { onUpdate: Shipment, when: "status == 'SHIPPED'" }
                    steps:
                      - { name: handle, kind: serviceTask }
                      - { name: done,   kind: end }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "shipping.glue");

        String trigger = contentOf("gen/events/DeliverTrigger.java");
        assertTrue(trigger.contains("return \"intent-test-Shipment-Shipment-updated\""),
                "an onUpdate trigger should bind to the entity's -updated topic via destination()");
        assertTrue(trigger.contains("if (!(java.util.Objects.equals(entity.Status, \"SHIPPED\")))"),
                "the trigger should gate Process.start on the translated when-guard");
        assertTrue(trigger.contains("Process.start(\"Deliver\""), "the trigger should start the process when the guard holds");
    }

    @Test
    void rollup_generates_create_and_delete_listeners_that_recompute_the_parent_count() {
        String yaml = """
                name: library
                entities:
                  - name: Member
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: loanCount, type: integer }
                  - name: Loan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: member, kind: manyToOne, to: Member }
                rollups:
                  - name: memberLoanCount
                    entity: Loan
                    via: member
                    field: loanCount
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "library.glue");

        String onCreate = contentOf("gen/events/LoanMemberRollupOnCreate.java");
        assertTrue(onCreate.contains("class LoanMemberRollupOnCreate implements MessageHandler"),
                "the create-side rollup listener should be generated");
        assertTrue(onCreate.contains("@Component") && onCreate.contains("return \"intent-test-Loan-Loan\""),
                "the create listener (self-describing @Component) binds the child's base topic via destination()");
        assertTrue(onCreate.contains("MemberEntity parent = parents.findById(entity.Member)"),
                "it should load the parent via the child's FK");
        assertTrue(
                onCreate.contains("new LoanRepository().findAll(Criteria.create().eq(\"Member\", entity.Member))")
                        && onCreate.contains("int count = rows.size();") && onCreate.contains("parent.LoanCount = count"),
                "it should recompute the count via a typed Criteria and write it to the parent counter");

        String onDelete = contentOf("gen/events/LoanMemberRollupOnDelete.java");
        assertTrue(onDelete.contains("@Component") && onDelete.contains("return \"intent-test-Loan-Loan-deleted\""),
                "the delete listener binds the child's -deleted topic via destination()");
    }

    @Test
    void sum_rollup_with_capacity_maintains_balance_and_sets_status() {
        // A sum roll-up with `capacity` also keeps a `balance` field (= capacity - sum) and derives a
        // `status` relation: whenFull (>= capacity) / whenPartial (0 < sum < capacity). This is the
        // payment-settlement engine: Bill.paid = sum of its payments, Bill.balance = total - paid,
        // Bill.Status -> PAID / PARTIAL.
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
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "billing.glue");

        String onCreate = contentOf("gen/events/BillPaymentBillRollupOnCreate.java");
        assertTrue(onCreate.contains("parent.Paid = sum"), "the sum roll-up should write the summed field");
        assertTrue(onCreate.contains("parent.Balance = capacity.subtract(sum)"),
                "with a capacity + balance, it should keep balance = capacity - sum");
        assertTrue(onCreate.contains("parent.Status = sum.compareTo(capacity) >= 0 ? 2 : 1"),
                "with a capacity + status, it should set the status relation to whenFull/whenPartial at the thresholds");
    }

    @Test
    void settlement_generates_on_payment_listener_and_on_invoice_delegate() {
        // A settlement auto-allocates a Payment across a Customer's open Invoices (oldest first) via the
        // InvoicePayment junction: an onPayment MessageHandler (payment create) + an onInvoice
        // JavaDelegate (wired as a delegate: service task once the invoice is payable).
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
                      - { name: Status,   kind: manyToOne, to: InvoiceStatus }
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
                  - name: InvoiceStatus
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string,  required: true, length: 50 }
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
                      match: [Customer], status: Status, payableStatuses: [3, 4, 6] }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "settle.glue");

        String onPayment = contentOf("gen/events/AutoSettleOnPayment.java");
        assertTrue(onPayment.contains("class AutoSettleOnPayment implements MessageHandler"),
                "the onPayment settlement listener should be generated");
        assertTrue(onPayment.contains("PaymentEntity payment = Json.parse(message, PaymentEntity.class)"),
                "it should deserialize the created payment from the event");
        assertTrue(onPayment.contains(".eq(\"Customer\", payment.Customer)"), "it should match invoices on the shared Customer");
        assertTrue(onPayment.contains("s == 3 || s == 4 || s == 6"), "it should only allocate to invoices in a payable status");
        assertTrue(onPayment.contains("new InvoicePaymentRepository().save(row)"),
                "it should create allocation rows through the junction repository (never the generic Store)");

        String onInvoice = contentOf("gen/events/AutoSettleOnInvoice.java");
        assertTrue(onInvoice.contains("class AutoSettleOnInvoice implements JavaDelegate"),
                "the onInvoice settlement delegate should be generated");
        assertTrue(onInvoice.contains("new PaymentRepository().findAll") && onInvoice.contains(".eq(\"Customer\", invoice.Customer)"),
                "it should pull the customer's payments matching on the shared Customer");
    }

    @Test
    void process_trigger_business_key_uses_the_flagged_field_not_the_primary_key() {
        // The trigger flags `orderNumber` as the business key; the listener must still LOAD the entity
        // by its primary key (findById), but start the process with the flagged field as the BPM
        // business key - so it is correlatable by the domain identifier, not the surrogate id.
        String yaml = """
                name: orders
                entities:
                  - name: SalesOrder
                    fields:
                      - { name: id,          type: integer, primaryKey: true, generated: true }
                      - { name: orderNumber, type: string }
                processes:
                  - name: Approve
                    trigger: { onCreate: SalesOrder, businessKey: orderNumber }
                    steps:
                      - { name: review, kind: serviceTask }
                      - { name: done,   kind: end }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "orders.glue");

        String trigger = contentOf("gen/events/ApproveTrigger.java");
        assertTrue(trigger.contains("repository.findById(created.Id)"), "the listener must still load the entity by its primary key");
        assertTrue(trigger.contains("String businessKey = String.valueOf(entity.OrderNumber);"),
                "the BPM business key must be the flagged field (OrderNumber), not the primary key");
        assertTrue(trigger.contains("Process.start(\"Approve\", businessKey,"),
                "the started process should receive the resolved business key as the second argument");
    }

    @Test
    void process_trigger_business_key_strategy_timestamp_mints_and_persists_the_field() {
        // businessKeyStrategy: timestamp -> the listener mints a yyyyMMddHHmmss value into the (blank)
        // number field, persists it via the existing update, and uses it as the business key.
        String yaml = """
                name: orders
                entities:
                  - name: SalesOrder
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                processes:
                  - name: Approve
                    trigger: { onCreate: SalesOrder, businessKey: number, businessKeyStrategy: timestamp }
                    steps:
                      - { name: review, kind: serviceTask }
                      - { name: done,   kind: end }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "orders.glue");

        String trigger = contentOf("gen/events/ApproveTrigger.java");
        assertTrue(trigger.contains("repository.findById(created.Id)"), "the listener must still load the entity by its primary key");
        assertTrue(
                trigger.contains("if (entity.Number == null || entity.Number.isBlank())")
                        && trigger.contains("DateTimeFormatter.ofPattern(\"yyyyMMddHHmmss\")"),
                "a timestamp strategy should mint a yyyyMMddHHmmss value into the flagged field when blank");
        assertTrue(trigger.contains("String businessKey = String.valueOf(entity.Number);"),
                "the business key must be the minted number field");
        // Targeted single-column writes, never a full-row update: a stale snapshot merge would revert
        // concurrent writes (the ProcessId write-back race). No event either - a system write.
        assertTrue(trigger.contains("repository.updateProperty(entity.Id, \"ProcessId\", processId)"),
                "ProcessId must be persisted via a targeted single-column update, not a full-row merge");
        assertTrue(trigger.contains("repository.updateProperty(entity.Id, \"Number\", minted)"),
                "the minted number must be persisted via its own targeted single-column update");
        assertFalse(trigger.contains("updateWithoutEvent"),
                "the trigger must not merge its stale full-row snapshot back (the ProcessId write-back race)");
    }

    @Test
    void service_task_handler_stub_is_scaffolded_under_custom_and_preserved() {
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // notifyCustomer has no `call`, so a Java JavaDelegate stub is scaffolded under custom/.
        assertTrue(resource("custom/NotifyCustomer.java").exists(), "a no-call service task should scaffold a custom/ Java stub");
        String stub = contentOf("custom/NotifyCustomer.java");
        assertTrue(stub.contains("package custom;") && stub.contains("class NotifyCustomer implements JavaDelegate"),
                "the stub should be a custom-package JavaDelegate");
        assertTrue(stub.contains("OrderApproval: notifyCustomer service task executed."), "the stub should log a default message");

        // The developer implements it; regeneration must NOT overwrite it.
        writeProjectFile("custom/NotifyCustomer.java", """
                package custom;
                import org.flowable.engine.delegate.DelegateExecution;
                import org.flowable.engine.delegate.JavaDelegate;
                public class NotifyCustomer implements JavaDelegate {
                    public void execute(DelegateExecution execution) { /* MY IMPLEMENTATION */ }
                }
                """);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        assertTrue(contentOf("custom/NotifyCustomer.java").contains("MY IMPLEMENTATION"),
                "the developer's service-task handler must be preserved across regeneration");
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
    void harmonia_form_page_generates_the_depends_on_runtime() {
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // The Harmonia full-stack template renders the dependsOn attributes into Order's document page
        // (Order has the header-items layout): an Alpine watcher on the trigger plus one
        // applyDependsOn method per dependent header property.
        generateFromModel("template-application-ui-harmonia-java/template/template.js", "orders.model");
        String documentPage = contentOf("gen/orders/js/components/pages/Order/OrderDocumentPage.js");
        assertTrue(documentPage.contains("$watch('form.Customer'"), "the document page should watch the trigger property");
        assertTrue(documentPage.contains("applyDependsOnCountry"), "the Country dropdown should get a dependsOn refresh method");
        assertTrue(documentPage.contains("applyDependsOnCreditSnapshot"), "the creditSnapshot scalar should get an auto-populate method");
        assertTrue(documentPage.contains("conditions: [{ propertyName: 'Id', operator: 'EQ'"),
                "the dropdown refresh should POST the /search EQ filter on the defaulted filterBy");
        assertTrue(documentPage.contains("CustomerController/' + encodeURIComponent(value)"),
                "the trigger's selected record should be loaded from its own controller URL");
        // The generic item-dialog machinery is model-independent but must be present for line items.
        assertTrue(documentPage.contains("applyDraftDependsOn") && documentPage.contains("dialogOptionsFor"),
                "the item dialog should carry the metadata-driven dependsOn machinery");
    }

    @Test
    void report_widget_generates_the_kpi_block_and_replaces_entity_tiles() {
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // The .report carries the resolved widget block: authored expressions became column aliases,
        // the `now` token stays symbolic (resolved client-side, type-aware via the bucket).
        String monthly = contentOf("OrdersByMonth.report");
        assertTrue(monthly.contains("\"kind\": \"value\""), "the value widget should carry its kind");
        assertTrue(monthly.contains("\"valueColumn\": \"Sum Total\""), "value should resolve to the measure column's alias");
        assertTrue(monthly.contains("\"valueType\": \"DECIMAL\""), "the value column type should ride along");
        assertTrue(monthly.contains("\"label\": \"Revenue (this month)\""), "the widget label should be carried");
        assertTrue(monthly.contains("\"bucket\": \"month\""), "a month(x) pin should carry its bucket kind");
        assertTrue(monthly.contains("\"token\": \"now\""), "the now pin should stay a symbolic token");
        assertTrue(monthly.contains("\"column\": \"Month Order Date\""), "the pin should resolve to the dimension column's alias");
        String bigItems = contentOf("BigOrderItems.report");
        assertTrue(bigItems.contains("\"kind\": \"count\""), "the count widget should carry its kind");
        assertTrue(bigItems.contains("\"icon\": \"alert-triangle\""), "the widget icon should be carried");

        // The .model root carries the custom widgets. (The per-entity count tiles are now suppressed
        // by the shell template itself when widgets are declared - the old `dashboardKpis` flag was
        // dropped in #6136 - so that suppression is asserted on the generated dashboard below.)
        String model = contentOf("orders.model");
        assertTrue(model.contains("\"widgetSystemHealth\""), "the custom kpi widget should land on the .model root with its tId");
        assertTrue(model.contains("\"kind\": \"page\""), "the custom page widget should carry its kind");

        generateFromModel("template-application-ui-harmonia-java/template/template.js", "orders.model");
        String dashboard = contentOf("gen/orders/js/components/pages/dashboardPage.js");
        // Per-entity count tiles were removed entirely in #6136 (the dashboard no longer bakes an
        // `entities` array); "replaces entity tiles" is now verified by the absence of any baked
        // per-entity count endpoint.
        assertFalse(dashboard.contains("apiPath: '/"), "no entity count tile should be baked when widgets are declared");
        assertTrue(dashboard.contains("loadKpis"), "the dashboard should carry the KPI loading machinery");
        assertTrue(dashboard.contains("loadWidgetValue"), "the KPI tiles should delegate to the reports store's widget fetch");
        // Custom widgets are baked into the page: the kpi fetches its endpoint, the page is iframed.
        assertTrue(dashboard.contains("url: '/services/js/orders/custom/health.js'"),
                "the custom kpi widget's endpoint should be baked into the dashboard");
        assertTrue(dashboard.contains("kind: 'page'") && dashboard.contains("url: '/services/web/orders/custom/funnel.html'"),
                "the custom page widget should be baked with its url");
        assertTrue(dashboard.contains("tkey: '" + PROJECT + ":orders-model.t.widgetSystemHealth'"),
                "the custom widget label should carry the model-catalog translation key");
        // ... and its label lands in the model translation catalog.
        String modelCatalog = contentOf("i18n/en-US/orders.model.json");
        assertTrue(modelCatalog.contains("\"widgetSystemHealth\": \"System Health\""),
                "the custom widget's label should land in the model catalog");

        // The report-file template also emits the report's label catalog (report + columns + the
        // widget's tile label) under the '<Name>-report' translation prefix.
        String reportPayload =
                "{\"template\":\"template-application-ui-harmonia-java/template/template-report-file.js\",\"parameters\":{}}";
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(reportPayload)
                                                 .when()
                                                 .post("/services/js/service-generate/generate.mjs/model/" + WORKSPACE + "/" + PROJECT
                                                         + "?path=OrdersByMonth.report")
                                                 .then()
                                                 .statusCode(201));
        String catalog = contentOf("i18n/en-US/OrdersByMonth.report.json");
        assertTrue(catalog.contains("\"OrdersByMonth-report\""), "the catalog should be keyed by the report translation prefix");
        assertTrue(catalog.contains("\"widgetOrdersByMonth\": \"Revenue (this month)\""),
                "the KPI widget's tile label should land in the report catalog");
        assertTrue(catalog.contains("\"OrdersByMonth\": \"Orders By Month\""), "the report label should land in the catalog");
    }

    @Test
    void expansion_generates_the_span_handlers_and_the_status_badge_stack() {
        // A non-document master with an EntityStatus badge, a date-function calculated field and a
        // month expansion spreading the principal across generated installments.
        String loanYaml =
                """
                        name: loans
                        entities:
                          - name: LoanStatus
                            kind: setting
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: name, type: string, required: true, length: 100 }
                          - name: Loan
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: name, type: string, required: true, length: 100 }
                              - { name: startDate, type: date, required: true }
                              - { name: endDate, type: date, required: true }
                              - { name: principal, type: decimal, required: true }
                              - { name: months, type: decimal, scale: 0, readOnly: true, calculatedOnCreate: "monthsBetween(StartDate, EndDate)", calculatedOnUpdate: "monthsBetween(StartDate, EndDate)" }
                              - { name: periods, type: integer, readOnly: true }
                            relations:
                              - { name: Status, kind: manyToOne, to: LoanStatus, function: EntityStatus, init: 1 }
                          - name: LoanInstallment
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: dueDate, type: date }
                              - { name: amount, type: decimal }
                            relations:
                              - { name: Loan, kind: manyToOne, to: Loan, composition: true, required: true }
                        expansions:
                          - name: installments
                            from: Loan
                            into: LoanInstallment
                            unit: month
                            between: { start: startDate, end: endDate }
                            map: { dueDate: period }
                            spread: { total: principal, into: amount, round: 2 }
                            count: periods
                        """;
        writeIntent(loanYaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));

        // The glue carries the two per-event expansion handlers with the pre-rendered Java pieces.
        String glue = contentOf("loans.glue");
        assertTrue(glue.contains("\"expansions\""), "the .glue should carry the expansions collection");
        assertTrue(glue.contains("InstallmentsExpansionOnCreate"), "an OnCreate handler entry is expected");
        assertTrue(glue.contains("InstallmentsExpansionOnUpdate"), "an OnUpdate handler entry is expected");

        // The EntityStatus relation lands as the DOCUMENT_STATUS widget on a NON-document entity.
        String model = contentOf("loans.model");
        assertTrue(model.contains("\"widgetType\": \"DOCUMENT_STATUS\""), "the EntityStatus FK should carry the status widget type");

        // Events template: the generated handler owns the child set, spreads with a last-row
        // remainder and writes the count back via a TARGETED single-column updateProperty (only the
        // count column is in the UPDATE statement, so the stale message copy of the master cannot
        // revert concurrent writes to other columns, and no event fires).
        generateFromModel("template-application-events-java/template/template.js", "loans.glue");
        String onCreate = contentOf("gen/events/InstallmentsExpansionOnCreate.java");
        assertTrue(onCreate.contains("intent-test-Loan-Loan\""), "the OnCreate handler binds the master's create topic");
        assertTrue(onCreate.contains("d.plusMonths(1)"), "unit month steps by month");
        assertTrue(onCreate.contains("total.subtract(share.multiply("), "the last row absorbs the rounding remainder");
        assertTrue(onCreate.contains("new LoanRepository().updateProperty(master.Id, \"Periods\", Integer.valueOf(periods.size()))"),
                "the count write-back must be a targeted single-column updateProperty");
        assertFalse(onCreate.contains("updateWithoutEvent"),
                "the count write-back must not full-row merge (updateWithoutEvent) - that reverts concurrent writes to other columns");
        String onUpdate = contentOf("gen/events/InstallmentsExpansionOnUpdate.java");
        assertTrue(onUpdate.contains("intent-test-Loan-Loan-updated\""), "the OnUpdate handler binds the -updated topic");

        // Harmonia UI: the status renders as the title-bar badge (not an editable input) and the
        // calculated field previews live via the calc evaluator with the date functions.
        generateFromModel("template-application-ui-harmonia-java/template/template.js", "loans.model");
        String formView = contentOf("gen/loans/views/Loan/Loan-form.html");
        assertTrue(formView.contains("statusVariant(statusText())"), "the form should render the status badge");
        assertFalse(formView.contains("f_Status"), "the status must not render as an editable input");
        String formJs = contentOf("gen/loans/js/components/pages/Loan/LoanFormPage.js");
        assertTrue(formJs.contains("harmoniaCalcEval"), "the form should carry the live calc evaluator");
        assertTrue(formJs.contains("monthsBetween"), "the evaluator should include the date functions");
        assertTrue(formJs.contains("recalcCalculated"), "the form should recompute calculated fields live");
    }

    @Test
    void month_and_week_fields_generate_the_harmonia_pickers() {
        // month (YYYY-MM) and week (YYYY-Www) are stored as VARCHAR strings; the widget is chosen from
        // the logical type, and the Harmonia form renders the dedicated pickers rather than plain inputs.
        String yaml = """
                name: planning
                entities:
                  - name: Plan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 100 }
                      - { name: period, type: month }
                      - { name: sprint, type: week }
                """;
        writeIntent(yaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));

        // The model carries the picker widget types (both columns are VARCHAR under the hood).
        String model = contentOf("planning.model");
        assertTrue(model.contains("\"widgetType\": \"MONTH\""), "the month field must carry the MONTH widget type");
        assertTrue(model.contains("\"widgetType\": \"WEEK\""), "the week field must carry the WEEK widget type");

        // The Harmonia form renders the real pickers, not plain <input type="month|week">.
        generateFromModel("template-application-ui-harmonia-java/template/template.js", "planning.model");
        String formView = contentOf("gen/planning/views/Plan/Plan-form.html");
        assertTrue(formView.contains("x-h-month-picker"), "the month field must render the Harmonia month picker");
        assertTrue(formView.contains("x-h-week-picker"), "the week field must render the Harmonia week picker");
        assertFalse(formView.contains("type=\"month\""), "the plain native month input must be gone");
        assertFalse(formView.contains("type=\"week\""), "the plain native week input must be gone");
    }

    @Test
    void postings_generates_the_idempotent_resumable_handler() {
        // A self-contained posting: an Order transitioning into POSTED (status 2) posts a Ledger with
        // two LedgerLine rows (debit + credit) determined by a PostingRule.
        String postingYaml = """
                name: postingtest
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                  - name: OrderStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 100 }
                  - name: PostingRule
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: documentType, type: string }
                    relations:
                      - { name: DebitAccount, kind: manyToOne, to: Account }
                      - { name: CreditAccount, kind: manyToOne, to: Account }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: amount, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
                  - name: Ledger
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: memo, type: string, length: 400 }
                    relations:
                      - { name: Order, kind: manyToOne, to: Order }
                  - name: LedgerLine
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal, precision: 18, scale: 2 }
                      - { name: credit, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Ledger, kind: manyToOne, to: Ledger, composition: true, required: true }
                      - { name: Account, kind: manyToOne, to: Account, required: true }
                postings:
                  - name: orderLedger
                    event: { onTransition: Order, when: "Status == 2" }
                    creates: Ledger
                    backReference: Order
                    map: { memo: "Order {number}" }
                    rule: { entity: PostingRule, match: { documentType: "Order" } }
                    items:
                      - { Account: rule(debitAccount), debit: "Amount" }
                      - { Account: rule(creditAccount), credit: "Amount" }
                """;
        writeIntent(postingYaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));

        // The glue carries the posting handler entry with the pre-rendered pieces.
        String glue = contentOf("postingtest.glue");
        assertTrue(glue.contains("\"postings\""), "the .glue should carry the postings collection");
        assertTrue(glue.contains("OrderLedger"), "the posting className should be carried in the glue");

        // Events template: the generated handler is idempotent + resumable (the cloud-native posting
        // semantics - no cross-step transaction): it skips a complete post and rebuilds a half-post.
        generateFromModel("template-application-events-java/template/template.js", "postingtest.glue");
        String posting = contentOf("gen/events/OrderLedgerPosting.java");
        assertTrue(posting.contains("implements MessageHandler"), "the posting is a self-describing message handler");
        assertTrue(posting.contains("-transitioned"), "it listens on the source's -transitioned channel");
        assertTrue(posting.contains("int expectedItems = 0"), "it computes the expected item count for the completeness check");
        assertTrue(posting.contains("existingTargets"), "it looks up an existing post by the back-reference (idempotency)");
        assertTrue(posting.contains("currentItems.size() >= expectedItems"), "a complete post is a no-op (idempotent)");
        assertTrue(posting.contains("itemsRepository.delete(stale)"), "a half-post rebuilds its items (resumable)");
    }

    @Test
    void generates_completion_hook_flips_the_source_via_targeted_update() {
        // A create-from with a sourceStatus completion hook: after the Invoice is created, the Proforma
        // flips to status 3 - via a TARGETED single-column write (updateProperty), never a full-row
        // merge of the stale pre-generation snapshot (which would clobber concurrent writes to the source).
        String genYaml = """
                name: proforma
                entities:
                  - name: ProformaStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 100 }
                  - name: Proforma
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                    relations:
                      - { name: Status, kind: manyToOne, to: ProformaStatus, function: EntityStatus, init: 1 }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                generates:
                  - name: invoice-from-proforma
                    from: Proforma
                    to: Invoice
                    forEntity: Proforma
                    sourceStatus: 3
                """;
        writeIntent(genYaml);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));

        generateFromModel("template-application-events-java/template/template.js", "proforma.glue");
        String generate = contentOf("gen/events/InvoiceFromProformaGenerate.java");
        // The completion hook flips the source status via the targeted single-column primitive...
        assertTrue(generate.contains("updateProperty(req.id, \"Status\", 3)"),
                "the source status must be flipped with the targeted updateProperty write");
        // ...and reloads before publishing so the -transitioned payload is the committed row...
        assertTrue(generate.contains("findById(req.id)"), "it should reload the source for the -transitioned payload");
        assertTrue(generate.contains("-transitioned"), "it should publish the source's -transitioned channel");
        // ...NOT the full-row merge that would revert a concurrent write to the source row (the actual
        // call pattern; an explanatory code comment naming it is expected and must not trip this).
        assertFalse(generate.contains("Repository().updateWithoutEvent(source)"),
                "the source flip must NOT go through a full-row updateWithoutEvent (stale-snapshot clobber)");
    }

    @Test
    void multilingual_entity_generates_the_translation_stack() {
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-ui-harmonia-java/template/template.js", "orders.model");

        // Schema: the multilingual Country gets its sibling language table with the codbex shape.
        String schema = contentOf("gen/orders/schema/" + PROJECT + ".schema");
        assertTrue(schema.contains("ORDERS_COUNTRY_LANG"), "the schema should declare the <TABLE>_LANG table");
        assertTrue(schema.contains("\"name\": \"Language\""), "the language table should carry the Language column");
        assertTrue(schema.contains("\"name\": \"GUID\""), "the language table should carry the GUID primary key");
        assertFalse(schema.contains("ORDERS_CUSTOMER_LANG"), "a non-multilingual entity must not get a language table");

        // Java DAO: every read overlays the translations for the caller's Accept-Language.
        String repository = contentOf("gen/orders/data/settings/CountryRepository.java");
        assertTrue(repository.contains("Translator.translateList(super.findAll(), User.getLanguage(), \"ORDERS_COUNTRY\")"),
                "the multilingual repository should overlay translations on findAll");
        assertTrue(repository.contains("Translator.translateEntity(super.findById(id)"),
                "the multilingual repository should overlay translations on findById");
        assertTrue(repository.contains("public java.util.Optional<CountryEntity> findOne(Object id)"),
                "the multilingual repository must also override findOne - the generated controller reads single records through it");
        String customerRepository = contentOf("gen/orders/data/customer/CustomerRepository.java");
        assertFalse(customerRepository.contains("Translator."), "a non-multilingual repository must stay untouched");

        // Shell config: the offered data languages feed the Region & Language setting.
        String config = contentOf("gen/orders/js/config.js");
        assertTrue(config.contains("languages: [\"en\",\"bg\"]"), "config.js should carry the app's data languages");
    }

    @Test
    void report_file_stack_generates_typed_column_filters() {
        writeIntent(INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // Replay the Harmonia report-file template like the editor does. The generation service
        // derives the gen folder from the report file name (each report owns gen/<lowercased name>).
        String payload = "{\"template\":\"template-application-ui-harmonia-java/template/template-report-file.js\",\"parameters\":{}}";
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(payload)
                                                 .when()
                                                 .post("/services/js/service-generate/generate.mjs/model/" + WORKSPACE + "/" + PROJECT
                                                         + "?path=OrdersByCustomer.report")
                                                 .then()
                                                 .statusCode(201));

        // Backend: the report repository validates and applies per-column conditions over the wrapped
        // query, typed from the report's own column metadata.
        String repository = contentOf("gen/ordersbycustomer/data/reports/OrdersByCustomerRepository.java");
        assertTrue(repository.contains("FILTER_COLUMNS"), "the report repository should carry the filterable-column allowlist");
        assertTrue(repository.contains("SELECT * FROM (\").append(QUERY).append(\") AS \\\"REPORT_DATA\\\" WHERE"),
                "conditions should wrap the report query");
        assertTrue(repository.contains("SELECT COUNT(*) AS \\\"REPORT_COUNT\\\" FROM ("),
                "the count alias must be quoted - PostgreSQL folds an unquoted alias to lower case and the case-sensitive read misses it");
        assertTrue(repository.contains("\"GTE\", \">=\""), "range operators should be whitelisted");
        String controller = contentOf("gen/ordersbycustomer/api/reports/OrdersByCustomerController.java");
        assertTrue(controller.contains("exportCsv(@Body Map<String, Object> filter)"), "export should honor the active filters");

        // Frontend: the generated report page carries typed column metadata and the filter machinery.
        // NB the case split: the UI files use the RAW genFolderName (the report file name,
        // "OrdersByCustomer"), while the Java files use the sanitized javaGenFolderName
        // ("ordersbycustomer") - two distinct folders on a case-sensitive filesystem.
        String page = contentOf("gen/OrdersByCustomer/reports/OrdersByCustomer/report.js");
        assertTrue(page.contains("reportColumns"), "the report page should embed the typed column metadata");
        assertTrue(page.contains("{ key: 'Customer', kind: 'text', align: 'left'"),
                "the joined dimension should be a left-aligned text column");
        assertTrue(page.contains("kind: 'number'"), "the aggregate measures should be number columns");
        assertTrue(page.contains("operator: 'GTE'") && page.contains("operator: 'LIKE'"),
                "the page should build range and contains conditions");
        String view = contentOf("gen/OrdersByCustomer/reports/OrdersByCustomer/index.html");
        assertTrue(view.contains("applyFilters()") && view.contains("data-lucide=\"filter\""),
                "the report view should carry the filter panel and toolbar toggle");
        assertTrue(view.contains("alignClass(col)") && view.contains("cellText(col, row)"),
                "the report table should align and format cells from the column metadata");
        assertTrue(page.contains("align: 'right'"), "decimal measures should be right-aligned");
        assertTrue(page.contains("pattern: '### ### ### ##0.00'"), "the page metadata should carry the money pattern for decimal columns");
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

    @Test
    void calculated_field_action_emits_an_imports_backed_callout_in_the_repository() {
        // A field can be computed server-side by a hand-written CalculatedField action instead of a
        // neutral expression; the owning entity declares the Java import so the generated repository
        // references the action by simple name (the implementation is hand-added under custom/).
        writeIntent("""
                name: invoicing
                entities:
                  - name: Invoice
                    imports: |
                      import custom.invoicing.InvoiceNumberAction;
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string,  length: 100, calculatedActionOnCreate: InvoiceNumberAction }
                      - { name: total,  type: decimal, precision: 18, scale: 2 }
                """);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        // The .model carries the action attribute on the property and the Base64-encoded imports on the
        // entity (the same importsCode the EDM editor's Imports tab produces).
        String model = contentOf("invoicing.model");
        assertTrue(model.contains("\"calculatedActionOnCreate\": \"InvoiceNumberAction\""),
                "the model property should carry the calculated-action class");
        assertTrue(model.contains("\"importsCode\""), "the model entity should carry the (Base64) custom imports");

        // The Java DAO template injects the imports and emits the action call-out
        // (Beans.get(...).calculate).
        generateFromModel("template-application-dao-java/template/template.js", "invoicing.model");
        String repository = contentOf("gen/invoicing/data/invoice/InvoiceRepository.java");
        assertTrue(repository.contains("import custom.invoicing.InvoiceNumberAction;"),
                "the entity Imports should be injected into the generated repository");
        assertTrue(repository.contains("import org.eclipse.dirigible.sdk.component.Beans;"),
                "Beans should be imported for the action call-out");
        assertTrue(repository.contains("entity.Number = Beans.get(InvoiceNumberAction.class).calculate(entity);"),
                "the calculated field should be assigned by calling the action via Beans");
    }

    @Test
    void editable_task_form_fields_are_coerced_to_their_java_type_on_write_back() {
        // A BPM task form opts fields back to editable; on completion the generated Writer persists them,
        // coercing each from its process variable to the entity's Java type
        // (date/timestamp/number/boolean),
        // not a raw toString. A single-action form needs no decision, so the rule doesn't apply here.
        writeIntent("""
                name: orders
                entities:
                  - name: SalesOrder
                    fields:
                      - { name: id,        type: integer,  primaryKey: true, generated: true }
                      - { name: shippedOn, type: date }
                      - { name: shippedAt, type: timestamp }
                      - { name: quantity,  type: integer }
                      - { name: approved,  type: boolean }
                processes:
                  - name: Approve
                    trigger: { onCreate: SalesOrder }
                    steps:
                      - { name: review, kind: userTask, args: { assignee: approver, form: ReviewOrder } }
                      - { name: done,   kind: end }
                forms:
                  - name: ReviewOrder
                    forEntity: SalesOrder
                    fields: [shippedOn, shippedAt, quantity, approved]
                    editable: [shippedOn, shippedAt, quantity, approved]
                    actions: [approve]
                """);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(GENERATE_URL)
                                                 .then()
                                                 .statusCode(200));
        generateFromModel("template-application-events-java/template/template.js", "orders.glue");

        String writer = contentOf("gen/events/ApproveReviewWrite.java");
        assertTrue(writer.contains("class ApproveReviewWrite implements JavaDelegate"),
                "a user task with editable fields should generate a Writer JavaDelegate");
        assertTrue(writer.contains("values.put(\"ShippedOn\", java.time.LocalDate.parse(ShippedOnValue.toString().trim()));"),
                "a date editable should be coerced with LocalDate.parse");
        assertTrue(writer.contains("values.put(\"ShippedAt\", java.time.Instant.parse(ShippedAtValue.toString().trim()));"),
                "a timestamp editable should be coerced with Instant.parse");
        assertTrue(writer.contains("((Number) QuantityValue).intValue()"), "an integer editable should be coerced to int");
        assertTrue(writer.contains("Boolean.valueOf(ApprovedValue.toString().trim())"),
                "a boolean editable should be coerced with Boolean.valueOf");
        assertTrue(writer.contains("repository.updateProperties(((Number) key).intValue(), values)"),
                "the writer must persist the edited columns in one targeted multi-column write");
        assertFalse(writer.contains("updateWithoutEvent"),
                "the writer must NOT full-row merge (updateWithoutEvent) - that reverts concurrent writes to unedited columns");
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
        assertTrue(edmXml.contains("isReadOnlyProperty=\"true\""),
                "system fields (ProcessId, audit columns) should be flagged read-only so forms render them in the read-only details block");

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
        // Country is declared `kind: setting`, so it is emitted as a SETTING entity (the template engine
        // routes it under the dashboard's Settings menu) instead of a top-level PRIMARY entity.
        assertTrue(modelBody.contains("\"type\": \"SETTING\""), "a setting entity should be emitted with type SETTING");
        assertTrue(edmXml.contains("entityType=\"SETTING\""), "the EDM mxGraph cell should mark the setting entity");
        // A relation that targets a setting entity points its dropdown at the global Settings perspective.
        assertTrue(modelBody.contains("\"relationshipEntityPerspectiveName\": \"Settings\""),
                "a relation targeting a setting entity should resolve to the Settings perspective");

        // Depends-On: Order.Country reacts to Order.Customer (valueFrom the customer's Country FK,
        // filterBy defaulting to the target's PK), and the creditSnapshot scalar auto-populates from
        // the customer's creditLimit (no filterBy on a scalar).
        assertTrue(edmXml.contains("widgetDependsOnProperty=\"Customer\""), "a dependsOn dependent should carry the trigger property name");
        assertTrue(edmXml.contains("widgetDependsOnEntity=\"Customer\""), "a dependsOn dependent should carry the trigger's target entity");
        assertTrue(edmXml.contains("widgetDependsOnValueFrom=\"Country\""),
                "the cascade should read the customer's Country FK (PascalCased from valueFrom: country)");
        assertTrue(edmXml.contains("widgetDependsOnFilterBy=\"Id\""), "filterBy should default to the dependent's own target primary key");
        assertTrue(edmXml.contains("widgetDependsOnValueFrom=\"CreditLimit\""),
                "the scalar auto-populate should read the customer's creditLimit");
        assertTrue(
                modelBody.contains("\"widgetDependsOnProperty\": \"Customer\"")
                        && modelBody.contains("\"widgetDependsOnValueFrom\": \"CreditLimit\""),
                "the .model JSON twin should carry the widgetDependsOn* attributes");

        // Multilingual: Country carries the EDM multilingual attribute (its translations live in
        // ORDERS_COUNTRY_LANG) and the intent's data languages land on the .model root.
        assertTrue(edmXml.contains("multilingual=\"true\""), "a multilingual entity should carry the EDM multilingual attribute");
        assertTrue(modelBody.contains("\"multilingual\": \"true\""), "the .model twin should carry the multilingual attribute");
        assertTrue(modelBody.contains("\"languages\"") && modelBody.contains("\"bg\""),
                "the intent's languages should land on the .model root");
    }

    private void assertGlue() {
        assertTrue(resource("orders.glue").exists(), "the .glue file should be generated");
        String glue = contentOf("orders.glue");
        // Triggers: one per onCreate process trigger.
        assertTrue(
                glue.contains("\"triggers\"") && glue.contains("\"process\": \"OrderApproval\"") && glue.contains("\"entity\": \"Order\""),
                "glue should carry the trigger for the OrderApproval process on Order");
        // Resolvers: one per relation.field referenced in a decision OR a user-task form.
        assertTrue(glue.contains("\"resolvers\"") && glue.contains("\"handler\": \"ResolveCustomerCreditLimit\""),
                "glue should carry the customer.creditLimit resolver (used by both the form and the decision)");
        assertTrue(glue.contains("\"handler\": \"ResolveCustomerName\"") && glue.contains("\"variable\": \"customer_name\""),
                "glue should carry the form-only customer.name resolver");
        assertTrue(
                glue.contains("\"fkProperty\": \"Customer\"") && glue.contains("\"targetEntity\": \"Customer\"")
                        && glue.contains("\"targetField\": \"CreditLimit\"") && glue.contains("\"variable\": \"customer_creditLimit\""),
                "the resolver should carry the FK property, target entity/field and the resolved variable");
        // Notifications: one per declarative notification, carrying the rendered Java expressions.
        assertTrue(
                glue.contains("\"notifications\"") && glue.contains("\"name\": \"orderUpdated\"")
                        && glue.contains("\"topicSuffix\": \"-updated\""),
                "glue should carry the orderUpdated notification bound to the -updated topic");
        assertTrue(glue.contains("\"toExpression\": \"\\\"ops@example.com\\\"\""),
                "glue should carry the notification recipient as a Java string expression");
        // Schedules: one per declarative schedule, carrying the cron + the typed Criteria expression.
        assertTrue(
                glue.contains("\"schedules\"") && glue.contains("\"name\": \"staleOrders\"") && glue.contains("\"cron\": \"0 0 9 * * ?\""),
                "glue should carry the staleOrders schedule with its cron");
        assertTrue(glue.contains("Criteria.create().lt(\\\"OrderDate\\\", java.time.LocalDate.now())"),
                "glue should carry the schedule's typed Criteria expression");
        // Integrations: one per outbound integration, carrying the HTTP method + URL expression.
        assertTrue(glue.contains("\"integrations\"") && glue.contains("\"name\": \"pushOrderToWarehouse\"")
                && glue.contains("\"clientMethod\": \"post\""), "glue should carry the pushOrderToWarehouse integration as a POST");
        assertTrue(glue.contains("Configurations.get(\\\"WAREHOUSE_URL\\\")"),
                "glue should carry the integration URL as a config lookup expression");
        // Inbound: one per webhook, carrying the path + the entity to create.
        assertTrue(glue.contains("\"inbound\"") && glue.contains("\"name\": \"ingestOrder\"") && glue.contains("\"path\": \"/ingest\""),
                "glue should carry the ingestOrder inbound webhook with its path");
        // Rollups: the two recompute listeners for the customerOrderCount counter.
        assertTrue(glue.contains("\"rollups\"") && glue.contains("\"className\": \"OrderCustomerRollupOnCreate\"")
                && glue.contains("\"countField\": \"OrderCount\""), "glue should carry the customerOrderCount rollup listeners");
    }

    private void assertAppTestManifest() {
        assertTrue(resource("orders.test").exists(), "the .test app-test manifest should be generated");
        String manifest = contentOf("orders.test");
        // module-level coordinates: module id + the sanitized REST base + standalone shell + id property
        assertTrue(manifest.contains("\"module\": \"orders\""), "the manifest names the module");
        assertTrue(manifest.contains("\"restBase\": \"/services/java/" + PROJECT + "/gen/orders/api\""),
                "the manifest carries the sanitized REST base");
        assertTrue(manifest.contains("\"standaloneShell\": \"/services/web/" + PROJECT + "/gen/orders/index.html\""),
                "the manifest carries the standalone shell URL");
        assertTrue(manifest.contains("\"idProperty\": \"Id\""), "the manifest carries the id property");
        // the document master renders as a document layout; the composition detail child is excluded
        assertTrue(manifest.contains("\"name\": \"Order\"") && manifest.contains("\"layout\": \"document\""),
                "the Order document master should be a document layout");
        assertFalse(manifest.contains("\"name\": \"OrderItem\""), "the composition detail child should be excluded");
        // a plain entity is a manage-list with its controller + route
        assertTrue(manifest.contains("\"name\": \"Customer\"") && manifest.contains("CustomerController")
                && manifest.contains("\"#/Customer\""), "the Customer entity should carry its controller api and route");
        // the multilingual setting entity is flagged
        assertTrue(manifest.contains("\"name\": \"Country\"") && manifest.contains("\"multilingual\": true"),
                "the multilingual Country entity should be flagged");
    }

    private void assertSettings() {
        assertTrue(resource("orders.settings").exists(), "the .settings file should be scaffolded");
        String settings = contentOf("orders.settings");
        assertTrue(settings.contains("\"generation\"") && settings.contains("template-application-ui-harmonia-java"),
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
        // keyProperty is unique to trigger entries (other glue blocks don't emit it), so its absence
        // means the opted-out trigger was not generated - other glue (notifications, etc.) still uses
        // "entity".
        assertFalse(glue.contains("\"keyProperty\""), "an opted-out trigger must not appear in the glue (no trigger entries)");
        // The resolver was not opted out, so it survives.
        assertTrue(glue.contains("\"handler\": \"ResolveCustomerCreditLimit\""), "a non-opted-out resolver should still be generated");
        // The developer's settings file is preserved verbatim, not overwritten by the scaffold.
        assertTrue(contentOf("orders.settings").contains("\"generate\": false"),
                "the edited settings must be preserved across regeneration");
    }

    private void assertBpmn() {
        String body = contentOf("OrderApproval.bpmn");
        // The process keeps its compact id but gets a human-readable name; tasks likewise.
        assertTrue(body.contains("<process id=\"OrderApproval\" name=\"Order Approval\""),
                "the process should keep its id but carry a humanized name");
        assertTrue(body.contains("<userTask id=\"managerReview\" name=\"Manager Review\""),
                "BPMN should map managerReview to a userTask with a humanized name");
        // The assignee "manager" resolves to the declared role "Manager" (case-insensitive); the
        // settings' candidateGroupsExtra (ADMINISTRATOR by default) is appended so an admin can claim.
        assertTrue(body.contains("flowable:candidateGroups=\"Manager,ADMINISTRATOR\""),
                "the userTask candidateGroups must be the resolved role plus the settings' extra groups");
        assertFalse(body.contains("flowable:candidateGroups=\"manager\""), "the candidate group must not keep the raw lower-case assignee");
        // The form key must be the served form-page URL so the Inbox "Open Form" navigates to the page
        // (a bare name resolves relative to the inbox and 404s).
        assertTrue(body.contains("flowable:formKey=\"/services/web/intent-test/gen/ApproveOrder/forms/ApproveOrder/index.html\""),
                "the userTask formKey must be the generated form page URL");
        assertTrue(body.contains("<exclusiveGateway id=\"bigOrder\" name=\"Big Order\""),
                "BPMN should map the decision step to an exclusiveGateway with a humanized name");
        // A service task with no `call` binds to a generated Java handler under custom/.
        assertTrue(
                body.contains("<serviceTask id=\"notifyCustomer\" name=\"Notify Customer\"")
                        && body.contains("<![CDATA[custom.NotifyCustomer]]>"),
                "a no-call service task should bind to ${JavaTask} -> custom.<Step>");
        assertTrue(body.contains("id=\"flow_bigOrder_then\" sourceRef=\"bigOrder\" targetRef=\"cfoReview\""),
                "the conditioned flow should target the `then` step");
        assertTrue(body.contains("id=\"flow_bigOrder_default\" sourceRef=\"bigOrder\" targetRef=\"notifyCustomer\""),
                "the gateway default flow should target the `else` step so small orders skip CFO review");
        // customer.creditLimit is referenced by BOTH the managerReview user-task form and the bigOrder
        // decision; customer.name only by the form. Each relation.field gets a JavaTask resolver inserted
        // before the EARLIEST step that needs it - here the managerReview form - so the form fields are
        // populated and the later decision still tests the (process-global, already-resolved) variable.
        // The resolver task id is the lower-camel form of the handler (unified with the authored step
        // ids), with a humanized name; the delegate still resolves the PascalCase handler class.
        assertTrue(
                body.contains("<serviceTask id=\"resolveCustomerCreditLimit\" name=\"Resolve Customer Credit Limit\"")
                        && body.contains("flowable:delegateExpression=\"${JavaTask}\""),
                "a JavaTask resolver service task should be generated for the shared relation.field");
        assertTrue(body.contains("gen.events.ResolveCustomerCreditLimit") && body.contains("gen.events.ResolveCustomerName"),
                "both the shared and the form-only relation.field should produce a resolver task pointing at its handler FQN");
        assertTrue(
                body.contains("sourceRef=\"start\" targetRef=\"resolveCustomerCreditLimit\"")
                        && body.contains("sourceRef=\"resolveCustomerName\" targetRef=\"managerReview\""),
                "the resolvers should sit at the head of the flow, right before the user-task form that needs them");
        assertTrue(body.contains("sourceRef=\"managerReview\" targetRef=\"bigOrder\""),
                "the decision should follow the form directly - its resolver already ran before the form, not just before the gateway");
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
        // A relation.field form field (customer.creditLimit / customer.name) binds to the resolver-set
        // process variable (<relation>_<field>, NOT a PascalCase property), is typed from the TARGET
        // entity's field (creditLimit is decimal -> input-number), is read-only, and is labelled by the
        // humanized path. The matching resolver step is asserted in assertBpmn / the glue test.
        assertTrue(body.contains("\"model\": \"customer_creditLimit\""),
                "a relation.field control should bind to the resolver-set process variable, not a PascalCase property");
        assertTrue(body.contains("\"model\": \"customer_name\""), "a form-only relation.field should also bind to its resolver variable");
        assertTrue(body.contains("\"label\": \"Customer Credit Limit\""), "a relation.field label should be the humanized path");
        assertTrue(body.contains("\"readonly\": true"),
                "a relation.field control should be read-only (resolved related data, not editable)");
        assertTrue(body.contains("\"type\": \"positive\""), "form should mark the approve button as positive");
        assertTrue(body.contains("onApproveClicked"), "form code should declare the approve handler");
        // The action handler must complete the BPM task, not be a no-op stub. (The .form code is
        // HTML-escaped by Gson - ' becomes \\u0027, = becomes \\u003d - so match escape-free substrings;
        // the form-builder un-escapes the code when it injects it into the controller.)
        assertTrue(body.contains("__completeTask("), "the action buttons should complete the task");
        assertTrue(body.contains("/services/inbox/tasks/") && body.contains("COMPLETE"),
                "the form should complete the task via the per-task permission-checked Inbox endpoint");
        assertFalse(body.contains("/services/bpm/bpm-processes/tasks/"),
                "the form must not use the role-guarded BPM endpoint, which would block candidate-group users");
        assertTrue(body.contains("closeWindow(") && body.contains("window.close("),
                "on completion the form should close its host (dialog via closeWindow, standalone via window.close)");
        assertFalse(body.contains("TODO: wire"), "the action handlers must no longer be TODO stubs");
    }

    private void assertReport() {
        String body = contentOf("OrdersByCustomer.report");
        assertTrue(body.contains("\"name\": \"OrdersByCustomer\""), "report should carry its declared name");
        assertTrue(body.contains("\"alias\": \"Order\""), "report alias should be the source entity");
        assertTrue(body.contains("\"table\": \"ORDERS_ORDER\""),
                "report table should be the same intent-prefixed table name the EDM declares as dataName");
        assertTrue(body.contains("\"aggregate\": \"COUNT\""), "count(*) should be parsed into an aggregate COUNT column");

        // month(field) buckets the date dimension into a sortable YYYYMM integer, grouped the same way.
        String monthly = contentOf("OrdersByMonth.report");
        assertTrue(
                monthly.contains(
                        "(EXTRACT(YEAR FROM Order.\\\"ORDER_ORDER_DATE\\\") * 100 + EXTRACT(MONTH FROM Order.\\\"ORDER_ORDER_DATE\\\"))"),
                "a month(field) dimension should emit the YYYYMM EXTRACT expression");
        assertTrue(monthly.contains("as \\\"Month Order Date\\\""), "the bucketed column should carry a humanized alias");
        assertTrue(monthly.contains("GROUP BY (EXTRACT(YEAR"), "the aggregation should group by the bucket expression");

        // Rendering metadata on the model: numeric columns right-align, decimals carry the money pattern.
        assertTrue(body.contains("\"align\": \"right\""), "numeric report columns should carry align: right");
        assertTrue(body.contains("\"pattern\": \"### ### ### ##0.00\""), "decimal report columns should carry the money pattern");
        assertTrue(body.contains("\"aggregate\": \"SUM\""), "sum(total) should be parsed into an aggregate SUM column");
        // The query is materialised SQL (not left empty): SELECT ... FROM <table> as <alias> ... GROUP BY.
        // Physical table/column identifiers are double-quoted so the SQL runs on PostgreSQL (which folds
        // unquoted identifiers to lower case); aliases stay unquoted.
        // The quotes are escaped (\") inside the JSON .report file's query string, so match that form.
        assertTrue(body.contains("SELECT ") && body.contains("FROM \\\"ORDERS_ORDER\\\" as Order") && body.contains("GROUP BY"),
                "report query should be a materialised SQL statement with GROUP BY, not empty");
        assertTrue(body.contains("SUM(Order.\\\"ORDER_TOTAL\\\")"), "sum(total) should aggregate the quoted, qualified ORDER_TOTAL column");
        assertTrue(body.contains("\"roleRead\":"), "report should carry default-role read security");
        // A bare to-one relation dimension (customer) joins the related table and shows its name field,
        // grouping by the name - not the raw FK id.
        assertTrue(
                body.contains(
                        "INNER JOIN \\\"ORDERS_CUSTOMER\\\" as Customer ON Order.\\\"ORDER_CUSTOMER\\\" = Customer.\\\"CUSTOMER_ID\\\""),
                "a bare relation dimension (customer) should INNER JOIN the related entity with quoted identifiers");
        assertTrue(body.contains("SELECT Customer.\\\"CUSTOMER_NAME\\\" as") && body.contains("GROUP BY Customer.\\\"CUSTOMER_NAME\\\""),
                "the bare relation dimension should select + group by the related entity's name, not its FK id");

        // A relation.field dimension joins the related table; the filter becomes a qualified WHERE.
        String joined = contentOf("BigOrderItems.report");
        assertTrue(
                joined.contains("INNER JOIN \\\"ORDERS_ORDER\\\" as Order ON OrderItem.\\\"ORDER_ITEM_ORDER\\\" = Order.\\\"ORDER_ID\\\""),
                "a relation.field dimension (order.orderDate) should INNER JOIN the related entity on its FK");
        assertTrue(joined.contains("WHERE OrderItem.\\\"ORDER_ITEM_QUANTITY\\\" > 1"),
                "the intent filter should become a WHERE with the field rewritten to its quoted, qualified column");

        // kind: balance - the six windowed totals around the runtime :fromDate/:toDate parameters,
        // declared on the .report in the editor's {name, type, initial} shape with all-time defaults.
        String balance = contentOf("OrderBalance.report");
        assertTrue(balance.contains("\"kind\": \"balance\""), "the balance report should carry its kind");
        assertTrue(balance.contains(
                "SUM(CASE WHEN Order.\\\"ORDER_ORDER_DATE\\\" < :fromDate THEN COALESCE(Order.\\\"ORDER_TOTAL\\\", 0) ELSE 0 END) as \\\"Opening Debit\\\""),
                "the opening debit should sum the debit amount strictly before :fromDate");
        assertTrue(balance.contains(
                "SUM(CASE WHEN Order.\\\"ORDER_ORDER_DATE\\\" >= :fromDate AND Order.\\\"ORDER_ORDER_DATE\\\" <= :toDate THEN COALESCE(Order.\\\"ORDER_CREDIT_SNAPSHOT\\\", 0) ELSE 0 END) as \\\"Credit\\\""),
                "the period credit should sum the credit amount inside the inclusive window");
        assertTrue(balance.contains(
                "SUM(CASE WHEN Order.\\\"ORDER_ORDER_DATE\\\" <= :toDate THEN COALESCE(Order.\\\"ORDER_TOTAL\\\", 0) ELSE 0 END) as \\\"Closing Debit\\\""),
                "the closing debit should sum everything up to and including :toDate");
        assertTrue(balance.contains("\"name\": \"fromDate\"") && balance.contains("\"name\": \"toDate\""),
                "the balance report should declare the two window parameters");
        assertTrue(balance.contains("\"initial\": \"1900-01-01\"") && balance.contains("\"initial\": \"9999-12-31\""),
                "the window parameters should default to the all-time balance");
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

        // The bg translation seed lands in the language table with the codbex _LANG shape.
        String langCsvim = contentOf("countries-bg.csvim");
        assertTrue(langCsvim.contains("\"table\": \"ORDERS_COUNTRY_LANG\""), "a language seed should target the <TABLE>_LANG table");
        String langCsv = contentOf("countries-bg.csv");
        assertTrue(langCsv.startsWith("GUID,Id,Name,Language"),
                "the language csv should carry GUID + Id + the referenced PascalCase translatable columns + Language");
        assertTrue(langCsv.contains("1,1,Афганистан,bg"), "the language csv should carry the translation rows with auto-numbered GUIDs");

        // A file seed (large authored data set) generates ONLY the .csvim, pointing at the
        // developer-owned CSV in its subfolder; no CSV body is generated (and none is scrubbed).
        String fileCsvim = contentOf("countries-extra.csvim");
        assertTrue(fileCsvim.contains("\"file\": \"/" + PROJECT + "/data/countries-extra.csv\""),
                "a file seed's csvim should point at the authored CSV");
        assertTrue(fileCsvim.contains("\"table\": \"ORDERS_COUNTRY\""), "a file seed still targets the entity's table");
        assertFalse(resource("countries-extra.csv").exists(), "a file seed must not generate a CSV body");
    }

    @AfterEach
    void removeProject() {
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
