/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.healthcheck.readiness;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges the platform readiness (#6448) onto Spring's {@code ApplicationAvailability}, so
 * {@code /actuator/health/readiness} - and therefore any orchestrator's readiness probe - refuses
 * traffic until the first synchronization pass has depleted its artefact queue. Off by default: it
 * changes what an orchestrator does with the instance, so it is an operator's decision
 * ({@code DIRIGIBLE_READINESS_AVAILABILITY_BRIDGE_ENABLED}).
 *
 * <p>
 * Spring Boot publishes {@code ACCEPTING_TRAFFIC} itself on {@link ApplicationReadyEvent}, so the
 * refusal has to be published after that event to win; the platform then flips to
 * {@code ACCEPTING_TRAFFIC} when the boot latch closes. Later passes (a publish) are deliberately
 * NOT bridged - a publish must never take a running application out of the load balancer.
 *
 * <p>
 * {@code DIRIGIBLE_READINESS_REQUIRE_CLEAN_BOOT} makes the acceptance wait for a
 * {@link PlatformReadiness#isCleanBoot() clean boot} as well (#7533): no failed artefact, and every
 * class the AOT markers list registered. Off by default, so a deployment that wants to serve with a
 * failed seed keeps doing so; a FAILED artefact that heals on a later retry pass releases the
 * traffic then. The acceptance stays one-way either way.
 */
@Component
class ReadinessAvailabilityBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadinessAvailabilityBridge.class);

    private final ApplicationEventPublisher eventPublisher;

    /** The acceptance is published once - later passes must not re-announce it on every transition. */
    private final AtomicBoolean trafficAccepted = new AtomicBoolean(false);

    ReadinessAvailabilityBridge(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    void refuseTrafficUntilArtefactsDeplete() {
        if (!DirigibleConfig.READINESS_AVAILABILITY_BRIDGE_ENABLED.getBooleanValue()) {
            return;
        }
        boolean requireCleanBoot = DirigibleConfig.READINESS_REQUIRE_CLEAN_BOOT.getBooleanValue();
        PlatformReadiness readiness = PlatformReadiness.getInstance();
        if (isReady(readiness, requireCleanBoot)) {
            // The first pass already depleted while the context was starting - nothing to hold back.
            return;
        }
        readiness.addStateListener(state -> {
            if (isReady(PlatformReadiness.getInstance(), requireCleanBoot)) {
                acceptTraffic();
            }
        });
        if (requireCleanBoot) {
            LOGGER.info("Refusing traffic until the boot is complete and clean - no failed artefact, every AOT-listed class registered");
        } else {
            LOGGER.info("Refusing traffic until the first synchronization pass depletes its artefacts");
        }
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
    }

    private static boolean isReady(PlatformReadiness readiness, boolean requireCleanBoot) {
        return requireCleanBoot ? readiness.isCleanBoot() : readiness.isBootCompleted();
    }

    private void acceptTraffic() {
        if (!trafficAccepted.compareAndSet(false, true)) {
            return;
        }
        LOGGER.info("The boot completed - accepting traffic");
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
    }
}
