/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.share;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantConfigurationScope;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantNotFoundException;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.engine.java.runtime.ClientBeanFactory;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

/**
 * The life of a share link (dirigible #8613) against a real table: what a token is, what it
 * resolves to, and every way it stops answering.
 */
class ShareLinkServiceTest {

    private static final String SUBJECT = "SALES_INVOICE";

    private static final byte[] PDF = "%PDF-1.7 issued copy".getBytes(StandardCharsets.UTF_8);

    private final Map<String, byte[]> files = new HashMap<>(Map.of("/Attachments/InvoiceCopy/one.pdf", PDF));

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-13T10:00:00Z"));

    private final Map<String, Boolean> shareable = new HashMap<>(Map.of("7", true));

    private String jdbcUrl;

    private Connection keepAlive;

    private ClientBeansHolder clientBeans;

    private TenantContext tenantContext;

    private ShareLinkStore store;

    private ShareLinkService service;

    @BeforeEach
    void setUp() throws Exception {
        jdbcUrl = "jdbc:h2:mem:share-" + UUID.randomUUID();
        keepAlive = DriverManager.getConnection(jdbcUrl);
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> {
            DirigibleConnection connection =
                    mock(DirigibleConnection.class, AdditionalAnswers.delegatesTo(DriverManager.getConnection(jdbcUrl)));
            doReturn(DatabaseSystem.H2).when(connection)
                                       .getDatabaseSystem();
            return connection;
        });
        DataSourcesManager dataSources = mock(DataSourcesManager.class);
        when(dataSources.getDefaultDataSource()).thenReturn(dataSource);
        store = new ShareLinkStore(dataSources);

