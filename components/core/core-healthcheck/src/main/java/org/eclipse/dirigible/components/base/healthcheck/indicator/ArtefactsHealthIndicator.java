/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.healthcheck.indicator;

import org.eclipse.dirigible.components.base.readiness.ArtefactCensus;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * The {@code artefacts} health component (#7533): the lifecycle census of every registered artefact
 * as the last synchronization pass left it - how many there are, how many are failed, and of which
 * type, plus how many database columns are kept although their table's definition no longer
 * declares them ({@code orphanColumns}, #7635). A failed artefact is a quality signal, not an
 * outage, so it never turns the component DOWN; a gate reads {@code failed}, and
 * {@code DIRIGIBLE_READINESS_REQUIRE_CLEAN_BOOT} is the opt-in that makes it withhold readiness.
 * UNKNOWN until the first pass has depleted.
 */
@Component
class ArtefactsHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        PlatformReadiness readiness = PlatformReadiness.getInstance();
        ArtefactCensus census = readiness.getArtefacts();
        Health.Builder builder = readiness.isBootCompleted() ? Health.up() : Health.unknown();
        return builder.withDetail("state", readiness.getState()
                                                    .name())
                      .withDetail("total", census.total())
                      .withDetail("failed", census.failed())
                      .withDetail("failedByType", census.failedByType())
                      .withDetail("orphanColumns", readiness.getOrphanColumns())
                      .build();
    }
}
