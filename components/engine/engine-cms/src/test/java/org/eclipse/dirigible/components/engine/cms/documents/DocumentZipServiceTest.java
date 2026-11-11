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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The walk that packs a CMS tree: for a request it packs the folder the caller may list, and for a
 * caller that packs on its own account it takes a prefix and a rule for what to leave out.
 */
class DocumentZipServiceTest {

    private CmsService cmsService;
    private DocumentsService documentsService;
    private DocumentZipService zipService;

    /**
     * The tree: /contracts/acme.pdf, /contracts/2026/q1.pdf, /logo.png, /__internal/state.json,
     * /__EXPORTS/dump.csv.
     */
    @BeforeEach
    void setUp() throws IOException {
        cmsService = mock(CmsService.class);
        documentsService = mock(DocumentsService.class);
        CmisFolder year = folder("/contracts/2026", "2026", document("q1.pdf", "q1"));
        CmisFolder contracts = folder("/contracts", "contracts", document("acme.pdf", "acme"), year);
        CmisFolder internal = folder("/__internal", "__internal", document("state.json", "{}"));
        CmisFolder exports = folder("/__EXPORTS", "__EXPORTS", document("dump.csv", "A,B"));
        CmisFolder root = folder("/", "", contracts, document("logo.png", "png"), internal, exports);
        when(documentsService.folderOrRoot("/")).thenReturn(root);
        when(documentsService.folderOrRoot(null)).thenReturn(root);
        when(documentsService.folderOrRoot("/contracts")).thenReturn(contracts);
        zipService = new DocumentZipService(cmsService, documentsService);
    }

    @Test
    void aTreeIsPackedUnderThePrefixWithoutWhatTheRuleLeavesOut() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int documents;
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            documents = zipService.packTree("/", "files/", path -> path.equals("/__EXPORTS") || path.startsWith("/__EXPORTS/"), zip);
            // the archive is the caller's: it can still add entries of its own
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write("{}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        Map<String, String> entries = entries(out);
        assertEquals(List.of("files/contracts/acme.pdf", "files/contracts/2026/q1.pdf", "files/logo.png", "manifest.json"),
                List.copyOf(entries.keySet()));
        assertEquals("q1", entries.get("files/contracts/2026/q1.pdf"));
        assertEquals(3, documents);
    }

    @Test
    void theBookkeepingFolderIsNeverPackedWhateverTheRule() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zipService.packTree("/", "", path -> false, zip);
        }

        assertEquals(List.of("contracts/acme.pdf", "contracts/2026/q1.pdf", "logo.png", "__EXPORTS/dump.csv"),
                List.copyOf(entries(out).keySet()));
    }

    @Test
    void aRuleCanLeaveOutASingleDocument() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int documents;
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            documents = zipService.packTree("/contracts", "", path -> path.equals("/contracts/acme.pdf"), zip);
        }

        assertEquals(List.of("2026/q1.pdf"), List.copyOf(entries(out).keySet()));
        assertEquals(1, documents);
    }

    @Test
    void aRequestStillPacksTheFolderItMayList() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        zipService.pack("/contracts", out, request);

        // the listing gate is asked for the folder, with the caller's request
        verify(documentsService).list("/contracts", request);
        assertEquals(List.of("acme.pdf", "2026/q1.pdf"), List.copyOf(entries(out).keySet()));
    }

    @Test
    void packingForARequestAsksTheListingGateAndPackingATreeDoesNot() throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new ByteArrayOutputStream())) {
            zipService.packTree("/contracts", "files/", path -> false, zip);
        }

        verify(documentsService).folderOrRoot("/contracts");
        verifyNoMoreInteractions(documentsService);
    }

    private static Map<String, String> entries(ByteArrayOutputStream out) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private static CmisFolder folder(String path, String name, CmisObject... children) throws IOException {
        CmisFolder folder = mock(CmisFolder.class);
        when(folder.getPath()).thenReturn(path);
        when(folder.getName()).thenReturn(name);
        when(folder.getType()).thenReturn(ObjectType.FOLDER);
        doReturn(List.of(children)).when(folder)
                                   .getChildren();
        return folder;
    }

    private CmisDocument document(String name, String content) throws IOException {
        CmisDocument document = mock(CmisDocument.class);
        when(document.getName()).thenReturn(name);
        when(document.getType()).thenReturn(ObjectType.DOCUMENT);
        when(cmsService.getDocumentStream(document)).thenAnswer(
                invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return document;
    }
}
