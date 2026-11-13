/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.share;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.share.ShareLink;
import org.eclipse.dirigible.components.engine.share.ShareLinkService;
import org.eclipse.dirigible.sdk.component.Beans;
import org.eclipse.dirigible.sdk.core.Configurations;
import org.eclipse.dirigible.sdk.security.User;

/**
 * Share links: a way to hand ONE stored document to a reader without an account, as a URL that can
 * travel by mail or messenger ({@code /public/share/<token>}).
 *
 * <p>
 * A link serves exactly the stored file it was created for - pass the immutable issued copy, never
 * a path a caller chose. Its token is random, kept only as a hash, and returned once, by
 * {@link #create}; the link can be revoked and may expire, and both take effect at once. The
 * current tenant may switch sharing off ({@code DIRIGIBLE_DOCUMENTS_SHARE_ENABLED}) and shorten
 * every link's life ({@code DIRIGIBLE_DOCUMENTS_SHARE_MAX_EXPIRY_DAYS}).
 *
 * <p>
 * Never share a personal document (a payslip, a leave request, a contract of employment): a link is
 * a bearer credential, and whoever holds it reads the file. The intent's {@code share:} refuses
 * such an entity at parse time; code calling this facade directly carries that duty itself.
 *
 * <p>
 * Example: {@code ShareLinks.create("SalesInvoice", invoice.Id, copy.StoragePath, copy.FileName,
 * copy.ContentType, 0).url()}.
 */
public final class ShareLinks {

    private static final String PUBLIC_PATH = "/public/share/";

    private ShareLinks() {}

    /**
     * One share link of a record.
     *
     * @param id the link's id - what {@link ShareLinks#revoke} names
     * @param fileName the file name the reader receives
     * @param createdBy who created the link
     * @param createdAt when it was created
     * @param expiresAt when it stops answering, {@code null} for a permanent link
     * @param revokedBy who revoked it, {@code null} while it is not revoked
     * @param revokedAt when it was revoked, {@code null} while it is not revoked
     * @param openCount how many times it was opened
     * @param lastOpenedAt when it was last opened, {@code null} when it never was
     * @param status {@code ACTIVE}, {@code EXPIRED} or {@code REVOKED}
     */
    public record Link(String id, String fileName, String createdBy, Instant createdAt, Instant expiresAt, String revokedBy,
            Instant revokedAt, long openCount, Instant lastOpenedAt, String status) {

        /**
         * The link as a JSON-ready map, its instants as ISO-8601 text.
         *
         * @return the map
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("fileName", fileName);
            map.put("createdBy", createdBy);
            map.put("createdAt", text(createdAt));
            map.put("expiresAt", text(expiresAt));
            map.put("revokedBy", revokedBy);
            map.put("revokedAt", text(revokedAt));
            map.put("openCount", openCount);
            map.put("lastOpenedAt", text(lastOpenedAt));
            map.put("status", status);
            return map;
        }

        private static String text(Instant instant) {
            return instant == null ? null : instant.toString();
        }
    }

    /**
     * A link that was just created, with the URL to hand out. The URL carries the token and cannot be
     * read again later - only its hash is kept.
     *
     * @param link the link
     * @param url the URL that opens the document; absolute when the application's base URL is
     *        configured ({@code DIRIGIBLE_APP_BASE_URL}), otherwise the path from the host's root
     */
    public record Created(Link link, String url) {

        /**
         * The created link as a JSON-ready map: the link's own keys plus {@code url}.
         *
         * @return the map
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = link.toMap();
            map.put("url", url);
            return map;
        }
    }

    /**
     * Whether the current tenant allows share links.
     *
     * @return {@code false} when the tenant switched sharing off
     */
    public static boolean enabled() {
        return service().isEnabled();
    }

    /**
     * Create a link to one stored file of a record, on behalf of the current user.
     *
     * @param entity the record's entity name
     * @param recordId the record's key
     * @param storagePath the stored file the link serves (the issued copy's {@code StoragePath})
     * @param fileName the file name the reader receives
     * @param contentType the stored file's content type
     * @param expiresAfterDays the link's life in days, {@code 0} for a permanent link
     * @return the link and the URL to hand out
     * @throws IllegalStateException when the tenant switched sharing off
     */
    public static Created create(String entity, Object recordId, String storagePath, String fileName, String contentType,
            int expiresAfterDays) {
        ShareLinkService.Created created =
                service().create(entity, String.valueOf(recordId), storagePath, fileName, contentType, expiresAfterDays, User.getName());
        return new Created(link(created.link()), Configurations.appBaseUrl() + PUBLIC_PATH + created.token());
    }

    /**
     * The links of a record, the newest first.
     *
     * @param entity the record's entity name
     * @param recordId the record's key
     * @return the links
     */
    public static List<Link> list(String entity, Object recordId) {
        return service().list(entity, String.valueOf(recordId))
                        .stream()
                        .map(ShareLinks::link)
                        .toList();
    }

    /**
     * Revoke one link of a record, on behalf of the current user. It stops answering at once.
     *
     * @param entity the record's entity name
     * @param recordId the record's key
     * @param linkId the link
     * @return whether a link was revoked - {@code false} when the record has no such live link
     */
    public static boolean revoke(String entity, Object recordId, String linkId) {
        return service().revoke(entity, String.valueOf(recordId), linkId, User.getName());
    }

    /**
     * Revoke every link of a record, on behalf of the current user - for a record that left the state
     * it may be shared in, or is being deleted.
     *
     * @param entity the record's entity name
     * @param recordId the record's key
     * @return how many links were revoked
     */
    public static int revokeAll(String entity, Object recordId) {
        return service().revokeAll(entity, String.valueOf(recordId), User.getName());
    }

    private static Link link(ShareLink link) {
        return new Link(link.id(), link.fileName(), link.createdBy(), link.createdAt(), link.expiresAt(), link.revokedBy(),
                link.revokedAt(), link.openCount(), link.lastOpenedAt(), link.status(Instant.now()));
    }

    private static ShareLinkService service() {
        return Beans.get(ShareLinkService.class);
    }
}
