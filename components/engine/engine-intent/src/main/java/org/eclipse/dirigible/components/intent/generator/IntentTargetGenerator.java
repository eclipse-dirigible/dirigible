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

/**
 * SPI implemented by every target-artefact generator. Each generator owns one slice of the {@code
 * gen/} output - entities, processes, forms, reports, permissions, controllers, schemas - and is
 * idempotent: calling {@link #generate(IntentGenerationContext)} twice for the same intent must
 * produce byte-identical files.
 *
 * <p>
 * Generators are Spring beans, discovered via classpath component scanning and aggregated by
 * {@link IntentGenerationService}. Order across generators (entities before forms before
 * controllers, etc.) is established by {@code @Order} on each implementation.
 */
public interface IntentTargetGenerator {

    /**
     * A short, stable identifier for the slice this generator owns. Used in logs and for
     * future-proofing (e.g. enabling / disabling individual slices via configuration).
     */
    String name();

    /**
     * Regenerate this generator's slice of the {@code gen/} output for the given intent. Writes
     * exclusively through {@link IntentGenerationContext#writeModelFile(String, String)}.
     *
     * @param context the per-regeneration call context
     */
    void generate(IntentGenerationContext context);
}
