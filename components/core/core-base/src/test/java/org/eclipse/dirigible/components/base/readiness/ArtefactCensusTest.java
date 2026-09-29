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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.junit.jupiter.api.Test;

/**
 * The artefact census (#7533): every registered artefact counts, and FAILED and FATAL both count as
 * failed, per type.
 */
class ArtefactCensusTest {

    @Test
    void countsEveryArtefactAndTheFailedOnesPerType() {
        ArtefactCensus census = ArtefactCensus.of(List.of(artefact("print", ArtefactLifecycle.FAILED),
                artefact("print", ArtefactLifecycle.FATAL), artefact("print", ArtefactLifecycle.CREATED),
                artefact("listener", ArtefactLifecycle.FAILED), artefact("job", ArtefactLifecycle.UPDATED)));

        assertEquals(5, census.total());
        assertEquals(3, census.failed());
        assertEquals(Map.of("print", 2, "listener", 1), census.failedByType());
    }

    @Test
    void aHealthyRegistryHasNoFailedType() {
        ArtefactCensus census = ArtefactCensus.of(List.of(artefact("job", ArtefactLifecycle.CREATED)));

        assertEquals(1, census.total());
        assertEquals(0, census.failed());
        assertEquals(Map.of(), census.failedByType());
    }

    private static Artefact artefact(String type, ArtefactLifecycle lifecycle) {
        Artefact artefact = new Artefact("/project/file." + type, "file", type, null, Set.of()) {};
        artefact.setLifecycle(lifecycle);
        return artefact;
    }
}
