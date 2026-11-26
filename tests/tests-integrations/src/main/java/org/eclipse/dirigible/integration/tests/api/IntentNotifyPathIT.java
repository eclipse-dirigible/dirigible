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
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.mail.GreenMailConfig;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * A notify path walks a chain of to-one relations - dirigible #8030.
 *
 * <p>
 * The parser and generator tests prove the descriptor; this proves that the generated loads compile
 * and read through the chain at all four notify sites - a lifecycle notification, a process step, a
 * transition and a schedule tick - and that a null hop renders {@code null} rather than failing the
 * send: a renewal reaches its plan's and its customer's names through its subscription.
 */
class IntentNotifyPathIT extends IntegrationTest {

    static {
        GreenMailConfig.configureDirigibleEmailService();
    }

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "notifypath";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CONFIRM = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/ConfirmTransition/run";
    private static final long TIMEOUT_SECONDS = 90;
    private static final long MAIL_TIMEOUT_SECONDS = 60;

    private static final String CUSTOMER = "acme@notify-path.test";
    private static final String REQUESTER = "requester@notify-path.test";
    private static final String PORTAL = "https://portal.notify-path.test/gold";

    private static final String INTENT_YAML = """
            name: notifypath
            description: a renewal mails its plan's and its customer's names, reached through its subscription

            entities:
              - name: Customer
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, length: 100 }
                  - { name: email, type: string, length: 200 }
              - name: Plan
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 100 }
                  - { name: portalUrl, type: string, length: 200 }
              - name: Subscription
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer, required: true }
                  # Optional, so a renewal of a subscription without a plan shows that a null hop renders null.
                  - { name: plan,     kind: manyToOne, to: Plan }
              - name: RenewalStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Renewal
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: requestedBy, type: string, length: 200 }
                relations:
                  - { name: subscription, kind: manyToOne, to: Subscription, required: true }
                  - { name: Status, kind: manyToOne, to: RenewalStatus, function: EntityStatus, init: 1 }

            notifications:
              - name: renewalRequested
                event: { onCreate: Renewal }
                to: requestedBy
                subject: "Your {subscription.plan.name} renewal for {subscription.customer.name}"
                body: "Open it at {subscription.plan.portalUrl}"

            processes:
              - name: Renewing
                trigger: { onCreate: Renewal }
                steps:
                  - name: notify
                    kind: serviceTask
                    args:
                      notify:
                        to: subscription.customer.email
                        subject: "Renewing for {subscription.customer.name}"
                        body: "Renewing."
                      next: done
                  - { name: done, kind: end }

            transitions:
              - name: Confirm
                forEntity: Renewal
                from: [1]
                setStatus: 2
                notify:
                  to: subscription.customer.email
                  subject: "Confirmed for {subscription.customer.name}"
                  body: "Confirmed."

            schedules:
              - name: renewalReminder
                cron: "0/5 * * * * ?"
                entity: Renewal
                notify:
                  to: subscription.customer.email
                  subject: "Reminder for {subscription.customer.name}"
                  body: "Reminder."

            seeds:
              - name: renewal-statuses
                entity: RenewalStatus
                rows:
                  - { id: 1, name: Open }
                  - { id: 2, name: Confirmed }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private GreenMail greenMail;

    @Test
    void every_notify_site_reads_through_the_chain() throws Exception {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int customer = create("customer/CustomerController", "{\"Name\":\"Acme\",\"Email\":\"" + CUSTOMER + "\"}");
        int plan = create("plan/PlanController", "{\"Name\":\"Gold\",\"PortalUrl\":\"" + PORTAL + "\"}");
        int subscription = create("subscription/SubscriptionController", "{\"Customer\":" + customer + ",\"Plan\":" + plan + "}");
        int renewal = create("renewal/RenewalController", "{\"RequestedBy\":\"" + REQUESTER + "\",\"Subscription\":" + subscription + "}");

        // A lifecycle notification: two chains through one shared prefix, in the subject and the body.
        MimeMessage requested = awaitMessage("Your Gold renewal for Acme");
        assertThat(requested.getAllRecipients()[0].toString()).isEqualTo(REQUESTER);
        assertThat(body(requested)).contains("Open it at " + PORTAL);

        // A process step, a transition and a schedule tick - each addressed through the chain too.
        assertThat(awaitMessage("Renewing for Acme").getAllRecipients()[0].toString()).isEqualTo(CUSTOMER);
        create(CONFIRM, "{\"id\":" + renewal + "}");
        assertThat(awaitMessage("Confirmed for Acme").getAllRecipients()[0].toString()).isEqualTo(CUSTOMER);
        assertThat(awaitMessage("Reminder for Acme").getAllRecipients()[0].toString()).isEqualTo(CUSTOMER);

        // A null hop anywhere renders empty, exactly as a null one-hop relation does - never the text
        // "null", the texts being catalog messages (#8787) - and the send still goes out.
        int planless = create("subscription/SubscriptionController", "{\"Customer\":" + customer + "}");
        create("renewal/RenewalController", "{\"RequestedBy\":\"" + REQUESTER + "\",\"Subscription\":" + planless + "}");
        assertThat(body(awaitMessage("Your  renewal for Acme"))).doesNotContain("null");
    }

    private MimeMessage awaitMessage(String subject) {
        AtomicReference<MimeMessage> found = new AtomicReference<>();
        await().atMost(MAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
               .until(() -> {
                   List<MimeMessage> matching = withSubject(subject);
                   if (matching.isEmpty()) {
                       return false;
                   }
                   found.set(matching.get(0));
                   return true;
               });
        return found.get();
    }

    private List<MimeMessage> withSubject(String subject) throws MessagingException {
        List<MimeMessage> matching = new ArrayList<>();
        for (MimeMessage message : greenMail.getReceivedMessages()) {
            if (subject.equals(message.getSubject())) {
                matching.add(message);
            }
        }
        return matching;
    }

    /**
     * The delivered body as GreenMail stores it - the sender wraps even a plain-text message in a
     * multipart.
     */
    private static String body(MimeMessage message) {
        return GreenMailUtil.getBody(message);
    }

    private int create(String controller, String body) {
        String url = controller.startsWith("/") ? controller : API + "/" + controller;
        AtomicReference<Object> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(url)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get() instanceof Number number ? number.intValue() : -1;
    }

    private void generateProject() {
        writeProjectFile("app.intent", INTENT_YAML);
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
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
