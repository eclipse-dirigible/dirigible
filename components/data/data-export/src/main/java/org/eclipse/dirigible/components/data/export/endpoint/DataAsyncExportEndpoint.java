/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.endpoint;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.export.domain.Export;
import org.eclipse.dirigible.components.data.export.domain.ExportStatus;
import org.eclipse.dirigible.components.data.export.service.DatabaseExportService;
import org.eclipse.dirigible.components.data.export.service.ExportService;
import org.eclipse.dirigible.components.data.management.service.DatabaseMetadataService;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

/**
 * Front facing REST service serving the raw data.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "export-async")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class DataAsyncExportEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DataAsyncExportEndpoint.class);

    static final String EXPORTS_FOLDER_NAME = "__EXPORTS";

    /** The database export service. */
    private final DatabaseExportService databaseExportService;

    /** The database metadata service. */
    private final DatabaseMetadataService databaseMetadataService;

    private final ExportService exportService;

    private final CmsService cmsService;



    /**
     * Instantiates a new database export endpoint.
     *
     * @param databaseExportService the database export service
     * @param databaseMetadataService the database metadata service
     */
    @Autowired
    public DataAsyncExportEndpoint(DatabaseExportService databaseExportService, DatabaseMetadataService databaseMetadataService,
            ExportService exportService, CmsService cmsService) {
        this.databaseExportService = databaseExportService;
        this.databaseMetadataService = databaseMetadataService;
        this.exportService = exportService;
        this.cmsService = cmsService;
    }

    /**
     * Gets the database export service.
     *
     * @return the database export service
     */
    public DatabaseExportService getDatabaseExportService() {
        return databaseExportService;
    }

    /**
     * Gets the database metadata service.
     *
     * @return the database metadata service
     */
    public DatabaseMetadataService getDatabaseMetadataService() {
        return databaseMetadataService;
    }

    public ExportService getExportService() {
        return exportService;
    }

    public CmsService getCmsService() {
        return cmsService;
    }


    /**
     * Execute statement export.
     *
     * @param datasource the datasource
     * @param statement the statement
     * @return the response
     * @throws SQLException the SQL exception
     */
    @PostMapping(value = "/{datasource}", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> exportStatement(@PathVariable("datasource") String datasource,
            @Valid @RequestBody String statement) throws SQLException {

        if (!databaseMetadataService.existsDataSourceMetadata(datasource)) {
            String error = format("Datasource {0} does not exist.", datasource);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        launchExportJob(datasource, statement);

        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Gets the export.
     *
     * @return the response
     */
    @GetMapping("/")
    public ResponseEntity<String> get() {

        try {
            List<Export> exports = getExportService().findAll();

            CmisFolder root = getCmsService().getRootFolder();
            CmisFolder exportsFolder = getOrCreate(root);
            if (exportsFolder == null) {
                exportsFolder = getCmsService().createFolder(root, EXPORTS_FOLDER_NAME);
            }
            List<? extends CmisObject> exportObjects = exportsFolder.getChildren();
            for (Export export : exports) {
                if (!exportObjects.stream()
                                  .anyMatch(obj -> obj.getName()
                                                      .equals(export.getName()))) {
                    if (ExportStatus.FINISHED.equals(export.getStatus())) {
                        export.setStatus(ExportStatus.FAILED);
                        export.setMessage(export.getName() + " file has been deleted from the storage.");
                        getExportService().save(export);
                    }
                }
            }

            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(GsonHelper.toJson(exports));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Deletes all the exports.
     *
     * @return the response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {

        try {
            Export export = getExportService().findById(id);

            CmisFolder root = getCmsService().getRootFolder();
            CmisFolder exportsFolder = getOrCreate(root);
            if (exportsFolder == null) {
                exportsFolder = getCmsService().createFolder(root, EXPORTS_FOLDER_NAME);
            }
            CmisDocument document = getCmsService().getChildDocumentByName(exportsFolder, export.getName());

            if (document != null) {
                document.delete();
            }
            getExportService().delete(export.getId());

            return ResponseEntity.noContent()
                                 .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Deletes the export.
     *
     * @return the response
     */
    @DeleteMapping("/")
    public ResponseEntity<String> deleteAll() {

        try {
            getExportService().deleteAll();

            CmisFolder root = getCmsService().getRootFolder();
            CmisFolder exportsFolder = getOrCreate(root);
            if (exportsFolder != null) {
                exportsFolder.delete();
            }

            return ResponseEntity.noContent()
                                 .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private void launchExportJob(String datasource, String statement) {
        executorService.submit(() -> {
            CmisFolder root;
            try {
                root = getCmsService().getRootFolder();
                final CmisFolder exportsFolder = getOrCreate(root);
                String name = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()) + "-" + sanitize(statement) + ".csv";
                PipedOutputStream pos = new PipedOutputStream();
                PipedInputStream pis = new PipedInputStream(pos, 1024);
                Timestamp from = Timestamp.from(Instant.now());
                Export export = new Export(name, ExportStatus.TRIGGRED, from, null, null);
                export.setId(exportService.save(export));
                new Thread(() -> {
                    try (PipedOutputStream producerPos = pos) {
                        databaseExportService.exportStatement(datasource, statement, producerPos);
                    } catch (Exception e) {
                        logger.error("Database export failed.", e);
                        export.setStatus(ExportStatus.FAILED);
                        export.setFinishedAt(from);
                        export.setMessage(e.getMessage());
                        exportService.save(export);
                    }
                }).start();

                try (PipedInputStream consumerPis = pis) {
                    cmsService.createDocument(exportsFolder, name, "text/csv", -1, consumerPis);
                    export.setStatus(ExportStatus.FINISHED);
                    Timestamp to = Timestamp.from(Instant.now());
                    export.setFinishedAt(to);
                    exportService.save(export);
                    logger.info("Export of [{}] finished in {} ms.", name, (to.getTime() - from.getTime()));
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("CMIS Document Creation failed.", e);
                        export.setStatus(ExportStatus.FAILED);
                        export.setFinishedAt(from);
                        export.setMessage(e.getMessage());
                        exportService.save(export);
                    }
                }
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        });

    }

    public CmisFolder getOrCreate(CmisFolder root) throws IOException {
        CmisFolder exportsFolder = getCmsService().getChildFolderByName(root, EXPORTS_FOLDER_NAME);
        if (exportsFolder == null) {
            exportsFolder = getCmsService().createFolder(root, EXPORTS_FOLDER_NAME);
        }
        return exportsFolder;
    }

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int end = Math.min(input.length(), 20);
        String truncatedString = input.substring(0, end);
        String finalString = truncatedString.replaceAll("[^a-zA-Z]", "_");
        return finalString;
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
