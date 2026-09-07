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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A task form showing a field of a CROSS-MODEL to-one (dirigible #7093): the Send form of a sales
 * invoice shows the recipient address, so the clerk sees an empty one before pressing Send.
 *
 * <p>
 * The one relation a billing document's form most needs to read a field of - its counterparty - was
 * the one the form validator refused, because it resolved the hop against LOCAL entities only,
 * while the same one-hop path already resolved cross-model as a {@code notify} recipient. It is now
 * resolved where every other cross-model reference is: at generation, against the owner model's
 * {@code .model}.
 *
 * <p>
 * Two modules: {@code xmfcustomers} owns the customer, {@code xmfinvoices} {@code uses:} it and
 * owns the invoice, the process and the form. The assertions walk out to the layer that matters -
 * the generated resolver names the OWNER's package, the form control binds the variable it
 * publishes, and the whole instance still COMPILES (a guessed package would take the client-Java
 * batch down). The loud path is covered too: a field the owner model does not declare is a 422, not
 * a silently skipped resolver behind a control that stays empty forever.
 */
// One Dirigible boot for the whole class: each method cleans up after itself, so the per-method
// context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCrossModelFormFieldIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String OWNER = "xmfcustomers";
    private static final String CONSUMER = "xmfinvoices";

    /** The owner module: it owns the counterparty and therefore its address. */
    private static final String OWNER_INTENT = """
            name: xmfcustomers
            description: cross-model form field fixture - owns the customer

            entities:
              - name: Customer
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, length: 100 }
            """;

    /** The consumer module: the invoice, the Send flow and the form that shows the address. */
    private static String consumerIntent(String path) {
        return """
                name: xmfinvoices
                description: cross-model form field fixture - the Send form shows the customer's address

                uses:
                  - { model: xmfcustomers }

                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, length: 32 }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, model: xmfcustomers, required: true }

                processes:
                  - name: SendFlow
                    trigger: { onCreate: SalesInvoice }
                    steps:
                      - { name: send, kind: userTask, args: { assignee: clerk, form: SendSalesInvoice } }
                      - { name: sent, kind: end }

                forms:
                  - name: SendSalesInvoice
                    forEntity: SalesInvoice
                    fields: [number, Customer, %s]
                    actions: [send]
                """.formatted(path);
    }

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_form_field_is_resolved_from_the_owner_model_and_compiles() {
        generateProject(OWNER, OWNER_INTENT);
        generateProject(CONSUMER, consumerIntent("Customer.email"));

        // The resolver behind the control loads the OWNER's record. Its package is the one fact only the
        // owner model carries; a guessed one fails the whole client-Java batch.
        String resolver = contentOf(CONSUMER, "gen/events/xmfinvoices/ResolveCustomerEmail.java");
        assertTrue(resolver.contains("import gen.xmfcustomers.data.customer.CustomerEntity;"),
                "the related record is the OWNER's entity: " + resolver);
        assertTrue(resolver.contains("import gen.xmfcustomers.data.customer.CustomerRepository;"),
                "it is loaded through the OWNER's repository: " + resolver);
        assertTrue(resolver.contains("execution.setVariable(\"Customer_email\", entity.Email);"),
                "the field must be published as the variable the form control binds to: " + resolver);

        // The step that needs it, and the control that shows it.
        assertTrue(contentOf(CONSUMER, "SendFlow.bpmn").contains("ResolveCustomerEmail"),
                "the resolver must be inserted before the user task");
        String form = contentOf(CONSUMER, "SendSalesInvoice.form");
        assertTrue(form.contains("\"Customer_email\""), "the form control must bind the resolved variable: " + form);

        // The outermost layer: it compiles. Resolving the owner's facts exists for a resolver that builds.
        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();
        String ours = "findAll { it.category == 'Compilation' && it.location.startsWith('/" + CONSUMER + "/') }";
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/ide/problems")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(ours + ".size()", equalTo(0)),
                60);
    }

    @Test
    void a_field_the_owner_model_does_not_declare_is_refused() {
        // Skipping it would leave the BPMN with a service task pointing at a handler nothing generated
        // and the control bound to a variable nothing ever sets - the silence this feature removes.
        generateProject(OWNER, OWNER_INTENT);
        writeIntent(CONSUMER, consumerIntent("Customer.mobile"));
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + CONSUMER
                                                         + "&path=app.intent")
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues.toString()",
                                                         both(containsString("Mobile")).and(containsString("xmfcustomers"))));
    }

    /** Write the intent and drive model-to-code, asserting each recipe of the one generate call. */
    private void generateProject(String project, String yaml) {
        writeIntent(project, yaml);
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent(String project, String yaml) {
        String path = projectPath(project) + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String contentOf(String project, String fileName) {
        return new String(repository.getResource(projectPath(project) + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(OWNER, CONSUMER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
