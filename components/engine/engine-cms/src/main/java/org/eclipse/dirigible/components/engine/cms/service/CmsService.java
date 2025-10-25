/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

/**
 * The Class CmsService.
 */
@Service
@Transactional
public class CmsService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CmsService.class);

    /**
     * Gets the CMIS object by path.
     *
     * @param path the path to the document
     * @return the CMIS object by path
     * @throws IOException if an I/O error occurs
     */
    public CmisObject getObjectByPath(String path) throws IOException {
        CmisObject cmisObject = CmisSessionFactory.getSession()
                                                  .getObjectByPath(path);
        return cmisObject;
    }

    /**
     * Gets the document content.
     *
     * @param cmisObject the CMIS object
     * @return the document by path
     * @throws IOException if an I/O error occurs
     */
    public byte[] getDocumentContent(CmisObject cmisObject) throws IOException {
        if (cmisObject != null && ObjectType.DOCUMENT.equals(cmisObject.getType()) && cmisObject instanceof CmisDocument) {
            byte[] content;
            content = IOUtils.toByteArray(((CmisDocument) cmisObject).getContentStream()
                                                                     .getInputStream());
            return content;
        }
        return null;
    }

    /**
     * Gets the document content.
     *
     * @param path the path to the document
     * @return the document content as byte array, or null if not found
     * @throws IOException if an I/O error occurs
     */
    public byte[] getDocument(String path) throws IOException {
        CmisObject cmisObject = getObjectByPath(path);
        return getDocumentContent(cmisObject);
    }

    /**
     * Checks existance of the resource by path.
     *
     * @param path the path
     * @return the true if exists and false otherwise
     */
    public boolean existDocument(String path) {
        CmisObject cmisObject;
        try {
            cmisObject = CmisSessionFactory.getSession()
                                           .getObjectByPath(path);
        } catch (IOException e) {
            return false;
        }
        ObjectType type = cmisObject.getType();
        if (ObjectType.DOCUMENT.equals(type) && cmisObject instanceof CmisDocument) {
            return true;
        }
        return false;
    }

    public CmisFolder getRootFolder() throws IOException {
        return CmisSessionFactory.getSession()
                                 .getRootFolder();
    }

    public CmisFolder createFolder(CmisFolder parent, String name) throws IOException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(CmisConstants.NAME, name);
        return parent.createFolder(properties);
    }

    public CmisFolder getChildFolderByName(CmisFolder parent, String name) throws IOException {
        Optional<? extends CmisObject> folderOptional = parent.getChildren()
                                                              .stream()
                                                              .filter(obj -> obj.getName()
                                                                                .equals(name))
                                                              .filter(obj -> obj.getType()
                                                                                .equals(ObjectType.FOLDER))
                                                              .findFirst();
        CmisFolder folder = null;
        if (folderOptional.isPresent()) {
            folder = (CmisFolder) folderOptional.get();
        }
        return folder;
    }

    public CmisDocument getChildDocumentByName(CmisFolder parent, String name) throws IOException {
        Optional<? extends CmisObject> documentOptional = parent.getChildren()
                                                                .stream()
                                                                .filter(obj -> obj.getName()
                                                                                  .equals(name))
                                                                .filter(obj -> obj.getType()
                                                                                  .equals(ObjectType.DOCUMENT))
                                                                .findFirst();
        CmisDocument document = null;
        if (documentOptional.isPresent()) {
            document = (CmisDocument) documentOptional.get();
        }
        return document;
    }

    public CmisDocument createDocument(CmisFolder parent, String name, String mimeType, int size, InputStream inputStream)
            throws IOException {
        CmisContentStream contentStream = CmisSessionFactory.getSession()
                                                            .getObjectFactory()
                                                            .createContentStream(name, size, mimeType, inputStream);
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT);
        properties.put(CmisConstants.NAME, name);
        CmisDocument newDocument = parent.createDocument(properties, contentStream, VersioningState.MAJOR);
        return newDocument;
    }


    public void updateDocument(CmisFolder parent, CmisDocument document, String mimeType, int size, InputStream inputStream)
            throws IOException {
        Date timestamp = new Date();
        String newName = document.getName() + "-" + timestamp.getTime();
        String oldName = document.getName();

        CmisDocument newDocument = createDocument(parent, newName, mimeType, size, inputStream);
        try {
            document.delete();
        } catch (Exception e) {
            // do nothing
        }
        newDocument.rename(oldName);
    };

    public InputStream getDocumentStream(CmisDocument document) throws IOException {
        return document.getContentStream()
                       .getInputStream();
    }

}
