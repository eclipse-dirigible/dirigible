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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.restassured.response.Response;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
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

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * {@code {shareUrl}} in a notify body (#8616), through the published application: the generated
 * sender mints a share link to the record's issued copy at send time, exactly as a user's Share
 * action does, and mails its absolute URL - built on the configured base URL - which a reader with
 * no session opens. A send that runs before the copy exists mails nothing and records why.
 */
class IntentNotifyShareUrlIT extends IntegrationTest {

    static {
        GreenMailConfig.configureDirigibleEmailService();
    }

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "sharemail";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String COPIES = API + "/invoice/InvoiceCopyController";
    private static final String DISPATCH = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/DispatchTransition/run";
    private static final String PUBLIC_SHARE = "/public/share/";
    private static final String HOST = "localhost";
    private static final String BASE_URL = "https://share-mail.test";
    private static final Pattern SHARE_URL = Pattern.compile(Pattern.quote(BASE_URL + PUBLIC_SHARE) + "([A-Za-z0-9_-]+)");
    private static final String CUSTOMER = "customer@share-mail.test";
    private static final long TIMEOUT_SECONDS = 120;
    private static final long MAIL_TIMEOUT_SECONDS = 90;

    private static final int ISSUED = 2;

    /**
     * The process mails the issued invoice: once BEFORE its copy is minted (which must fail and say so)
     * and once after. The transition and the lifecycle notification mint the same way; the schedule
     * never fires here and is in the fixture to be compiled.
     */
    private static final String INTENT_YAML = """
            name: sharemail
            description: an issued invoice mailed as a share link

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: Invoice
                function: Document
                share:
                  when: "status != DRAFT"
                  copy: InvoiceCopy
                  roles: [Sharer]
                  expiresAfterDays: 30
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string, length: 60, required: true, function: DocumentTitle }
                  - { name: email, type: string, length: 200 }
                  - { name: early, type: string, length: 64 }
                  - { name: late,  type: string, length: 64 }
                relations:
                  - { name: status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus }

              - name: InvoiceLine
                function: DocumentItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 60 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

              - name: InvoiceCopy
                function: Snapshot
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

            permissions:
              - { role: Sharer, description: May hand an issued invoice to its customer }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
                  - { id: 3, name: SENT }

            notifications:
              - name: invoiceChanged
                event: { onUpdate: Invoice }
                to: email
                subject: "Changed {title}"
                body: "Read it at {shareUrl}"

            transitions:
              - name: Dispatch
                forEntity: Invoice
                from: [2]
                setStatus: 3
                notify:
                  to: email
                  subject: "Sent {title}"
                  body: "Read it at {shareUrl}"

            schedules:
              - name: reminders
                cron: "0 0 5 1 1 ?"
                entity: Invoice
                notify:
                  to: email
                  subject: "Reminder {title}"
                  body: "Read it at {shareUrl}"

            processes:
              - name: InvoiceMail
                trigger: { onCreate: Invoice }
                steps:
                  - name: early
                    kind: serviceTask
                    args:
                      onError: mint
                      notify: { to: email, subject: "Too early {title}", body: "Read it at {shareUrl}", outcome: early }
                      next: mint
                  - { name: mint, kind: serviceTask, args: { delegate: gen.events.InvoiceSnapshotGenerator, next: mail } }
                  - name: mail
                    kind: serviceTask
                    args:
                      notify:
                        to: email
                        subject: "Invoice {title}"
                        body: "Read it at {shareUrl}"
                        html: '<p>Read it <a href="{shareUrl}">here</a>.</p>'
                        outcome: late
                      next: done
                  - { name: done, kind: end }
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
    void a_mailed_share_url_opens_the_issued_copy_without_a_session() throws MessagingException {
        Configuration.set(DirigibleConfig.APP_BASE_URL.getKey(), BASE_URL);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int invoice = createInvoice("First");

        // (1) The step after the mint mails a link built on the CONFIGURED base URL, in both parts...
        MimeMessage mailed = awaitMessage("Invoice First");
        assertThat(mailed.getAllRecipients()[0].toString()).isEqualTo(CUSTOMER);
        String token = token(text(mailed, "text/plain"));
        assertThat(text(mailed, "text/html")).contains(BASE_URL + PUBLIC_SHARE + token);

        // ...and that link, opened with no session, serves the issued copy byte for byte.
        Map<String, Object> copy = awaitCopy(invoice);
        assertArrayEquals(download((Integer) copy.get("Id")), open(token), "the mailed link serves the stored copy");

        // (2) The step BEFORE the mint could not create a link: it mailed nothing, and the record says
        // why - a mail with a blank where the link should be never goes out.
        assertThat(withSubject("Too early First")).isEmpty();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Early", startsWith("failed: "))
                                                 .body("Early", containsString("no issued copy"))
                                                 .body("Late", equalTo("sent")));

        // (3) The link is the record's like any other: listed with the system as its creator, the send
        // named, with the declared life - and never with its token.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice + "/share-links")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(1))
                                                 .body("[0].createdBy", equalTo("system (notify InvoiceMail.mail)"))
                                                 .body("[0].expiresAt", notNullValue())
                                                 .body(not(containsString(token))));

        // (4) A transition's notify and a lifecycle notification mint their own link the same way.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + invoice + "}")
                                                 .when()
                                                 .post(DISPATCH)
                                                 .then()
                                                 .statusCode(200));
        String dispatched = token(text(awaitMessage("Sent First"), "text/plain"));
        assertThat(open(dispatched)).isNotEmpty();
        update(invoice);
        String changed = token(text(awaitMessage("Changed First"), "text/plain"));
        assertThat(open(changed)).isNotEmpty();
        assertThat(List.of(dispatched, changed)).doesNotContain(token);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice + "/share-links")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(3))
                                                 .body("createdBy", hasItem("system (notify Dispatch)"))
                                                 .body("createdBy", hasItem("system (notify invoiceChanged)"))
                                                 .body("expiresAt", everyItem(notNullValue())));
    }

    @Test
    void a_share_url_of_an_entity_that_declares_no_share_is_refused() {
        String yaml = """
                name: noshare
                entities:
                  - name: Memo
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string, length: 200 }
                notifications:
                  - name: memoChanged
                    event: { onUpdate: Memo }
                    to: email
                    subject: "Memo"
                    body: "Read it at {shareUrl}"
                """;
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(yaml)
                                                 .when()
                                                 .post("/services/ide/intent/parse")
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues",
                                                         hasItem(containsString("uses {shareUrl}, but [Memo] declares no share:"))));
    }

    /**
     * The token of the one share URL a text carries - asserting it is built on the configured origin.
     */
    private static String token(String text) {
        Matcher matcher = SHARE_URL.matcher(text);
        assertThat(matcher.find()).as("a share URL on the configured base URL in [%s]", text)
                                  .isTrue();
        return matcher.group(1);
    }

    /** Opens a share link with no session at all. */
    private byte[] open(String token) {
        AtomicReference<byte[]> bytes = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            Response opened = given().when()
                                     .get(PUBLIC_SHARE + token);
            opened.then()
                  .statusCode(200)
                  .header("Content-Type", startsWith("application/pdf"));
            bytes.set(opened.asByteArray());
        }, HOST);
        return bytes.get();
    }

    private int createInvoice(String title) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Title\":\"" + title + "\",\"Email\":\"" + CUSTOMER + "\",\"Status\":"
                                                                + ISSUED + "}")
                                                        .when()
                                                        .post(INVOICES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void update(int invoice) {
        restAssuredExecutor.execute(() -> {
            Map<String, Object> stored = given().when()
                                                .get(INVOICES + "/" + invoice)
                                                .then()
                                                .statusCode(200)
                                                .extract()
                                                .jsonPath()
                                                .getMap("");
            stored.put("Title", "First");
            given().contentType("application/json")
                   .body(stored)
                   .when()
                   .put(INVOICES + "/" + invoice)
                   .then()
                   .statusCode(200);
        });
    }

    /** The copy the process minted - asynchronous, so polled. */
    private Map<String, Object> awaitCopy(int invoice) {
        AtomicReference<Map<String, Object>> copy = new AtomicReference<>();
        restAssuredExecutor.execute(() -> copy.set(given().when()
                                                          .get(COPIES + "?Invoice=" + invoice)
                                                          .then()
                                                          .statusCode(200)
                                                          .body("", hasSize(1))
                                                          .extract()
                                                          .jsonPath()
                                                          .getMap("[0]")),
                TIMEOUT_SECONDS);
        return copy.get();
    }

    private byte[] download(int copy) {
        AtomicReference<byte[]> bytes = new AtomicReference<>();
        restAssuredExecutor.execute(() -> bytes.set(given().when()
                                                           .get(COPIES + "/" + copy + "/download")
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .asByteArray()));
        return bytes.get();
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
     * The decoded text of a message's part of one type - decoded, because a quoted-printable transfer
     * encoding wraps a long URL across lines.
     */
    private static String text(Part part, String type) {
        try {
            if (part.isMimeType(type)) {
                return part.getContent()
                           .toString();
            }
            if (part.getContent() instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    String found = text(multipart.getBodyPart(i), type);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read the mailed " + type + " part", e);
        }
    }

    private void generateProject() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
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

    @AfterEach
    void cleanup() {
        Configuration.remove(DirigibleConfig.APP_BASE_URL.getKey());
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
