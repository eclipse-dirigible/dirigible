/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

import org.eclipse.dirigible.DirigibleApplication;
import org.eclipse.dirigible.components.api.platform.LifecycleFacade;
import org.eclipse.dirigible.components.api.platform.ProblemsFacade;
import org.eclipse.dirigible.components.api.platform.WorkspaceFacade;
import org.eclipse.dirigible.components.problems.service.ProblemService;
import org.eclipse.dirigible.components.workspace.service.WorkspaceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Boots the default application without the IDE (#8046): no IDE module is on the classpath, the
 * server answers, the runtime records problems, and the APIs that reach the IDE's workspaces refuse
 * instead of failing the start.
 */
@SpringBootTest(classes = DirigibleApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class RuntimeWithoutIdeTest {

    /** Every IDE backend module keeps its classes under this package. */
    private static final String IDE_CLASSES = "classpath*:org/eclipse/dirigible/components/ide/**/*.class";

    private static final String PROBLEM_LOCATION = "/runtime-without-ide/broken.table";

    private static final String PROBLEM_TYPE = "table";

    private static final String PROBLEM_CATEGORY = "RuntimeWithoutIdeTest";

    @LocalServerPort
    private int port;

    @Autowired
    private ProblemService problemService;

    @Test
    void noIdeModuleIsOnTheClasspath() throws IOException {
        Resource[] ideClasses = new PathMatchingResourcePatternResolver().getResources(IDE_CLASSES);

        assertEquals(0, ideClasses.length, () -> "IDE classes on the classpath: " + Arrays.toString(ideClasses));
    }

    @Test
    void serverAcceptsTraffic() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health/readiness"))
                                         .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), response.body());
        }
    }

    @Test
    void problemsAreRecorded() {
        ProblemsFacade.save(PROBLEM_LOCATION, PROBLEM_TYPE, "", "", "cause", "", PROBLEM_CATEGORY, "", "", "");
        try {
            assertEquals(1, problemService.findByLocationAndTypeAndCategory(PROBLEM_LOCATION, PROBLEM_TYPE, PROBLEM_CATEGORY)
                                          .size());
        } finally {
            ProblemsFacade.deleteProblem(PROBLEM_LOCATION, PROBLEM_TYPE, PROBLEM_CATEGORY);
        }
    }

    @Test
    void workspaceOperationsAreRefused() {
        assertThrows(WorkspaceUnavailableException.class, () -> WorkspaceFacade.getWorkspace("workspace"));
        assertFalse(LifecycleFacade.publish("admin", "workspace", "project"));
    }
}
