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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.readiness.ArtefactCensus;
import org.eclipse.dirigible.components.base.readiness.CompiledModulesCensus;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The opt-in clean-boot condition of the availability bridge (#7533): with it, traffic is accepted
 * only once no artefact is failed and every AOT-listed class is registered; without it, as soon as
 * the boot latch closes.
 */
class ReadinessAvailabilityBridgeTest {

    private final List<ReadinessState> published = new ArrayList<>();

    private final ApplicationEventPublisher publisher =
            event -> published.add((ReadinessState) ((AvailabilityChangeEvent<?>) event).getState());

    private final PlatformReadiness readiness = PlatformReadiness.getInstance();

    @BeforeEach
    void enableTheBridge() {
        readiness.reset();
        DirigibleConfig.READINESS_AVAILABILITY_BRIDGE_ENABLED.setBooleanValue(true);
    }

    @AfterEach
    void restoreTheDefaults() {
        DirigibleConfig.READINESS_AVAILABILITY_BRIDGE_ENABLED.setBooleanValue(false);
        DirigibleConfig.READINESS_REQUIRE_CLEAN_BOOT.setBooleanValue(false);
        // The singleton outlives the test - leave the boot latch closed so unrelated tests are unaffected.
        readiness.reset();
        readiness.passCompleted(0);
    }

    @Test
    void byDefaultTheBootLatchAloneReleasesTheTraffic() {
        new ReadinessAvailabilityBridge(publisher).refuseTrafficUntilArtefactsDeplete();

        readiness.recordArtefacts(new ArtefactCensus(3, Map.of("print", 1)));
        readiness.passCompleted(1);

        assertEquals(List.of(ReadinessState.REFUSING_TRAFFIC, ReadinessState.ACCEPTING_TRAFFIC), published,
                "a failed seed must not withhold readiness unless the operator asked for a clean boot");
    }

    @Test
    void aRequiredCleanBootWaitsForTheFailedArtefactToHealAndTheAotCensus() {
        DirigibleConfig.READINESS_REQUIRE_CLEAN_BOOT.setBooleanValue(true);
        new ReadinessAvailabilityBridge(publisher).refuseTrafficUntilArtefactsDeplete();

        readiness.recordArtefacts(new ArtefactCensus(3, Map.of("print", 1)));
        readiness.passCompleted(1);
        readiness.recordCompiledModules(new CompiledModulesCensus(1, 2, 2));
        assertEquals(List.of(ReadinessState.REFUSING_TRAFFIC), published, "a failed artefact withholds readiness");

        readiness.passStarted();
        readiness.recordArtefacts(new ArtefactCensus(3, Map.of()));
        readiness.passCompleted(0);
        assertEquals(List.of(ReadinessState.REFUSING_TRAFFIC, ReadinessState.ACCEPTING_TRAFFIC), published,
                "the retry pass that heals it releases the traffic");

        readiness.passStarted();
        readiness.recordArtefacts(new ArtefactCensus(3, Map.of("print", 1)));
        readiness.passCompleted(1);
        assertEquals(2, published.size(), "the acceptance is one-way - a later failure never takes the instance out");
    }

    @Test
    void aRequiredCleanBootWaitsForEveryAotClass() {
        DirigibleConfig.READINESS_REQUIRE_CLEAN_BOOT.setBooleanValue(true);
        new ReadinessAvailabilityBridge(publisher).refuseTrafficUntilArtefactsDeplete();

        readiness.passCompleted(0);
        readiness.recordCompiledModules(new CompiledModulesCensus(1, 2, 1));
        assertEquals(List.of(ReadinessState.REFUSING_TRAFFIC), published, "a partly loaded AOT module withholds readiness");

        readiness.recordCompiledModules(new CompiledModulesCensus(1, 2, 2));
        assertEquals(List.of(ReadinessState.REFUSING_TRAFFIC, ReadinessState.ACCEPTING_TRAFFIC), published);
    }
}
