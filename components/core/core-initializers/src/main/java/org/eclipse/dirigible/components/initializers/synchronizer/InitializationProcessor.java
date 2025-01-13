/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import org.eclipse.dirigible.components.base.initializer.Initializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The Class InitializationProcessor.
 */
@Component
public class InitializationProcessor {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InitializationProcessor.class);

    /** The initializers. */
    private final List<Initializer> initializers;

    /**
     * Instantiates a new initialization processor.
     *
     * @param initializers the initializers
     */
    @Autowired
    public InitializationProcessor(List<Initializer> initializers) {
        this.initializers = initializers;
    }

    /**
     * Process initializers.
     */
    void processInitializers() {
        for (var initializer : initializers) {
            try {
                LOGGER.info("Executing initializer [{}]", initializer);
                initializer.initialize();
                LOGGER.info("Initializer [{}] has completed", initializer);
            } catch (Exception e) {
                LOGGER.error("Initializer [{}] failed", initializer, e);
            }

        }
    }
}
