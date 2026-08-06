/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.documents;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Packs a CMS folder into a zip and unpacks a zip into a CMS folder.
 * <p>
 * The CMIS layer has no archive support, so the traversal lives here: writing streams entry by
 * entry into the response, reading creates the intermediate folders as it goes.
 */
@Component
public class DocumentZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentZipService.class);

    private final CmsService cmsService;
    private final DocumentsService documentsService;

    DocumentZipService(CmsService cmsService, DocumentsService documentsService) {
        this.cmsService = cmsService;
        this.documentsService = documentsService;
    }

    /**
     * Writes a folder and everything under it into the given stream as a zip.
     *
     * @param folderPath the folder to pack
     * @param output where the archive is written
     * @param request the current request
     * @throws IOException when the CMS cannot be read or the stream cannot be written
     */
    public void pack(String folderPath, OutputStream output, HttpServletRequest request) throws IOException {
        CmisFolder folder = documentsService.folderOrRoot(folderPath);
        // Reuse the listing gate: a folder the caller may not read must not be packable either.
        documentsService.list(folder.getPath(), request);
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            packFolder(folder, "", zip);
        }
    }

    private void packFolder(CmisFolder folder, String prefix, ZipOutputStream zip) throws IOException {
        for (CmisObject child : folder.getChildren()) {
            String entryName = prefix + child.getName();
            if (DocumentsService.isHidden(DocumentsService.childPath(folder.getPath(), child.getName()))) {
                continue;
            }
            if (DocumentsService.isFolder(child)) {
                // No explicit directory entry: it would carry no content, and every reader recreates
                // the tree from the file paths. A folder with no files anywhere under it is therefore
                // absent from the archive - the documented trade-off for not writing empty entries.
                packFolder((CmisFolder) child, entryName + "/", zip);
            } else {
                zip.putNextEntry(new ZipEntry(entryName));
                try (InputStream content = cmsService.getDocumentStream((CmisDocument) child)) {
                    IOUtils.copy(content, zip);
                } catch (IOException e) {
                    LOGGER.error("Failed to pack [{}]", entryName, e);
                    throw e;
                }
                zip.closeEntry();
            }
        }
    }

    /**
     * Unpacks an archive into a folder, creating intermediate folders as needed.
     *
     * @param folderPath the target folder
     * @param archive the archive
     * @param request the current request
     * @throws IOException when the CMS cannot be written or the archive is unreadable
     */
    public void unpack(String folderPath, InputStream archive, HttpServletRequest request) throws IOException {
        CmisFolder target = documentsService.folderOrRoot(folderPath);
        try (ZipInputStream zip = new ZipInputStream(archive)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = safeEntryName(entry.getName());
                if (name.isBlank()) {
                    continue;
                }
                if (entry.isDirectory()) {
                    resolveFolder(target, name);
                    continue;
                }
                int separator = name.lastIndexOf('/');
                CmisFolder parent = separator < 0 ? target : resolveFolder(target, name.substring(0, separator));
                String fileName = separator < 0 ? name : name.substring(separator + 1);
                byte[] content = IOUtils.toByteArray(zip);
                documentsService.upload(parent.getPath(), fileName, null, content.length, new ByteArrayInputStream(content), true, request);
            }
        }
    }

    /**
     * The name an archive entry may be unpacked under: relative, separator-normalized, and with no
     * traversal segment.
     * <p>
     * An archive is untrusted input, so an entry named {@code ../../elsewhere} must never be able to
     * write outside the folder it is unpacked into - the target here is a CMS path, but the internal
     * provider maps it onto real storage, so the escape would be real.
     *
     * @param name the entry name as the archive declares it
     * @return the name to use, empty when the entry must be skipped
     */
    static String safeEntryName(String name) {
        if (name == null) {
            return "";
        }
        Deque<String> segments = new ArrayDeque<>();
        for (String segment : name.replace('\\', '/')
                                  .split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                // Resolve it rather than drop it, so a legitimate but unnormalized entry still lands
                // where its author meant - and clamp at the root, which is what keeps it contained.
                segments.pollLast();
                continue;
            }
            segments.addLast(segment);
        }
        return String.join("/", segments);
    }

    /** The folder for a relative archive path, created segment by segment when missing. */
    private CmisFolder resolveFolder(CmisFolder root, String relativePath) throws IOException {
        CmisFolder current = root;
        for (String segment : relativePath.split("/")) {
            if (segment.isBlank()) {
                continue;
            }
            CmisFolder existing = cmsService.getChildFolderByName(current, segment);
            current = existing != null ? existing : cmsService.createFolder(current, segment);
        }
        return current;
    }
}
