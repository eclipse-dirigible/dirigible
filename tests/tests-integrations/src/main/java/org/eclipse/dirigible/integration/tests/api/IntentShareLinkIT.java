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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.response.Response;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The intent's {@code share:} at runtime (#8613), through the published application: an issued
 * document gets a link a user creates, a reader without a session opens the stored copy through it,
 * and every way the link must NOT answer - a draft, a caller without the role, the record's own
 * identifiers in place of the token, another tenant's host, a revoke, an expiry, a void - is asked
 * over HTTP, at the public route itself.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentShareLinkIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "sharing";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String COPIES = API + "/invoice/InvoiceCopyController";
    private static final String NOTES = API + "/note/NoteController";
    private static final String PUBLIC_SHARE = "/public/share/";
    private static final String HOST = "localhost";
    private static final long TIMEOUT_SECONDS = 120;

    private static final int DRAFT = 1;
    private static final int ISSUED = 2;
    private static final int VOIDED = 3;

    private static final String OUTSIDER = "share-it-outsider";
    private static final String SHARER = "share-it-sharer";
    private static final String PASSWORD = "share-it-password";

    /**
     * An invoice whose issued copy may be shared while it is ISSUED, by the Sharer role; its copy is
     * minted by the process its creation starts. Note declares no share: - it must get no route.
     */
    private static final String INTENT_YAML = """
            name: sharing
            description: share fixture - an issued document handed out through a link

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: Invoice
                function: Document
                share:
                  when: "status == ISSUED"
                  copy: InvoiceCopy
                  roles: [Sharer]
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string, length: 60, required: true, function: DocumentTitle }
                  - { name: ref,   type: uuid }
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

              - name: Note
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string, length: 60 }

            permissions:
              - { role: Sharer, description: May hand an issued invoice to its customer }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
                  - { id: 3, name: VOIDED }

            processes:
              - name: InvoiceMint
                trigger: { onCreate: Invoice }
                steps:
                  - { name: mint,   kind: serviceTask, args: { delegate: gen.events.InvoiceSnapshotGenerator, next: minted } }
                  - { name: minted, kind: end }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void an_issued_document_is_read_through_its_link_and_through_nothing_else() throws SQLException {
        DirigibleTestTenant otherTenant = new DirigibleTestTenant("share-it-other");
        createTenants(otherTenant);
        waitForTenantsProvisioning(List.of(otherTenant));

        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        securityUtil.ensureUserInDefaultTenant(OUTSIDER, PASSWORD);
        securityUtil.ensureUserInDefaultTenant(SHARER, PASSWORD, "Sharer");

        int invoice = createInvoice("First invoice");
        Map<String, Object> copy = awaitCopy(invoice);
        byte[] issuedCopy = download((Integer) copy.get("Id"));
        assertTrue(new String(issuedCopy, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF"), "the minted copy is a PDF");

        // (1) A DRAFT has a copy and still cannot be shared: the declared state decides, not the file.
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(INVOICES + "/" + invoice + "/share-links")
                                                 .then()
                                                 .statusCode(409)
                                                 .body(containsString("cannot be shared in its current state")));
        moveTo(invoice, ISSUED);

        // (2) Creating a link takes one of the declared roles - a signed-in user without it is refused
        // on all three endpoints, and so is nobody at all.
        restAssuredExecutor.execute(() -> {
            given().when()
                   .post(INVOICES + "/" + invoice + "/share-links")
                   .then()
                   .statusCode(403);
            given().when()
                   .get(INVOICES + "/" + invoice + "/share-links")
                   .then()
                   .statusCode(403);
            given().when()
                   .delete(INVOICES + "/" + invoice + "/share-links/anything")
                   .then()
                   .statusCode(403);
        }, OUTSIDER, PASSWORD);

        // (3) A holder of the role creates the link; the answer carries the URL once, and no listing
        // gives the token back.
        AtomicReference<String> url = new AtomicReference<>();
        AtomicReference<String> linkId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            Response created = given().when()
                                      .post(INVOICES + "/" + invoice + "/share-links");
            created.then()
                   .statusCode(201)
                   .body("status", equalTo("ACTIVE"))
                   .body("expiresAt", nullValue())
                   .body("fileName", equalTo(copy.get("FileName")))
                   .body("url", containsString(PUBLIC_SHARE));
            url.set(created.path("url"));
            linkId.set(created.path("id"));
        }, SHARER, PASSWORD);
        String token = url.get()
                          .substring(url.get()
                                        .indexOf(PUBLIC_SHARE)
                                  + PUBLIC_SHARE.length());
        assertEquals(43, token.length(), "the token is 256 random bits, base64url");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice + "/share-links")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(1))
                                                 .body("[0].id", equalTo(linkId.get()))
                                                 .body("[0].openCount", equalTo(0))
                                                 .body(not(containsString(token))));

        // (4) A reader with no session opens exactly the issued copy - and the answer creates no
        // session, is not cacheable, not indexable and leaks no referrer.
        restAssuredExecutor.execute(() -> {
            Response opened = given().when()
                                     .get(PUBLIC_SHARE + token);
            opened.then()
                  .statusCode(200)
                  .header("Content-Type", startsWith("application/pdf"))
                  .header("Content-Disposition", startsWith("inline"))
                  .header("Cache-Control", containsString("no-store"))
                  .header("X-Robots-Tag", containsString("noindex"))
                  .header("Referrer-Policy", equalTo("no-referrer"))
                  .header("Set-Cookie", nullValue());
            assertArrayEquals(issuedCopy, opened.asByteArray(), "the link serves the stored copy, byte for byte");
        }, HOST);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice + "/share-links")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].openCount", equalTo(1))
                                                 .body("[0].lastOpenedAt", notNullValue()));

        // (5) What identifies the document does not open it: the record's uuid is printed on it and is
        // no secret, and neither the link's id nor the record's key is the token.
        String recordUuid = recordUuid(invoice);
        assertMiss(recordUuid, HOST);
        assertMiss(linkId.get(), HOST);

        // (6) The token is bound to its tenant: on another tenant's host it is nobody's link.
        assertMiss(token, otherTenant.getHost());

        // (7) An entity that declares no share: has no route at all.
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(NOTES + "/1/share-links")
                                                 .then()
                                                 .statusCode(404));

        // (8) A revoke is immediate, and takes the link through the record it belongs to only.
        restAssuredExecutor.execute(() -> {
            given().when()
                   .delete(INVOICES + "/" + (invoice + 1000) + "/share-links/" + linkId.get())
                   .then()
                   .statusCode(404);
            given().when()
                   .delete(INVOICES + "/" + invoice + "/share-links/" + linkId.get())
                   .then()
                   .statusCode(greaterThanOrEqualTo(200));
            given().when()
                   .get(INVOICES + "/" + invoice + "/share-links")
                   .then()
                   .statusCode(200)
                   .body("[0].status", equalTo("REVOKED"))
                   .body("[0].revokedBy", equalTo(SHARER));
        }, SHARER, PASSWORD);
        assertMiss(token, HOST);

        // (9) An expired link answers the same 404.
        String expiring = createLink(invoice);
        assertOpens(expiring);
        expire(expiring);
        assertMiss(expiring, HOST);

        // (10) A voided document takes its links with it: the link stops answering, is revoked for good,
        // and no new one can be created.
        String voided = createLink(invoice);
        assertOpens(voided);
        moveTo(invoice, VOIDED);
        assertMiss(voided, HOST);
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(INVOICES + "/" + invoice + "/share-links")
                   .then()
                   .statusCode(200)
                   .body("[0].status", equalTo("REVOKED"))
                   .body("[0].revokedBy", equalTo("system"));
            given().when()
                   .post(INVOICES + "/" + invoice + "/share-links")
                   .then()
                   .statusCode(409);
        });

        // (11) An address that keeps asking for links that do not answer is throttled - 429 with a
        // Retry-After - well before it could try a meaningful number of tokens.
        boolean throttled = false;
        for (int attempt = 0; attempt < 12 && !throttled; attempt++) {
            AtomicInteger status = new AtomicInteger();
            AtomicReference<String> retryAfter = new AtomicReference<>();
            String guess = "guess-" + attempt;
            restAssuredExecutor.execute(() -> {
                Response answer = given().when()
                                         .get(PUBLIC_SHARE + guess);
                status.set(answer.statusCode());
                retryAfter.set(answer.header("Retry-After"));
            }, HOST);
            if (status.get() == 429) {
                assertNotNull(retryAfter.get(), "a throttled answer says when to come back");
                throttled = true;
            } else {
                assertEquals(404, status.get());
            }
        }
        assertTrue(throttled, "repeated misses from one address must be throttled");
    }

    private void assertOpens(String token) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PUBLIC_SHARE + token)
                                                 .then()
                                                 .statusCode(200),
                HOST);
    }

    /** An unknown, expired, revoked or foreign token: one and the same 404, with nothing in it. */
    private void assertMiss(String token, String host) {
        restAssuredExecutor.execute(() -> {
            Response missed = given().when()
                                     .get(PUBLIC_SHARE + token);
            missed.then()
                  .statusCode(404)
                  .header("Set-Cookie", nullValue());
            assertFalse(missed.asString()
                              .contains("%PDF"),
                    "a miss serves no document");
            assertEquals("", missed.asString(), "a miss says nothing about why");
        }, host);
    }

    private String createLink(int invoice) {
        AtomicReference<String> url = new AtomicReference<>();
        restAssuredExecutor.execute(() -> url.set(given().when()
                                                         .post(INVOICES + "/" + invoice + "/share-links")
                                                         .then()
                                                         .statusCode(201)
                                                         .extract()
                                                         .path("url")));
        return url.get()
                  .substring(url.get()
                                .indexOf(PUBLIC_SHARE)
                          + PUBLIC_SHARE.length());
    }

    /**
     * Puts the newest link of the default tenant in the past - the one thing a test cannot wait for.
     */
    private void expire(String token) throws SQLException {
        String sql = "UPDATE \"DIRIGIBLE_SHARE_LINKS\" SET \"SHARE_EXPIRES_AT\" = ? WHERE \"SHARE_REVOKED_AT\" IS NULL";
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()
                                                            .minusSeconds(60)));
            assertEquals(1, statement.executeUpdate(), "exactly the one live link [" + token.substring(0, 4) + "...] expires");
        }
    }

    private int createInvoice(String title) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Title\":\"" + title + "\",\"Status\":" + DRAFT + "}")
                                                        .when()
                                                        .post(INVOICES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void moveTo(int invoice, int status) {
        restAssuredExecutor.execute(() -> {
            Map<String, Object> stored = given().when()
                                                .get(INVOICES + "/" + invoice)
                                                .then()
                                                .statusCode(200)
                                                .extract()
                                                .jsonPath()
                                                .getMap("");
            stored.put("Status", status);
            given().contentType("application/json")
                   .body(stored)
                   .when()
                   .put(INVOICES + "/" + invoice)
                   .then()
                   .statusCode(200)
                   .body("Status", equalTo(status));
        });
    }

    private String recordUuid(int invoice) {
        AtomicReference<String> uuid = new AtomicReference<>();
        restAssuredExecutor.execute(() -> uuid.set(given().when()
                                                          .get(INVOICES + "/" + invoice)
                                                          .then()
                                                          .statusCode(200)
                                                          .body("Ref", notNullValue())
                                                          .extract()
                                                          .path("Ref")));
        return uuid.get();
    }

    /** The copy the creation's process minted - asynchronous, so polled. */
    private Map<String, Object> awaitCopy(int invoice) {
        AtomicReference<Map<String, Object>> copy = new AtomicReference<>();
        restAssuredExecutor.execute(() -> copy.set(given().when()
                                                          .get(COPIES + "?Invoice=" + invoice)
                                                          .then()
                                                          .statusCode(200)
                                                          .body("", hasSize(1))
                                                          .body("[0].StoragePath", startsWith("/Attachments/InvoiceCopy/"))
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

    private void generateProject() {
        writeIntent();
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

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
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
