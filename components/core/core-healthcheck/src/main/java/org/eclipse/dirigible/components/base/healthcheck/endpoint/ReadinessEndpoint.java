/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.healthcheck.endpoint;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The platform readiness (#6448): the state keyed to artefact depletion, consumable by probes,
 * headless pipelines and the IDE status-bar indicator. Public like the healthcheck.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "readiness")
public class ReadinessEndpoint {

    /** The readiness payload. */
    public record Readiness(String status, boolean acceptingTraffic, int pendingArtefacts, int failedArtefacts, String since) {
    }

    /**
     * The current readiness state.
     *
     * @return the state, the one-way boot latch, the last pass's terminally failed artefact count, and
     *         when the state was entered
     */
    @GetMapping
    public ResponseEntity<Readiness> getReadiness() {
        PlatformReadiness readiness = PlatformReadiness.getInstance();
        return ResponseEntity.ok(new Readiness(readiness.getState()
                                                        .name(),
                readiness.isBootCompleted(), readiness.getPendingArtefacts(), readiness.getFailedArtefacts(), readiness.getSince()
                                                                                                                       .toString()));
    }

}