        clientBeans = mock(ClientBeansHolder.class);
        contribute(new Subject(SUBJECT));
        tenantContext = mock(TenantContext.class);
        service = service(false);
    }

    @AfterEach
    void tearDown() throws SQLException {
        Configuration.remove(DirigibleConfig.DOCUMENTS_SHARE_ENABLED.getKey());
        Configuration.remove(DirigibleConfig.DOCUMENTS_SHARE_MAX_EXPIRY_DAYS.getKey());
        keepAlive.close();
    }

    @Test
    void aCreatedLinkOpensTheFileItWasCreatedForAndCountsTheOpen() {
        ShareLinkService.Created created = create(0);

        Optional<ShareLinkService.Opened> opened = service.open(created.token(), "203.0.113.5", "Viber");

        assertArrayEquals(PDF, opened.orElseThrow()
                                     .content());
        assertEquals("INV-7.pdf", opened.get()
                                        .link()
                                        .fileName());
        ShareLink listed = service.list(SUBJECT, "7")
                                  .get(0);
        assertEquals(1, listed.openCount());
        assertEquals(clock.instant(), listed.lastOpenedAt());
        assertNull(listed.expiresAt());
        assertEquals(ShareLink.STATUS_ACTIVE, listed.status(clock.instant()));
    }

    @Test
    void theTokenIsRandomLongAndStoredOnlyAsItsHash() throws SQLException {
        ShareLinkService.Created first = create(0);
        ShareLinkService.Created second = create(0);

        // 256 bits, base64url: 43 characters - and never the same twice.
        assertEquals(43, first.token()
                              .length());
        assertTrue(first.token()
                        .matches("[A-Za-z0-9_-]+"));
        assertNotEquals(first.token(), second.token());
        try (Statement statement = keepAlive.createStatement();
                var rows = statement.executeQuery("SELECT \"SHARE_TOKEN_HASH\" FROM \"DIRIGIBLE_SHARE_LINKS\"")) {
            while (rows.next()) {
                assertEquals(64, rows.getString(1)
                                     .length());
                assertNotEquals(first.token(), rows.getString(1));
            }
        }
    }

    @Test
    void whatIdentifiesTheDocumentDoesNotOpenIt() {
        ShareLinkService.Created created = create(0);

        // The link's own id, the record's key and a uuid are identifiers - none of them is the token.
        assertTrue(service.open(created.link()
                                       .id(),
                "203.0.113.5", null)
                          .isEmpty());
        assertTrue(service.open("7", "203.0.113.5", null)
                          .isEmpty());
        assertTrue(service.open(UUID.randomUUID()
                                    .toString(),
                "203.0.113.5", null)
                          .isEmpty());
        assertTrue(service.open("", "203.0.113.5", null)
                          .isEmpty());
        assertTrue(service.open("x".repeat(ShareTokens.MAX_LENGTH + 1), "203.0.113.5", null)
                          .isEmpty());
    }

    @Test
    void aRevokedLinkStopsAnsweringAtOnce() {
        ShareLinkService.Created created = create(0);

        assertTrue(service.revoke(SUBJECT, "7", created.link()
                                                       .id(),
                "clerk"));

        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());
        ShareLink listed = service.list(SUBJECT, "7")
                                  .get(0);
        assertEquals("clerk", listed.revokedBy());
        assertEquals(ShareLink.STATUS_REVOKED, listed.status(clock.instant()));
        // Revoked once: a second revoke finds nothing to do.
        assertFalse(service.revoke(SUBJECT, "7", created.link()
                                                        .id(),
                "clerk"));
    }

    @Test
    void aLinkIsRevokedOnlyThroughTheRecordItBelongsTo() {
        ShareLinkService.Created created = create(0);

        assertFalse(service.revoke(SUBJECT, "8", created.link()
                                                        .id(),
                "clerk"));
        assertFalse(service.revoke("OTHER_ENTITY", "7", created.link()
                                                               .id(),
                "clerk"));

        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isPresent());
    }

    @Test
    void anExpiredLinkStopsAnswering() {
        ShareLinkService.Created created = create(30);
        assertEquals(clock.instant()
                          .plus(Duration.ofDays(30)),
                created.link()
                       .expiresAt());

        clock.advance(Duration.ofDays(29));
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isPresent());
        clock.advance(Duration.ofDays(1));
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());
        assertEquals(ShareLink.STATUS_EXPIRED, service.list(SUBJECT, "7")
                                                      .get(0)
                                                      .status(clock.instant()));
    }

    @Test
    void theTenantCapShortensALifeAndNeverLengthensIt() {
        assertEquals(0, ShareLinkService.effectiveExpiryDays(0, 0));
        assertEquals(30, ShareLinkService.effectiveExpiryDays(30, 0));
        assertEquals(7, ShareLinkService.effectiveExpiryDays(0, 7));
        assertEquals(7, ShareLinkService.effectiveExpiryDays(30, 7));
        assertEquals(3, ShareLinkService.effectiveExpiryDays(3, 7));

        Configuration.set(DirigibleConfig.DOCUMENTS_SHARE_MAX_EXPIRY_DAYS.getKey(), "7");
        assertEquals(clock.instant()
                          .plus(Duration.ofDays(7)),
                create(0).link()
                         .expiresAt());
    }

    @Test
    void aRecordThatLeftItsShareableStateTakesItsLinkWithItForGood() {
        ShareLinkService.Created created = create(0);

        shareable.put("7", false);
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());
        ShareLink listed = service.list(SUBJECT, "7")
                                  .get(0);
        assertEquals(ShareLinkService.REVOKED_BY_STATE, listed.revokedBy());

        // Back in the state, the old link stays dead - a new one is somebody's decision.
        shareable.put("7", true);
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());
    }

    @Test
    void anEntityNoApplicationSharesAnswersNothingAndLosesNothing() {
        ShareLinkService.Created created = create(0);

        contribute(new Subject("SOMETHING_ELSE"));
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());
        when(clientBeans.current()).thenReturn(null);
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());

        // The application is back (a regeneration ended): the link was not revoked meanwhile.
        contribute(new Subject(SUBJECT));
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isPresent());
    }

    @Test
    void aLinkWhoseFileIsGoneAnswersNothing() {
        ShareLinkService.Created created = create(0);

        files.clear();

        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());
        assertEquals(0, service.list(SUBJECT, "7")
                               .get(0)
                               .openCount());
    }

    @Test
    void aTenantThatSwitchedSharingOffCreatesNoLinkAndServesNone() {
        ShareLinkService.Created created = create(0);

        Configuration.set(DirigibleConfig.DOCUMENTS_SHARE_ENABLED.getKey(), "false");

        assertFalse(service.isEnabled());
        assertThrows(IllegalStateException.class, () -> create(0));
        assertTrue(service.open(created.token(), "203.0.113.5", null)
                          .isEmpty());
    }

    @Test
    void aLinkWithoutAStoredFileIsNotCreated() {
        assertThrows(IllegalArgumentException.class, () -> service.create(SUBJECT, "7", " ", "INV-7.pdf", "application/pdf", 0, "clerk"));
    }

    @Test
    void everyLinkOfARecordIsRevokedTogether() {
        ShareLinkService.Created first = create(0);
        ShareLinkService.Created second = create(0);

        assertEquals(2, service.revokeAll(SUBJECT, "7", "clerk"));

        assertTrue(service.open(first.token(), "203.0.113.5", null)
                          .isEmpty());
        assertTrue(service.open(second.token(), "203.0.113.5", null)
                          .isEmpty());
        assertEquals(0, service.revokeAll(SUBJECT, "7", "clerk"));
    }

    @Test
    void anOpenBeforeAnyLinkExistsFindsNothingAndCreatesNoTable() throws SQLException {
        assertTrue(service.open(ShareTokens.newSecret(), "203.0.113.5", null)
                          .isEmpty());
        assertTrue(service.list(SUBJECT, "7")
                          .isEmpty());
        try (var tables = keepAlive.getMetaData()
                                   .getTables(null, null, "DIRIGIBLE_SHARE_LINKS", null)) {
            assertFalse(tables.next(), "a read must not create the table");
        }
    }

    @Test
    void whereOneHostServesEveryTenantTheTokenNamesItsTenant() throws Exception {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn("tenant-a");
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        when(tenantContext.execute(eq("tenant-a"), any())).thenAnswer(
                invocation -> invocation.<CallableResultAndException<?, ?>>getArgument(1)
                                        .call());
        when(tenantContext.execute(eq("tenant-b"), any())).thenThrow(new TenantNotFoundException("tenant-b"));
        service = service(true);

        ShareLinkService.Created created = create(0);

        assertTrue(created.token()
                          .startsWith("tenant-a."));
        assertNotNull(service.open(created.token(), "203.0.113.5", null)
                             .orElseThrow());
        // The secret alone names no tenant, and the same secret addressed to another tenant is not its
        // link.
        String secret = created.token()
                               .substring("tenant-a.".length());
        assertTrue(service.open(secret, "203.0.113.5", null)
                          .isEmpty());
        assertTrue(service.open("tenant-b." + secret, "203.0.113.5", null)
                          .isEmpty());
    }

    private ShareLinkService.Created create(int expiresAfterDays) {
        return service.create(SUBJECT, "7", "/Attachments/InvoiceCopy/one.pdf", "INV-7.pdf", "application/pdf", expiresAfterDays, "clerk");
    }

    private ShareLinkService service(boolean tenantAddressedTokens) {
        TenantConfigurationScope scope = new TenantConfigurationScope() {

            @Override
            public <Result, Exc extends Throwable> Result executeWithTenantOverrides(CallableResultAndException<Result, Exc> work)
                    throws Exc {
                return work.call();
            }
        };
        return new ShareLinkService(store, tenantContext, scope, clientBeans, this::read, clock, tenantAddressedTokens);
    }

    private byte[] read(String path) throws IOException {
        byte[] content = files.get(path);
        if (content == null) {
            throw new IOException("No such file: " + path);
        }
        return content;
    }

    private void contribute(ShareSubject subject) {
        ClientBeanFactory beans = mock(ClientBeanFactory.class);
        when(beans.getAll(ShareSubject.class)).thenReturn(List.of(subject));
        when(clientBeans.current()).thenReturn(beans);
    }

    private final class Subject implements ShareSubject {

        private final String name;

        private Subject(String name) {
            this.name = name;
        }

        @Override
        public String shareSubject() {
            return name;
        }

        @Override
        public boolean isShareable(String recordId) {
            return shareable.getOrDefault(recordId, false);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
