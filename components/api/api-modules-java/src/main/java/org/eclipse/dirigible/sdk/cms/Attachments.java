/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.cms;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.dirigible.components.api.cms.AttachmentsFacade;

/**
 * Client SDK for record attachments: store an uploaded file against a master entity (into the
 * tenant CMS under {@code /Attachments/<Master>/<yyyy>/<MM>/<uuid>/<file>}), and open a stored file
 * for download. The owning entity keeps the returned {@link Attachment#path()} (plus its metadata)
 * as its handle; the bytes live in the CMS and are browseable in the Documents perspective.
 *
 * <p>
 * Intended for the generated attachment controllers (upload/download/delete verbs) and for
 * hand-written {@code custom/} code; authorization is the caller's responsibility (the generated
 * controller checks the master entity's role before calling here).
 */
public final class Attachments {

    private Attachments() {}

    /**
     * Store an uploaded file as a new attachment of the given master entity.
     *
     * @param masterEntity the owning entity type name (e.g. {@code Company})
     * @param fileName the original file name
     * @param contentType the MIME type
     * @param content the file bytes
     * @return the stored attachment's metadata (path, name, content type, size, uuid)
     */
    public static Attachment store(String masterEntity, String fileName, String contentType, byte[] content) {
        try {
            AttachmentsFacade.StoredAttachment stored = AttachmentsFacade.store(masterEntity, fileName, contentType, content);
            return new Attachment(stored.path(), stored.fileName(), stored.contentType(), stored.size(), stored.uuid());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store attachment [" + fileName + "] for [" + masterEntity + "]", e);
        }
    }

    /**
     * Open a stored attachment's content for reading. The caller must consume/close the stream.
     *
     * @param path the stored attachment path
     * @return the content stream
     */
    public static InputStream open(String path) {
        try {
            return AttachmentsFacade.read(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open attachment [" + path + "]", e);
        }
    }

    /**
     * Delete a stored attachment file. A missing file is a no-op.
     *
     * @param path the stored attachment path
     */
    public static void delete(String path) {
        try {
            AttachmentsFacade.delete(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete attachment [" + path + "]", e);
        }
    }

    /**
     * Metadata of a stored attachment - the handle the owning entity records.
     *
     * @param path the full CMS document path
     * @param fileName the stored file name
     * @param contentType the MIME type
     * @param size the size in bytes
     * @param uuid the per-upload folder identifier
     */
    public record Attachment(String path, String fileName, String contentType, long size, String uuid) {
    }
}
