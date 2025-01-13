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
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;

import java.util.Set;

/**
 * The Interface SynchronizerCallback.
 */
public interface SynchronizerCallback {

    /**
     * Adds the error.
     *
     * @param error the error
     */
    void addError(String error);

    /**
     * Gets the errors.
     *
     * @return the errors
     */
    Set<String> getErrors();

    /**
     * Register errors.
     *
     * @param remained the remained
     * @param lifecycle the lifecycle
     */
    void registerErrors(Set<TopologyWrapper<? extends Artefact>> remained, ArtefactLifecycle lifecycle);

    /**
     * Register errors.
     *
     * @param remained the remained
     */
    void registerFatals(Set<TopologyWrapper<? extends Artefact>> remained);

    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle);

    /**
     * Register errors.
     *
     * @param synchronizer the synchronizer
     * @param wrapper the wrapper
     * @param lifecycle the lifecycle
     * @param message the message
     */
    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle, String message);

    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle, String message, Throwable cause);

    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle, Throwable cause);

    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle);

    /**
     * Register errors.
     *
     * @param synchronizer the synchronizer
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param message the message
     */
    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle, String message);

    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle, Throwable cause);

    void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle, String message,
            Throwable cause);

}
