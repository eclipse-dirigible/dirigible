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
                                                 .body("reports", hasSize(2))
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
                                                                 "OrdersByCustomer.report", "orders.roles", "orders.glue",
                                                                 "countries.csvim", "countries.csv", "Order.print"))
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
        String rollupCreate = contentOf("gen/events/CustomerOrderCountRollupOnCreate.java");
        assertTrue(
                rollupCreate.contains("@Component") && rollupCreate.contains("return \"intent-test-Order-Order\"")
                        && rollupCreate.contains(
                                "new OrderRepository().findAll(Criteria.create().eq(\"Customer\", entity.Customer)).size()")
                        && rollupCreate.contains("parent.OrderCount = count"),
                "the rollup create-listener should recompute the parent count via Criteria");
        assertTrue(contentOf("gen/events/CustomerOrderCountRollupOnDelete.java").contains("intent-test-Order-Order-deleted"),
                "the rollup delete-listener should bind the child's -deleted topic");
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

        // Run the glue-code template: each setter becomes a JavaDelegate that loads the entity by id,
        // assigns the field, and persists WITHOUT re-publishing an update event.
        generateFromModel("template-application-events-java/template/template.js", "members.glue");
        String activate = contentOf("gen/events/MemberApprovalActivate.java");
        assertTrue(activate.contains("class MemberApprovalActivate implements JavaDelegate"),
                "the setter should be generated as a Flowable JavaDelegate");
        assertTrue(activate.contains("import gen.members.data.member.MemberEntity") && activate.contains("execution.getVariable(\"Id\")"),
                "the setter should import the entity from its real Java package and load it by the PK process variable");
        assertTrue(activate.contains("entity.Status = \"ACTIVE\"") && activate.contains("repository.updateWithoutEvent(entity)"),
                "the setter should assign the field and persist without re-firing an update event");
        assertTrue(contentOf("gen/events/MemberApprovalReject.java").contains("entity.Status = \"REJECTED\""),
                "the reject setter should assign the rejected status");
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

        String onCreate = contentOf("gen/events/MemberLoanCountRollupOnCreate.java");
        assertTrue(onCreate.contains("class MemberLoanCountRollupOnCreate implements MessageHandler"),
                "the create-side rollup listener should be generated");
        assertTrue(onCreate.contains("@Component") && onCreate.contains("return \"intent-test-Loan-Loan\""),
                "the create listener (self-describing @Component) binds the child's base topic via destination()");
        assertTrue(onCreate.contains("MemberEntity parent = parents.findById(entity.Member)"),
                "it should load the parent via the child's FK");
        assertTrue(
                onCreate.contains("new LoanRepository().findAll(Criteria.create().eq(\"Member\", entity.Member)).size()")
                        && onCreate.contains("parent.LoanCount = count"),
                "it should recompute the count via a typed Criteria and write it to the parent counter");

        String onDelete = contentOf("gen/events/MemberLoanCountRollupOnDelete.java");
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

        String onCreate = contentOf("gen/events/BillPaidRollupOnCreate.java");
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
        assertTrue(trigger.contains("repository.updateWithoutEvent(entity)"),
                "the minted number and ProcessId must be persisted via the silent update (no spurious -updated event)");
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
        assertTrue(repository.contains("\"GTE\", \">=\""), "range operators should be whitelisted");
        String controller = contentOf("gen/ordersbycustomer/api/reports/OrdersByCustomerController.java");
        assertTrue(controller.contains("exportCsv(@Body Map<String, Object> filter)"), "export should honor the active filters");

        // Frontend: the generated report page carries typed column metadata and the filter machinery.
        String page = contentOf("gen/ordersbycustomer/reports/OrdersByCustomer/report.js");
        assertTrue(page.contains("reportColumns"), "the report page should embed the typed column metadata");
        assertTrue(page.contains("{ key: 'Customer', kind: 'text' }"), "the joined dimension should be a text column");
        assertTrue(page.contains("kind: 'number'"), "the aggregate measures should be number columns");
        assertTrue(page.contains("operator: 'GTE'") && page.contains("operator: 'LIKE'"),
                "the page should build range and contains conditions");
        String view = contentOf("gen/ordersbycustomer/reports/OrdersByCustomer/index.html");
        assertTrue(view.contains("applyFilters()") && view.contains("data-lucide=\"filter\""),
                "the report view should carry the filter panel and toolbar toggle");
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
        assertTrue(writer.contains("entity.ShippedOn = java.time.LocalDate.parse(ShippedOnValue.toString().trim());"),
                "a date editable should be coerced with LocalDate.parse");
        assertTrue(writer.contains("entity.ShippedAt = java.time.Instant.parse(ShippedAtValue.toString().trim());"),
                "a timestamp editable should be coerced with Instant.parse");
        assertTrue(writer.contains("((Number) QuantityValue).intValue()"), "an integer editable should be coerced to int");
        assertTrue(writer.contains("Boolean.valueOf(ApprovedValue.toString().trim())"),
                "a boolean editable should be coerced with Boolean.valueOf");
        assertTrue(writer.contains("repository.updateWithoutEvent(entity)"), "the writer must persist without re-firing an update event");
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
        assertTrue(glue.contains("\"rollups\"") && glue.contains("\"className\": \"CustomerOrderCountRollupOnCreate\"")
                && glue.contains("\"countField\": \"OrderCount\""), "glue should carry the customerOrderCount rollup listeners");
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
