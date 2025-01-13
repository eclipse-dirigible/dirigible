/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.version.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.Engine;
import org.eclipse.dirigible.components.version.domain.Version;
import org.eclipse.dirigible.repository.api.IRepository;
import org.springframework.stereotype.Service;

/**
 * The Class VersionService.
 */
@Service
public class VersionService {

    /** The Constant DIRIGIBLE_PROPERTIES_PATH. */
    private static final String DIRIGIBLE_PROPERTIES_PATH = "/dirigible.properties";

    /** The Constant DIRIGIBLE_PRODUCT_NAME. */
    private static final String DIRIGIBLE_PRODUCT_NAME = "DIRIGIBLE_PRODUCT_NAME";

    /** The Constant DIRIGIBLE_PRODUCT_VERSION. */
    private static final String DIRIGIBLE_PRODUCT_VERSION = "DIRIGIBLE_PRODUCT_VERSION";

    /** The Constant DIRIGIBLE_PRODUCT_REPOSITORY. */
    private static final String DIRIGIBLE_PRODUCT_REPOSITORY = "DIRIGIBLE_PRODUCT_REPOSITORY";

    /** The Constant DIRIGIBLE_PRODUCT_COMMIT_ID. */
    private static final String DIRIGIBLE_PRODUCT_COMMIT_ID = "DIRIGIBLE_PRODUCT_COMMIT_ID";

    /** The Constant DIRIGIBLE_PRODUCT_TYPE. */
    private static final String DIRIGIBLE_PRODUCT_TYPE = "DIRIGIBLE_PRODUCT_TYPE";

    /** The Constant DIRIGIBLE_INSTANCE_NAME. */
    private static final String DIRIGIBLE_INSTANCE_NAME = "DIRIGIBLE_INSTANCE_NAME";

    /** The engines. */
    private List<Engine> engines;

    /**
     * Instantiates a new version service.
     *
     * @param engines the engines
     */
    public VersionService(List<Engine> engines) {
        this.engines = engines;
    }

    /**
     * Gets the version.
     *
     * @return the version
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Version getVersion() throws IOException {
        Version version = new Version();
        final Properties properties = new Properties();
        properties.load(VersionService.class.getResourceAsStream(DIRIGIBLE_PROPERTIES_PATH));
        version.setProductName(properties.getProperty(DIRIGIBLE_PRODUCT_NAME));
        version.setProductVersion(properties.getProperty(DIRIGIBLE_PRODUCT_VERSION));
        version.setProductRepository(properties.getProperty(DIRIGIBLE_PRODUCT_REPOSITORY));
        version.setProductCommitId(properties.getProperty(DIRIGIBLE_PRODUCT_COMMIT_ID));
        version.setProductType(properties.getProperty(DIRIGIBLE_PRODUCT_TYPE));
        version.setInstanceName(properties.getProperty(DIRIGIBLE_INSTANCE_NAME));
        version.setRepositoryProvider(Configuration.get(IRepository.DIRIGIBLE_REPOSITORY_PROVIDER, "local"));
        // version.setDatabaseProvider(Configuration.get(IDatabase.DIRIGIBLE_DATABASE_PROVIDER));

        List<String> enginesNames = engines.stream()
                                           .map(Engine::getName)
                                           .collect(Collectors.toList());
        Collections.sort(enginesNames);
        version.getEngines()
               .addAll(enginesNames);

        return version;
    }
}
