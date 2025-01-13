/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.endpoint;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.api.utils.UrlFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.workspace.service.TransportService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryExportException;
import org.eclipse.dirigible.repository.api.RepositoryImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The Class TransportEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "transport")
public class TransportEndpoint {

    /** The constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TransportEndpoint.class);

    /** The transport service. */
    @Autowired
    private TransportService transportService;

    /**
     * Import project in a given path.
     *
     * @param path the path
     * @param file the file
     * @return the response
     */
    @PostMapping(value = "/project", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<?> importProjectInPath(@Validated @RequestParam("path") String path,
            @Validated @RequestParam("file") MultipartFile file) {
        path = URLDecoder.decode(path, StandardCharsets.UTF_8);
        try {
            return importProject(path, false, file);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Project upload failed: " + e.getMessage(), e);
        }

    }

    /**
     * Import project in a given workspace.
     *
     * @param workspace the workspace
     * @param file the file
     * @return the response
     * @throws RepositoryImportException the repository import exception
     */
    @PostMapping(value = "/project/{workspace}", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<?> importProjectInWorkspace(@Validated @PathVariable("workspace") String workspace,
            @Validated @RequestParam("file") MultipartFile file) throws RepositoryImportException {

        try {
            return importProject(workspace, true, file);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Project upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Import project.
     *
     * @param path the path
     * @param isPathWorkspace the is path workspace
     * @param file the file
     * @return the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private ResponseEntity<?> importProject(String path, boolean isPathWorkspace, MultipartFile file) throws IOException {

        InputStream is = file.getInputStream();
        if (isPathWorkspace) {
            transportService.importProjectInWorkspace(path, is);
        } else {
            transportService.importProjectInPath(path, is);
        }
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Import zip to folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @param folder internal folder (url encoded)
     * @param file the file
     * @return the response
     * @throws RepositoryExportException the repository export exception
     * @throws DecoderException the repository export exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @PostMapping(value = "/zipimport/{workspace}/{project}/{*folder}", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<?> importZipToFolder(@Validated @PathVariable("workspace") String workspace,
            @Validated @PathVariable("project") String project, @Nullable @PathVariable("folder") String folder,
            @Validated @RequestParam("file") MultipartFile file) throws RepositoryExportException, DecoderException, IOException {

        String relativePath;
        if (folder == null || folder.isEmpty() || folder.trim()
                                                        .isEmpty()
                || folder.equals("/"))
            relativePath = "";
        else {
            relativePath = UrlFacade.decode(folder, null);
        }

        transportService.importZipToPath(workspace, project, relativePath, file.getBytes(), true);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Export project.
     *
     * @param workspace the workspace
     * @param project the project
     * @param folder Internal folder (url encoded)
     * @return the response
     * @throws RepositoryExportException the repository export exception
     * @throws UnsupportedEncodingException the repository export exception
     * @throws DecoderException the repository export exception
     */
    @GetMapping(value = "/project/{workspace}/{project}/{*folder}", produces = "multipart/form-data")
    public ResponseEntity<?> exportProject(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("folder") String folder) throws RepositoryExportException, UnsupportedEncodingException, DecoderException {

        SimpleDateFormat pattern = getDateFormat();
        byte[] zip;

        if ("*".equals(project)) {
            zip = transportService.exportWorkspace(workspace);

            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentDisposition(
                    ContentDisposition.parse("attachment; filename=\"" + workspace + "-" + pattern.format(new Date()) + ".zip\""));
            return new ResponseEntity(zip, httpHeaders, HttpStatus.OK);
        } else if (folder == null || folder.isEmpty() || folder.trim()
                                                               .isEmpty()
                || folder.equals("/"))
            zip = transportService.exportProject(workspace, project);
        else
            zip = transportService.exportFolder(workspace, project, folder);

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDisposition(
                ContentDisposition.parse("attachment; filename=\"" + project + "-" + pattern.format(new Date()) + ".zip\""));
        return new ResponseEntity(zip, httpHeaders, HttpStatus.OK);
    }

    /**
     * Import snapshot.
     *
     * @param file the file
     * @return the response
     * @throws RepositoryImportException the repository import exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @PostMapping(value = "/snapshot", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<?> importSnapshot(@Validated @RequestParam("file") MultipartFile file)
            throws RepositoryImportException, IOException {

        transportService.importSnapshot(file.getBytes());
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Export snapshot.
     *
     * @return the response
     * @throws RepositoryExportException the repository export exception
     */
    @GetMapping(value = "/snapshot", produces = "multipart/form-data")
    public ResponseEntity<?> exportSnapshot() throws RepositoryExportException {

        SimpleDateFormat pattern = getDateFormat();
        byte[] zip = transportService.exportSnapshot();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentDisposition(
                ContentDisposition.parse("attachment; filename=\"repository-snapshot-" + pattern.format(new Date()) + ".zip\""));
        return new ResponseEntity(zip, httpHeaders, HttpStatus.OK);
    }

    /**
     * Import file to folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @param folder Internal folder (url encoded)
     * @param file the file
     * @return the response
     * @throws RepositoryExportException the repository export exception
     * @throws DecoderException the repository export exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @PostMapping(value = "/fileimport/{workspace}/{project}/{*folder}", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<?> importFilesToFolder(@Validated @PathVariable("workspace") String workspace,
            @Validated @PathVariable("project") String project, @Nullable @PathVariable("folder") String folder,
            @Validated @RequestParam("file") MultipartFile file) throws RepositoryExportException, DecoderException, IOException {

        String relativePath;
        if (folder == null || folder.isEmpty() || folder.trim()
                                                        .isEmpty()
                || folder.equals("/"))
            relativePath = "";
        else {
            relativePath = UrlFacade.decode(folder, null);
        }

        InputStream in = file.getInputStream();
        byte[] bytes = IOUtils.toByteArray(in);
        transportService.importFileToPath(workspace, project, relativePath + IRepository.SEPARATOR + file.getOriginalFilename(), bytes);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Gets the date format.
     *
     * @return the date format
     */
    private SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat("yyyyMMddhhmmss");
    }

}
