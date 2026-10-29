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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.publisher.PublisherHandler;
import org.eclipse.dirigible.components.base.script.ScriptEngineAvailability;
import org.eclipse.dirigible.components.extensions.modules.ScriptModuleSkipListener;
import org.eclipse.dirigible.components.problems.domain.Problem;
import org.eclipse.dirigible.components.problems.service.ProblemService;
import org.eclipse.dirigible.components.ide.workspace.deprecation.ServerSideScripts.Kind;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Makes the deprecation of server-side JavaScript and TypeScript (removed in 16.0) visible where a
 * developer works: each publish reviews the projects it touched with
 * {@link ServerSideScriptDetector}, and a project that carries any gets one warning in the log and
 * one Problems view entry naming what was found and linking the migration guide. The entry is
 * replaced on every review and removed once the project carries none, or is unpublished.
 *
 * <p>
 * Where the JavaScript engine is not deployed, the same entry counts what the instance skips - the
 * project's script extension modules and its artefacts with a script handler - so dropping the
 * engine is never silent. An application that is never published - its modules arrive in module
 * jars or an external registry folder - is reported where its script extension modules are skipped:
 * the extension module reader tells each one, and the project gets one warning and an entry
 * counting them.
 *
 * <p>
 * Only a publish reviews a project and only application modules are ever skipped, so the content
 * the platform deploys to the registry itself is never flagged. The signals are informational:
 * nothing here changes how a script runs.
 */
@Component
class ServerSideScriptDeprecation implements PublisherHandler, ScriptModuleSkipListener {

    /**
     * How to move server-side JavaScript and TypeScript to Java - the migration guide on dirigible.io.
     */
    static final String MIGRATION_GUIDE = "https://www.dirigible.io/help/develop/migrate-typescript-to-java";

    /** The Problems view category of the entries, one per project. */
    static final String PROBLEM_CATEGORY = "JavaScript deprecation";

    /** The Problems view type of the entries: each concerns a whole project. */
    static final String PROBLEM_TYPE = "project";

    private static final String PROBLEM_MODULE = "ide-workspace";

    /** The source of an entry a publish wrote, counting the project's published files. */
    private static final String PROBLEM_SOURCE = "ServerSideScriptDeprecation";

    /** The source of an entry counting the script extension modules the instance skipped. */
    private static final String SKIPPED_SOURCE = "ExtensionModuleReader";

    private static final String PROBLEM_PROGRAM = "Eclipse Dirigible";

    /** The length of the Problems view cause column. */
    private static final int PROBLEM_CAUSE_MAX = 1024;

