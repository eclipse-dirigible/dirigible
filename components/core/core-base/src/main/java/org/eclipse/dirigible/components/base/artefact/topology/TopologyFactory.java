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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;

/**
 * A factory for creating Topology objects.
 */
public class TopologyFactory {

    /**
     * Wrap.
     *
     * @param artefacts the artefacts
     * @param synchronizers the synchronizers
     * @return the list of topology wrappers
     */
    public static final List<TopologyWrapper<? extends Artefact>> wrap(Collection<? extends Artefact> artefacts,
            List<Synchronizer<?, ?>> synchronizers) {
        List<TopologyWrapper<? extends Artefact>> list = new ArrayList<>();
        Map<String, TopologyWrapper<Artefact>> wrappers = new HashMap<>();
        for (Artefact artefact : artefacts) {
            for (Synchronizer<?, ?> synchronizer : synchronizers) {
                if (synchronizer.isAccepted(artefact.getType())) {
                    TopologyWrapper<Artefact> wrapper = new TopologyWrapper(artefact, wrappers, synchronizer);
                    list.add(wrapper);
                    break;
                }
            }
        }
        return list;
    }

}
