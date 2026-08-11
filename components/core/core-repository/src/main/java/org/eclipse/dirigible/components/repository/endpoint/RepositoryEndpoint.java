/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.repository.endpoint;

import static java.text.MessageFormat.format;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.repository.service.RepositoryService;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.RolesAllowed;

/**
 * The Class RepositoryEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "repository")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class RepositoryEndpoint {

    /** The time stamp appended to the name of an exported archive. */
    private static final DateTimeFormatter EXPORT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** The repository service. */
    private final RepositoryService repositoryService;

    /**
     * Instantiates a new repository endpoint.
     *
     * @param repositoryService the repository service
     */
    @Autowired
    public RepositoryEndpoint(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Gets the resource.
     *
     * @param path the path
     * @return the resource
     */
    @GetMapping("/{*path}")
    public ResponseEntity<?> getRepositoryResource(@PathVariable("path") String path) {

        final HttpHeaders httpHeaders = new HttpHeaders();

        IResource resource = repositoryService.getResource(path);
        if (!resource.exists()) {
            ICollection collection = repositoryService.getCollection(path);
            if (!collection.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, path);
            }
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity(repositoryService.renderRepository(collection), httpHeaders, HttpStatus.OK);
        }
        if (resource.isBinary()) {
            httpHeaders.setContentType(MediaType.valueOf(resource.getContentType()));
            return new ResponseEntity(resource.getContent(), httpHeaders, HttpStatus.OK);
        }
        httpHeaders.setContentType(MediaType.valueOf(resource.getContentType()));
        return new ResponseEntity(resource.getContent(), httpHeaders, HttpStatus.OK);
    }

    /**
     * Exports the collection or the resource at the given path as a download. A collection is exported
     * as a zip archive of its content, including all its subfolders; a resource is exported as the file
     * itself.
     *
     * @param path the path
     * @return the download
     */
    @GetMapping(value = "/{*path}", params = "export")
    public ResponseEntity<byte[]> exportRepositoryResource(@PathVariable("path") String path) {

        IResource resource = repositoryService.getResource(path);
        if (resource.exists()) {
            byte[] content = resource.getContent();
            return download(content == null ? new byte[0] : content, resource.getName());
        }
        ICollection collection = repositoryService.getCollection(path);
        if (!collection.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, path);
        }
        return download(repositoryService.exportZip(path), zipFileName(collection));
    }

    /**
     * Builds an attachment response. The content type is deliberately opaque - the response is a
     * download and the file name carries the extension.
     *
     * @param content the content
     * @param fileName the file name to download as
     * @return the response
     */
    private static ResponseEntity<byte[]> download(byte[] content, String fileName) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentDisposition(ContentDisposition.attachment()
                                                            .filename(fileName, StandardCharsets.UTF_8)
                                                            .build());
        return new ResponseEntity<>(content, httpHeaders, HttpStatus.OK);
    }

    /**
     * The archive name for a collection export - its own name, stamped with the export time.
     *
     * @param collection the collection
     * @return the file name
     */
    private static String zipFileName(ICollection collection) {
        String name = collection.getName();
        if (name == null || name.isBlank()) {
            name = "repository";
        }
        return name + "-" + LocalDateTime.now()
                                         .format(EXPORT_TIMESTAMP)
                + ".zip";
    }

    /**
     * Creates the resource.
     *
     * @param path the path
     * @param content the content
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     */
    @PostMapping("/{*path}")
    public ResponseEntity<URI> createResource(@PathVariable("path") String path, byte[] content) throws URISyntaxException {

        if (path.endsWith(IRepositoryStructure.SEPARATOR)) {
            ICollection collection = repositoryService.createCollection(path);
            return ResponseEntity.created(
                    repositoryService.getURI(RepositoryService.escape(collection.getPath() + IRepositoryStructure.SEPARATOR)))
                                 .build();
        }

        IResource resource = repositoryService.getResource(path);
        if (resource.exists()) {
            String message = format("Resource at location {0} already exists", path);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        resource = repositoryService.createResource(path, content);
        return ResponseEntity.created(repositoryService.getURI(RepositoryService.escape(resource.getPath())))
                             .build();
    }

    /**
     * Update resource.
     *
     * @param path the path
     * @param content the content
     * @return the response
     */
    @PutMapping("/{*path}")
    public ResponseEntity<URI> updateResource(@PathVariable("path") String path, byte[] content) {

        IResource resource = repositoryService.getResource(path);
        if (!resource.exists()) {
            String message = format("Resource at location {0} does not exist", path);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
        resource = repositoryService.updateResource(path, content);
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Delete resource.
     *
     * @param path the path
     * @return the response
     */
    @DeleteMapping("/{*path}")
    public ResponseEntity<?> deleteResource(@PathVariable("path") String path) {

        if (path.endsWith(IRepositoryStructure.SEPARATOR)) {
            repositoryService.deleteCollection(path);
            ResponseEntity.noContent()
                          .build();
        }

        IResource resource = repositoryService.getResource(path);
        if (!resource.exists()) {
            ICollection collection = repositoryService.getCollection(path);
            if (collection.exists()) {
                repositoryService.deleteCollection(path);
                return ResponseEntity.noContent()
                                     .build();
            }
            String message = format("Collection or Resource at location {0} does not exist", path);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
        repositoryService.deleteResource(path);
        return ResponseEntity.noContent()
                             .build();
    }

}
