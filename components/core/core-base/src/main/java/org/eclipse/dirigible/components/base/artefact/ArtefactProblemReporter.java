/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact;

import java.util.Collection;

/**
 * Surfaces the artefacts a synchronizer could not reconcile as entries in the IDE Problems view, so
 * a deploy or parse failure is visible to the developer rather than only in the server log.
 *
 * <p>
 * Implemented by a higher-level module (the Problems store) and consumed by the synchronization
 * processor through an {@code ObjectProvider}; in an assembly without the Problems store it is
 * simply absent and the processor skips it.
 */
public interface ArtefactProblemReporter {

    /**
     * Makes the registered synchronization problems match the given failed artefacts: records (or
     * refreshes) a problem with its cause for each, and clears the problem of any artefact that is no
     * longer failing - one that has healed on a later pass or whose source file was removed.
     *
     * @param failedArtefacts every artefact currently in a {@code FAILED} or {@code FATAL} lifecycle
     */
    void reconcileSynchronizationProblems(Collection<? extends Artefact> failedArtefacts);
}
