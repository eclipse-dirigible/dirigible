/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.synchronizer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * The synchronizers whose artefacts are materialized once per tenant.
 *
 * <p>
 * Two features need exactly this set and have to agree on it: the post-provisioning step that
 * re-triggers a synchronization for a newly provisioned tenant, and the calculation of how far such
 * a tenant's initialization has got. Deriving it twice would let the two drift apart, and the
 * failure would be silent - an initialization reported complete because the watcher was looking at
 * a different set of artefact types than the one being materialized.
 */
@Component
public class MultitenantSynchronizers {

    /** The multitenant synchronizers. */
    private final List<Synchronizer<?, ?>> synchronizers;

    /** The artefact types they own. */
    private final Set<String> artefactTypes;

    /**
     * Instantiates the set from every synchronizer on the classpath.
     *
     * @param allSynchronizers all registered synchronizers
     */
    MultitenantSynchronizers(List<Synchronizer<?, ?>> allSynchronizers) {
        this.synchronizers = allSynchronizers.stream()
                                             .filter(Synchronizer::multitenantExecution)
                                             .collect(Collectors.toList());
        this.artefactTypes = this.synchronizers.stream()
                                               .map(Synchronizer::getArtefactType)
                                               .collect(Collectors.toSet());
    }

    /**
     * The multitenant synchronizers.
     *
     * @return the synchronizers
     */
    public List<Synchronizer<?, ?>> getSynchronizers() {
        return synchronizers;
    }

    /**
     * The artefact types materialized per tenant, e.g. {@code table} or {@code job}.
     *
     * @return the artefact types
     */
    public Set<String> getArtefactTypes() {
        return artefactTypes;
    }
}
