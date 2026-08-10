/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the perspective aggregator
 * ({@code /services/js/platform-core/extension-services/perspectives.js}) placing a perspective it
 * cannot match to a declared group.
 * <p>
 * The failure this guards (issue #6646) was silent in every layer: a contributed perspective whose
 * {@code groupId} matched no group was dropped by the aggregator, so the shell rendered its group
 * empty - indistinguishable from a deployment that contributes no perspectives at all. A group may
 * now declare itself the default of its extension point and adopt both the un-matched and the
 * un-grouped perspectives; the platform point keeps its 'undefined-group' catch-all.
 */
class PerspectiveGroupAggregationIT extends IntegrationTest {

    private static final String SERVICE = "/services/js/platform-core/extension-services/perspectives.js?extensionPoints=";

    private static final String PROJECT = "/perspective-group-aggregation-it";

    /** The single-group shell point: resources-personal's 'personal' group is its default. */
    private static final String PERSONAL_POINT = "application-personal-perspectives";

    /** The platform point: no default group, but the 'undefined-group' catch-all. */
    private static final String PLATFORM_POINT = "application-perspectives";

    private static final String STALE_ID = "perspective-group-aggregation-it-stale";

    private static final String GROUPLESS_ID = "perspective-group-aggregation-it-groupless";

    private static final String PLATFORM_STALE_ID = "perspective-group-aggregation-it-platform-stale";

    /** The published extensions are synchronized synchronously, but the service can lag slightly. */
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    /** The registry paths this test published, in creation order, for the teardown. */
    private final Map<String, String> published = new LinkedHashMap<>();

    @Test
    void adopts_an_unmatched_and_an_ungrouped_perspective_into_the_default_group() {
        publishPerspective(STALE_ID, "a-navigation-group-that-does-not-exist", PERSONAL_POINT);
        publishPerspective(GROUPLESS_ID, null, PERSONAL_POINT);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(SERVICE + PERSONAL_POINT)
                                                 .then()
                                                 .statusCode(200)
                                                 // A stale group id is what a platform-side group rename leaves behind in every
                                                 // already-generated module; the perspective must still reach the user.
                                                 .body(inGroup("personal"), hasItem(STALE_ID))
                                                 // No group id at all is what the generator now emits - the shell owns the placement.
                                                 .body(inGroup("personal"), hasItem(GROUPLESS_ID))
                                                 // ... and neither is handed back standalone, which would leave the group empty.
                                                 .body(standalone(STALE_ID), empty())
                                                 .body(standalone(GROUPLESS_ID), empty()),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void falls_back_to_the_catch_all_group_where_no_default_is_declared() {
        publishPerspective(PLATFORM_STALE_ID, "a-navigation-group-that-does-not-exist", PLATFORM_POINT);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(SERVICE + PLATFORM_POINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(inGroup("undefined-group"), hasItem(PLATFORM_STALE_ID)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    /**
     * Publish a perspective module and the extension registering it on the given extension point. The
     * module declares no {@code kind}: the aggregator places perspectives by group alone, the kind
     * being the consuming shell's concern.
     */
    private void publishPerspective(String id, String groupId, String extensionPoint) {
        String module = PROJECT + "/" + id + ".js";
        createResource(module, """
                const perspectiveData = {
                    id: '%s',
                    label: '%s',
                    path: '/services/web%s/index.html',
                    %s
                    order: 9998
                };
                if (typeof exports !== 'undefined') {
                    exports.getPerspective = () => perspectiveData;
                }
                """.formatted(id, id, PROJECT, groupId == null ? "" : "groupId: '" + groupId + "',"), "application/javascript");
        createResource(PROJECT + "/" + id + ".extension", """
                {
                    "module": "%s",
                    "extensionPoint": "%s",
                    "description": "Perspective group aggregation test - %s"
                }
                """.formatted(module.substring(1), extensionPoint, id), "application/json");
    }

    private void createResource(String location, String content, String contentType) {
        String path = IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8), false, contentType, true);
        published.put(location, path);
    }

    /** A JsonPath expression selecting the ids of the perspectives inside the given group. */
    private static String inGroup(String groupId) {
        return "perspectives.find { it.id == '" + groupId + "' }.items.id";
    }

    /** A JsonPath expression selecting the perspective if it was returned outside any group. */
    private static String standalone(String id) {
        return "perspectives.findAll { it.id == '" + id + "' }";
    }

    @AfterEach
    void removePublishedArtefacts() {
        published.values()
                 .stream()
                 .filter(repository::hasResource)
                 .forEach(repository::removeResource);
        published.clear();
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
