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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.eclipse.dirigible.components.engine.cms.documents.DocumentZipService;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;

/**
 * A tenant export's files in the tenant's CMS: the work folder the schema export writes the tables
 * into, and the archive packed from it and from the tenant's documents.
 *
 * <p>
 * Everything is under {@code __EXPORTS/tenant-exports/<id>/} - a folder the Documents perspective
 * shows to administrators and operators only, so an owner reaches the archive through the export's
 * own download route and nowhere else. {@code __EXPORTS} also holds the administrators' statement
 * dumps; nothing here touches anything outside {@code tenant-exports/}.
 *
 * <p>
 * Every method reads and writes the <b>current tenant's</b> CMS tree, so the caller must be in that
 * tenant's context - outside one the CMS silently resolves the default tenant's tree.
 */
@Component
class TenantExportArchives {

    /** The folder the CMS keeps exports in, which the documents of the tenant do not include. */
    static final String EXPORTS_FOLDER = "/__EXPORTS";

    /** The folder of the tenant exports. */
    static final String ROOT = EXPORTS_FOLDER + "/tenant-exports";

    /** The archive's folder of the tables. */
    static final String DATABASE_PREFIX = "database/";

    /** The archive's folder of the documents. */
    static final String FILES_PREFIX = "files/";

    /** The archive's manifest. */
    static final String MANIFEST = "manifest.json";

    /** The version of the archive's layout. */
    static final int FORMAT = 1;

    private static final String ZIP_MEDIA_TYPE = "application/zip";

    private static final String CSV_EXTENSION = ".csv";

    private static final DateTimeFormatter NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                                                                             .withZone(ZoneOffset.UTC);

    private final DocumentZipService zipService;

    TenantExportArchives(DocumentZipService zipService) {
        this.zipService = zipService;
    }

    /**
     * A stored archive.
     *
     * @param path where it is in the tenant's CMS
     * @param sizeBytes its size
     */
    record Stored(String path, long sizeBytes) {
    }

    /**
     * A table in the archive.
     *
     * @param name the table
     * @param rows how many rows its CSV holds
     */
    record TableEntry(String name, long rows) {
    }

    /**
     * A file the schema export wrote into the work folder.
     *
     * @param name its name
     * @param content its content
     */
    record WorkFile(String name, Content content) {
    }

    /**
     * Where a work file's content is read from.
     */
    @FunctionalInterface
    interface Content {

        /**
         * Opens the content.
         *
         * @return the content
         * @throws IOException when it cannot be read
         */
        InputStream open() throws IOException;
    }

    /**
     * The folder of one export.
     *
     * @param exportId the export
     * @return its path
     */
    static String exportFolder(long exportId) {
        return ROOT + "/" + exportId;
    }

    /**
     * The folder the schema export writes one export's tables into.
     *
     * @param exportId the export
     * @return its path
     */
    static String workFolder(long exportId) {
        return exportFolder(exportId) + "/database";
    }

    /**
     * The archive's file name.
     *
     * @param tenantId the tenant
     * @param requestedAt when the export was asked for
     * @return the name
     */
    static String archiveName(String tenantId, Instant requestedAt) {
        return "tenant-" + tenantId + "-data-export-" + NAME_TIMESTAMP.format(requestedAt) + ".zip";
    }

    /**
     * Whether a CMS path is in the exports folder, which an archive never includes.
     *
     * @param path the absolute CMS path
     * @return whether it is
     */
    static boolean isExportsPath(String path) {
        return path != null && (path.equals(EXPORTS_FOLDER) || path.startsWith(EXPORTS_FOLDER + "/"));
    }

