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
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.eclipse.dirigible.components.api.sharepoint.SharepointFacade;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.TenantPathResolver;
import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CmisSharepointFolder extends CmisSharepointObject implements CmisFolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmisSharepointFolder.class);
    private final boolean rootFolder;

    public CmisSharepointFolder(String id, String name, boolean rootFolder) {
        super(id, name);
        this.rootFolder = rootFolder;
    }

    @Override
    protected boolean isCollection() {
        return true;
    }

    @Override
    public String getPath() {
        return this.getId();
    }

    @Override
    public CmisSharepointFolder createFolder(Map<String, String> properties) {
        String name = properties.get(CmisConstants.NAME);
        if (rootFolder) {
            String folderName = name + IRepository.SEPARATOR;
            SharepointFacade.createFolder(folderName);
            return new CmisSharepointFolder(folderName, folderName, false);
        } else {
            String fromRootPath = this.getId() + name + IRepository.SEPARATOR;
            String folderName = fromRootPath.startsWith(IRepository.SEPARATOR) ? fromRootPath.substring(1) : fromRootPath;
            SharepointFacade.createFolder(folderName);
            return new CmisSharepointFolder(fromRootPath, folderName, false);
        }
    }

    @Override
    public CmisSharepointDocument createDocument(Map<String, String> properties, CmisContentStream contentStream,
            VersioningState versioningState) {
        return createDocument(properties, contentStream);
    }

    @Override
    public CmisSharepointDocument createDocument(Map<String, String> properties, CmisContentStream contentStream) {
        String name = properties.get(CmisConstants.NAME);
        String folderName = CmisSharepointUtils.removeDoubleSlash(this.getId() + IRepository.SEPARATOR + name);

        SharepointFacade.put(folderName, contentStream.getInputStream(), contentStream.getMimeType());
        return new CmisSharepointDocument(folderName, name);
    }

    @Override
    public List<CmisSharepointObject> getChildren() {
        String path = this.getId();
        TenantPathResolver tenantPathResolver = BeanProvider.getBean(TenantPathResolver.class);
        String tenantPath = tenantPathResolver.resolve(path);
        List<DriveItem> driveItems = SharepointFacade.listObjects(tenantPath)
                                                     .stream()
                                                     .toList();

        Set<SharepointObjectDescriptor> descriptors = CmisSharepointUtils.getDirectChildren(tenantPath, driveItems);

        List<CmisSharepointObject> objects = descriptors.stream()
                                                        .map(this::toChildSharepointObject)
                                                        .toList();
        LOGGER.debug("Found {} direct children for {}", objects, tenantPath);
        return objects;
    }

    private CmisSharepointObject toChildSharepointObject(SharepointObjectDescriptor descriptor) {
        String name = descriptor.getName();
        String id = this.getId() + IRepository.SEPARATOR + name;
        return descriptor.isFolder() ? new CmisSharepointFolder(id, name, false) : new CmisSharepointDocument(id, name);
    }

    @Override
    public CmisSharepointFolder getFolderParent() {
        String parentFolder = CmisSharepointUtils.findParentFolder(this.getId());
        return isRootFolder() || null == parentFolder ? this
                : new CmisSharepointFolder(parentFolder, CmisSharepointUtils.findCurrentFolder(parentFolder), false);
    }

    @Override
    public boolean isRootFolder() {
        return rootFolder;
    }

}
