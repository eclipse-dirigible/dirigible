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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Documents browser's REST surface: list, upload, create folder, rename, delete, archive,
 * preview and download over the CMS.
 * <p>
 * Replaces the JavaScript service that used to serve this at
 * {@code /services/js/documents/api/documents.js}; the route shapes and the response payload are
 * kept identical so both user interfaces (the perspective and the application shells' Documents
 * section) only change their base URL.
 * <p>
 * Authorization is deliberately NOT a blanket platform role: ordinary users browse documents in the
 * application shells. Reads and writes are gated per path by {@link DocumentAccessEvaluator}, and
 * the folders holding the CMS's own bookkeeping and the database export dumps are handled in
 * {@link DocumentsService}.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "documents")
public class DocumentsEndpoint extends BaseEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsEndpoint.class);

    private final DocumentsService documentsService;
    private final DocumentZipService zipService;

    DocumentsEndpoint(DocumentsService documentsService, DocumentZipService zipService) {
        this.documentsService = documentsService;
        this.zipService = zipService;
    }

    /**
     * Lists a folder, or the root when no path is given.
     *
     * @param path the folder path
     * @param request the current request
     * @return the folder with its visible children
     */
    @GetMapping
    public ResponseEntity<FolderDto> list(@RequestParam(value = "path", required = false) String path, HttpServletRequest request) {
        return ResponseEntity.ok(call(() -> documentsService.list(path, request)));
    }

    /**
     * Uploads one or more documents into a folder.
     *
     * @param path the target folder
     * @param overwrite whether existing documents of the same name are replaced
     * @param files the uploaded parts
     * @param request the current request
     * @return the stored documents' paths
     */
    @PostMapping
    public ResponseEntity<List<String>> upload(@RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite,
            @RequestParam("file") List<MultipartFile> files, HttpServletRequest request) {
        List<String> stored = new ArrayList<>();
        for (MultipartFile file : files) {
            stored.add(call(() -> {
                try (InputStream content = file.getInputStream()) {
                    return documentsService.upload(path, file.getOriginalFilename(), file.getContentType(), (int) file.getSize(), content,
                            overwrite, request);
                }
            }));
        }
        return ResponseEntity.ok(stored);
    }

    /**
     * Renames a document or folder.
     *
     * @param body the object's path and its new name
     * @param request the current request
     * @return no content
     */
    @PutMapping
    public ResponseEntity<Void> rename(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String path = required(body, "path");
        String name = required(body, "name");
        call(() -> {
            documentsService.rename(path, name, request);
            return null;
        });
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Deletes documents and folders.
     *
     * @param paths the absolute paths
     * @param forceDelete whether non-empty folders are deleted with their content
     * @param request the current request
     * @return no content
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody List<String> paths,
            @RequestParam(value = "forceDelete", required = false, defaultValue = "true") boolean forceDelete, HttpServletRequest request) {
        call(() -> {
            documentsService.delete(paths, forceDelete, request);
            return null;
        });
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Creates a folder.
     *
     * @param body the parent folder and the new folder's name
     * @param request the current request
     * @return the created folder
     */
    @PostMapping("/folder")
    public ResponseEntity<FolderDto> createFolder(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String parent = body.get("parentFolder");
        String name = required(body, "name");
        return ResponseEntity.ok(call(() -> documentsService.createFolder(parent, name, request)));
    }

    /**
     * Downloads a folder as a zip archive.
     *
     * @param path the folder
     * @param request the current request
     * @return the archive
     */
    @GetMapping("/zip")
    public ResponseEntity<byte[]> downloadZip(@RequestParam(value = "path", required = false) String path, HttpServletRequest request) {
        byte[] archive = call(() -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            zipService.pack(path, buffer, request);
            return buffer.toByteArray();
        });
        String name = folderName(path) + ".zip";
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                             .contentType(MediaType.APPLICATION_OCTET_STREAM)
                             .body(archive);
    }

    /**
     * Uploads archives and unpacks them into a folder.
     *
     * @param path the target folder
     * @param files the uploaded archives
     * @param request the current request
     * @return no content
     */
    @PostMapping("/zip")
    public ResponseEntity<Void> uploadZip(@RequestParam(value = "path", required = false) String path,
            @RequestParam("file") List<MultipartFile> files, HttpServletRequest request) {
        for (MultipartFile file : files) {
            call(() -> {
                try (InputStream content = file.getInputStream()) {
                    zipService.unpack(path, content, request);
                }
                return null;
            });
        }
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Serves a document inline, under its resolved content type.
     *
     * @param path the document
     * @param request the current request
     * @return the content
     */
    @GetMapping("/preview")
    public ResponseEntity<byte[]> preview(@RequestParam("path") String path, HttpServletRequest request) {
        return send(path, request, false);
    }

    /**
     * Serves a document as an attachment.
     *
     * @param path the document
     * @param request the current request
     * @return the content
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("path") String path, HttpServletRequest request) {
        return send(path, request, true);
    }

    private ResponseEntity<byte[]> send(String path, HttpServletRequest request, boolean asAttachment) {
        DocumentsService.DocumentContent content = call(() -> documentsService.read(path, request));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                                                            .header(HttpHeaders.CONTENT_TYPE, content.contentType());
        if (asAttachment) {
            response.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + content.name() + "\"");
        }
        return response.body(content.content());
    }

    /**
     * Runs a CMS operation, mapping its failures onto the HTTP statuses the clients expect.
     *
     * @param <T> the result type
     * @param operation the operation
     * @return its result
     */
    private <T> T call(CmsOperation<T> operation) {
        try {
            return operation.run();
        } catch (DocumentAccessDeniedException e) {
            LOGGER.debug("Access denied", e);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (DocumentNotFoundException e) {
            LOGGER.debug("Not found", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (DocumentConflictException e) {
            LOGGER.debug("Conflict", e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (DocumentInvalidPathException e) {
            LOGGER.debug("Invalid path", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Documents operation failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static String required(Map<String, String> body, String key) {
        String value = body == null ? null : body.get(key);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The [" + key + "] must be provided");
        }
        return value;
    }

    private static String folderName(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "root";
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int separator = trimmed.lastIndexOf('/');
        return separator < 0 ? trimmed : trimmed.substring(separator + 1);
    }

    /**
     * A CMS operation that may fail with an {@link IOException}.
     *
     * @param <T> the result type
     */
    private interface CmsOperation<T> {

        /**
         * Runs it.
         *
         * @return the result
         * @throws IOException when the CMS cannot be read or written
         */
        T run() throws IOException;
    }
}
