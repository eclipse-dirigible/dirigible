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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import org.springframework.test.annotation.DirtiesContext;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.icegreen.greenmail.util.GreenMail;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;

/**
 * A notify block's subject and body are translated like any other message - dirigible #8787.
 *
 * <p>
 * The generated en-US catalog carries each block's texts under {@code mail.<site>.<name>}, a
 * translator's bg-BG catalog translates them, and the generated sender resolves them in the
 * language the mail is delivered in - here the recipient's, read off the record through
 * {@code languageFrom:} on a plain notice with no attachment. A bg recipient reads the bg texts
 * with the fields filled, an en recipient the English ones, and a block the bg catalog does not
 * translate falls back to the authored text.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentNotifyLanguageIT extends IntegrationTest {

    static {
        GreenMailConfig.configureDirigibleEmailService();
    }

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "notifylang";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String TICKETS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/ticket/TicketController";
    private static final String CLOSE_TICKET = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/CloseTicketTransition/run";
    private static final long TIMEOUT_SECONDS = 90;
    private static final long MAIL_TIMEOUT_SECONDS = 60;

    private static final String BG_READER = "bg.reader@notify-language.test";
    private static final String EN_READER = "en.reader@notify-language.test";

    private static final String INTENT_YAML = """
            name: notifylang
            description: a mail is delivered in its recipient's language

            entities:
              - name: Language
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, length: 2 }
              - name: TicketStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Ticket
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, length: 200 }
                relations:
                  - { name: Language, kind: manyToOne, to: Language }
                  - { name: Status, kind: manyToOne, to: TicketStatus, function: EntityStatus, init: 1 }

            notifications:
              - name: ticketOpened
                event: { onCreate: Ticket }
                to: email
                subject: "Ticket {name} opened"
                body: "Ticket {name} was opened for {email}."
                languageFrom: Language.code

            transitions:
              - name: CloseTicket
                forEntity: Ticket
                from: [1]
                setStatus: 2
                notify:
                  to: email
                  subject: "Ticket {name} closed"
                  body: "Ticket {name} is closed."
                  languageFrom: Language.code

            seeds:
              - name: languages
                entity: Language
                rows:
                  - { id: 1, code: bg }
                  - { id: 2, code: en }
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: Open }
                  - { id: 2, name: Closed }
            """;

    /** The translator's overlay: the notification is translated, the transition's notice is not. */
    private static final String BG_CATALOG = """
            {
              "notifylang-model": {
                "mail": {
                  "notifications": {
                    "ticketOpened": {
                      "subject": "Заявка {name} е отворена",
                      "body": "Заявка {name} е отворена за {email}."
                    }
                  }
                }
              }
            }
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
    void a_mail_is_delivered_in_its_recipients_language() throws Exception {
        generateProject();
        assertGeneratedCatalog();
        writeProjectFile("i18n/bg-BG/notifylang.model.json", BG_CATALOG);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int bgTicket = create("{\"Name\":\"T1\",\"Email\":\"" + BG_READER + "\",\"Language\":1}");
        MimeMessage opened = awaitMail(BG_READER, "Заявка T1 е отворена");
        assertEquals("Заявка T1 е отворена за " + BG_READER + ".", plainText(opened));

        create("{\"Name\":\"T2\",\"Email\":\"" + EN_READER + "\",\"Language\":2}");
        MimeMessage english = awaitMail(EN_READER, "Ticket T2 opened");
        assertEquals("Ticket T2 was opened for " + EN_READER + ".", plainText(english));

        // The bg catalog does not translate the transition's notice: the authored text goes out.
        create(CLOSE_TICKET, "{\"id\":" + bgTicket + "}");
        MimeMessage closed = awaitMail(BG_READER, "Ticket T1 closed");
        assertEquals("Ticket T1 is closed.", plainText(closed));
    }

    /**
     * The generated en-US catalog carries every notify block's texts under the key its sender reads.
     */
    private void assertGeneratedCatalog() {
        IResource catalog = repository.getResource(PROJECT_PATH + "/i18n/en-US/notifylang.model.json");
        assertTrue(catalog.exists(), "the en-US catalog of the model was not generated");
        JsonObject mail = JsonParser.parseString(new String(catalog.getContent(), StandardCharsets.UTF_8))
                                    .getAsJsonObject()
                                    .getAsJsonObject("notifylang-model")
                                    .getAsJsonObject("mail");
        JsonObject opened = mail.getAsJsonObject("notifications")
                                .getAsJsonObject("ticketOpened");
        assertEquals("Ticket {name} opened", opened.get("subject")
                                                   .getAsString());
        assertEquals("Ticket {name} was opened for {email}.", opened.get("body")
                                                                    .getAsString());
        assertEquals("Ticket {name} closed", mail.getAsJsonObject("transitions")
                                                 .getAsJsonObject("CloseTicket")
                                                 .get("subject")
                                                 .getAsString());
    }

    /** The one message with this subject delivered to the recipient, once it has arrived. */
    private MimeMessage awaitMail(String recipient, String subject) throws MessagingException {
        await().atMost(MAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
               .until(() -> received(recipient, subject).size() == 1);
        return received(recipient, subject).get(0);
    }

    private List<MimeMessage> received(String recipient, String subject) throws MessagingException {
        List<MimeMessage> matching = new ArrayList<>();
        for (MimeMessage message : greenMail.getReceivedMessages()) {
            if (subject.equals(message.getSubject()) && message.getRecipients(Message.RecipientType.TO)[0].toString()
                                                                                                          .equals(recipient)) {
                matching.add(message);
            }
        }
        return matching;
    }

    /** The decoded plain-text part of a message. */
    private static String plainText(MimeMessage message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text.trim();
        }
        Multipart multipart = (Multipart) content;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                return String.valueOf(part.getContent())
                             .trim();
            }
        }
        throw new AssertionError("the message has no plain-text part");
    }

    private int create(String body) {
        return create(TICKETS, body);
    }

    private int create(String controller, String body) {
        AtomicReference<Object> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
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
