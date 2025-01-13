/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.master.jar;

import java.io.IOException;

import org.eclipse.dirigible.repository.api.IMasterRepository;
import org.eclipse.dirigible.repository.local.LocalRepositoryException;

/**
 * The Jar Master Repository.
 */
public class JarMasterRepository extends JarRepository implements IMasterRepository {

    /** The Constant TYPE. */
    public static final String TYPE = "jar";

    /** The Constant DIRIGIBLE_MASTER_REPOSITORY_JAR_PATH. */
    public static final String DIRIGIBLE_MASTER_REPOSITORY_JAR_PATH = "DIRIGIBLE_MASTER_REPOSITORY_JAR_PATH";

    /**
     * Instantiates a new jar master repository.
     *
     * @param zip the zip
     * @throws LocalRepositoryException the local repository exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public JarMasterRepository(String zip) throws LocalRepositoryException, IOException {
        super(zip);
    }

}
