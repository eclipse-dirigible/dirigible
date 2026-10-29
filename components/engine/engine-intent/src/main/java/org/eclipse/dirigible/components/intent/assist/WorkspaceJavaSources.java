/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.workspace.domain.File;
import org.eclipse.dirigible.components.workspace.domain.Project;
import org.eclipse.dirigible.components.intent.LoggedValue;
import org.eclipse.dirigible.engine.java.runtime.JavaSourceParser;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads a workspace project's Java sources and its intent document - the context the Java assistant
 * reasons and compiles against.
 */
@Component
class WorkspaceJavaSources {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceJavaSources.class);

    private static final String JAVA_EXTENSION = ".java";
    private static final String INTENT_EXTENSION = ".intent";

    /**
     * Every Java source in the project except the one at {@code excludedPath}, which the assistant is
     * replacing and which is therefore contributed by the proposal instead.
     *
     * @param project the workspace project
     * @param excludedPath the target file's project-relative path
     * @return the sibling sources, in walk order
     */
    List<ProjectSource> siblings(Project project, String excludedPath) {
        List<ProjectSource> sources = new ArrayList<>();
        collect(project, project.getPath(), "", excludedPath, sources);
        return sources;
    }

    /**
     * Every Java source the proposal is compiled with: the project's own, except the one at
     * {@code excludedPath}, and those of every other project of the workspace the file being worked on
     * names.
     *
     * <p>
     * The runtime compiles all published projects in one batch, so a class may import another project's
     * repositories - a module's custom service reading the currencies module, say - and is only
     * compilable with that project's sources in the batch. A project is taken in when the file names
     * it, by its name (a TypeScript import path) or by the Java package its generated code lives in
     * ({@code codbex-currencies} is {@code codbex_currencies}), so a workspace of many modules does not
     * put every one of them into every compile. Their paths read relative to this project,
     * {@code ../codbex-currencies/...}.
     *
     * @param project the workspace project
     * @param excludedPath the target file's project-relative path
     * @param referencingText the file being worked on, whose references decide the other projects
     * @return the sibling sources, this project's first
     */
    List<ProjectSource> siblings(Project project, String excludedPath, String referencingText) {
        List<ProjectSource> sources = siblings(project, excludedPath);
        ICollection workspace = project.getParent();
        if (workspace == null || referencingText == null || referencingText.isEmpty()) {
            return sources;
        }
        String own = project.getName();
        Set<String> types = new HashSet<>();
        sources.forEach(source -> types.add(source.fqn()));
        for (ICollection other : workspace.getCollections()) {
            String name = other.getName();
            if (name.equals(own) || !names(referencingText, name)) {
                continue;
            }
            List<ProjectSource> theirs = new ArrayList<>();
            collect(other, other.getPath(), "../" + name + "/", null, theirs);
            // A type two projects both declare is one compilation unit too many; this project's wins.
            theirs.stream()
                  .filter(source -> types.add(source.fqn()))
                  .forEach(sources::add);
        }
        return sources;
    }

    private static boolean names(String text, String project) {
        return text.contains(project) || text.contains(project.replace('-', '_'));
    }

    /**
     * The project's intent document. An intent project has exactly one at its root; a classic project
     * has none.
     *
     * @param project the workspace project
     * @return the intent YAML, or {@code null} when the project has none
     */
    String intentYaml(Project project) {
        for (IResource resource : project.getResources()) {
            if (resource.getName()
                        .endsWith(INTENT_EXTENSION)) {
                return new String(resource.getContent(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Read one file's content.
     *
     * @param project the workspace project
     * @param path the project-relative path
     * @return the content, or {@code null} when the file does not exist
     */
    String read(Project project, String path) {
        File file = project.getFile(path);
        return file.exists() ? new String(file.getContent(), StandardCharsets.UTF_8) : null;
    }

    private static void collect(ICollection collection, String projectPath, String pathPrefix, String excludedPath,
            List<ProjectSource> sources) {
        for (IResource resource : collection.getResources()) {
            if (!resource.getName()
                         .endsWith(JAVA_EXTENSION)) {
                continue;
            }
            String path = pathPrefix + relativize(resource.getPath(), projectPath);
            if (path.equals(excludedPath)) {
                continue;
            }
            String source = new String(resource.getContent(), StandardCharsets.UTF_8);
            try {
                sources.add(new ProjectSource(path, JavaSourceParser.parse(source)
                                                                    .fqn(),
                        source));
            } catch (RuntimeException ex) {
                // A .java file with no parseable type declaration cannot be a compilation unit; leaving it
                // out keeps the batch compilable instead of failing the whole assist on somebody's scratch file.
                LOGGER.debug("Skipping unparseable Java source [{}]", LoggedValue.of(path), ex);
            }
        }
        for (ICollection child : collection.getCollections()) {
            collect(child, projectPath, pathPrefix, excludedPath, sources);
        }
    }

    private static String relativize(String resourcePath, String projectPath) {
        return resourcePath.startsWith(projectPath + "/") ? resourcePath.substring(projectPath.length() + 1) : resourcePath;
    }
}
