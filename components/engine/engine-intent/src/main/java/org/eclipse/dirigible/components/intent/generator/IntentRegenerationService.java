/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.List;

import org.eclipse.dirigible.components.intent.domain.Intent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the regeneration pass for a single {@link Intent}. Hands every registered
 * {@link IntentTargetGenerator} the same {@link IntentGenerationContext} in {@code @Order} order
 * and isolates per-generator failures so one broken slice does not block the others.
 */
@Component
public class IntentRegenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentRegenerationService.class);

    private final List<IntentTargetGenerator> generators;
    private final IRepository repository;

    public IntentRegenerationService(List<IntentTargetGenerator> generators, IRepository repository) {
        this.generators = generators;
        this.repository = repository;
    }

    /**
     * Re-emit every gen/ artefact for the given intent. The intent's {@code location} is used to derive
     * the project root ({@code /registry/public/<project>/...}).
     *
     * @param intent the intent whose gen/ output should be refreshed
     */
    public void regenerate(Intent intent) {
        IntentModel model = IntentParser.parse(intent.getContent());
        intent.setModel(model);
        String projectRoot = resolveProjectRoot(intent.getLocation());
        IntentGenerationContext context = new IntentGenerationContext(intent, model, projectRoot, repository);
        LOGGER.info("Regenerating gen/ for intent [{}] under [{}] via {} generator(s)", intent.getName(), projectRoot, generators.size());
        for (IntentTargetGenerator generator : generators) {
            try {
                generator.generate(context);
            } catch (RuntimeException e) {
                LOGGER.error("Intent generator [{}] failed for intent [{}]", generator.name(), intent.getName(), e);
            }
        }
    }

    /**
     * Strip the file segment from an intent location, returning the project root path.
     */
    private static String resolveProjectRoot(String location) {
        if (location == null) {
            return "";
        }
        int lastSlash = location.lastIndexOf('/');
        return lastSlash <= 0 ? location : location.substring(0, lastSlash);
    }
}
