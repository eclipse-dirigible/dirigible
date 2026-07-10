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
}
