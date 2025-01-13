/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.synchronizer;

import org.eclipse.dirigible.components.base.artefact.Artefact;

/**
 * The Class MultitenantBaseSynchronizer.
 *
 */
public abstract class MultitenantBaseSynchronizer<A extends Artefact, ID> extends BaseSynchronizer<A, ID> {

    /**
     * Multitenant execution.
     *
     * @return true, if successful
     */
    @Override
    public final boolean multitenantExecution() {
        return true;
    }

    /**
     * Checks if is multitenant artefact.
     *
     * @param artefact the artefact
     * @return true, if is multitenant artefact
     */
    @Override
    protected boolean isMultitenantArtefact(A artefact) {
        return true;
    }

}
