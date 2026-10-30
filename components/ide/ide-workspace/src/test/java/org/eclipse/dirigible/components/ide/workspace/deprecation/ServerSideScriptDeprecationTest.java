/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.deprecation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.dirigible.components.base.publisher.PublisherHandler.AfterPublishMetadata;
import org.eclipse.dirigible.components.base.script.ScriptEngineAvailability;
import org.eclipse.dirigible.components.problems.domain.Problem;
import org.eclipse.dirigible.components.problems.service.ProblemService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * The deprecation signals of a publish (#8049): one warning and one Problems view entry per project
 * with server-side JavaScript or TypeScript, replaced on republish, removed with the scripts, and
 * on an instance without the JavaScript engine the count of what it skips.
 */
class ServerSideScriptDeprecationTest {

    private static final String WORKSPACE = "/users/admin/workspace";

    private static final String REGISTRY = "/registry/public";

    private static final String EXPECTED_WARNING = "Project [shop] uses server-side JavaScript or TypeScript (1 TypeScript file, 1 job),"
            + " which is deprecated and will be removed in 16.0. Migration guide: https://www.dirigible.io/help/develop/migrate-typescript-to-java";

    @TempDir
    Path root;

    private IRepository repository;

    private final ProblemService problemService = mock(ProblemService.class);

    private final ScriptEngineAvailability scriptEngineAvailability = mock(ScriptEngineAvailability.class);

    private final ListAppender<ILoggingEvent> log = new ListAppender<>();

    private ServerSideScriptDeprecation deprecation;

    @BeforeEach
    void setUp() {
        repository = new LocalRepository(root.toString(), true);
        when(scriptEngineAvailability.isAvailable()).thenReturn(true);
        deprecation = new ServerSideScriptDeprecation(repository, problemService, scriptEngineAvailability);
        log.start();
        logger().addAppender(log);
    }

    @AfterEach
    void detachLog() {
        logger().detachAppender(log);
    }

    @Test
    void aPublishedProjectWithServerSideScriptsGetsOneWarningAndOneEntry() {
        publishShopWithScripts();

        deprecation.afterPublish(WORKSPACE + "/shop", REGISTRY + "/shop", new AfterPublishMetadata("workspace", "shop", "", true));

        Problem entry = savedEntry();
        assertThat(entry.getLocation()).isEqualTo("/shop");
        assertThat(entry.getType()).isEqualTo(ServerSideScriptDeprecation.PROBLEM_TYPE);
        assertThat(entry.getCategory()).isEqualTo(ServerSideScriptDeprecation.PROBLEM_CATEGORY);
        assertThat(entry.getCause()).isEqualTo(EXPECTED_WARNING);
        assertThat(log.list).singleElement()
                            .satisfies(event -> {
                                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                                assertThat(event.getFormattedMessage()).isEqualTo(EXPECTED_WARNING);
                            });
    }

    @Test
    void aRepublishReplacesTheEntryInsteadOfAddingOne() {
        publishShopWithScripts();

        deprecation.review("shop");
        deprecation.review("shop");

        InOrder order = inOrder(problemService);
        order.verify(problemService)
             .deleteByLocationAndTypeAndCategory("/shop", ServerSideScriptDeprecation.PROBLEM_TYPE,
                     ServerSideScriptDeprecation.PROBLEM_CATEGORY);
        order.verify(problemService)
             .save(any(Problem.class));
        order.verify(problemService)
             .deleteByLocationAndTypeAndCategory("/shop", ServerSideScriptDeprecation.PROBLEM_TYPE,
                     ServerSideScriptDeprecation.PROBLEM_CATEGORY);
        order.verify(problemService)
             .save(any(Problem.class));
    }

    @Test
    void aProjectThatNoLongerHasServerSideScriptsLosesItsEntry() {
        write(REGISTRY + "/shop/gen/api/OrderController.java", "package gen.api; public class OrderController {}");
        write(REGISTRY + "/shop/gen/ui/js/app.js", "angular.module('app', []);");

        deprecation.review("shop");

        verify(problemService).deleteByLocationAndTypeAndCategory("/shop", ServerSideScriptDeprecation.PROBLEM_TYPE,
                ServerSideScriptDeprecation.PROBLEM_CATEGORY);
        verify(problemService, never()).save(any(Problem.class));
        assertThat(log.list).isEmpty();
    }

    @Test
    void anUnpublishedProjectLosesItsEntry() {
        deprecation.afterUnpublish(REGISTRY + "/shop");

        verify(problemService).deleteByLocationAndTypeAndCategory("/shop", ServerSideScriptDeprecation.PROBLEM_TYPE,
                ServerSideScriptDeprecation.PROBLEM_CATEGORY);
        verify(problemService, never()).save(any(Problem.class));
    }

    @Test
    void withoutTheJavaScriptEngineTheEntryCountsWhatIsSkipped() {
        when(scriptEngineAvailability.isAvailable()).thenReturn(false);
        publishShopWithScripts();
        write(REGISTRY + "/shop/extensions/menu.extension", "{\"module\": \"shop/ui/menu.js\", \"extensionPoint\": \"shop-menu\"}");

        deprecation.review("shop");

        assertThat(savedEntry().getCause()).isEqualTo(
                "Project [shop] uses server-side JavaScript or TypeScript (1 TypeScript file, 1 extension module, 1 job) and this instance"
                        + " has no JavaScript engine, so it does not run - JavaScript extension modules and artefacts skipped: 2."
                        + " Migration guide: https://www.dirigible.io/help/develop/migrate-typescript-to-java");
        assertThat(log.list).hasSize(1);
    }

    @Test
    void aPublishOfTheWholeWorkspaceReviewsItsProjectsOnly() {
        write(WORKSPACE + "/shop/api/orders.ts", "export const x = 1;");
        write(WORKSPACE + "/billing/gen/api/InvoiceController.java", "package gen.api; public class InvoiceController {}");
        publishShopWithScripts();
        // Content the platform itself deploys to the registry, never published from a workspace.
        write(REGISTRY + "/platform-sdk/db/query.ts", "export const query = {};");

        deprecation.afterPublish(WORKSPACE, REGISTRY, new AfterPublishMetadata("workspace", "", "", true));

        ArgumentCaptor<String> reviewed = ArgumentCaptor.forClass(String.class);
        verify(problemService, times(2)).deleteByLocationAndTypeAndCategory(reviewed.capture(), anyString(), anyString());
        assertThat(reviewed.getAllValues()).containsExactlyInAnyOrder("/shop", "/billing");
        assertThat(savedEntry().getLocation()).isEqualTo("/shop");
    }

    @Test
    void theScriptModulesSkippedForAnUnpublishedProjectGetOneWarningAndACountingEntry() {
        deprecation.scriptModulesSkipped(List.of("gaia/ui/orders.js", "/gaia/ui/customers.js"));
        deprecation.scriptModulesSkipped(List.of("gaia/ui/customers.js", "gaia/ui/invoices.js", "billing/menu.js"));
        deprecation.scriptModulesSkipped(List.of("gaia/ui/orders.js"));

        ArgumentCaptor<Problem> saved = ArgumentCaptor.forClass(Problem.class);
        verify(problemService, times(3)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Problem::getLocation)
                                        .containsExactly("/gaia", "/gaia", "/billing");
        Problem gaia = saved.getAllValues()
                            .get(1);
        assertThat(gaia.getCause()).isEqualTo("Project [gaia] uses server-side JavaScript or TypeScript (3 extension modules) and this"
                + " instance has no JavaScript engine, so it does not run - JavaScript extension modules and artefacts skipped: 3."
                + " Migration guide: https://www.dirigible.io/help/develop/migrate-typescript-to-java");
        assertThat(gaia.getCategory()).isEqualTo(ServerSideScriptDeprecation.PROBLEM_CATEGORY);
        assertThat(gaia.getSource()).isEqualTo("ExtensionModuleReader");
        assertThat(log.list).extracting(ILoggingEvent::getFormattedMessage)
                            .containsExactly(skippedWarning("gaia"), skippedWarning("billing"));
    }

    @Test
    void anEntryAPublishWroteIsNotReplacedByTheSkippedCount() {
        Problem published = entry("/gaia");
        published.setSource("ServerSideScriptDeprecation");
        when(problemService.findByLocationAndTypeAndCategory("/gaia", ServerSideScriptDeprecation.PROBLEM_TYPE,
                ServerSideScriptDeprecation.PROBLEM_CATEGORY)).thenReturn(List.of(published));

        deprecation.scriptModulesSkipped(List.of("gaia/ui/orders.js"));

        verify(problemService, never()).deleteByLocationAndTypeAndCategory(anyString(), anyString(), anyString());
        verify(problemService, never()).save(any(Problem.class));
        assertThat(log.list).extracting(ILoggingEvent::getFormattedMessage)
                            .containsExactly(skippedWarning("gaia"));
    }

    @Test
    void aPublishReplacesTheEntryOfTheSkippedModules() {
        deprecation.scriptModulesSkipped(List.of("shop/ui/menu.js"));
        publishShopWithScripts();

        deprecation.review("shop");

        ArgumentCaptor<Problem> saved = ArgumentCaptor.forClass(Problem.class);
        verify(problemService, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()
                        .get(1)
                        .getCause()).isEqualTo(EXPECTED_WARNING);
        assertThat(saved.getAllValues()
                        .get(1)
                        .getSource()).isEqualTo("ServerSideScriptDeprecation");
    }

    @Test
    void theFlaggedProjectsAreTheEntriesOfTheCategory() {
        when(problemService.findByCategory(ServerSideScriptDeprecation.PROBLEM_CATEGORY)).thenReturn(
                List.of(entry("/shop"), entry("/billing")));

        assertThat(deprecation.flaggedProjects()).containsExactly("billing", "shop");
    }

    private void publishShopWithScripts() {
        write(REGISTRY + "/shop/api/orders.ts", "export function list() { return []; }");
        write(REGISTRY + "/shop/jobs/cleanup.job", "{\"expression\": \"0 0 * * * ?\", \"handler\": \"shop/jobs/cleanup.ts\"}");
        write(REGISTRY + "/shop/gen/ui/js/app.js", "angular.module('app', []);");
    }

    private Problem savedEntry() {
        ArgumentCaptor<Problem> saved = ArgumentCaptor.forClass(Problem.class);
        verify(problemService).save(saved.capture());
        return saved.getValue();
    }

    private void write(String path, String content) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String skippedWarning(String project) {
        return "Project [" + project + "] uses server-side JavaScript and this instance has no JavaScript engine, so its JavaScript"
                + " extension modules are skipped - the Problems view counts them. Migration guide:"
                + " https://www.dirigible.io/help/develop/migrate-typescript-to-java";
    }

    private static Problem entry(String location) {
        Problem problem = new Problem();
        problem.setLocation(location);
        return problem;
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(ServerSideScriptDeprecation.class);
    }
}
