/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import java.util.Date;

import org.eclipse.dirigible.repository.api.IEntityInformation;
import org.eclipse.dirigible.repository.api.RepositoryPath;

/**
 * The file system based implementation of {@link IEntityInformation}.
 */
public class LocalEntityInformation implements IEntityInformation {

    /** The wrapper path. */
    private RepositoryPath wrapperPath;

    /** The master. */
    private LocalObject master;

    /** The size. */
    private long size;

    /**
     * Instantiates a new local entity information.
     *
     * @param wrapperPath the wrapper path
     * @param master the master
     */
    public LocalEntityInformation(RepositoryPath wrapperPath, LocalObject master) {
        this.wrapperPath = wrapperPath;
        this.master = master;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getName()
     */
    @Override
    public String getName() {
        return this.wrapperPath.getLastSegment();
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getPath()
     */
    @Override
    public String getPath() {
        return this.wrapperPath.toString();
    }

    /**
     * Gets the permissions.
     *
     * @return the permissions
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getPermissions()
     */
    @Override
    public int getPermissions() {
        return this.master.getPermissions();
    }

    /**
     * Gets the size.
     *
     * @return the size
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getSize()
     */
    @Override
    public Long getSize() {
        return this.size;
    }

    /**
     * Gets the created by.
     *
     * @return the created by
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getCreatedBy()
     */
    @Override
    public String getCreatedBy() {
        return this.master.getCreatedBy();
    }

    /**
     * Gets the created at.
     *
     * @return the created at
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getCreatedAt()
     */
    @Override
    public Date getCreatedAt() {
        return this.master.getCreatedAt();
    }

    /**
     * Gets the modified by.
     *
     * @return the modified by
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getModifiedBy()
     */
    @Override
    public String getModifiedBy() {
        return this.master.getModifiedBy();
    }

    /**
     * Gets the modified at.
     *
     * @return the modified at
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IEntityInformation#getModifiedAt()
     */
    @Override
    public Date getModifiedAt() {
        return this.master.getModifiedAt();
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LocalEntityInformation)) {
            return false;
        }
        final LocalEntityInformation other = (LocalEntityInformation) obj;
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
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

}