    /**
     * Packs the export's archive from its work folder and the tenant's documents, stores it beside the
     * work folder and deletes the work folder. The archive goes through a temporary file, never through
     * memory. An archive a failed earlier attempt left is replaced.
     *
     * @param exportId the export
     * @param tenantId the tenant
     * @param requestedAt when the export was asked for
     * @return the stored archive
     * @throws IOException when the CMS cannot be read or written
     */
    Stored pack(long exportId, String tenantId, Instant requestedAt) throws IOException {
        CmisSession session = CmisSessionFactory.getSession();
        String name = archiveName(tenantId, requestedAt);
        File temp = File.createTempFile("tenant-export-" + exportId + "-", ".zip");
        try {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(temp))) {
                write(out, tenantId, Instant.now(), workFiles(folder(session, workFolder(exportId))));
            }
            CmisFolder exportFolder = ensureFolder(session, exportFolder(exportId));
            deleteChild(exportFolder, name);
            try (InputStream in = new BufferedInputStream(new FileInputStream(temp))) {
                CmisContentStream content = session.getObjectFactory()
                                                   .createContentStream(name, temp.length(), ZIP_MEDIA_TYPE, in);
                exportFolder.createDocument(
                        Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, CmisConstants.NAME, name), content);
            }
            CmisFolder workFolder = folder(session, workFolder(exportId));
            if (workFolder != null) {
                deleteRecursively(workFolder);
            }
            return new Stored(exportFolder(exportId) + "/" + name, temp.length());
        } finally {
            temp.delete();
        }
    }

    /**
     * Writes the archive: the tables, the documents, the manifest.
     *
     * @param out where the archive is written
     * @param tenantId the tenant
     * @param createdAt when the archive is made
     * @param database the files the schema export wrote, none when the tenant has no tables
     * @throws IOException when the CMS cannot be read or the archive cannot be written
     */
    void write(OutputStream out, String tenantId, Instant createdAt, List<WorkFile> database) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            List<TableEntry> tables = packDatabase(database, zip);
            int files = zipService.packTree("/", FILES_PREFIX, TenantExportArchives::isExportsPath, zip);
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(manifest(tenantId, createdAt, tables, files).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private static List<WorkFile> workFiles(CmisFolder workFolder) throws IOException {
        List<WorkFile> files = new ArrayList<>();
        if (workFolder == null) {
            return files;
        }
        for (CmisObject child : workFolder.getChildren()) {
            if (child instanceof CmisDocument document) {
                files.add(new WorkFile(child.getName(), () -> document.getContentStream()
                                                                      .getInputStream()));
            }
        }
        return files;
    }

    private static List<TableEntry> packDatabase(List<WorkFile> database, ZipOutputStream zip) throws IOException {
        List<TableEntry> tables = new ArrayList<>();
        for (WorkFile file : database) {
            String name = file.name();
            zip.putNextEntry(new ZipEntry(DATABASE_PREFIX + name));
            try (InputStream content = file.content()
                                           .open()) {
                if (name.endsWith(CSV_EXTENSION)) {
                    long records = copyCountingCsvRecords(content, zip);
                    // the first record is the header
                    tables.add(new TableEntry(name.substring(0, name.length() - CSV_EXTENSION.length()), Math.max(0, records - 1)));
                } else {
                    content.transferTo(zip);
                }
            }
            zip.closeEntry();
        }
        tables.sort((left, right) -> left.name()
                                         .compareTo(right.name()));
        return tables;
    }

    /**
     * Copies a CSV and counts its records: a line break inside a quoted value belongs to the value.
     *
     * @param in the CSV
     * @param out where it is copied
     * @return how many records it holds
     * @throws IOException when the copy fails
     */
    static long copyCountingCsvRecords(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        long records = 0;
        boolean quoted = false;
        boolean pending = false;
        int read;
        while ((read = in.read(buffer)) != -1) {
            for (int index = 0; index < read; index++) {
                byte current = buffer[index];
                if (current == '"') {
                    quoted = !quoted;
                    pending = true;
                } else if (current == '\n' && !quoted) {
                    if (pending) {
                        records++;
                    }
                    pending = false;
                } else if (current != '\r') {
                    pending = true;
                }
            }
            out.write(buffer, 0, read);
        }
        return pending ? records + 1 : records;
    }

    private static String manifest(String tenantId, Instant createdAt, List<TableEntry> tables, int files) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("format", FORMAT);
        manifest.put("tenantId", tenantId);
        manifest.put("createdAt", createdAt.toString());
        manifest.put("platformVersion", DirigibleConfig.PRODUCT_VERSION.getStringValue());
        List<Map<String, Object>> tableEntries = new ArrayList<>();
        for (TableEntry table : tables) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", table.name());
            entry.put("rows", table.rows());
            tableEntries.add(entry);
        }
        manifest.put("tables", tableEntries);
        manifest.put("files", files);
        return new GsonBuilder().setPrettyPrinting()
                                .serializeNulls()
                                .create()
                                .toJson(manifest);
    }

    /**
     * Opens a stored archive.
     *
     * @param archivePath where it is
     * @return its content
     * @throws IOException when it cannot be read
     */
    InputStream open(String archivePath) throws IOException {
        CmisObject object = CmisSessionFactory.getSession()
                                              .getObjectByPath(archivePath);
        if (!(object instanceof CmisDocument document)) {
            throw new IOException("There is no archive at [" + archivePath + "]");
        }
        return document.getContentStream()
                       .getInputStream();
    }

    /**
     * Deletes everything an export has in the CMS - its work folder and its archive. Finding nothing is
     * success.
     *
     * @param exportId the export
     * @throws IOException when the CMS cannot be written
     */
    void deleteExportFolder(long exportId) throws IOException {
        CmisFolder folder = folder(CmisSessionFactory.getSession(), exportFolder(exportId));
        if (folder != null) {
            deleteRecursively(folder);
        }
    }

    private static CmisFolder folder(CmisSession session, String path) throws IOException {
        CmisFolder folder = session.getRootFolder();
        for (String segment : segments(path)) {
            folder = childFolder(folder, segment);
            if (folder == null) {
                return null;
            }
        }
        return folder;
    }

    private static CmisFolder ensureFolder(CmisSession session, String path) throws IOException {
        CmisFolder folder = session.getRootFolder();
        for (String segment : segments(path)) {
            CmisFolder existing = childFolder(folder, segment);
            folder = existing != null ? existing
                    : folder.createFolder(
                            Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_FOLDER, CmisConstants.NAME, segment));
        }
        return folder;
    }

    private static List<String> segments(String path) {
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (!segment.isBlank()) {
                segments.add(segment);
            }
        }
        return segments;
    }

    private static CmisFolder childFolder(CmisFolder parent, String name) throws IOException {
        for (CmisObject child : parent.getChildren()) {
            if (child instanceof CmisFolder folder && name.equals(child.getName())) {
                return folder;
            }
        }
        return null;
    }

    private static void deleteChild(CmisFolder parent, String name) throws IOException {
        for (CmisObject child : parent.getChildren()) {
            if (!(child instanceof CmisFolder) && name.equals(child.getName())) {
                child.delete();
            }
        }
    }

    private static void deleteRecursively(CmisFolder folder) throws IOException {
        for (CmisObject child : folder.getChildren()) {
            if (child instanceof CmisFolder childFolder) {
                deleteRecursively(childFolder);
            } else {
                child.delete();
            }
        }
        folder.delete();
    }
}
