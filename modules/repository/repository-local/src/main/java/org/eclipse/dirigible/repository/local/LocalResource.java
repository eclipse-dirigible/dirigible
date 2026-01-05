/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.repository.api.*;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static java.text.MessageFormat.format;

/**
 * The file system based implementation of {@link IResource}.
 */
public class LocalResource extends LocalEntity implements IResource {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(LocalResource.class);

    /** The binary. */
    private boolean binary = false;

    /** The content type. */
    private String contentType;

    /**
     * Instantiates a new local resource.
     *
     * @param repository the repository
     * @param path the path
     */
    public LocalResource(FileSystemRepository repository, RepositoryPath path) {
        super(repository, path);
        try {
            LocalFile localFile = getDocument();
            if (localFile != null) {
                this.binary = localFile.isBinary();
                this.contentType = localFile.getContentType();

            }
        } catch (RepositoryReadException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the {@link LocalFile} object matching this {@link LocalResource}. If there is no such
     * object, then <code>null</code> is returned.
     *
     * @return the document
     * @throws RepositoryReadException the repository read exception
     */
    protected LocalFile getDocument() throws RepositoryReadException {
        final LocalObject object = getLocalObject();
        if (object == null) {
            return null;
        }
        if (!(object instanceof LocalFile)) {
            return null;
        }
        return (LocalFile) object;
    }

    /**
     * Creates the.
     *
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#create()
     */
    @Override
    public void create() throws RepositoryWriteException {
        getParent().createResource(getName(), (byte[]) null, false, CONTENT_TYPE_DEFAULT);
    }

    /**
     * Delete.
     *
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#delete()
     */
    @Override
    public void delete() throws RepositoryWriteException {
        final LocalFile document = getDocumentSafe();
        try {
            document.delete();
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not delete resource {0} ", this.getName()), ex);
        }
    }

    /**
     * Returns the {@link LocalFile} object matching this {@link LocalResource}. If there is no such
     * object, then an {@link RepositoryNotFoundException} is thrown.
     *
     * @return the document safe
     * @throws RepositoryNotFoundException the repository not found exception
     */
    protected LocalFile getDocumentSafe() throws RepositoryNotFoundException {
        final LocalFile document = getDocument();
        if (document == null) {
            throw new RepositoryNotFoundException(format("There is no resource at path ''{0}''.", getPath()));
        }
        return document;
    }

    /**
     * Rename to.
     *
     * @param name the name
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#renameTo(java.lang.String)
     */
    @Override
    public void renameTo(String name) throws RepositoryWriteException {
        final LocalFile document = getDocumentSafe();
        try {
            document.rename(RepositoryPath.normalizePath(getParent().getPath(), name));
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not rename resource {0}", this.getName()), ex);
        }
    }

    /**
     * Move to.
     *
     * @param path the path
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#moveTo(java.lang.String)
     */
    @Override
    public void moveTo(String path) throws RepositoryWriteException {
        final LocalFile document = getDocumentSafe();
        try {
            document.rename(path);
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not move resource {0}", this.getName()), ex);
        }
    }

    /**
     * Copy to.
     *
     * @param path the path
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#copyTo(java.lang.String)
     */
    @Override
    public void copyTo(String path) throws RepositoryWriteException {
        try {
            String repositorySourcePath = getRepositoryPath().toString();
            String repositoryTargetPath = RepositoryPath.normalizePath(path, getName());
            // String workspacePathOld = LocalWorkspaceMapper.getMappedName(getRepository(),
            // repositorySourcePath);
            // String workspacePathNew = LocalWorkspaceMapper.getMappedName(getRepository(),
            // repositoryTargetPath);
            getRepository().getRepositoryDao()
                           .copyFile(repositorySourcePath, repositoryTargetPath);
            // FileSystemUtils.copyFile(workspacePathOld, workspacePathNew);
        } catch (LocalRepositoryException e) {
            throw new RepositoryWriteException(e);
        }
    }

    /**
     * Exists.
     *
     * @return true, if successful
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#exists()
     */
    @Override
    public boolean exists() throws RepositoryReadException {
        String repositoryPath = getRepositoryPath().toString();
        // String localPath = LocalWorkspaceMapper.getMappedName(getRepository(), repositoryPath);
        return getRepository().getRepositoryDao()
                              .fileExists(repositoryPath);// (FileSystemUtils.fileExists(localPath));
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#isEmpty()
     */
    @Override
    public boolean isEmpty() throws RepositoryReadException {
        return (getContent().length == 0);
    }

    /**
     * Gets the content.
     *
     * @return the content
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IResource#getContent()
     */
    @Override
    public byte[] getContent() throws RepositoryReadException {
        final LocalFile document = getDocumentSafe();
        try {
            byte[] bytes = document.getData();
            return bytes;
        } catch (LocalRepositoryException ex) {
            throw new RepositoryReadException("Could not read resource content.", ex);
        }
    }

    /**
     * Sets the content.
     *
     * @param content the new content
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IResource#setContent(byte[])
     */
    @Override
    public void setContent(byte[] content) throws RepositoryWriteException {

        this.contentType = ContentTypeHelper.getContentType(ContentTypeHelper.getExtension(getName()));
        this.binary = ContentTypeHelper.isBinary(contentType);

        if (exists()) {
            final LocalFile document = getDocumentSafe();
            try {
                document.setData(content);
            } catch (LocalRepositoryException ex) {
                throw new RepositoryWriteException("Could not update document.", ex);
            }
        } else {
            getParent().createResource(getName(), content, this.binary, this.contentType);
        }
    }

    @Override
    public InputStream getContentStream() throws RepositoryReadException {
        final LocalFile document = getDocumentSafe();
        try {
            return document.getDataStream();
        } catch (LocalRepositoryException ex) {
            throw new RepositoryReadException("Could not read resource content.", ex);
        }
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.local.LocalEntity#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LocalResource other)) {
            return false;
        }
        return getPath().equals(other.getPath());
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.local.LocalEntity#hashCode()
     */
    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    /**
     * Returns the {@link LocalFile} object matching this {@link LocalResource}. If there is no such
     * object, then <code>null</code> is returned.
     *
     * @return the local file
     * @throws RepositoryReadException the repository read exception
     */
    protected LocalFile getFile() throws RepositoryReadException {
        return getDocument();
    }

    /**
     * Checks if is binary.
     *
     * @return true, if is binary
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IResource#isBinary()
     */
    @Override
    public boolean isBinary() {
        return binary;
    }

    /**
     * Gets the content type.
     *
     * @return the content type
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IResource#getContentType()
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content.
     *
     * @param content the content
     * @param isBinary the is binary
     * @param contentType the content type
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IResource#setContent(byte[], boolean, java.lang.String)
     */
    @Override
    public void setContent(byte[] content, boolean isBinary, String contentType) throws RepositoryWriteException {

        this.binary = isBinary;
        this.contentType = contentType;

        if (!isBinary) {
            setContent(content);
        }

        if (exists()) {
            final LocalFile document = getDocumentSafe();
            try {
                document.setData(content);
            } catch (LocalRepositoryException ex) {
                throw new RepositoryWriteException("Could not update document.", ex);
            }
        } else {
            getParent().createResource(getName(), content, binary, contentType);
        }

    }

}
