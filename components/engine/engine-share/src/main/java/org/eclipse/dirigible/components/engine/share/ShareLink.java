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

import java.time.Instant;

/**
 * One share link of the current tenant: the handle through which a reader without an account opens
 * one stored document (issue #8613). It never carries the token - only its hash is stored, and the
 * plain value is handed out once, by {@link ShareLinkService#create}.
 *
 * @param id the link's id - what a revoke names, and what the audit trail of opens refers to
 * @param entity the entity the shared record belongs to
 * @param recordId the shared record's key
 * @param storagePath the one stored file the link serves
 * @param fileName the file name the reader receives
 * @param contentType the stored file's content type
 * @param createdBy who created the link
 * @param createdAt when it was created
 * @param expiresAt when it stops answering, {@code null} for a permanent link
 * @param revokedBy who revoked it, {@code null} while it is not revoked
 * @param revokedAt when it was revoked, {@code null} while it is not revoked
 * @param openCount how many times it was opened
 * @param lastOpenedAt when it was last opened, {@code null} when it never was
 */
public record ShareLink(String id, String entity, String recordId, String storagePath, String fileName, String contentType,
        String createdBy, Instant createdAt, Instant expiresAt, String revokedBy, Instant revokedAt, long openCount, Instant lastOpenedAt) {

    /** A link that answers. */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** A link past its expiry. */
    public static final String STATUS_EXPIRED = "EXPIRED";

    /** A link somebody revoked. */
    public static final String STATUS_REVOKED = "REVOKED";

    /**
     * Whether the link answers at the given moment: not revoked and not past its expiry.
     *
     * @param now the moment
     * @return whether it answers
     */
    public boolean isLive(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    /**
     * The link's state at the given moment - a revoke outranks an expiry, because it is the one a
     * person decided.
     *
     * @param now the moment
     * @return {@link #STATUS_ACTIVE}, {@link #STATUS_EXPIRED} or {@link #STATUS_REVOKED}
     */
    public String status(Instant now) {
        if (revokedAt != null) {
            return STATUS_REVOKED;
        }
        return isLive(now) ? STATUS_ACTIVE : STATUS_EXPIRED;
    }
}
