/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator.RoleGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Guards the URL-layer role matrix.
 * <p>
 * The monitoring endpoints declare {@code @RolesAllowed({ADMINISTRATOR, DEVELOPER, OPERATOR})} but
 * sit under the DEVELOPER-gated {@code /services/ide/**} and {@code /services/bpm/**} prefixes, so
 * an OPERATOR used to be rejected by the filter chain before method security ever ran. These tests
 * fail if that regresses, and equally if the alignment ever spills over onto the design-time
 * surfaces.
 */
class HttpSecurityURIConfiguratorTest {

    private static final PathMatcher MATCHER = new AntPathMatcher();

    private static final Set<String> OPERATIONS_ROLES = Set.of("ADMINISTRATOR", "DEVELOPER", "OPERATOR");

    @ParameterizedTest
    @ValueSource(strings = {"/services/ide/monitoring/metrics", "/services/ide/monitoring/counts", "/services/ide/monitoring/threads",
            "/services/ide/logs", "/services/ide/logs/dirigible.log", "/services/ide/loggers", "/services/ide/messaging-monitoring/summary",
            "/services/bpm/bpm-processes/instances", "/services/bpm/bpm-processes/instance/42/jobs", "/websockets/ide/console"})
    void monitoringSurfacesAreReachableByOperators(String uri) {
        assertEquals(OPERATIONS_ROLES, requiredRoles(uri), "the monitoring surface must match the declared @RolesAllowed of " + uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/services/ide/workspaces/workspace", "/services/ide/git/workspace/project/clone", "/services/ide/problems",
            "/services/ide/publisher/request/workspace/project", "/services/bpm/models/workspace/project/process.bpmn",
            "/services/bpm/stencil-sets", "/websockets/ide/java-lsp"})
    void designTimeSurfacesStayDeveloperOnly(String uri) {
        assertEquals(Set.of("DEVELOPER"), requiredRoles(uri), "the alignment must not widen the design-time surface at " + uri);
    }

    @Test
    void operationalInfrastructureStaysOperatorOnly() {
        assertEquals(Set.of("OPERATOR"), requiredRoles("/actuator/metrics"));
        assertEquals(Set.of("OPERATOR"), requiredRoles("/spring-admin/applications"));
    }

    @Test
    void nativeAppManagementStaysOpenToAllOperationalRoles() {
        assertEquals(OPERATIONS_ROLES, requiredRoles("/services/native-apps"));
        assertEquals(OPERATIONS_ROLES, requiredRoles("/services/native-apps/my-app/start"));
    }

    /**
     * The data management surface has a URL gate of its own, so it no longer falls through to "any
     * authenticated user" with method security as the only check. The gate carries the widest set the
     * endpoints declare - narrowing it here would reject callers the controllers allow, while the
     * stricter half (export / import / anonymize / datasource CRUD, which are ADMINISTRATOR + OPERATOR)
     * stays enforced by their own {@code @RolesAllowed}.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/services/data", "/services/data/metadata", "/services/data/metadata/DefaultDB/PUBLIC",
            "/services/data/definition/DefaultDB/PUBLIC/VABLE", "/services/data/DefaultDB/query", "/services/data/DefaultDB/update",
            "/services/data/DefaultDB/procedure", "/services/data/sources", "/services/data/export/DefaultDB", "/services/data/import"})
    void dataManagementIsGatedAtTheUrlLayer(String uri) {
        assertEquals(OPERATIONS_ROLES, requiredRoles(uri), "the data surface must be gated to the operational roles at " + uri);
    }

    /**
     * The data transfer websocket has no {@code @RolesAllowed} of its own, so this gate is the whole of
     * its authorization - a regression here does not degrade the defense, it removes it.
     */
    @Test
    void theDataTransferWebsocketIsGated() {
        assertEquals(OPERATIONS_ROLES, requiredRoles("/websockets/data/transfer"),
                "the data transfer websocket has no method-level check - this gate is all there is");
    }

    /**
     * The gates are evaluated in order and the monitoring patterns are all sub-paths of the DEVELOPER
     * ones - reordering them silently reinstates the 403.
     */
    @Test
    void theMonitoringGateIsEvaluatedBeforeTheDeveloperGate() {
        List<RoleGate> gates = HttpSecurityURIConfigurator.ROLE_GATES;
        int monitoringIndex = indexOfGateMatching(gates, "/services/ide/monitoring/metrics");
        int developerIndex = indexOfGateMatching(gates, "/services/ide/workspaces/workspace");

        assertTrue(monitoringIndex < developerIndex,
                "the monitoring gate must precede the DEVELOPER gate, otherwise the broader pattern wins");
    }

    private static Set<String> requiredRoles(String uri) {
        int index = indexOfGateMatching(HttpSecurityURIConfigurator.ROLE_GATES, uri);
        assertTrue(index >= 0, "no role gate matches " + uri);
        return Set.of(HttpSecurityURIConfigurator.ROLE_GATES.get(index)
                                                            .roles());
    }

    private static int indexOfGateMatching(List<RoleGate> gates, String uri) {
        for (int i = 0; i < gates.size(); i++) {
            if (Arrays.stream(gates.get(i)
                                   .patterns())
                      .anyMatch(pattern -> MATCHER.match(pattern, uri))) {
                return i;
            }
        }
        return -1;
    }
}
