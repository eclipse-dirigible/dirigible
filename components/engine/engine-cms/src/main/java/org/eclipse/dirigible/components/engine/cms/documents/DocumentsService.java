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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Documents browser's operations over the CMS: listing, upload, folder creation, rename and
 * delete.
 * <p>
 * Every path is a CMS path (tenant-resolved downstream by the CMIS session), and every operation is
 * gated by {@link DocumentAccessEvaluator}. Two folders are never exposed here: {@code __internal},
 * which holds the CMS's own bookkeeping, and {@code __EXPORTS}, which holds database export dumps
 * and is readable only by the roles entitled to produce one.
 */
@Component
public class DocumentsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsService.class);

    /** The CMS's own bookkeeping folder - never listed, never addressable. */
    static final String INTERNAL_FOLDER = "__internal";

    /** Where the asynchronous database export writes its dumps. */
    static final String EXPORTS_FOLDER = "__EXPORTS";

    private final CmsService cmsService;
    private final DocumentAccessEvaluator accessEvaluator;
    private final ContentTypeResolver contentTypeResolver;

    DocumentsService(CmsService cmsService, DocumentAccessEvaluator accessEvaluator, ContentTypeResolver contentTypeResolver) {
        this.cmsService = cmsService;
        this.accessEvaluator = accessEvaluator;
        this.contentTypeResolver = contentTypeResolver;
    }

    /**
     * Lists a folder, or the root when no path is given.
     *
     * @param path the folder path, null or blank for the root
     * @param request the current request
     * @return the folder with its visible children
     * @throws IOException when the CMS cannot be read
     */
    public FolderDto list(String path, HttpServletRequest request) throws IOException {
        // Deny on the REQUESTED path before resolving it: a hidden folder must answer the same whether
        // or not it happens to exist, otherwise its existence leaks through the status code.
        requireCleanPath(path);
        assertNotHidden(path);
        CmisFolder folder = folderOrRoot(path);
        String folderPath = folder.getPath();
        if (isHidden(folderPath) || !accessEvaluator.isReadable(folderPath, request)) {
            throw new DocumentAccessDeniedException(folderPath);
        }
        List<ChildDto> children = new ArrayList<>();
        for (CmisObject child : folder.getChildren()) {
            String childPath = childPath(folderPath, child.getName());
            if (isHidden(childPath) || !isExportsAccessible(childPath, request)) {
                continue;
            }
            DocumentAccessEvaluator.AccessFlags flags = accessEvaluator.flags(childPath, request);
            if (!flags.readable()) {
                continue;
            }
            children.add(new ChildDto(child.getName(), child.getType()
                                                            .getId(),
                    child.getId(), childPath, flags.readOnly(), true));
        }
        children.sort(Comparator.comparing(ChildDto::path));
        return new FolderDto(folder.getName(), folder.getId(), folderPath, parentId(folder), children);
    }

    /**
     * Reads a document for preview or download.
     *
     * @param path the document path
     * @param request the current request
     * @return the document's name, content and resolved content type
     * @throws IOException when the CMS cannot be read
     */
    public DocumentContent read(String path, HttpServletRequest request) throws IOException {
        requireCleanPath(path);
        assertReadable(path, request);
        CmisObject object = cmsService.getObjectByPath(path);
        if (!(object instanceof CmisDocument document)) {
            throw new DocumentNotFoundException(path);
        }
        byte[] content = cmsService.getDocumentContent(document);
        if (content == null) {
            throw new DocumentNotFoundException(path);
        }
        String storedType = document.getContentStream()
                                    .getMimeType();
        return new DocumentContent(document.getName(), content, contentTypeResolver.beforeDownload(document.getName(), storedType));
    }

    /**
     * Uploads a document into a folder.
     *
     * @param folderPath the target folder
     * @param name the document name
     * @param contentType the announced content type
     * @param size the content length
     * @param content the content
     * @param overwrite whether an existing document of that name is replaced
     * @param request the current request
     * @return the stored document's path
     * @throws IOException when the CMS cannot be written
     */
    public String upload(String folderPath, String name, String contentType, int size, InputStream content, boolean overwrite,
            HttpServletRequest request) throws IOException {
        requireCleanPath(folderPath);
        requireCleanPath(name);
        CmisFolder folder = folderOrRoot(folderPath);
        assertWritable(folder.getPath(), request);
        String resolvedType = contentTypeResolver.beforeUpload(name, contentType);
        CmisDocument existing = cmsService.getChildDocumentByName(folder, name);
        if (existing != null) {
            if (!overwrite) {
                throw new DocumentConflictException(childPath(folder.getPath(), name));
            }
            cmsService.updateDocument(folder, existing, resolvedType, size, content);
        } else {
            cmsService.createDocument(folder, name, resolvedType, size, content);
        }
        return childPath(folder.getPath(), name);
    }

    /**
     * Creates a folder.
     *
     * @param parentPath the parent folder, null or blank for the root
     * @param name the new folder's name
     * @param request the current request
     * @return the created folder, listed
     * @throws IOException when the CMS cannot be written
     */
    public FolderDto createFolder(String parentPath, String name, HttpServletRequest request) throws IOException {
        requireCleanPath(parentPath);
        requireCleanPath(name);
        CmisFolder parent = folderOrRoot(parentPath);
        assertWritable(parent.getPath(), request);
        assertNotHidden(childPath(parent.getPath(), name));
        CmisFolder created = cmsService.createFolder(parent, name);
        return new FolderDto(created.getName(), created.getId(), created.getPath(), parent.getId(), List.of());
    }

    /**
     * Renames a document or folder.
     *
     * @param path the object's path
     * @param name the new name
     * @param request the current request
     * @throws IOException when the CMS cannot be written
     */
    public void rename(String path, String name, HttpServletRequest request) throws IOException {
        requireCleanPath(path);
        requireCleanPath(name);
        assertNotHidden(path);
        assertWritable(path, request);
        cmsService.getObjectByPath(path)
                  .rename(name);
    }

    /**
     * Deletes documents and folders.
     *
     * @param paths the absolute paths to delete
     * @param forceDelete whether a non-empty folder is deleted with its content
     * @param request the current request
     * @throws IOException when the CMS cannot be written
     */
    public void delete(List<String> paths, boolean forceDelete, HttpServletRequest request) throws IOException {
        for (String path : paths) {
            requireCleanPath(path);
            assertNotHidden(path);
            assertWritable(path, request);
            CmisObject object = cmsService.getObjectByPath(path);
            if (isFolder(object) && forceDelete) {
                deleteTree((CmisFolder) object);
            } else {
                object.delete();
            }
        }
    }

    /**
     * Deletes a folder and everything under it, depth first - the CMIS layer has no recursive delete.
     *
     * @param folder the folder to remove
     * @throws IOException when the CMS cannot be written
     */
    void deleteTree(CmisFolder folder) throws IOException {
        for (CmisObject child : folder.getChildren()) {
            if (isFolder(child)) {
                deleteTree((CmisFolder) child);
            } else {
                child.delete();
            }
        }
        folder.delete();
    }

    /**
     * Whether the object is a folder, compared by type id - {@link ObjectType} does not implement
     * {@code equals}, so an identity check would depend on the provider handing back the constants.
     *
     * @param object the CMIS object
     * @return true when it is a folder
     */
    static boolean isFolder(CmisObject object) {
        return ObjectType.FOLDER.getId()
                                .equals(object.getType()
                                              .getId());
    }

    /**
     * A folder by path, falling back to the root - the JavaScript predecessor's behaviour, which both
     * user interfaces rely on when they open with no path.
     *
     * @param path the folder path
     * @return the folder, or the root
     * @throws IOException when the CMS cannot be read
     */
    CmisFolder folderOrRoot(String path) throws IOException {
        requireCleanPath(path);
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return cmsService.getRootFolder();
        }
        CmisObject object = cmsService.getObjectByPath(path);
        if (!(object instanceof CmisFolder folder)) {
            throw new DocumentNotFoundException(path);
        }
        return folder;
    }

    /** The child's absolute path, without doubling the root's separator. */
    static String childPath(String folderPath, String name) {
        return folderPath.endsWith("/") ? folderPath + name : folderPath + "/" + name;
    }

    /**
     * Rejects a path that carries characters a CMS path can never legitimately contain.
     * <p>
     * A caller-supplied path reaches logging, the CMIS query layer and the tenant path resolver, so a
     * control character in it could forge a log record or split a query. No document or folder name can
     * hold one, which makes rejecting them at the boundary both safe and the only place this has to be
     * done.
     *
     * @param path the requested path, may be null
     * @return the same path
     */
    static String requireCleanPath(String path) {
        if (path == null) {
            return null;
        }
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            if (character == '\n' || character == '\r' || character == '\0' || Character.isISOControl(character)) {
                throw new DocumentInvalidPathException();
            }
        }
        return path;
    }

    /** Whether the path is one the Documents surface never exposes. */
    static boolean isHidden(String path) {
        return firstSegment(path).equals(INTERNAL_FOLDER);
    }

    private boolean isExportsAccessible(String path, HttpServletRequest request) {
        if (!firstSegment(path).equals(EXPORTS_FOLDER)) {
            return true;
        }
        return request == null || request.isUserInRole("ADMINISTRATOR") || request.isUserInRole("OPERATOR");
    }

    /** The first path segment, or an empty string for the root. */
    private static String firstSegment(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int separator = normalized.indexOf('/');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }

    private void assertReadable(String path, HttpServletRequest request) {
        assertNotHidden(path);
        if (!isExportsAccessible(path, request) || !accessEvaluator.isReadable(path, request)) {
            throw new DocumentAccessDeniedException(path);
        }
    }

    private void assertWritable(String path, HttpServletRequest request) {
        if (!isExportsAccessible(path, request) || !accessEvaluator.isWritable(path, request)) {
            throw new DocumentAccessDeniedException(path);
        }
    }

    private static void assertNotHidden(String path) {
        if (isHidden(path)) {
            throw new DocumentAccessDeniedException(path);
        }
    }

    private String parentId(CmisFolder folder) {
        if (folder.isRootFolder()) {
            return null;
        }
        try {
            CmisFolder parent = folder.getFolderParent();
            return parent == null ? null : parent.getId();
        } catch (IOException e) {
            LOGGER.debug("Failed to resolve the parent of [{}]", folder.getPath(), e);
            return null;
        }
    }

    /**
     * A document's content as served to the client.
     *
     * @param name the document's name
     * @param content the bytes
     * @param contentType the resolved content type
     */
    public record DocumentContent(String name, byte[] content, String contentType) {
    }
}
