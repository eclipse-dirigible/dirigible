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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * The file system based implementation of {@link ICollection}.
 */
public class LocalCollection extends LocalEntity implements ICollection {

    /**
     * Instantiates a new local collection.
     *
     * @param repository the repository
     * @param path the path
     */
    public LocalCollection(FileSystemRepository repository, RepositoryPath path) {
        super(repository, path);
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
        final ICollection parent = getParent();
        if (parent == null) {
            throw new LocalRepositoryException("Cannot create root collection.");
        }
        parent.createCollection(getName());
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
        final LocalFolder folder = getFolderSafe();
        try {
            folder.deleteTree();
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not delete collection {0} ", this.getName()), ex);
        }
    }

    /**
     * Gets the folder safe.
     *
     * @return the folder safe
     * @throws RepositoryNotFoundException the repository not found exception
     */
    protected LocalFolder getFolderSafe() throws RepositoryNotFoundException {
        final LocalFolder folder = getFolder();
        if (folder == null) {
            throw new RepositoryNotFoundException(String.format("There is no collection at path '%s'.", getPath()));
        }
        return folder;
    }

    /**
     * Gets the folder.
     *
     * @return the folder
     * @throws RepositoryReadException the repository read exception
     */
    public LocalFolder getFolder() throws RepositoryReadException {
        final LocalObject object = getLocalObject();
        if (object == null) {
            return null;
        }
        if (!(object instanceof LocalFolder)) {
            return null;
        }
        return (LocalFolder) object;
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
        final LocalFolder folder = getFolderSafe();
        try {
            folder.renameFolder(RepositoryPath.normalizePath(getParent().getPath(), name));
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not rename collection {0}", this.getName()), ex);
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
        final LocalFolder folder = getFolderSafe();
        try {
            folder.renameFolder(path);
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not move collection {0}", this.getName()), ex);
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
        final LocalFolder folder = getFolderSafe();
        try {
            folder.copyFolder(path);
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not copy collection {0}", this.getName()), ex);
        }
    }

    /**
     * Exists.
     *
     * @return true, if successful
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntity#exists()
     */
    @Override
    public boolean exists() throws RepositoryWriteException {
        String repositoryPath = getRepositoryPath().toString();
        if (IRepository.SEPARATOR.equals(repositoryPath)) {
            return true;
        }
        // String localPath = LocalWorkspaceMapper.getMappedName(getRepository(), repositoryPath);
        return getRepository().getRepositoryDao()
                              .directoryExists(repositoryPath);
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
        return getResources().isEmpty() && getCollections().isEmpty();
    }

    /**
     * Gets the collections.
     *
     * @return the collections
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#getCollections()
     */
    @Override
    public List<ICollection> getCollections() throws RepositoryReadException {
        // return new ArrayList<ICollection>(collections.values());
        final List<String> collectionNames = getCollectionsNames();
        final List<ICollection> result = new ArrayList<ICollection>(collectionNames.size());
        for (String collectionName : collectionNames) {
            result.add(getCollection(collectionName));
        }
        return result;
    }

    /**
     * Gets the collections names.
     *
     * @return the collections names
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#getCollectionsNames()
     */
    @Override
    public List<String> getCollectionsNames() throws RepositoryReadException {
        final List<String> result = new ArrayList<String>();
        final LocalFolder folder = getFolderSafe();
        try {
            for (LocalObject child : folder.getChildren()) {
                if (child instanceof LocalFolder) {
                    result.add(child.getName());
                }
            }
        } catch (LocalRepositoryException ex) {
            throw new RepositoryReadException(format("Could not get child collection names {0} ", this.getName()), ex);
        }
        return result;
    }

    /**
     * Creates the collection.
     *
     * @param name the name
     * @return the i collection
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#createCollection(java.lang.String)
     */
    @Override
    public ICollection createCollection(String name) throws RepositoryWriteException {
        createAncestorsAndSelfIfMissing();
        final LocalFolder folder = getFolderSafe();
        try {
            folder.createFolder(name);
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not create child collection {0}", name), ex);
        }
        return getCollection(name);
    }

    /**
     * Gets the collection.
     *
     * @param name the name
     * @return the collection
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#getCollection(java.lang.String)
     */
    @Override
    public ICollection getCollection(String name) {
        final RepositoryPath path = getRepositoryPath().append(name);
        return new LocalCollection(getRepository(), path);
    }

    /**
     * Removes the collection.
     *
     * @param childCollection the child collection
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#removeCollection(org.eclipse.dirigible.
     * repository.api. ICollection)
     */
    @Override
    public void removeCollection(ICollection childCollection) throws RepositoryWriteException {
        removeCollection(childCollection.getName());
    }

    /**
     * Removes the collection.
     *
     * @param name the name
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#removeCollection(java.lang.String)
     */
    @Override
    public void removeCollection(String name) throws RepositoryWriteException {
        final ICollection collection = getCollection(name);
        collection.delete();
    }

    /**
     * Gets the resources.
     *
     * @return the resources
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#getResources()
     */
    @Override
    public List<IResource> getResources() throws RepositoryReadException {
        final List<String> resourceNames = getResourcesNames();
        final List<IResource> result = new ArrayList<IResource>(resourceNames.size());
        for (String resourceName : resourceNames) {
            result.add(getResource(resourceName));
        }
        return result;
    }

    /**
     * Gets the resources names.
     *
     * @return the resources names
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#getResourcesNames()
     */
    @Override
    public List<String> getResourcesNames() throws RepositoryReadException {
        final List<String> result = new ArrayList<String>();
        final LocalFolder folder = getFolderSafe();
        try {
            for (LocalObject child : folder.getChildren()) {
                if (child instanceof LocalFile) {
                    result.add(child.getName());
                }
            }
        } catch (LocalRepositoryException ex) {
            throw new RepositoryReadException(format("Could not get child resource names {0}", this.getName()), ex);
        }
        return result;
    }

    /**
     * Creates the resource.
     *
     * @param name the name
     * @param content the content
     * @return the i resource
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#createResource(java.lang.String, byte[])
     */
    @Override
    public IResource createResource(String name, byte[] content) throws RepositoryWriteException {
        String contentType = ContentTypeHelper.getContentType(ContentTypeHelper.getExtension(name));
        boolean isBinary = ContentTypeHelper.isBinary(contentType);
        return createResource(name, content, isBinary, contentType);
    }

    /**
     * Creates the resource.
     *
     * @param name the name
     * @param content the content
     * @param isBinary the is binary
     * @param contentType the content type
     * @return the i resource
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#createResource(java.lang.String, byte[],
     * boolean, java.lang.String)
     */
    @Override
    public IResource createResource(String name, byte[] content, boolean isBinary, String contentType) throws RepositoryWriteException {
        createAncestorsAndSelfIfMissing();
        final LocalFolder folder = getFolderSafe();
        try {
            folder.createFile(name, content, isBinary, contentType);
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not create child document {0}", name), ex);
        }
        return getResource(name);
    }

    /**
     * Gets the resource.
     *
     * @param name the name
     * @return the resource
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#getResource(java.lang.String)
     */
    @Override
    public IResource getResource(String name) throws RepositoryReadException {
        final RepositoryPath path = getRepositoryPath().append(name);
        return new LocalResource(getRepository(), path);
    }

    @Override
    public IResource createResource(String name, InputStream contentInputStream, boolean isBinary, String contentType)
            throws RepositoryWriteException {
        createAncestorsAndSelfIfMissing();
        final LocalFolder folder = getFolderSafe();
        try {
            folder.createFile(name, contentInputStream);
        } catch (LocalRepositoryException ex) {
            throw new RepositoryWriteException(format("Could not create child document {0}", name), ex);
        }
        return getResource(name);
    }

    /**
     * Removes the resource.
     *
     * @param resource the resource
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.dirigible.repository.api.ICollection#removeResource(org.eclipse.dirigible.repository.
     * api.IResource)
     */
    @Override
    public void removeResource(IResource resource) throws RepositoryWriteException {
        removeResource(resource.getName());
    }

    /**
     * Removes the resource.
     *
     * @param name the name
     * @throws RepositoryWriteException the repository write exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#removeResource(java.lang.String)
     */
    @Override
    public void removeResource(String name) throws RepositoryWriteException {
        final IResource resource = getResource(name);
        resource.delete();
    }

    /**
     * Gets the children.
     *
     * @return the children
     * @throws RepositoryReadException the repository read exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.ICollection#getChildren()
     */
    @Override
    public List<IEntity> getChildren() throws RepositoryReadException {
        final List<IEntity> result = new ArrayList<IEntity>();
        result.addAll(getCollections());
        result.addAll(getResources());
        return result;
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
        if (!(obj instanceof LocalCollection other)) {
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

}
