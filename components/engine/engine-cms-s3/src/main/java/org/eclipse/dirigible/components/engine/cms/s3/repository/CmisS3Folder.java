/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.s3.repository;

import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.eclipse.dirigible.components.api.s3.S3Facade;
import org.eclipse.dirigible.components.api.s3.TenantPathResolved;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.repository.api.IRepository;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Class CmisS3Folder.
 */
public class CmisS3Folder extends CmisS3Object implements CmisFolder {

    /**
     * Is root folder.
     */
    private final boolean rootFolder;

    public CmisS3Folder(String id, String name, boolean rootFolder) {
        super(id, name);
        this.rootFolder = rootFolder;
    }

    /**
     * Checks if is collection.
     *
     * @return true, if is collection
     */
    @Override
    protected boolean isCollection() {
        return true;
    }

    /**
     * Returns the Path of this CmisS3Folder.
     *
     * @return the path
     */
    @Override
    public String getPath() {
        return this.getId();
    }

    /**
     * Creates a new folder under this CmisS3Folder.
     *
     * @param properties the properties
     * @return CmisS3Folder
     */
    public CmisS3Folder createFolder(Map<String, String> properties) {
        String name = properties.get(CmisConstants.NAME);
        if (rootFolder) {
            String folderName = name + IRepository.SEPARATOR;
            S3Facade.put(folderName, new byte[0], "");
            return new CmisS3Folder(folderName, folderName, false);
        } else {
            String fromRootPath = this.getId() + name + IRepository.SEPARATOR;
            String folderName = fromRootPath.startsWith(IRepository.SEPARATOR) ? fromRootPath.substring(1) : fromRootPath;
            S3Facade.put(folderName, new byte[0], "");
            return new CmisS3Folder(fromRootPath, folderName, false);
        }
    }

    /**
     * Creates a new document under this CmisS3Folder.
     *
     * @param properties the properties
     * @param contentStream the content stream
     * @param versioningState the version state
     * @return CmisDocument
     * @throws IOException IO Exception
     */
    @Override
    public CmisS3Document createDocument(Map<String, String> properties, CmisContentStream contentStream, VersioningState versioningState)
            throws IOException {
        return createDocument(properties, contentStream);
    }

    @Override
    public CmisS3Document createDocument(Map<String, String> properties, CmisContentStream contentStream) throws IOException {
        String name = properties.get(CmisConstants.NAME);
        String folderName = this.getId() + name;

        S3Facade.put(folderName, contentStream.getInputStream(), contentStream.getLength(), contentStream.getMimeType());
        return new CmisS3Document(folderName, name);
    }

    /**
     * Gets the children.
     *
     * @return the children
     */
    @Override
    public List<CmisS3Object> getChildren() {
        String path = this.getId();
        TenantPathResolved tenantPathResolved = BeanProvider.getBean(TenantPathResolved.class);
        String tenantPath = tenantPathResolved.resolve(path);
        List<String> objectKeys = S3Facade.listObjects(tenantPath)
                                          .stream()
                                          .map(S3Object::key)
                                          .toList();
        Set<S3ObjectUtil.S3ObjectDescriptor> descriptors = S3ObjectUtil.getDirectChildren(tenantPath, objectKeys);

        return descriptors.stream()
                          .map(this::toS3Object)
                          .toList();
    }

    private CmisS3Object toS3Object(S3ObjectUtil.S3ObjectDescriptor descriptor) {
        String name = descriptor.getName();
        String id = this.getId() + name;
        return descriptor.isFolder() ? new CmisS3Folder(id, name, false) : new CmisS3Document(id, name);
    }

    /**
     * Returns the parent CmisS3Folder of this CmisS3Folder.
     *
     * @return CmisS3Folder
     */
    @Override
    public CmisS3Folder getFolderParent() {
        String parentFolder = CmisS3Utils.findParentFolder(this.getId());
        return isRootFolder() || null == parentFolder ? this
                : new CmisS3Folder(parentFolder, CmisS3Utils.findCurrentFolder(parentFolder), false);
    }

    /**
     * Returns true if this CmisS3Folder is a root folder and false otherwise.
     *
     * @return whether it is a root folder
     */
    @Override
    public boolean isRootFolder() {
        return rootFolder;
    }

}
