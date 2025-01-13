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

import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The File System based Local Repository implementation of {@link IRepository}.
 */
public class LocalRepository extends FileSystemRepository {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(LocalRepository.class);

    /** The Constant TYPE. */
    public static final String TYPE = "local";

    /** The Constant lastModified. */
    private static final AtomicLong lastModified = new AtomicLong(0);

    /**
     * Constructor with default root folder - user.dir and without database initialization
     *
     * @throws LocalRepositoryException in case the repository cannot be created
     */
    public LocalRepository() throws LocalRepositoryException {
        this(null);
    }

    /**
     * Constructor with root folder parameter.
     *
     * @param rootFolder the root folder
     * @throws LocalRepositoryException in case the repository cannot be created
     */
    public LocalRepository(String rootFolder) throws LocalRepositoryException {
        super(rootFolder);
        lastModified.set(System.currentTimeMillis());
    }

    /**
     * Constructor with root folder parameter.
     *
     * @param rootFolder the root folder
     * @param absolute whether the root folder is absolute
     * @throws LocalRepositoryException in case the repository cannot be created
     */
    public LocalRepository(String rootFolder, boolean absolute) throws LocalRepositoryException {
        super(rootFolder, absolute);
        lastModified.set(System.currentTimeMillis());
    }

    /**
     * Instantiates a new local repository.
     *
     * @param rootFolder the root folder
     * @param absolute the absolute
     * @param versioned the versioned
     * @throws LocalRepositoryException the local repository exception
     */
    public LocalRepository(String rootFolder, boolean absolute, boolean versioned) throws LocalRepositoryException {
        super(rootFolder, absolute, versioned);
        lastModified.set(System.currentTimeMillis());
    }

    /**
     * Initialize.
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IRepositoryReader#initialize()
     */
    @Override
    public void initialize() {
        Configuration.loadModuleConfig("/dirigible-repository-local.properties");
        if (logger.isDebugEnabled()) {
            logger.debug(this.getClass()
                             .getCanonicalName()
                    + " module initialized.");
        }
    }

    /**
     * Gets the last modified.
     *
     * @return the last modified
     */
    @Override
    public long getLastModified() {
        return lastModified.get();
    }

    /**
     * Sets the last modified.
     *
     * @param time the new last modified
     */
    void setLastModified(long time) {
        lastModified.set(time);
    }

}
