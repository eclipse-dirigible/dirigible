/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The single CMS access point for the document engine. It has two sides:
 * <ul>
 * <li><b>Seeding</b> — {@link #seed(String, byte[])} copies a file placed under a project's
 * {@code doc/} folder into the (tenant-scoped) CMS at the mirrored path, <b>create-if-absent</b>:
 * an already existing document is a user customization and is never overwritten. Generic — any
 * path, any content.</li>
 * <li><b>Plain file operations</b> — list, read, write, move and delete documents and folders. The
 * CMS stays a plain file store: what a print template, a version or the active layout is, is
 * {@link PrintTemplateCatalog}'s knowledge, built on these operations.</li>
 * <li><b>Binary reads</b> — {@link #readDocument(String, long)} reads any document's raw bytes and
 * media type, bounded, for an image a print template embeds.</li>
 * </ul>
 */
@Component
class CmsStore {

    private static final Logger logger = LoggerFactory.getLogger(CmsStore.class);

    private static final String PATH_SEPARATOR = "/";
    private static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";

    /**
     * Seeds a file into the CMS at the given absolute path — create-if-absent only: an already existing
     * document is a user customization and is never overwritten. Missing parent folders are created.
     *
     * @param cmsPath the absolute CMS path, e.g.
     *        {@code /Templates/SalesInvoice/Print/en/standard.print}
     * @param content the raw file content
     * @throws IOException on CMS access failure
     */
    void seed(String cmsPath, byte[] content) throws IOException {
        String normalized = normalize(cmsPath);
        int lastSeparator = normalized.lastIndexOf(PATH_SEPARATOR);
        String folderPath = normalized.substring(0, lastSeparator);
        String documentName = normalized.substring(lastSeparator + 1);

        CmisSession session = CmisSessionFactory.getSession();
        if (exists(session, normalized)) {
            logger.debug("CMS document [{}] already exists - keeping the existing version", normalized);
            return;
        }
        CmisFolder folder = folderPath.isEmpty() ? session.getRootFolder() : ensureFolder(session, folderPath);
        createDocument(session, folder, documentName, content);
        logger.info("Seeded CMS document [{}]", normalized);
    }

    /**
     * Lists the names of the folders directly under the given folder.
     *
     * @param folderPath the absolute CMS folder path
     * @return the child folder names, empty when the folder is missing
     * @throws IOException on CMS access failure
     */
    List<String> listFolders(String folderPath) throws IOException {
        return listChildren(folderPath, CmisFolder.class);
    }

    /**
     * Lists the names of the documents directly under the given folder.
     *
     * @param folderPath the absolute CMS folder path
     * @return the child document names, empty when the folder is missing
     * @throws IOException on CMS access failure
     */
    List<String> listDocuments(String folderPath) throws IOException {
        return listChildren(folderPath, CmisDocument.class);
    }

    /**
     * Reads a document's whole content.
     *
     * @param cmsPath the absolute CMS path
     * @return the content, empty when the path names no document
     * @throws IOException on CMS access failure other than absence
     */
    Optional<byte[]> read(String cmsPath) throws IOException {
        Optional<CmisDocument> document = findDocument(CmisSessionFactory.getSession(), normalize(cmsPath));
        if (document.isEmpty()) {
            return Optional.empty();
        }
        try (InputStream inputStream = document.get()
                                               .getContentStream()
                                               .getStream()) {
            return Optional.of(inputStream.readAllBytes());
        }
    }

    /**
     * Writes a document, replacing an existing one at the same path. Missing parent folders are
     * created.
     *
     * <p>
     * The CMS has no in-place content update and no rename every backend honours, so a replacement is a
     * delete followed by a create. Should the create fail, the previous content is written back before
     * the failure is reported - a failed write never costs the document it was replacing.
     *
     * @param cmsPath the absolute CMS path
     * @param content the raw content
     * @throws IOException on CMS access failure
     */
    void write(String cmsPath, byte[] content) throws IOException {
        String normalized = normalize(cmsPath);
        CmisSession session = CmisSessionFactory.getSession();
        int lastSeparator = normalized.lastIndexOf(PATH_SEPARATOR);
        String folderPath = normalized.substring(0, lastSeparator);
        String documentName = normalized.substring(lastSeparator + 1);
        CmisFolder folder = folderPath.isEmpty() ? session.getRootFolder() : ensureFolder(session, folderPath);
        Optional<CmisDocument> existing = findDocument(session, normalized);
        if (existing.isEmpty()) {
            createDocument(session, folder, documentName, content);
            return;
        }
        byte[] previous;
        try (InputStream inputStream = existing.get()
                                               .getContentStream()
                                               .getStream()) {
            previous = inputStream.readAllBytes();
        }
        existing.get()
                .delete();
        try {
            createDocument(session, folder, documentName, content);
        } catch (IOException | RuntimeException e) {
            try {
                createDocument(session, folder, documentName, previous);
                logger.warn("CMS document [{}] could not be replaced - its previous content was restored", LoggedPath.of(normalized), e);
            } catch (IOException | RuntimeException restore) {
                e.addSuppressed(restore);
                logger.error("CMS document [{}] could not be replaced, nor its previous content ({} bytes) restored",
                        LoggedPath.of(normalized), previous.length, e);
            }
            throw e;
        }
    }

    /**
     * Moves a document to another name in its folder by copying it there and deleting the original -
     * never through the CMS rename, which the S3 backend does not implement (it silently does nothing)
     * and SharePoint refuses. The copy is read back before the original is deleted; should the delete
     * fail, the copy is removed again, so a failed move leaves the document where it was.
     *
     * @param cmsPath the absolute CMS path of the document
     * @param newName the new document name
     * @param content the content the moved document gets - its own, or a rewritten one
     * @throws IOException on CMS access failure, when the path names no document or the new name is
     *         taken
     */
    void move(String cmsPath, String newName, byte[] content) throws IOException {
        String normalized = normalize(cmsPath);
        CmisSession session = CmisSessionFactory.getSession();
        CmisDocument original =
                findDocument(session, normalized).orElseThrow(() -> new IOException("CMS document [" + normalized + "] does not exist"));
        String folderPath = normalized.substring(0, normalized.lastIndexOf(PATH_SEPARATOR));
        String target = folderPath + PATH_SEPARATOR + newName;
        if (exists(session, target)) {
            throw new IOException("CMS document [" + target + "] already exists");
        }
        CmisFolder folder = folderPath.isEmpty() ? session.getRootFolder() : ensureFolder(session, folderPath);
        createDocument(session, folder, newName, content);
        Optional<byte[]> copied = read(target);
        if (copied.isEmpty() || !Arrays.equals(copied.get(), content)) {
            delete(target);
            throw new IOException("CMS document [" + target + "] did not read back as written - [" + normalized + "] is left in place");
        }
        try {
            original.delete();
        } catch (IOException | RuntimeException e) {
            delete(target);
            throw e;
        }
    }

    /**
     * Deletes a document.
     *
     * @param cmsPath the absolute CMS path
     * @return true if a document was deleted, false when the path names none
     * @throws IOException on CMS access failure
     */
    boolean delete(String cmsPath) throws IOException {
        Optional<CmisDocument> document = findDocument(CmisSessionFactory.getSession(), normalize(cmsPath));
        if (document.isEmpty()) {
            return false;
        }
        document.get()
                .delete();
        return true;
    }

    /**
     * Reads a document's raw bytes and media type, refusing anything larger than {@code maxBytes}.
     *
     * <p>
     * The bound is checked twice on purpose: against the declared length first (so an oversized
     * document is never streamed at all), and again while reading, because a CMS backend may report no
     * length or a stale one and the caller's ceiling exists to protect the heap.
     *
     * @param cmsPath the absolute CMS path
     * @param maxBytes the largest content that may be read, in bytes
     * @return the content, empty when the path names no document or the content exceeds the bound
     * @throws IOException on CMS access failure other than absence
     */
    Optional<Content> readDocument(String cmsPath, long maxBytes) throws IOException {
        String normalized = normalize(cmsPath);
        CmisSession session = CmisSessionFactory.getSession();
        CmisObject object;
        try {
            object = session.getObjectByPath(normalized);
        } catch (IOException ex) {
            logger.debug("CMS document [{}] does not exist", LoggedPath.of(normalized), ex);
            return Optional.empty();
        }
        if (!(object instanceof CmisDocument document)) {
            logger.debug("CMS object [{}] is not a document", LoggedPath.of(normalized));
            return Optional.empty();
        }
        CmisContentStream stream = document.getContentStream();
        if (stream.getLength() > maxBytes) {
            logger.warn("CMS document [{}] is {} bytes, above the {} byte limit - it is skipped", LoggedPath.of(normalized),
                    stream.getLength(), maxBytes);
            return Optional.empty();
        }
        byte[] content;
        try (InputStream inputStream = stream.getStream()) {
            content = inputStream.readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE));
        }
        if (content.length > maxBytes) {
            logger.warn("CMS document [{}] is above the {} byte limit - it is skipped", LoggedPath.of(normalized), maxBytes);
            return Optional.empty();
        }
        String mediaType = stream.getMimeType();
        return Optional.of(new Content(content, mediaType == null || mediaType.isBlank() ? mediaType(normalized) : mediaType));
    }

    /**
     * A document's content as read from the CMS.
     *
     * @param content the raw bytes
     * @param mediaType the media type the CMS reports, falling back to the one implied by the extension
     */
    record Content(byte[] content, String mediaType) {
    }

    private static String normalize(String cmsPath) {
        return cmsPath.startsWith(PATH_SEPARATOR) ? cmsPath : PATH_SEPARATOR + cmsPath;
    }

    private <T extends CmisObject> List<String> listChildren(String folderPath, Class<T> kind) throws IOException {
        Optional<CmisFolder> folder = findFolder(CmisSessionFactory.getSession(), normalize(folderPath));
        if (folder.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (CmisObject child : folder.get()
                                      .getChildren()) {
            if (kind.isInstance(child)) {
                // The S3 CMS names a child folder with its trailing separator (en/) while the
                // internal CMS does not - normalize so the names are backend-neutral.
                String name = child.getName();
                while (name.endsWith(PATH_SEPARATOR)) {
                    name = name.substring(0, name.length() - 1);
                }
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private Optional<CmisDocument> findDocument(CmisSession session, String path) {
        try {
            if (session.getObjectByPath(path) instanceof CmisDocument document) {
                return Optional.of(document);
            }
            return Optional.empty();
        } catch (IOException ex) {
            logger.debug("CMS document [{}] does not exist", LoggedPath.of(path), ex);
            return Optional.empty();
        }
    }

    /**
     * The CMS signals a missing object with an {@code IOException} from {@code getObjectByPath} —
     * absence is an expected outcome here, not an error.
     */
    private boolean exists(CmisSession session, String path) {
        try {
            session.getObjectByPath(path);
            return true;
        } catch (IOException ex) {
            logger.debug("CMS object [{}] does not exist", path, ex);
            return false;
        }
    }

    private Optional<CmisFolder> findFolder(CmisSession session, String path) {
        try {
            CmisObject object = session.getObjectByPath(path);
            if (object instanceof CmisFolder folder) {
                return Optional.of(folder);
            }
            logger.warn("CMS object [{}] is not a folder but [{}]", path, object);
            return Optional.empty();
        } catch (IOException ex) {
            logger.debug("CMS folder [{}] does not exist", path, ex);
            return Optional.empty();
        }
    }

    /**
     * Walks the given absolute folder path level by level, creating each missing level —
     * {@code CmisFolder.createFolder} creates a single level only.
     */
    private CmisFolder ensureFolder(CmisSession session, String path) throws IOException {
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

    private void createDocument(CmisSession session, CmisFolder folder, String name, byte[] content) throws IOException {
        Map<String, String> properties = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, CmisConstants.NAME, name);
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            CmisContentStream contentStream = session.getObjectFactory()
                                                     .createContentStream(name, content.length, mediaType(name), inputStream);
            folder.createDocument(properties, contentStream);
        }
    }

    /** A best-effort media type from the file extension; the CMS stores the bytes regardless. */
    private static String mediaType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".print") || lower.endsWith(".xml")) {
            return "text/xml";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html";
        }
        if (lower.endsWith(".txt") || lower.endsWith(".csv")) {
            return "text/plain";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return DEFAULT_MEDIA_TYPE;
    }
}
