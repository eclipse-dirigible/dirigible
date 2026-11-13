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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.cms.AttachmentsFacade;
import org.eclipse.dirigible.components.base.tenant.TenantConfigurationScope;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantNotFoundException;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.eclipse.dirigible.engine.java.runtime.ClientBeanResolver;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The share links of the current tenant (issue #8613): creating one for a stored document, listing
 * and revoking the links of a record, and resolving a presented token to the one file it serves.
 *
 * <p>
 * This service decides nothing about WHAT may be shared. That is the application's declaration (the
 * intent's {@code share:}), enforced by the generated code that calls {@link #create}: only an
 * entity that declares it has a caller at all, and it passes the immutable issued copy. What the
 * service owns is the link itself - a token that is random, stored only hashed and handed out once;
 * a lookup that never leaves the tenant; an expiry and a revoke that are read from the database on
 * every request - and the tenant's switch to turn the whole thing off.
 */
@Component
public class ShareLinkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareLinkService.class);

    /** Who a link is revoked by when its record is found outside its shareable state. */
    static final String REVOKED_BY_STATE = "system";

    private final ShareLinkStore store;

    private final TenantContext tenantContext;

    private final TenantConfigurationScope tenantConfigurationScope;

    private final ClientBeansHolder clientBeans;

    private final StoredFiles storedFiles;

    private final Clock clock;

    private final boolean tenantAddressedTokens;

    // Two constructors - the second takes the clock and the file reader a test supplies - so Spring is
    // told which one is the bean's.
    @Autowired
    ShareLinkService(ShareLinkStore store, TenantContext tenantContext, TenantConfigurationScope tenantConfigurationScope,
            ClientBeansHolder clientBeans) {
        this(store, tenantContext, tenantConfigurationScope, clientBeans, ShareLinkService::readFromDocumentStore, Clock.systemUTC(),
                TenantResolutionStrategy.fromConfiguration() == TenantResolutionStrategy.TOKEN_GROUPS);
    }

    ShareLinkService(ShareLinkStore store, TenantContext tenantContext, TenantConfigurationScope tenantConfigurationScope,
            ClientBeansHolder clientBeans, StoredFiles storedFiles, Clock clock, boolean tenantAddressedTokens) {
        this.clientBeans = clientBeans;
        this.storedFiles = storedFiles;
        this.store = store;
        this.tenantContext = tenantContext;
        this.tenantConfigurationScope = tenantConfigurationScope;
        this.clock = clock;
        this.tenantAddressedTokens = tenantAddressedTokens;
    }

    /**
     * A created link together with its token - the only moment the plain token exists outside the
     * reader's hands.
     *
     * @param link the stored link
     * @param token the plain token, to be put in the URL and never stored
     */
    public record Created(ShareLink link, String token) {
    }

    /** Reads the stored file a link serves, in the current tenant's document store. */
    @FunctionalInterface
    interface StoredFiles {

        /**
         * Reads a stored file.
         *
         * @param storagePath the file's path
         * @return its bytes
         * @throws IOException when the file is gone or cannot be read
         */
        byte[] read(String storagePath) throws IOException;
    }

    /**
     * A resolved link and the bytes it serves.
     *
     * @param link the link
     * @param content the stored file
     */
    record Opened(ShareLink link, byte[] content) {
    }

    /**
     * Whether the current tenant allows share links at all.
     *
     * @return {@code false} when the tenant switched the feature off
     */
    public boolean isEnabled() {
        return DirigibleConfig.DOCUMENTS_SHARE_ENABLED.getBooleanValue();
    }

    /**
     * Creates a link to one stored file of a record of the current tenant.
     *
     * @param entity the record's entity
     * @param recordId the record's key
     * @param storagePath the stored file the link serves
     * @param fileName the file name the reader receives
     * @param contentType the stored file's content type
     * @param expiresAfterDays the declared life in days, {@code 0} for a permanent link; the tenant's
     *        {@code DIRIGIBLE_DOCUMENTS_SHARE_MAX_EXPIRY_DAYS} shortens it
     * @param createdBy who creates the link
     * @return the link and its token
     * @throws IllegalStateException when the tenant switched sharing off, or the link cannot be stored
     */
    public Created create(String entity, String recordId, String storagePath, String fileName, String contentType, int expiresAfterDays,
            String createdBy) {
        if (!isEnabled()) {
            throw new IllegalStateException("Sharing documents through a link is switched off for this tenant");
        }
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("A share link needs the stored file it serves");
        }
        Instant now = clock.instant();
        int days = effectiveExpiryDays(expiresAfterDays, DirigibleConfig.DOCUMENTS_SHARE_MAX_EXPIRY_DAYS.getIntValue());
        Instant expiresAt = days > 0 ? now.plus(Duration.ofDays(days)) : null;
        String secret = ShareTokens.newSecret();
        String token = tenantAddressedTokens ? tenantContext.getCurrentTenant()
                                                            .getId()
                + ShareTokens.TENANT_SEPARATOR + secret : secret;
        try {
            ShareLink link =
                    store.insert(ShareTokens.hash(token), entity, recordId, storagePath, fileName, contentType, createdBy, now, expiresAt);
            LOGGER.info("Share link [{}] created by [{}] for [{}] [{}], expires [{}]", link.id(), LogSanitizer.sanitize(createdBy),
                    LogSanitizer.sanitize(entity), LogSanitizer.sanitize(recordId), expiresAt);
            return new Created(link, token);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to store a share link for [" + entity + "] [" + recordId + "]", e);
        }
    }

    /**
     * The links of one record of the current tenant, the newest first.
     *
     * @param entity the record's entity
     * @param recordId the record's key
     * @return the links
     */
    public List<ShareLink> list(String entity, String recordId) {
        try {
            return store.findByRecord(entity, recordId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read the share links of [" + entity + "] [" + recordId + "]", e);
        }
    }

    /**
     * Revokes one link of a record. From the moment this returns the link answers 404, on every node.
     *
     * @param entity the record's entity
     * @param recordId the record's key
     * @param linkId the link
     * @param revokedBy who revokes it
     * @return whether a link was revoked - {@code false} when the record has no such link, or it was
     *         revoked already
     */
    public boolean revoke(String entity, String recordId, String linkId, String revokedBy) {
        if (linkId == null || linkId.isBlank()) {
            return false;
        }
        return revoke(entity, recordId, linkId, revokedBy, "revoked") > 0;
    }

    /**
     * Revokes every link of a record - what a record leaving its shareable state, or being deleted,
     * calls.
     *
     * @param entity the record's entity
     * @param recordId the record's key
     * @param revokedBy who causes it
     * @return how many links were revoked
     */
    public int revokeAll(String entity, String recordId, String revokedBy) {
        return revoke(entity, recordId, null, revokedBy, "revoked with every link of its record");
    }

    private int revoke(String entity, String recordId, String linkId, String revokedBy, String how) {
        try {
            int revoked = store.revoke(entity, recordId, linkId, revokedBy, clock.instant());
            if (revoked > 0) {
                LOGGER.info("Share link [{}] of [{}] [{}] {} by [{}]", linkId == null ? "*" : LogSanitizer.sanitize(linkId),
                        LogSanitizer.sanitize(entity), LogSanitizer.sanitize(recordId), how, LogSanitizer.sanitize(revokedBy));
            }
            return revoked;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to revoke the share links of [" + entity + "] [" + recordId + "]", e);
        }
    }

    /**
     * Resolves a presented token to the file it serves and records the open. Every reason a token does
     * not answer - unknown, another tenant's, expired, revoked, sharing switched off, its record out of
     * the shareable state, the stored file gone - is the same empty result, so the caller has one
     * answer for all of them.
     *
     * @param token the presented token
     * @param address the remote address, for the audit row
     * @param userAgent the reader's user agent, for the audit row
     * @return the link and its content, or empty
     */
    Optional<Opened> open(String token, String address, String userAgent) {
        if (token == null || token.isBlank() || token.length() > ShareTokens.MAX_LENGTH) {
            return Optional.empty();
        }
        if (!tenantAddressedTokens) {
            // The host named the tenant, so the lookup runs in the tenant the request already is in: a
            // link of another tenant is simply not in this tenant's table.
            return openInCurrentTenant(token, address, userAgent);
        }
        String tenantId = ShareTokens.tenantOf(token);
        if (tenantId == null) {
            return Optional.empty();
        }
        try {
            return tenantContext.execute(tenantId,
                    () -> tenantConfigurationScope.executeWithTenantOverrides(() -> openInCurrentTenant(token, address, userAgent)));
        } catch (TenantNotFoundException e) {
            LOGGER.debug("A share link names tenant [{}], which does not exist", LogSanitizer.sanitize(tenantId), e);
            return Optional.empty();
        }
    }

    private Optional<Opened> openInCurrentTenant(String token, String address, String userAgent) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            Instant now = clock.instant();
            Optional<ShareLink> link = store.findByTokenHash(ShareTokens.hash(token))
                                            .filter(found -> found.isLive(now));
            if (link.isEmpty() || !stillShareable(link.get(), now)) {
                return Optional.empty();
            }
            byte[] content = read(link.get());
            if (content == null) {
                return Optional.empty();
            }
            store.recordOpen(link.get()
                                 .id(),
                    now, address, userAgent);
            LOGGER.info("Share link [{}] opened from [{}]", link.get()
                                                                .id(),
                    LogSanitizer.sanitize(address));
            return Optional.of(new Opened(link.get(), content));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to resolve a share link", e);
        }
    }

    /**
     * Whether the link's record is still in the state its entity declared shareable - asked of the
     * application, on every open. A record that left the state takes its link with it for good: the
     * link is revoked here, so a voided invoice does not become readable again should it ever be
     * re-issued (a new link is a decision somebody makes then). An application that contributes no
     * answer - not published, or its entity no longer declares {@code share:} - gets none of its links
     * served, and none revoked: a regeneration in progress must not destroy them.
     */
    private boolean stillShareable(ShareLink link, Instant now) throws SQLException {
        Optional<ShareSubject> subject = subject(link.entity());
        if (subject.isEmpty()) {
            LOGGER.debug("Share link [{}] names [{}], which no published application shares", link.id(), link.entity());
            return false;
        }
        boolean shareable;
        try {
            shareable = subject.get()
                               .isShareable(link.recordId());
        } catch (RuntimeException e) {
            LOGGER.warn("Share link [{}] was not served: [{}] could not say whether record [{}] is shareable", link.id(), link.entity(),
                    link.recordId(), e);
            return false;
        }
        if (!shareable) {
            store.revoke(link.entity(), link.recordId(), link.id(), REVOKED_BY_STATE, now);
            LOGGER.info("Share link [{}] revoked: record [{}] of [{}] is no longer in its shareable state", link.id(), link.recordId(),
                    link.entity());
        }
        return shareable;
    }

    private Optional<ShareSubject> subject(String name) {
        ClientBeanResolver resolver = clientBeans.current();
        if (resolver == null) {
            return Optional.empty();
        }
        return resolver.getAll(ShareSubject.class)
                       .stream()
                       .filter(subject -> name.equals(subject.shareSubject()))
                       .findFirst();
    }

    /** The stored file of a link, or {@code null} when it is gone (its record was deleted). */
    private byte[] read(ShareLink link) {
        try {
            return storedFiles.read(link.storagePath());
        } catch (IOException e) {
            LOGGER.warn("Share link [{}] names a stored file that cannot be read [{}]", link.id(), link.storagePath(), e);
            return null;
        }
    }

    private static byte[] readFromDocumentStore(String storagePath) throws IOException {
        try (InputStream in = AttachmentsFacade.read(storagePath)) {
            return in.readAllBytes();
        }
    }

    /**
     * The life a link is created with: the declared days, shortened by the tenant's cap. A cap never
     * lengthens, and against a permanent declaration ({@code 0}) it is the life.
     *
     * @param declaredDays the entity's {@code expiresAfterDays}, {@code 0} for permanent
     * @param capDays the tenant's cap, {@code 0} for none
     * @return the days, {@code 0} for a permanent link
     */
    static int effectiveExpiryDays(int declaredDays, int capDays) {
        int declared = Math.max(0, declaredDays);
        if (capDays <= 0) {
            return declared;
        }
        return declared == 0 ? capDays : Math.min(declared, capDays);
    }
}
