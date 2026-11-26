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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * The handler of an event works in the language of the user whose write raised it (#8776).
 *
 * <p>
 * The handler runs off the source's topic, on a broker thread that serves no request, so a catalog
 * message it resolved came out in the default text and a multilingual row it read in its base
 * value, whatever language the writer reads. The outbox now records the language with the event,
 * beside the user (#8228), the platform's producer stamps it on the message, and the listener binds
 * it for the handler.
 *
 * <p>
 * The handler here is hand-written client Java: on a created payment it reads the payment's
 * (multilingual) country and refuses through {@code CheckMessages} with the message of the remark's
 * generated check, then records both texts in a remark of its own. Both deliveries are walked: the
 * in-process one right after a write sent in Bulgarian (and, as the control, one sent without a
 * language), and the relay's redelivery of an entry left in the outbox, as a broker outage would
 * have left one.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentEventLanguageIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "eventlang";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String PAYMENTS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/payment/PaymentController";
    private static final String REMARKS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/remark/RemarkController";
    /** What the generated repository publishes a created payment on. */
    private static final String PAYMENT_CREATED_TOPIC = PROJECT + "-Payment-Payment";
    private static final String OUTBOX_TABLE = "DIRIGIBLE_EVENT_OUTBOX";
    private static final long TIMEOUT_SECONDS = 90;
    /** The handler runs off the source's topic, after the commit. */
    private static final long HANDLER_TIMEOUT_SECONDS = 30;
    /** A payment only the relay delivers: its stranded event names it, no row needs to exist. */
    private static final int RELAYED_PAYMENT = 900001;
    private static final int BULGARIA = 3;

    private static final String INTENT = """
            name: eventlang
            description: an event's handler works in the language of the write that raised it
            languages: [en, bg]

            entities:
              - name: Country
                kind: setting
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }

              - name: Remark
                checks:
                  - { kind: compare, field: amount, op: ge, value: 0, message: "A remark amount is never negative" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: paymentId, type: integer }
                  - { name: amount,    type: decimal, precision: 18, scale: 2 }
                  - { name: text,      type: string,  length: 500 }

            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 1, name: Afghanistan }
                  - { id: 2, name: Albania }
                  - { id: 3, name: Bulgaria }
              - name: countries-bg
                entity: Country
                language: bg
                rows:
                  - { id: 1, name: "Афганистан" }
                  - { id: 2, name: "Албания" }
                  - { id: 3, name: "България" }
            """;

    /**
     * A translator's overlay of the check's message, under the key the generated en-US catalog writes.
     */
    private static final String BG_OVERLAY = """
            {
              "eventlang-model": {
                "checks": {
                  "Remark_compare_0": "Сумата на бележката не може да е отрицателна"
                }
              }
            }
            """;

    /**
     * The handler: a refusal with the message of the remark's generated check (whose key the generated
     * en-US catalog carries and the overlay translates), and a multilingual read, both recorded in a
     * remark - text written by the handler, so reading it back needs no language of its own.
     */
    private static final String HANDLER = """
            package custom;

            import java.math.BigDecimal;

            import org.eclipse.dirigible.sdk.component.Component;
            import org.eclipse.dirigible.sdk.db.CheckMessages;
            import org.eclipse.dirigible.sdk.db.ValidationException;
            import org.eclipse.dirigible.sdk.messaging.ListenerKind;
            import org.eclipse.dirigible.sdk.messaging.MessageHandler;
            import org.eclipse.dirigible.sdk.utils.Json;

            import gen.eventlang.data.payment.PaymentEntity;
            import gen.eventlang.data.remark.RemarkEntity;
            import gen.eventlang.data.remark.RemarkRepository;
            import gen.eventlang.data.settings.CountryRepository;

            @Component
            public class RemarkOnPayment implements MessageHandler {

                @Override
                public String destination() {
                    return "eventlang-Payment-Payment";
                }

                @Override
                public ListenerKind kind() {
                    return ListenerKind.TOPIC;
                }

                @Override
                public void onMessage(String message) {
                    PaymentEntity payment = Json.parse(message, PaymentEntity.class);
                    String country = new CountryRepository().findById(payment.Country).Name;
                    String refusal;
                    try {
                        refuseNegative(payment.Amount.negate());
                        refusal = "not refused";
                    } catch (ValidationException e) {
                        refusal = e.getMessage();
                    }
                    RemarkRepository remarks = new RemarkRepository();
                    RemarkEntity remark = new RemarkEntity();
                    remark.PaymentId = payment.Id;
                    remark.Amount = BigDecimal.ZERO;
                    remark.Text = country + " | " + refusal;
                    remarks.save(remark);
                }

                private static void refuseNegative(BigDecimal amount) {
                    if (amount.signum() < 0) {
                        throw CheckMessages.refusal("eventlang:eventlang-model.checks.Remark_compare_0",
                                "A remark amount is never negative");
                    }
                }
            }
            """;

    private static final String BG_REMARK = "България | Сумата на бележката не може да е отрицателна";
    private static final String DEFAULT_REMARK = "Bulgaria | A remark amount is never negative";

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private Scheduler scheduler;

    @Test
    void the_handler_of_an_event_resolves_messages_and_reads_rows_in_the_language_of_the_write() throws Exception {
        writeProjectFile("app.intent", INTENT);
        generateProject();
        writeProjectFile("i18n/bg-BG/eventlang.model.json", BG_OVERLAY);
        writeProjectFile("custom/RemarkOnPayment.java", HANDLER);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The in-process delivery of a write sent in Bulgarian, handled on a broker thread.
        assertRemark(createPayment("bg"), BG_REMARK);
        // The control: a write that names no language is handled in the default one, as before.
        assertRemark(createPayment(null), DEFAULT_REMARK);

        // The relay's redelivery: an entry the in-process delivery could not hand over, left for the
        // relay job, whose thread has no language of its own.
        plantStrandedEvent("{\"Id\":" + RELAYED_PAYMENT + ",\"Amount\":7,\"Country\":" + BULGARIA + "}", "bg");
        scheduler.triggerJob(JobKey.jobKey("EventOutboxRelayJob", "system"));
        assertRemark(RELAYED_PAYMENT, BG_REMARK);
    }

    private int createPayment(String language) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(request(language).body("{\"Amount\":10,\"Country\":" + BULGARIA + "}")
                                                                  .when()
                                                                  .post(PAYMENTS)
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private static RequestSpecification request(String language) {
        RequestSpecification request = given().contentType(ContentType.JSON);
        return language == null ? request : request.header("Accept-Language", language);
    }

    /** The single remark the handler recorded for the payment, read without a language. */
    private void assertRemark(int payment, String text) {
        String ofPayment = "findAll { it.PaymentId == " + payment + " }";
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(REMARKS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(ofPayment, hasSize(1))
                                                 .body(ofPayment + "[0].Text", equalTo(text)),
                HANDLER_TIMEOUT_SECONDS);
    }

    /** An outbox entry already due, as a write whose in-process publish failed would have left one. */
    private void plantStrandedEvent(String payload, String language) throws Exception {
        Timestamp overdue = Timestamp.from(Instant.now()
                                                  .minus(Duration.ofMinutes(5)));
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO \"" + OUTBOX_TABLE
                        + "\" (\"EVENT_ID\", \"EVENT_TOPIC\", \"EVENT_PAYLOAD\", \"EVENT_CREATED_AT\", \"EVENT_ATTEMPTS\","
                        + " \"EVENT_NEXT_ATTEMPT_AT\", \"EVENT_LANGUAGE\") VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, UUID.randomUUID()
                                       .toString());
            statement.setString(2, PAYMENT_CREATED_TOPIC);
            statement.setString(3, payload);
            statement.setTimestamp(4, overdue);
            statement.setInt(5, 0);
            statement.setTimestamp(6, overdue);
            statement.setString(7, language);
            statement.executeUpdate();
        }
    }

    private void generateProject() {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
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

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
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

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
