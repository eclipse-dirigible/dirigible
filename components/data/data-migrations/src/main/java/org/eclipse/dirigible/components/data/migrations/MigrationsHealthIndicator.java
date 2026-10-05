/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * The {@code migrations} health component, next to the {@code artefacts} census (#7533): how many
 * migrations are registered, how many are still pending (parsed, not yet applied everywhere) and
 * how many failed in some database. Like the census it is a quality signal and never turns DOWN; a
 * FAILED migration is a failed artefact, so {@code DIRIGIBLE_READINESS_REQUIRE_CLEAN_BOOT} already
 * withholds readiness for it, and the boot latch withholds it while the first pass is still
 * applying them. UNKNOWN until the first pass has depleted.
 */
@Component
class MigrationsHealthIndicator implements HealthIndicator {

    private final MigrationService migrationService;

    MigrationsHealthIndicator(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public Health health() {
        List<Migration> migrations = migrationService.getAll();
        long pending = migrations.stream()
                                 .filter(migration -> ArtefactLifecycle.NEW.equals(migration.getLifecycle())
                                         || ArtefactLifecycle.MODIFIED.equals(migration.getLifecycle()))
                                 .count();
        long failed = migrations.stream()
                                .filter(migration -> ArtefactLifecycle.FAILED.equals(migration.getLifecycle())
                                        || ArtefactLifecycle.FATAL.equals(migration.getLifecycle()))
                                .count();
        Health.Builder builder = PlatformReadiness.getInstance()
                                                  .isBootCompleted() ? Health.up() : Health.unknown();
        return builder.withDetail("total", migrations.size())
                      .withDetail("pending", pending)
                      .withDetail("failed", failed)
                      .build();
    }
}
