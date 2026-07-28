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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;

import io.restassured.path.json.JsonPath;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * dirigible #6422: an owner module retiring a field must not invalidate the already-generated code of
 * the modules that reference it cross-model.
 *
 * <p>
 * The shape that used to break: the consumer's generated print feeder named every property of the
 * owner's entity ({@code invoiceMap.put("CustomerEmail", invoice.CustomerEmail)}) - a snapshot of
 * ANOTHER model's schema, baked into this module's compiled code. The owner then retired the field
 * and regenerated cleanly (its own pass is green); the consumer was never regenerated, because the
 * consumer did not change. The next client-Java pass failed on {@code cannot find symbol}, and since
 * that batch is all-or-nothing it took every module's beans down with it - not just the consumer's.
 *
 * <p>
 * The assertions walk the three layers the emission contract asks for:
 * <ol>
 * <li><b>Emission</b> - the consumer's feeder names no field of the owner and copies the record
 * reflectively instead.</li>
 * <li><b>Report</b> - regenerating the owner AFTER the removal warns, naming the consumer project as
 * the regeneration set (nothing else would have said so before javac did).</li>
 * <li><b>Runtime, the outermost layer</b> - with the owner regenerated WITHOUT the field and the
 * consumer deliberately NOT regenerated, the whole instance still compiles: the consumer's controller
 * answers, and its print feeder serves a payload carrying the owner's CURRENT fields (no
 * {@code CustomerEmail}, and the surviving ones present).</li>
 * </ol>
 */
class IntentCrossModelFieldRetirementIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /** Owns the referenced model. Its project name differs from the model alias on purpose. */
    private static final String OWNER = "retire-owner";
    /** References the owner's entity, and is never regenerated after the removal. */
    private static final String CONSUMER = "retire-consumer";

    private static final String OWNER_MODEL = "invoices";
    private static final String CONSUMER_MODEL = "journal";

    /**
     * The owner module. {@code customerEmail} is present in the first wave and retired in the second -
     * the whole scenario.
     */
    private static String ownerIntent(boolean withCustomerEmail) {
        return """
                name: invoices
                description: cross-model field retirement fixture - the owner of the referenced entity

                entities:
                  - name: Invoice
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string,  required: true, length: 100 }
                %s
                  - name: InvoiceItem
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 100 }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                """.formatted(withCustomerEmail ? "      - { name: customerEmail, type: string, length: 200 }" : "");
    }

    /** The consumer module: a document whose print feeder dereferences the owner's Invoice. */
    private static final String CONSUMER_INTENT = """
            name: journal
            description: cross-model field retirement fixture - the consumer that is never regenerated

            uses:
              - { model: invoices, project: retire-owner }

            entities:
              - name: JournalEntry
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string,  required: true, length: 100 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, model: invoices }
              - name: JournalEntryItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void retiring_an_owner_field_neither_breaks_nor_silently_invalidates_its_cross_model_consumers() {
        writeIntent(OWNER, ownerIntent(true));
        generateProject(OWNER);
        writeIntent(CONSUMER, CONSUMER_INTENT);
        generateProject(CONSUMER);

        assertConsumerNamesNoOwnerField();

        // Retire the field and regenerate ONLY the owner - exactly what happened in #6422. The consumer
        // is left with the generated code it already had.
        writeIntent(OWNER, ownerIntent(false));
        assertRemovalNamesTheRegenerationSet(generateProject(OWNER));

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        assertConsumerStillLive();
    }

    /**
     * Layer 1: the consumer's feeder must not name a single field of the owner - that is the coupling
     * that goes stale. The record it loads is copied as it is at runtime.
     */
    private void assertConsumerNamesNoOwnerField() {
        String feeder = contentOf(CONSUMER, "gen/events/" + CONSUMER_MODEL + "/JournalEntryPrintFeeder.java");

        assertTrue(feeder.contains("copyRecordFields(invoice, invoiceMap)"),
                "a cross-model record is copied reflectively, so the owner's schema is read at runtime; feeder was:\n" + feeder);
        assertFalse(feeder.contains("invoice.CustomerEmail"),
                "the owner's field must NOT be named here - that is the reference that stops compiling when the owner retires it");
        assertFalse(feeder.contains("invoice.Number"), "no field of the owner is named, not even one that happens to survive");

        // The same-model half is unchanged: this project's own fields ARE named, line by line.
        assertTrue(feeder.contains("document.put(\"Number\", root.Number)"), "the document's own fields stay explicit (the audit trail)");
    }

    /**
     * Layer 2: the owner's pass reports the removal AND the projects it invalidates. Before this, the
     * owner's generation was green and nothing named the consumer until javac did, in another project.
     */
    private void assertRemovalNamesTheRegenerationSet(List<String> warnings) {
        String reported = String.join("\n", warnings);
        assertTrue(reported.contains("[Invoice.CustomerEmail]"), "the retired member is named; got: " + reported);
        assertTrue(reported.contains(CONSUMER), "the consumer project is named as the regeneration set; got: " + reported);
    }

    /**
     * Layer 3 - the promise: the instance is live with an owner that no longer has the field and a
     * consumer that was never regenerated. If the consumer's generated code still dereferenced the
     * retired field, the all-or-nothing client-Java batch would have failed and NO controller below
     * would answer.
     */
    private void assertConsumerStillLive() {
        String ownerApi = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
        String consumerApi = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";

        AtomicInteger invoiceId = new AtomicInteger();
        restAssuredExecutor.execute(() -> invoiceId.set(given().contentType("application/json")
                                                               .body("{\"Number\":\"INV-1\"}")
                                                               .when()
                                                               .post(ownerApi + "/invoice/InvoiceController")
                                                               .then()
                                                               .statusCode(200)
                                                               .extract()
                                                               .path("Id")),
                60);

        AtomicInteger entryId = new AtomicInteger();
        restAssuredExecutor.execute(() -> entryId.set(given().contentType("application/json")
                                                             .body("{\"Number\":\"JE-1\",\"Invoice\":" + invoiceId.get() + "}")
                                                             .when()
                                                             .post(consumerApi + "/journalentry/JournalEntryController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .path("Id")));

        // The print payload carries the owner's CURRENT fields: the retired one is simply gone, the
        // surviving ones are there - with no regeneration of this project. (The feeder answers
        // text/plain, so the body is asserted as a string rather than through a content-type parser.)
        AtomicReference<String> payload = new AtomicReference<>();
        restAssuredExecutor.execute(() -> payload.set(given().when()
                                                             .get("/services/java/" + CONSUMER + "/gen/events/" + CONSUMER_MODEL
                                                                     + "/JournalEntryPrintFeeder/" + entryId.get())
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .asString()));

        JsonPath document = JsonPath.from(payload.get());
        assertEquals("JE-1", document.getString("document.Number"));
        assertEquals("INV-1", document.getString("document.Invoice.Number"), "a surviving field of the owner is still projected");
        assertEquals("INV-1", document.getString("document.Invoice.__label"), "the label resolves through the copied map");
        assertNotNull(document.get("document.Invoice.Id"));
        assertFalse(payload.get()
                           .contains("CustomerEmail"),
                "the retired field is simply absent - it did not have to break anything to disappear; payload: " + payload.get());
    }

    /**
     * Write the intent, generate the model files and drive model-to-code from the generate response's
     * own plan.
     *
     * @param project the workspace project
     * @return the non-fatal warnings the generate pass reported
     */
    private List<String> generateProject(String project) {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        AtomicReference<List<String>> warnings = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            var response = given().when()
                                  .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + project + "&path=app.intent")
                                  .then()
                                  .statusCode(200)
                                  .extract()
                                  .jsonPath();
            plan.set(response.getList("codeGenerations"));
            warnings.set(response.getList("warnings"));
        });
        for (Map<String, Object> codeGeneration : plan.get()) {
            String template = String.valueOf(codeGeneration.get("templateId"));
            String modelPath = String.valueOf(codeGeneration.get("path"));
            String parameters = new Gson().toJson(codeGeneration.get("parameters"));
            String payload = "{\"template\":\"" + template + "\",\"parameters\":" + parameters + "}";
            restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                     .body(payload)
                                                     .when()
                                                     .post("/services/js/service-generate/generate.mjs/model/" + WORKSPACE + "/" + project
                                                             + "?path=" + modelPath)
                                                     .then()
                                                     .statusCode(201));
        }
        return warnings.get() == null ? List.of() : warnings.get();
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
        for (String project : List.of(CONSUMER, OWNER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(greaterThanOrEqualTo(200)));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
