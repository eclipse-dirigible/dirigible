/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.eclipse.dirigible.components.engine.cms.documents.DocumentZipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The archive's layout: the tables under {@code database/} as the schema export wrote them, the
 * documents under {@code files/}, and a manifest that says what is inside.
 */
class TenantExportArchiveTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-11T10:15:30Z");
    private static final String ARCHIVE_NAME = "tenant-acme-data-export-20261111-101530.zip";
    private static final String ARCHIVE = "/__EXPORTS/tenant-exports/7/" + ARCHIVE_NAME;

    private DocumentZipService zipService;
    private TenantExportArchives archives;

    @BeforeEach
    void setUp() throws IOException {
        zipService = mock(DocumentZipService.class);
        // the documents walk writes two files under the prefix it is given
        when(zipService.packTree(eq("/"), eq("files/"), any(), any())).thenAnswer(invocation -> {
            ZipOutputStream zip = invocation.getArgument(3);
            zip.putNextEntry(new ZipEntry("files/contracts/acme.pdf"));
            zip.write("pdf".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("files/logo.png"));
            zip.write("png".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            return 2;
        });
        archives = new TenantExportArchives(zipService);
    }

    @Test
    void theArchiveHoldsTheTablesTheDocumentsAndAManifest() throws IOException {
        List<TenantExportArchives.WorkFile> database = List.of(document("export-topology.json", "[\"LIBRARY_AUTHOR\",\"LIBRARY_BOOK\"]"),
                document("LIBRARY_BOOK.json", "{}"), document("LIBRARY_BOOK.csv", "BOOK_ID,BOOK_TITLE\n1,Dune\n2,\"Line one\nline two\"\n"),
                document("LIBRARY_AUTHOR.json", "{}"), document("LIBRARY_AUTHOR.csv", "AUTHOR_ID,AUTHOR_NAME\n"));

        Map<String, String> entries = write(database);

        assertEquals(List.of("database/export-topology.json", "database/LIBRARY_BOOK.json", "database/LIBRARY_BOOK.csv",
                "database/LIBRARY_AUTHOR.json", "database/LIBRARY_AUTHOR.csv", "files/contracts/acme.pdf", "files/logo.png",
                "manifest.json"), List.copyOf(entries.keySet()));
        assertEquals("BOOK_ID,BOOK_TITLE\n1,Dune\n2,\"Line one\nline two\"\n", entries.get("database/LIBRARY_BOOK.csv"),
                "a table's CSV is copied as the schema export wrote it, so import-schema can read it back");

        JsonObject manifest = JsonParser.parseString(entries.get("manifest.json"))
                                        .getAsJsonObject();
        assertEquals(1, manifest.get("format")
                                .getAsInt());
        assertEquals("acme", manifest.get("tenantId")
                                     .getAsString());
        assertEquals("2026-11-11T10:15:30Z", manifest.get("createdAt")
                                                     .getAsString());
        assertTrue(manifest.has("platformVersion"));
        assertEquals(2, manifest.get("files")
                                .getAsInt());
        JsonArray tables = manifest.getAsJsonArray("tables");
        assertEquals(2, tables.size());
        assertEquals("LIBRARY_AUTHOR", tables.get(0)
                                             .getAsJsonObject()
                                             .get("name")
                                             .getAsString());
        assertEquals(0, tables.get(0)
                              .getAsJsonObject()
                              .get("rows")
                              .getAsLong());
        assertEquals("LIBRARY_BOOK", tables.get(1)
                                           .getAsJsonObject()
                                           .get("name")
                                           .getAsString());
        assertEquals(2, tables.get(1)
                              .getAsJsonObject()
                              .get("rows")
                              .getAsLong(),
                "a line break inside a quoted value does not start a row");
    }

    @Test
    void aTenantWithoutTablesStillGetsItsDocuments() throws IOException {
        Map<String, String> entries = write(List.of());

        assertEquals(List.of("files/contracts/acme.pdf", "files/logo.png", "manifest.json"), List.copyOf(entries.keySet()));
        assertEquals(0, JsonParser.parseString(entries.get("manifest.json"))
                                  .getAsJsonObject()
                                  .getAsJsonArray("tables")
                                  .size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void theDocumentsLeaveOutTheExportsFolderWithTheWorkFolderAndEarlierArchives() throws IOException {
        write(List.of());

        ArgumentCaptor<Predicate<String>> skip = ArgumentCaptor.forClass(Predicate.class);
        verify(zipService).packTree(eq("/"), eq("files/"), skip.capture(), any());
        // __internal is left out by the documents walk itself
        assertTrue(skip.getValue()
                       .test("/__EXPORTS"));
        assertTrue(skip.getValue()
                       .test("/__EXPORTS/tenant-exports/7/database"));
        assertTrue(skip.getValue()
                       .test("/__EXPORTS/tenant-exports/3/acme-20261101-080000.zip"));
        assertFalse(skip.getValue()
                        .test("/contracts"));
        assertFalse(skip.getValue()
                        .test("/__EXPORTS_OF_MINE/report.pdf"));
        assertFalse(skip.getValue()
                        .test("/contracts/__EXPORTS/report.pdf"));
    }

    @Test
    void theArchiveAndItsWorkFolderAreUnderTheTenantExportsFolder() {
        assertEquals("/__EXPORTS/tenant-exports/7", TenantExportArchives.exportFolder(7));
        assertEquals("/__EXPORTS/tenant-exports/7/database", TenantExportArchives.workFolder(7));
        assertEquals("tenant-acme-data-export-20261111-101530.zip", TenantExportArchives.archiveName("acme", CREATED_AT));
    }

    @Test
    void csvRecordsAreCountedWhateverTheLineEndingAndTheLastLine() throws IOException {
        assertEquals(0, records(""));
        assertEquals(1, records("A,B\n"));
        assertEquals(1, records("A,B"));
        assertEquals(3, records("A,B\r\n1,2\r\n3,4\r\n"));
        assertEquals(2, records("A\n\"x\ny\"\"z\n\"\n"));
        assertEquals(2, records("A\n1\n\n"));
    }

    @Test
    void aStoredArchiveIsOpened() throws IOException {
        CmisFolder root =
                cmisFolder("", cmisFolder("__EXPORTS", cmisFolder("tenant-exports", cmisFolder("7", cmisDocument(ARCHIVE_NAME)))));

        Optional<InputStream> content = open(root, ARCHIVE);

        assertTrue(content.isPresent());
        assertEquals(ARCHIVE_NAME, new String(content.get()
                                                     .readAllBytes(),
                StandardCharsets.UTF_8));
    }

    @Test
    void anArchiveThatIsNotStoredIsMissingNotAFailure() throws IOException {
        CmisFolder withoutTheFolder = cmisFolder("", cmisFolder("__EXPORTS", cmisFolder("tenant-exports")));
        CmisFolder withoutTheFile = cmisFolder("", cmisFolder("__EXPORTS", cmisFolder("tenant-exports", cmisFolder("7"))));
        CmisFolder withAFolderOfItsName =
                cmisFolder("", cmisFolder("__EXPORTS", cmisFolder("tenant-exports", cmisFolder("7", cmisFolder(ARCHIVE_NAME)))));

        assertEquals(Optional.empty(), open(withoutTheFolder, ARCHIVE));
        assertEquals(Optional.empty(), open(withoutTheFile, ARCHIVE));
        assertEquals(Optional.empty(), open(withAFolderOfItsName, ARCHIVE));
    }

    @Test
    void aStorageThatCannotBeReadIsAFailure() throws IOException {
        CmisFolder exportFolder = cmisFolder("7");
        when(exportFolder.getChildren()).thenThrow(new IOException("the CMS is away"));
        CmisFolder root = cmisFolder("", cmisFolder("__EXPORTS", cmisFolder("tenant-exports", exportFolder)));

        assertThrows(IOException.class, () -> open(root, ARCHIVE));
    }

    private Optional<InputStream> open(CmisFolder root, String path) throws IOException {
        CmisSession session = mock(CmisSession.class);
        when(session.getRootFolder()).thenReturn(root);
        try (MockedStatic<CmisSessionFactory> factory = mockStatic(CmisSessionFactory.class)) {
            factory.when(CmisSessionFactory::getSession)
                   .thenReturn(session);
            return archives.open(path);
        }
    }

    private static CmisFolder cmisFolder(String name, CmisObject... children) throws IOException {
        CmisFolder folder = mock(CmisFolder.class);
        when(folder.getName()).thenReturn(name);
        doReturn(List.of(children)).when(folder)
                                   .getChildren();
        return folder;
    }

    /** A document whose content is its own name. */
    private static CmisDocument cmisDocument(String name) throws IOException {
        CmisDocument document = mock(CmisDocument.class);
        when(document.getName()).thenReturn(name);
        CmisContentStream content = mock(CmisContentStream.class);
        when(content.getInputStream()).thenReturn(new ByteArrayInputStream(name.getBytes(StandardCharsets.UTF_8)));
        when(document.getContentStream()).thenReturn(content);
        return document;
    }

    private static long records(String csv) throws IOException {
        return TenantExportArchives.copyCountingCsvRecords(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream());
    }

    private Map<String, String> write(List<TenantExportArchives.WorkFile> database) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        archives.write(out, "acme", CREATED_AT, database);
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private static TenantExportArchives.WorkFile document(String name, String content) {
        return new TenantExportArchives.WorkFile(name, () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
