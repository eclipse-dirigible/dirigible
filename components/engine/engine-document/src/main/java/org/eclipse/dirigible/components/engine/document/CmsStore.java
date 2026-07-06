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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
 * <li><b>Print reads</b> — {@link #listLanguages(String)} / {@link #findTemplate(String, String)}
 * resolve print templates under {@code Templates/<EntityName>/Print/<language>/} for the print
 * endpoint.</li>
 * </ul>
 */
@Component
class CmsStore {

    private static final Logger logger = LoggerFactory.getLogger(CmsStore.class);

    private static final String TEMPLATES_ROOT = "Templates";
    private static final String PRINT_SEGMENT = "Print";
    private static final String TEMPLATE_EXTENSION = ".print";
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
        String normalized = cmsPath.startsWith(PATH_SEPARATOR) ? cmsPath : PATH_SEPARATOR + cmsPath;
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
     * Lists the language codes for which the given entity has print templates — the child folder names
     * of {@code Templates/<EntityName>/Print}.
     *
     * @param entityName the domain entity name
     * @return the language codes, empty when the folder is missing
     * @throws IOException on CMS access failure
     */
    List<String> listLanguages(String entityName) throws IOException {
        CmisSession session = CmisSessionFactory.getSession();
        Optional<CmisFolder> printFolder = findFolder(session, printFolderPath(entityName));
        if (printFolder.isEmpty()) {
            return List.of();
        }
        List<String> languages = new ArrayList<>();
        for (CmisObject child : printFolder.get()
                                           .getChildren()) {
            if (child instanceof CmisFolder) {
                languages.add(child.getName());
            }
        }
        return languages;
    }

    /**
     * Finds the print template for the given entity and language — the first {@code .print} document
     * (or any document as fallback) under {@code Templates/<EntityName>/Print/<language>}.
     *
     * @param entityName the domain entity name
     * @param language the language code
     * @return the template source, empty when no template exists
     * @throws IOException on CMS access failure
     */
    Optional<String> findTemplate(String entityName, String language) throws IOException {
        CmisSession session = CmisSessionFactory.getSession();
        Optional<CmisFolder> languageFolder = findFolder(session, printFolderPath(entityName) + PATH_SEPARATOR + language);
        if (languageFolder.isEmpty()) {
            return Optional.empty();
        }
        CmisDocument template = null;
        for (CmisObject child : languageFolder.get()
                                              .getChildren()) {
            if (child instanceof CmisDocument document) {
                if (child.getName()
                         .toLowerCase()
                         .endsWith(TEMPLATE_EXTENSION)) {
                    template = document;
                    break;
                }
                if (template == null) {
                    template = document;
                }
            }
        }
        if (template == null) {
            return Optional.empty();
        }
        return Optional.of(readContent(template));
    }

    private String printFolderPath(String entityName) {
        return PATH_SEPARATOR + TEMPLATES_ROOT + PATH_SEPARATOR + entityName + PATH_SEPARATOR + PRINT_SEGMENT;
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

    private String readContent(CmisDocument document) throws IOException {
        try (InputStream inputStream = document.getContentStream()
                                               .getStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
