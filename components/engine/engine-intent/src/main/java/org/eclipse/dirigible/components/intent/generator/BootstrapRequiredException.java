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

import org.eclipse.dirigible.components.intent.parser.IntentValidationException;

/**
 * The one unresolvable cross-model reference a BOOTSTRAP pass can get past: a {@code generates}
 * whose target (or {@code fromUses:} source) is owned by a model whose {@code .model} does not
 * exist yet (dirigible #6539).
 *
 * <p>
 * It is its own type only so the caller can say so: for a MUTUAL cycle - model A mints a document
 * into model B while B holds a foreign key back to A - "generate the dependency first" is advice
 * neither project can follow, and the way out is to run this Generate once with
 * {@code bootstrap=true} (everything but the create-from is emitted), generate the dependency, then
 * regenerate here. Every other unresolvable cross-model reference stays a plain
 * {@link IntentValidationException}: a relation's table / key column / FK type cannot be invented,
 * and skipping it would emit a broken schema rather than a missing button.
 */
public class BootstrapRequiredException extends IntentValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * @param issues the issues describing the create-from that cannot be resolved, and the bootstrap
     *        recipe
     */
    public BootstrapRequiredException(List<String> issues) {
        super(issues);
    }
}
