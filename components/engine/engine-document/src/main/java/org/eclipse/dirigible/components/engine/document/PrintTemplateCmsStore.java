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
 * The single CMS access point for print templates. Templates live under
 * {@code Templates/<EntityName>/Print/<language>/} in the (tenant-scoped) CMS; the synchronizer
 * seeds them there and the print endpoint reads them back.
 */
@Component
class PrintTemplateCmsStore {

    private static final Logger logger = LoggerFactory.getLogger(PrintTemplateCmsStore.class);

    private static final String TEMPLATES_ROOT = "Templates";
    private static final String PRINT_SEGMENT = "Print";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_TEMPLATE_NAME = "standard.print";
    private static final String TEMPLATE_EXTENSION = ".print";
    private static final String TEMPLATE_MEDIA_TYPE = "text/xml";
    private static final String PATH_SEPARATOR = "/";

    /**
     * Seeds the default template for the given entity at
     * {@code Templates/<EntityName>/Print/en/standard.print} — create-if-absent only: an already
     * existing document is a user customization and is never overwritten.
     *
     * @param entityName the domain entity name
     * @param content the template source
     * @throws IOException on CMS access failure
     */
    void seedDefaultTemplate(String entityName, String content) throws IOException {
        String folderPath = printFolderPath(entityName) + PATH_SEPARATOR + DEFAULT_LANGUAGE;
        String documentPath = folderPath + PATH_SEPARATOR + DEFAULT_TEMPLATE_NAME;

        CmisSession session = CmisSessionFactory.getSession();
        if (exists(session, documentPath)) {
            logger.debug("Print template [{}] already exists - keeping the user's version", documentPath);
            return;
        }
        CmisFolder folder = ensureFolder(session, folderPath);
        createDocument(session, folder, DEFAULT_TEMPLATE_NAME, content);
        logger.info("Seeded print template [{}]", documentPath);
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

    private void createDocument(CmisSession session, CmisFolder folder, String name, String content) throws IOException {
        Map<String, String> properties = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, CmisConstants.NAME, name);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            CmisContentStream contentStream = session.getObjectFactory()
                                                     .createContentStream(name, bytes.length, TEMPLATE_MEDIA_TYPE, inputStream);
            folder.createDocument(properties, contentStream);
        }
    }

    private String readContent(CmisDocument document) throws IOException {
        try (InputStream inputStream = document.getContentStream()
                                               .getStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
