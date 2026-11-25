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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
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

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * {@code share: expiry:} (#8620), through the published application: a link's life is read from the
 * record it is created on - here one hop away, the customer's kind - so the invoice of a private
 * individual gets an expiring link while a business's stays permanent. It holds for the two ways a
 * link is created, the Share action and a notify's {@code {shareUrl}}, the tenant's cap still
 * shortens both, and an expired link stops answering.
 */
class IntentShareExpiryIT extends IntegrationTest {

    static {
        GreenMailConfig.configureDirigibleEmailService();
    }

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "shareexpiry";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String PUBLIC_SHARE = "/public/share/";
    private static final String HOST = "localhost";
    private static final String BASE_URL = "https://share-expiry.test";
    private static final Pattern SHARE_URL = Pattern.compile(Pattern.quote(BASE_URL + PUBLIC_SHARE) + "([A-Za-z0-9_-]+)");
    private static final String MAILED_BY = "system (notify InvoiceMail.mail)";
    private static final long TIMEOUT_SECONDS = 120;
    private static final long MAIL_TIMEOUT_SECONDS = 90;

    private static final int ISSUED = 2;
    private static final int BUSINESS = 1;
    private static final int INDIVIDUAL = 2;

    /**
     * Permanent links by default; 30 days for an invoice whose customer is an individual. Creating an
     * invoice mints its copy and then mails it with a link.
     */
    private static final String INTENT_YAML = """
            name: shareexpiry
            description: a share link that expires for a private individual's invoice only

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: CustomerKind
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: Customer
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, length: 60 }
                  - { name: email, type: string, length: 200 }
                relations:
                  - { name: Kind, kind: manyToOne, to: CustomerKind }

              - name: Invoice
                function: Document
                share:
                  when: "status != DRAFT"
                  copy: InvoiceCopy
                  roles: [Sharer]
                  expiresAfterDays: 0
                  expiry:
                    - { when: "Customer.Kind == 2", days: 30 }
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string, length: 60, required: true, function: DocumentTitle }
                relations:
                  - { name: status,   kind: manyToOne, to: InvoiceStatus, function: EntityStatus }
                  - { name: Customer, kind: manyToOne, to: Customer }

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
              - name: customer-kinds
                entity: CustomerKind
                rows:
                  - { id: 1, name: Business }
                  - { id: 2, name: Individual }

            processes:
              - name: InvoiceMail
                trigger: { onCreate: Invoice }
                steps:
                  - { name: mint, kind: serviceTask, args: { delegate: gen.events.InvoiceSnapshotGenerator, next: mail } }
                  - name: mail
                    kind: serviceTask
                    args:
                      notify: { to: Customer.email, subject: "Invoice {title}", body: "Read it at {shareUrl}" }
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

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void a_link_lives_as_long_as_its_record_says_whichever_way_it_is_created() throws Exception {
        Configuration.set(DirigibleConfig.APP_BASE_URL.getKey(), BASE_URL);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int business = invoice("Expiry business", customer("Acme Ltd", "acme@share-expiry.test", BUSINESS));
        int individual = invoice("Expiry individual", customer("Jane Doe", "jane@share-expiry.test", INDIVIDUAL));

        // (1) The mailed links: the business's is permanent, the individual's lives 30 days.
        String businessToken = token(awaitMessage("Invoice Expiry business"));
        String individualToken = token(awaitMessage("Invoice Expiry individual"));
        Map<String, Object> businessMailed = onlyLink(business);
        Map<String, Object> individualMailed = onlyLink(individual);
        assertThat(businessMailed).containsEntry("createdBy", MAILED_BY)
                                  .containsEntry("expiresAt", null);
        assertThat(individualMailed).containsEntry("createdBy", MAILED_BY);
        assertLife(individualMailed, 30);

        // (2) The Share action reads the same record the same way.
        assertThat(createLink(business)).containsEntry("expiresAt", null);
        assertLife(createLink(individual), 30);

        // (3) The tenant's cap stays the ceiling of both: it gives the permanent link a life and
        // shortens the longer one.
        Configuration.set(DirigibleConfig.DOCUMENTS_SHARE_MAX_EXPIRY_DAYS.getKey(), "7");
        assertLife(createLink(business), 7);
        assertLife(createLink(individual), 7);
        Configuration.remove(DirigibleConfig.DOCUMENTS_SHARE_MAX_EXPIRY_DAYS.getKey());

        // (4) Both mailed links open; once the individual's life has run out it no longer does, while
        // the business's still does.
        assertOpens(businessToken, 200);
        assertOpens(individualToken, 200);
        expire((String) individualMailed.get("id"));
        assertOpens(individualToken, 404);
        assertOpens(businessToken, 200);
    }

    /**
     * Asserts a link expires the given number of days after it was created (an hour's slack for DST).
     */
    private static void assertLife(Map<String, Object> link, int days) {
        assertThat(link.get("expiresAt")).as("the link [%s] expires", link)
                                         .isNotNull();
        Duration life = Duration.between(Instant.parse((String) link.get("createdAt")), Instant.parse((String) link.get("expiresAt")));
        assertThat(life.minus(Duration.ofDays(days))
                       .abs()).as("the life of %s", link)
                              .isLessThanOrEqualTo(Duration.ofHours(1));
    }

    private Map<String, Object> onlyLink(int invoice) {
        AtomicReference<Map<String, Object>> link = new AtomicReference<>();
        restAssuredExecutor.execute(() -> link.set(given().when()
                                                          .get(INVOICES + "/" + invoice + "/share-links")
                                                          .then()
                                                          .statusCode(200)
                                                          .body("", hasSize(1))
                                                          .extract()
                                                          .jsonPath()
                                                          .getMap("[0]")));
        return link.get();
    }

    private Map<String, Object> createLink(int invoice) {
        AtomicReference<Map<String, Object>> link = new AtomicReference<>();
        restAssuredExecutor.execute(() -> link.set(given().when()
                                                          .post(INVOICES + "/" + invoice + "/share-links")
                                                          .then()
                                                          .statusCode(201)
                                                          .extract()
                                                          .jsonPath()
                                                          .getMap("")));
        return link.get();
    }

    private void assertOpens(String token, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PUBLIC_SHARE + token)
                                                 .then()
                                                 .statusCode(status),
                HOST);
    }

    /** Puts one link's expiry in the past - the one thing a test cannot wait for. */
    private void expire(String linkId) throws SQLException {
        String sql = "UPDATE \"DIRIGIBLE_SHARE_LINKS\" SET \"SHARE_EXPIRES_AT\" = ? WHERE \"SHARE_ID\" = ?";
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()
                                                            .minusSeconds(60)));
            statement.setString(2, linkId);
            assertEquals(1, statement.executeUpdate(), "the link [" + linkId + "] expires");
        }
    }

    private int customer(String name, String email, int kind) {
        return create(CUSTOMERS, "{\"Name\":\"" + name + "\",\"Email\":\"" + email + "\",\"Kind\":" + kind + "}");
    }

    private int invoice(String title, int customer) {
        return create(INVOICES, "{\"Title\":\"" + title + "\",\"Status\":" + ISSUED + ",\"Customer\":" + customer + "}");
    }

    private int create(String controller, String json) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(json)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    /** The token of the one share URL a mail carries. */
    private static String token(MimeMessage message) throws Exception {
        String text = text(message);
        Matcher matcher = SHARE_URL.matcher(text);
        assertThat(matcher.find()).as("a share URL on the configured base URL in [%s]", text)
                                  .isTrue();
        return matcher.group(1);
    }

    /** The decoded text of a part - decoded, because a quoted-printable encoding wraps a long URL. */
    private static String text(Part part) throws Exception {
        if (part.getContent() instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(text(multipart.getBodyPart(i)));
            }
            return text.toString();
        }
        return String.valueOf(part.getContent());
    }

    private MimeMessage awaitMessage(String subject) {
        AtomicReference<MimeMessage> found = new AtomicReference<>();
        await().atMost(MAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
               .until(() -> {
                   for (MimeMessage message : greenMail.getReceivedMessages()) {
                       if (subject.equals(message.getSubject())) {
                           found.set(message);
                           return true;
                       }
                   }
                   return false;
               });
        return found.get();
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
        Configuration.remove(DirigibleConfig.DOCUMENTS_SHARE_MAX_EXPIRY_DAYS.getKey());
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
