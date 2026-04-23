/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.sharepoint.repository;

import com.microsoft.graph.models.DriveItem;
import org.eclipse.dirigible.components.api.sharepoint.SharepointFacade;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class CmisSharepointSession implements CmisSession {

    private static final String ROOT = "/";
    private static final Logger LOGGER = LoggerFactory.getLogger(CmisSharepointSession.class);

    @Override
    public CmisSharepointRepositoryInfo getRepositoryInfo() {
        return new CmisSharepointRepositoryInfo(this);
    }

    @Override
    public CmisSharepointObjectFactory getObjectFactory() {
        return new CmisSharepointObjectFactory();
    }

    @Override
    public CmisSharepointFolder getRootFolder() {
        return new CmisSharepointFolder(ROOT, ROOT, true);
    }

    @Override
    public CmisSharepointObject getObjectByPath(String path) throws IOException {
        LOGGER.debug("Get object by path: {}", path);
        return getObject(path);
    }

    @Override
    public CmisSharepointObject getObject(String id) throws IOException {
        String fixedId = CmisSharepointUtils.removeDoubleSlash(id);
        LOGGER.debug("Get object by id: [{}] using id [{}]", id, fixedId);

        if (isFolder(fixedId)) {
            return new CmisSharepointFolder(id, CmisSharepointUtils.findCurrentFolder(fixedId), ROOT.equals(fixedId));
        }

        Optional<DriveItem> optionalItem = SharepointFacade.getById(fixedId);

        if (optionalItem.isPresent()) {
            DriveItem driveItem = optionalItem.get();
            boolean folder = driveItem.getFolder() != null;
            boolean root = ROOT.equals(fixedId);

            return folder ? new CmisSharepointFolder(fixedId, driveItem.getName(), root)
                    : new CmisSharepointDocument(fixedId, CmisSharepointUtils.findCurrentFile(fixedId));
        }

        // try to handle folders which ids don't have trailing slash
        if (!fixedId.endsWith(IRepository.SEPARATOR)) {
            String folderId = fixedId + IRepository.SEPARATOR;
            if (SharepointFacade.exists(folderId)) {
                return new CmisSharepointFolder(folderId, CmisSharepointUtils.findCurrentFolder(folderId), ROOT.equals(folderId));
            }
        }

        throw new IOException("Missing object with id [" + fixedId + "]");
    }

    private boolean isFolder(String path) {
        return path.endsWith(IRepository.SEPARATOR);
    }
}