    private static final String PUBLISH_ALL = "*";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSideScriptDeprecation.class);

    private final IRepository repository;

    private final ProblemService problemService;

    private final ScriptEngineAvailability scriptEngineAvailability;

    /** The script extension modules skipped so far, by project. Guarded by this. */
    private final Map<String, Set<String>> skippedModules = new HashMap<>();

    /**
     * Instantiates the deprecation signals.
     *
     * @param repository the repository the projects are published in
     * @param problemService the Problems view's store
     * @param scriptEngineAvailability whether the JavaScript engine is deployed
     */
    ServerSideScriptDeprecation(IRepository repository, ProblemService problemService, ScriptEngineAvailability scriptEngineAvailability) {
        this.repository = repository;
        this.problemService = problemService;
        this.scriptEngineAvailability = scriptEngineAvailability;
    }

    /**
     * Reviews the published project, or each project of a published workspace - never the registry's
     * other content.
     */
    @Override
    public void afterPublish(String workspaceLocation, String registryLocation, AfterPublishMetadata metadata) {
        String project = metadata.projectName();
        if (project.isEmpty()) {
            repository.getCollection(workspaceLocation)
                      .getCollectionsNames()
                      .forEach(this::review);
        } else {
            review(project);
        }
    }

    /**
     * Reviews the project the unpublished location belongs to, or each flagged project when the
     * location names none: an entry outlives no project it describes.
     */
    @Override
    public void afterUnpublish(String location) {
        String[] segments = new RepositoryPath(location).getSegments();
        // /registry/public/<project>/...
        if (segments.length > 2 && !PUBLISH_ALL.equals(segments[2])) {
            review(segments[2]);
        } else {
            flaggedProjects().forEach(this::review);
        }
    }

    /**
     * The projects with an entry in the Problems view.
     *
     * @return the project names, sorted
     */
    List<String> flaggedProjects() {
        return problemService.findByCategory(PROBLEM_CATEGORY)
                             .stream()
                             .map(problem -> problem.getLocation()
                                                    .substring(1))
                             .distinct()
                             .sorted()
                             .toList();
    }

    /**
     * Replaces the project's entry with what its published content carries now. Synchronized, so two
     * publishes of one project cannot both find no entry and add one each.
     *
     * @param project the project name
     */
    synchronized void review(String project) {
        String location = IRepositoryStructure.SEPARATOR + project;
        ICollection published = repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC + location);
        ServerSideScripts scripts = published.exists() ? ServerSideScriptDetector.detect(published) : ServerSideScripts.NONE;
        // The publish counts the project's files; the modules skipped before it are among them.
        skippedModules.remove(project);
        problemService.deleteByLocationAndTypeAndCategory(location, PROBLEM_TYPE, PROBLEM_CATEGORY);
        if (scripts.isEmpty()) {
            return;
        }
        String message = message(LogSanitizer.sanitize(project), scripts, scriptEngineAvailability.isAvailable());
        LOGGER.warn("{}", message);
        save(location, message, PROBLEM_SOURCE);
    }

    /**
     * Counts the skipped script extension modules per project: the first one of a project logs the
     * project's warning, and each count that grows replaces its entry - unless a publish wrote it,
     * since the publish counts the project's own files, these modules among them.
     */
    @Override
    public synchronized void scriptModulesSkipped(List<String> modules) {
        Map<String, List<String>> byProject = modules.stream()
                                                     .map(ServerSideScriptDeprecation::stripLeadingSeparator)
                                                     .collect(Collectors.groupingBy(ServerSideScriptDeprecation::projectOf,
                                                             LinkedHashMap::new, Collectors.toList()));
        byProject.remove("");
        byProject.forEach((project, projectModules) -> {
            Set<String> skipped = skippedModules.computeIfAbsent(project, name -> new HashSet<>());
            boolean first = skipped.isEmpty();
            if (!skipped.addAll(projectModules)) {
                return;
            }
            if (first) {
                LOGGER.warn(
                        "Project [{}] uses server-side JavaScript and this instance has no JavaScript engine, so its JavaScript"
                                + " extension modules are skipped - the Problems view counts them. Migration guide: {}",
                        LogSanitizer.sanitize(project), MIGRATION_GUIDE);
            }
            recordSkipped(project, skipped.size());
        });
    }

    private void recordSkipped(String project, int skipped) {
        String location = IRepositoryStructure.SEPARATOR + project;
        boolean published = problemService.findByLocationAndTypeAndCategory(location, PROBLEM_TYPE, PROBLEM_CATEGORY)
                                          .stream()
                                          .anyMatch(problem -> PROBLEM_SOURCE.equals(problem.getSource()));
        if (published) {
            return;
        }
        problemService.deleteByLocationAndTypeAndCategory(location, PROBLEM_TYPE, PROBLEM_CATEGORY);
        ServerSideScripts scripts = new ServerSideScripts(Map.of(Kind.EXTENSION_MODULE, skipped));
        save(location, message(LogSanitizer.sanitize(project), scripts, false), SKIPPED_SOURCE);
    }

    private static String stripLeadingSeparator(String module) {
        return module.startsWith(IRepositoryStructure.SEPARATOR) ? module.substring(1) : module;
    }

    /** The project of a module path - its first segment, or none for a module outside any project. */
    private static String projectOf(String module) {
        int end = module.indexOf(IRepositoryStructure.SEPARATOR);
        return end < 0 ? "" : module.substring(0, end);
    }

    private void save(String location, String message, String source) {
        problemService.save(new Problem(location, PROBLEM_TYPE, "", "", truncate(message), "", PROBLEM_CATEGORY, PROBLEM_MODULE, source,
                PROBLEM_PROGRAM));
    }

    private static String message(String project, ServerSideScripts scripts, boolean engineDeployed) {
        String found = "Project [" + project + "] uses server-side JavaScript or TypeScript (" + scripts.describe() + ")";
        String consequence = engineDeployed ? ", which is deprecated and will be removed in 16.0."
                : " and this instance has no JavaScript engine, so it does not run - JavaScript extension modules and artefacts skipped: "
                        + scripts.skippedWithoutEngine() + ".";
        return found + consequence + " Migration guide: " + MIGRATION_GUIDE;
    }

    private static String truncate(String cause) {
        return cause.length() > PROBLEM_CAUSE_MAX ? cause.substring(0, PROBLEM_CAUSE_MAX) : cause;
    }
}
