/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible
 * contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.internal.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * The Class CmisInternalFolder.
 */
public class CmisInternalFolder extends CmisInternalObject implements CmisFolder {

    /** The session. */
    private CmisInternalSession session;

    /** The internal folder. */
    private ICollection internalFolder;

    /** The repository. */
    private IRepository repository;

    /** The root folder. */
    private boolean rootFolder = false;

    /**
     * Instantiates a new folder.
     *
     * @param session the session
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public CmisInternalFolder(CmisInternalSession session) throws IOException {
        super(session, IRepository.SEPARATOR);
        this.session = session;
        this.repository = (IRepository) session.getCmisRepository()
                                               .getInternalObject();
        this.internalFolder = repository.getRoot();
        this.rootFolder = true;
    }

    /**
     * Instantiates a new folder.
     *
     * @param session the session
     * @param internalCollection the internal collection
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public CmisInternalFolder(CmisInternalSession session, ICollection internalCollection) throws IOException {
        super(session, internalCollection.getPath());
        if (IRepository.SEPARATOR.equals(internalCollection.getPath())) {
            this.rootFolder = true;
        }
        this.session = session;
        this.repository = (IRepository) session.getCmisRepository()
                                               .getInternalObject();
        this.internalFolder = internalCollection;
    }

    /**
     * Instantiates a new folder.
     *
     * @param session the session
     * @param id the id
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public CmisInternalFolder(CmisInternalSession session, String id) throws IOException {
        super(session, id);
        id = sanitize(id);
        if (IRepository.SEPARATOR.equals(id)) {
            this.rootFolder = true;
        }
        this.session = session;
        this.repository = (IRepository) session.getCmisRepository()
                                               .getInternalObject();
        this.internalFolder = this.repository.getCollection(id);
    }

    /**
     * Gets the internal folder.
     *
     * @return the internal folder
     */
    public ICollection getInternalFolder() {
        return internalFolder;
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
     * Returns the Path of this CmisInternalFolder.
     *
     * @return the path
     */
    @Override
    public String getPath() {
        return this.getInternalEntity()
                   .getPath();
    }

    /**
     * Creates a new folder under this CmisInternalFolder.
     *
     * @param properties the properties
     * @return CmisInternalFolder
     * @throws IOException IO Exception
     */
    public CmisInternalFolder createFolder(Map<String, String> properties) throws IOException {
        String name = properties.get(CmisConstants.NAME);
        return new CmisInternalFolder(this.session, this.internalFolder.createCollection(name));
    }

    /**
     * Creates a new document under this CmisInternalFolder.
     *
     * @param properties the properties
     * @param contentStream the content stream
     * @param versioningState the version state
     * @return CmisDocument
     * @throws IOException IO Exception
     */
    public CmisInternalDocument createDocument(Map<String, String> properties, CmisInternalContentStream contentStream,
            VersioningState versioningState) throws IOException {
        String name = properties.get(CmisConstants.NAME);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(contentStream.getStream(), out);
        return new CmisInternalDocument(this.session,
                this.internalFolder.createResource(name, out.toByteArray(), true, contentStream.getMimeType()));
    }

    /**
     * Gets the children.
     *
     * @return the children
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public List<CmisInternalObject> getChildren() throws IOException {
        List<CmisInternalObject> children = new ArrayList<CmisInternalObject>();
        List<ICollection> collections = this.internalFolder.getCollections();
        for (ICollection collection : collections) {
            children.add(new CmisInternalFolder(this.session, collection));
        }
        List<IResource> resources = this.internalFolder.getResources();
        for (IResource resource : resources) {
            children.add(new CmisInternalDocument(this.session, resource));
        }
        return children;
    }

    /**
     * Returns true if this CmisInternalFolder is a root folder and false otherwise.
     *
     * @return whether it is a root folder
     */
    @Override
    public boolean isRootFolder() {
        return rootFolder;
    }

    /**
     * Returns the parent CmisInternalFolder of this CmisInternalFolder.
     *
     * @return CmisInternalFolder
     * @throws IOException IO Exception
     */
    public CmisInternalFolder getFolderParent() throws IOException {
        if (this.internalFolder.getParent() != null) {
            return new CmisInternalFolder(this.session, this.internalFolder.getParent());
        }
        return new CmisInternalFolder(this.session);
    }

}
