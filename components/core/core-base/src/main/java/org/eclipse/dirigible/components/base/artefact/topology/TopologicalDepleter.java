/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact.topology;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The Class TopologicalDepleter.
 *
 * @param <T> the generic type
 */
public class TopologicalDepleter<T extends TopologicallyDepletable> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TopologicalDepleter.class);

    /**
     * Deplete.
     *
     * @param list the list
     * @param flow the flow
     * @return the list
     */
    @WithSpan
    public Set<T> deplete(Set<T> list, ArtefactPhase flow) {
        Span span = Span.current();
        span.setAttribute("flow", flow.getValue());
        span.setAttribute("artifacts.count", list.size());

        Set<T> depletables = new HashSet<>();
        depletables.addAll(list);
        int count = depletables.size();
        boolean repeat = true;
        do {
            Iterator<T> iterator = depletables.iterator();
            while (iterator.hasNext()) {
                TopologicallyDepletable depletable = iterator.next();
                try {
                    if (depletable.complete(flow)) {
                        iterator.remove();
                    }
                } catch (Exception e) {
                    logger.error("Error has been thrown on depleting artefact: [{}] at phase: [{}]", depletable.getId(), flow.getValue());
                    iterator.remove();
                }
            }
            repeat = count > depletables.size();
            count = depletables.size();
        } while (repeat);
        return depletables;
    }

}
