/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.readiness;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;

/**
 * The lifecycle census of every registered artefact, taken at the end of a synchronization pass
 * (#7533) - the number a boot gate reads instead of grepping the log for
 * {@code Undepleted artefact}. An artefact counts as failed when it is
 * {@link ArtefactLifecycle#FAILED} (retried) or {@link ArtefactLifecycle#FATAL} (stripped until its
 * source changes): either way it is not running.
 *
 * @param total the registered artefacts
 * @param failedByType the failed artefacts per artefact type, types without a failure omitted
 */
public record ArtefactCensus(int total, Map<String, Integer> failedByType) {

    /** The census before the first pass has taken one. */
    public static final ArtefactCensus NONE = new ArtefactCensus(0, Map.of());

    /**
     * An immutable census.
     *
     * @param total the registered artefacts
     * @param failedByType the failed artefacts per artefact type
     */
    public ArtefactCensus {
        failedByType = Map.copyOf(failedByType);
    }

    /**
     * Takes the census of the given artefacts.
     *
     * @param artefacts the registered artefacts
     * @return the census
     */
    public static ArtefactCensus of(Collection<? extends Artefact> artefacts) {
        Map<String, Integer> failedByType = new TreeMap<>();
        for (Artefact artefact : artefacts) {
            if (isFailed(artefact)) {
                failedByType.merge(artefact.getType(), 1, Integer::sum);
            }
        }
        return new ArtefactCensus(artefacts.size(), failedByType);
    }

    /**
     * The failed artefacts across all types.
     *
     * @return the failed artefact count
     */
    public int failed() {
        return failedByType.values()
                           .stream()
                           .mapToInt(Integer::intValue)
                           .sum();
    }

    private static boolean isFailed(Artefact artefact) {
        return ArtefactLifecycle.FAILED.equals(artefact.getLifecycle()) || ArtefactLifecycle.FATAL.equals(artefact.getLifecycle());
    }
}
