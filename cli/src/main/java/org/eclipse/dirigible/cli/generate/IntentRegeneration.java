/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.generate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.dirigible.commons.config.StaticObjects;
import org.eclipse.dirigible.components.api.platform.WorkspaceFacade;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.components.initializers.classpath.ClasspathExpander;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationService;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.opentelemetry.api.OpenTelemetry;

/**
 * Regenerates a project from its {@code *.intent} the way the IDE's Generate does, without a
 * running platform (#7642): the same {@link IntentGenerationService} pass, over a private local
 * repository, with the template modules laid into its registry from the classpath.
 *
 * <p>
 * Only the generation beans are created - no web server, no data source, no synchronizer. They
 * still live in a Spring context, because the template descriptors are JavaScript modules whose
 * runner resolves its optional collaborators through {@link BeanProvider}.
 *
 * <p>
 * The project is copied in, never generated in place: the caller decides what to do with the
 * regenerated tree (write it back, write it elsewhere, or compare it with the committed one).
 */
final class IntentRegeneration implements AutoCloseable {

    /** The workspace the project is generated in - the IDE's default one. */
    static final String WORKSPACE = "workspace";

    private static final String INTENT_EXTENSION = ".intent";

    /**
     * The packages holding the generation beans: the intent generators, the model pipeline, the
     * engines.
     */
    private static final String[] GENERATION_PACKAGES = {"org.eclipse.dirigible.components.intent.generator",
            "org.eclipse.dirigible.components.ide.template.service.model", "org.eclipse.dirigible.components.engine.template"};

    private final Path root;
    private final IRepository repository;
    private final AnnotationConfigApplicationContext context;

    /**
     * Creates the private repository and the generation beans.
     *
     * @throws IOException when the repository folder cannot be created
     */
    IntentRegeneration() throws IOException {
        // the real path: the script engine resolves modules against canonical paths, and a temp folder
        // reached through a symbolic link (macOS /var -> /private/var) would not be the registry it reads
        root = Files.createTempDirectory("dirigible-generate-")
                    .toRealPath();
        repository = new LocalRepository(root.toString(), true);
        // the template-module runner finds the registry through it
        StaticObjects.set(StaticObjects.REPOSITORY, repository);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(IRepository.class, () -> repository);
        context.registerBean(BeanProvider.class);
        // every script run is traced; a generation run has no collector to send the spans to
        context.registerBean(OpenTelemetryProvider.class);
        context.registerBean(OpenTelemetry.class, OpenTelemetry::noop);
        context.registerBean(RegistryMutationTracker.class);
        context.registerBean(JavascriptService.class);
        context.registerBean(WorkspaceService.class);
        // the template modules' own scripts (project.json.mjs) read the workspace through it
        context.registerBean(WorkspaceFacade.class);
        // nothing is published anywhere - the registry holds the templates and nothing else
        context.registerBean(PublisherService.class,
                () -> new PublisherService(repository, List.of(), context.getBean(RegistryMutationTracker.class)));
        context.scan(GENERATION_PACKAGES);
        context.refresh();
        new ClasspathExpander(repository).expandContent();
    }

    /**
     * Regenerates a project: copies it - and the sibling projects its intent {@code uses:} - into the
     * workspace, and runs the Generate of every {@code *.intent} at its root.
     *
     * @param project the project folder
     * @return the regenerated project folder, valid until this regeneration is closed
     * @throws IOException when the project cannot be read
     * @throws GenerationFailedException when the intent is refused or a code generation fails
     */
    Path regenerate(Path project) throws IOException {
        List<Path> intents = intentsOf(project);
        if (intents.isEmpty()) {
            throw new GenerationFailedException("No *" + INTENT_EXTENSION + " file at the root of [" + project + "]");
        }
        String workspacePath = context.getBean(WorkspaceService.class)
                                      .getWorkspace(WORKSPACE)
                                      .getPath();
        String projectName = project.getFileName()
                                    .toString();
        copyIn(project, workspacePath + "/" + projectName);
        for (Path sibling : usedSiblings(project, intents)) {
            copyIn(sibling, workspacePath + "/" + sibling.getFileName());
        }

        String projectRoot = workspacePath + "/" + projectName;
        IntentGenerationService generation = context.getBean(IntentGenerationService.class);
        for (Path intent : intents) {
            String fileName = intent.getFileName()
                                    .toString();
            String baseName = fileName.substring(0, fileName.length() - INTENT_EXTENSION.length());
            IntentGenerationService.GenerationResult result;
            try {
                result = generation.generate(Files.readString(intent, StandardCharsets.UTF_8), projectRoot, projectName, WORKSPACE,
                        baseName);
            } catch (RuntimeException e) {
                throw new GenerationFailedException("Generating from [" + fileName + "] failed: " + e.getMessage(), e);
            }
            requireCodeGenerated(fileName, result.codeGenerations());
        }
        return Path.of(repository.getInternalResourcePath(projectRoot));
    }

    /**
     * A code generation that failed leaves part of {@code gen/} behind - neither a result nor a drift.
     */
    private static void requireCodeGenerated(String intent, List<Map<String, Object>> codeGenerations) {
        for (Map<String, Object> codeGeneration : codeGenerations) {
            if (!Boolean.TRUE.equals(codeGeneration.get("generated"))) {
                throw new GenerationFailedException("Generating code from [" + codeGeneration.get("path") + "] of [" + intent + "] failed: "
                        + codeGeneration.get("error"));
            }
        }
    }

    private static List<Path> intentsOf(Path project) throws IOException {
        try (Stream<Path> files = Files.list(project)) {
            return files.filter(file -> Files.isRegularFile(file) && file.getFileName()
                                                                         .toString()
                                                                         .endsWith(INTENT_EXTENSION))
                        .sorted()
                        .toList();
        }
    }

    /**
     * The projects next to this one that its intent names in {@code uses:} - what a cross-model
     * reference resolves against, as the sibling projects of an IDE workspace.
     */
    private static Set<Path> usedSiblings(Path project, List<Path> intents) throws IOException {
        Path parent = project.toAbsolutePath()
                             .getParent();
        Set<Path> siblings = new LinkedHashSet<>();
        for (Path intent : intents) {
            for (UsesIntent uses : IntentParser.parse(Files.readString(intent, StandardCharsets.UTF_8))
                                               .getUses()) {
                String name = uses.getProject() != null ? uses.getProject() : uses.getModel();
                Path sibling = parent == null || name == null ? null : parent.resolve(name);
                if (sibling != null && Files.isDirectory(sibling) && !sibling.equals(project.toAbsolutePath())) {
                    siblings.add(sibling);
                }
            }
        }
        return siblings;
    }

    private void copyIn(Path folder, String repositoryPath) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(Files::isRegularFile)
                        .filter(file -> !isVersionControl(folder.relativize(file)))
                        .toList();
        }
        for (Path file : files) {
            String relative = folder.relativize(file)
                                    .toString()
                                    .replace('\\', '/');
            repository.createResource(repositoryPath + "/" + relative, Files.readAllBytes(file));
        }
    }

    private static boolean isVersionControl(Path relative) {
        return relative.getNameCount() > 0 && ".git".equals(relative.getName(0)
                                                                    .toString());
    }

    @Override
    public void close() {
        context.close();
        StaticObjects.set(StaticObjects.REPOSITORY, null);
        deleteRecursively(root);
    }

    private static void deleteRecursively(Path folder) {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(paths::add);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot clean up [" + folder + "]", e);
        }
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot clean up [" + path + "]", e);
            }
        }
    }
}
