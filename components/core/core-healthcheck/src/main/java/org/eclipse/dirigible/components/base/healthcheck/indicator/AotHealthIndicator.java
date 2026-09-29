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

import java.util.Optional;

import org.eclipse.dirigible.components.base.readiness.CompiledModulesCensus;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * The {@code aot} health component (#7533): what the last AOT compiled-module discovery found - the
 * modules carrying a {@code .compiled} marker, the classes their markers list, and how many of
 * those registered. {@code registeredClasses != expectedClasses} is a module jar that loaded only
 * partly; like a failed artefact it is reported rather than turned DOWN, and
 * {@code DIRIGIBLE_READINESS_REQUIRE_CLEAN_BOOT} is the opt-in that withholds readiness on it.
 * UNKNOWN until the discovery has run.
 */
@Component
class AotHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        Optional<CompiledModulesCensus> taken = PlatformReadiness.getInstance()
                                                                 .getCompiledModules();
        if (taken.isEmpty()) {
            return Health.unknown()
                         .build();
        }
        CompiledModulesCensus census = taken.get();
        return Health.up()
                     .withDetail("modules", census.modules())
                     .withDetail("expectedClasses", census.expectedClasses())
                     .withDetail("registeredClasses", census.registeredClasses())
                     .build();
    }
}
