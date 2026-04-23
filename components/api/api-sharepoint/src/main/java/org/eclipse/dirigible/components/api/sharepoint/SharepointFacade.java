/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.sharepoint;

import com.microsoft.graph.models.DriveItem;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.engine.cms.TenantPathResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class SharepointFacade {

    private static final String FOLDER_SUFFIX = "/";

    private final TenantPathResolver tenantPathResolver;
    private final SharePointService sharePointService;

    public SharepointFacade(TenantPathResolver tenantPathResolver, SharePointService sharePointService) {
        this.tenantPathResolver = tenantPathResolver;
        this.sharePointService = sharePointService;
    }

    public static void delete(String name) {
        String tenantName = get().tenantPathResolver.resolve(name);
        get().sharePointService.deleteFile(tenantName);
    }

    public static SharepointFacade get() {
        return BeanProvider.getBean(SharepointFacade.class);
    }

    public static void deleteFolder(String prefix) {
        String tenantPrefix = get().tenantPathResolver.resolve(prefix);
        get().sharePointService.deleteFolder(tenantPrefix);
    }

    public static byte[] get(String name) throws IOException {
        String tenantName = get().tenantPathResolver.resolve(name);
        return get().sharePointService.getFileContent(tenantName + FOLDER_SUFFIX);
    }

    public static void put(String name, byte[] input, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(input)) {
            put(name, inputStream, contentType);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to update the content for: " + name, e);
        }
    }

    public static void put(String name, InputStream inputStream, String contentType) {
        String tenantName = get().tenantPathResolver.resolve(name);
        get().sharePointService.updateFileContent(tenantName, inputStream, contentType);
    }

    public static List<DriveItem> listObjects(String path) {
        String tenantPath = get().tenantPathResolver.resolve(path);
        return get().sharePointService.listObjects(tenantPath);
    }

    /**
     * Exists.
     *
     * @param keyName the key name
     * @return true, if successful
     */
    public static boolean exists(String keyName) {
        String tenantKeyName = get().tenantPathResolver.resolve(keyName);
        return get().sharePointService.exists(tenantKeyName);
    }

    public static String getObjectContentType(String keyName) {
        String tenantKeyName = get().tenantPathResolver.resolve(keyName);
        return get().sharePointService.getContentType(tenantKeyName);
    }

    public static InputStream getInputStream(String id) {
        String tenantId = get().tenantPathResolver.resolve(id);
        return get().sharePointService.getFileInputStream(tenantId);
    }

    public static void createFolder(String folderName) {
        String tenantFolderName = get().tenantPathResolver.resolve(folderName);
        get().sharePointService.createFolder(tenantFolderName);
    }

    public static Optional<DriveItem> getById(String id) {
        String tenantId = get().tenantPathResolver.resolve(id);
        return get().sharePointService.getById(tenantId);
    }
}
