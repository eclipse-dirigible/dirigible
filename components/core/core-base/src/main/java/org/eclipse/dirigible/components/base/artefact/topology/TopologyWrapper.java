/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact.topology;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Class TopologyWrapper.
 */
public class TopologyWrapper<A extends Artefact> implements TopologicallySortable, TopologicallyDepletable {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TopologyWrapper.class);

    /** The artefact. */
    private final A artefact;

    /** The wrappers. */
    private final Map<String, TopologyWrapper<A>> wrappers;

    /** The synchronizer. */
    private final Synchronizer<A, ?> synchronizer;

    /**
     * Instantiates a new topology wrapper.
     *
     * @param artefact the artefact
     * @param wrappers the wrappers
     * @param synchronizer the synchronizer
     */
    public TopologyWrapper(A artefact, Map<String, TopologyWrapper<A>> wrappers, Synchronizer<A, ?> synchronizer) {
        this.artefact = artefact;
        this.wrappers = wrappers;
        this.synchronizer = synchronizer;
        this.wrappers.put(getId(), this);
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    @Override
    public String getId() {
        return this.artefact.getKey();
    }

    /**
     * Gets the synchronizer.
     *
     * @return the synchronizer
     */
    public Synchronizer<A, ?> getSynchronizer() {
        return synchronizer;
    }

    /**
     * Gets the dependencies.
     *
     * @return the dependencies
     */
    @Override
    public List<TopologicallySortable> getDependencies() {
        List<TopologicallySortable> dependencies = new ArrayList<TopologicallySortable>();
        if (this.artefact.getDependencies() != null) {
            Set<String> keys = this.artefact.getDependencies();
            for (String key : keys) {
                if (!wrappers.containsKey(key)) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Dependency is not present in this cycle: " + key);
                    }
                } else {
                    dependencies.add(wrappers.get(key));
                }
            }
        }
        return dependencies;
    }

    /**
     * Complete.
     *
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    public boolean complete(ArtefactPhase flow) {
        if (synchronizer.isAccepted(getArtefact().getType())) {
            try {
                return synchronizer.complete(this, flow);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Complete failed in this cycle: " + e.getMessage(), e);
                }
                if (logger.isErrorEnabled()) {
                    logger.error("Failed for artefact: {} and phase: {}", getArtefact().getKey(), flow.getValue());
                }

                return false;
            }
        }
        return true;
    }

    /**
     * Gets the artefact.
     *
     * @return the artefact
     */
    public A getArtefact() {
        return artefact;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TopologyWrapper [artefact=" + artefact + ", synchronizer=" + synchronizer + "]";
    }

}
