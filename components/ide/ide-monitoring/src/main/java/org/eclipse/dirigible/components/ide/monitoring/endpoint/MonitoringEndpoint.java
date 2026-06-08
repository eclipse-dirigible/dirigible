/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.monitoring.endpoint;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.monitoring.dto.CountMetrics;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot;
import org.eclipse.dirigible.components.ide.monitoring.dto.ThreadDetail;
import org.eclipse.dirigible.components.ide.monitoring.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes a point-in-time snapshot of JVM runtime metrics (CPU, memory, threads, GC) to the browser
 * IDE.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "monitoring")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER", "OPERATOR"})
public class MonitoringEndpoint {

    private final MonitoringService monitoringService;

    public MonitoringEndpoint(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping(value = "/metrics", produces = "application/json")
    public ResponseEntity<MonitoringSnapshot> metrics() {
        return ResponseEntity.ok(monitoringService.snapshot());
    }

    @GetMapping(value = "/threads", produces = "application/json")
    public ResponseEntity<List<ThreadDetail>> threads() {
        return ResponseEntity.ok(monitoringService.threads());
    }

    @GetMapping(value = "/counts", produces = "application/json")
    public ResponseEntity<CountMetrics> counts() {
        return ResponseEntity.ok(monitoringService.counts());
    }
}
