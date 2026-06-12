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

import org.eclipse.dirigible.components.intent.domain.Intent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.repository.api.IRepository;

/**
 * Per-regeneration call context handed to every {@link IntentTargetGenerator}. Carries the parsed
 * intent, the originating artefact (for location / key metadata), the project root inside the
 * Dirigible repository, and a handle to {@link IRepository} for writing files under {@code gen/}.
 *
 * <p>
 * Generators are forbidden from writing anywhere other than {@code <projectRoot>/gen/} - the
 * synchronizer relies on this to scrub stale gen/ files between cycles without risking
 * developer-authored files.
 */
public final class IntentGenerationContext {

    /** Repository sub-path of the project root, e.g. {@code /registry/public/orders}. */
    private final String projectRoot;

    /** Repository sub-path of the gen folder, always {@code <projectRoot>/gen}. */
    private final String genRoot;

    private final Intent intent;
    private final IntentModel model;
    private final IRepository repository;

    public IntentGenerationContext(Intent intent, IntentModel model, String projectRoot, IRepository repository) {
        this.intent = intent;
        this.model = model;
        this.projectRoot = projectRoot;
        this.genRoot = projectRoot + "/gen";
        this.repository = repository;
    }

    /**
     * Project name derived from the project root. The intent location is
     * {@code /<project>/.../app.intent} inside the repository so the project name is the first
     * non-empty path segment of {@link #projectRoot}.
     *
     * @return the project name, never null but possibly empty for malformed roots
     */
    public String getProjectName() {
        if (projectRoot == null || projectRoot.isEmpty()) {
            return "";
        }
        int start = projectRoot.startsWith("/") ? 1 : 0;
        int next = projectRoot.indexOf('/', start);
        return next < 0 ? projectRoot.substring(start) : projectRoot.substring(start, next);
    }

    public Intent getIntent() {
        return intent;
    }

    public IntentModel getModel() {
        return model;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public String getGenRoot() {
        return genRoot;
    }

    public IRepository getRepository() {
        return repository;
    }
}
