/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;

/**
 * Stores and reads record attachments in the CMS (the same tenant-scoped store the Documents
 * perspective browses), under the {@link AttachmentPath} layout
 * {@code /Attachments/<Master>/<yyyy>/<MM>/<uuid>/<file>}. The bytes live in the CMS; the calling
 * entity keeps only the returned path (+ metadata) as its handle.
 *
 * <p>
 * Reuses the folder-ensure + content-stream write pattern of the engine-document {@code CmsStore};
 * exposed to client code through the {@code org.eclipse.dirigible.sdk.cms.Attachments} SDK facade.
 * The CMS session is resolved per call via {@link CmisSessionFactory}, so writes/reads land in the
 * current tenant's store.
 */
public final class AttachmentsFacade {

    private static final String PATH_SEPARATOR = "/";

    private AttachmentsFacade() {}

    /**
     * Store an uploaded file as a new attachment of the given master entity.
     *
     * @param masterEntity the owning entity type name (e.g. {@code Company})
     * @param fileName the original file name
     * @param contentType the MIME type (best-effort; the bytes are stored regardless)
     * @param content the file bytes
     * @return the stored-attachment metadata (path, name, content type, size, uuid)
     * @throws IOException if the CMS write fails
     */
    public static StoredAttachment store(String masterEntity, String fileName, String contentType, byte[] content) throws IOException {
        String uuid = UUID.randomUUID()
                          .toString();
        LocalDate today = LocalDate.now();
        String folderPath = AttachmentPath.folder(masterEntity, today, uuid);
        String safeName = AttachmentPath.fileName(fileName);
        String documentPath = folderPath + PATH_SEPARATOR + safeName;

        CmisSession session = CmisSessionFactory.getSession();
        CmisFolder folder = ensureFolder(session, folderPath);
        Map<String, String> properties =
                Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, CmisConstants.NAME, safeName);
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            CmisContentStream stream = session.getObjectFactory()
                                              .createContentStream(safeName, content.length, contentType, inputStream);
            folder.createDocument(properties, stream);
        }
        return new StoredAttachment(documentPath, safeName, contentType, content.length, uuid);
    }

    /**
     * Open an attachment's content for reading (streaming to an HTTP response). The caller must consume
     * the stream before the request completes.
     *
     * @param path the stored attachment path
     * @return the content stream
     * @throws IOException if the object is missing or not a document
     */
    public static InputStream read(String path) throws IOException {
        CmisObject object = CmisSessionFactory.getSession()
                                              .getObjectByPath(path);
        if (object instanceof CmisDocument document) {
            return document.getContentStream()
                           .getStream();
        }
        throw new IOException("Attachment is not a document: " + path);
    }

    /**
     * Delete an attachment file from the CMS. A missing object is a no-op (idempotent).
     *
     * @param path the stored attachment path
     * @throws IOException if the delete fails for a reason other than absence
     */
    public static void delete(String path) throws IOException {
        CmisSession session = CmisSessionFactory.getSession();
        CmisObject object;
        try {
            object = session.getObjectByPath(path);
        } catch (IOException absent) {
            return;
        }
        object.delete();
    }

    /**
     * Ensure every folder level of {@code path} exists, creating the missing ones ({@code createFolder}
     * is single-level). Mirrors the engine-document {@code CmsStore} approach.
     */
    private static CmisFolder ensureFolder(CmisSession session, String path) throws IOException {
        CmisFolder current = session.getRootFolder();
        StringBuilder currentPath = new StringBuilder();
        for (String segment : path.split(PATH_SEPARATOR)) {
            if (segment.isEmpty()) {
                continue;
            }
            currentPath.append(PATH_SEPARATOR)
                       .append(segment);
            Optional<CmisFolder> existing = findFolder(session, currentPath.toString());
            if (existing.isPresent()) {
                current = existing.get();
            } else {
                current = current.createFolder(
                        Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_FOLDER, CmisConstants.NAME, segment));
            }
        }
        return current;
    }

    /** The CMS signals a missing object with an {@link IOException} from {@code getObjectByPath}. */
    private static Optional<CmisFolder> findFolder(CmisSession session, String path) {
        try {
            CmisObject object = session.getObjectByPath(path);
            return object instanceof CmisFolder folder ? Optional.of(folder) : Optional.empty();
        } catch (IOException absent) {
            return Optional.empty();
        }
    }

    /**
     * Metadata of a stored attachment - what the owning entity records as its handle.
     *
     * @param path the full CMS document path
     * @param fileName the (sanitized) stored file name
     * @param contentType the MIME type
     * @param size the size in bytes
     * @param uuid the per-upload folder identifier
     */
    public record StoredAttachment(String path, String fileName, String contentType, long size, String uuid) {
    }
}
