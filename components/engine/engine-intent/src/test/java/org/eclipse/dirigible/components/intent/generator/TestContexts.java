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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.repository.api.IRepository;

/**
 * Test-only factory for the package-private {@link IntentGenerationContext}, so generator tests in
 * sub-packages can exercise build paths that need a context (naming, security) without a
 * repository.
 */
public final class TestContexts {

    private TestContexts() {}

    /** A repository-less context over the given model. */
    public static IntentGenerationContext context(IntentModel model) {
        return new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", null);
    }

    /**
     * A context backed by a real repository, for exercising the write surface itself (what a generator
     * creates, overwrites, or leaves alone) rather than only the content it builds.
     *
     * @param model the parsed intent model
     * @param repository the repository the context writes through
     * @param projectRoot the repository-absolute project root
     * @param fallbackName the base name for single-file outputs
     * @return the context
     */
    public static IntentGenerationContext context(IntentModel model, IRepository repository, String projectRoot, String fallbackName) {
        return new IntentGenerationContext(model, projectRoot, "proj", "workspace", fallbackName, repository);
    }

    /**
     * A repository-backed context running as the declared BOOTSTRAP pass of a mutual cross-model cycle
     * (dirigible #6539).
     *
     * @param model the parsed intent model
     * @param repository the repository the context reads and writes through
     * @param projectRoot the repository-absolute project root
     * @return the context
     */
    public static IntentGenerationContext bootstrapContext(IntentModel model, IRepository repository, String projectRoot) {
        return new IntentGenerationContext(model, projectRoot, "proj", "workspace", "app", repository, false, true);
    }
}
