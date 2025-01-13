/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service;

import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.template.GenerationException;
import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.eclipse.dirigible.components.engine.template.TemplateEnginesManager;
import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadata;
import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadataSource;
import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateParameters;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.text.MessageFormat.format;

/**
 * Processing the Generation Service incoming requests.
 */
@Component
public class GenerationService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(GenerationService.class);

    /** The Constant ACTION_COPY. */
    public static final String ACTION_COPY = "copy";

    /** The Constant ACTION_GENERATE. */
    public static final String ACTION_GENERATE = "generate";

    /** The Constant GENERATION_ENGINE_DEFAULT. */
    public static final String GENERATION_ENGINE_DEFAULT = "mustache";

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The publish service. */
    @Autowired
    private PublisherService publisherService;

    /** The template engines manager. */
    @Autowired
    private TemplateEnginesManager templateEnginesManager;

    /**
     * Gets the workspace service.
     *
     * @return the workspace service
     */
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    /**
     * Gets the publisher service.
     *
     * @return the publisher service
     */
    public PublisherService getPublisherService() {
        return publisherService;
    }

    /**
     * Gets the template engines manager.
     *
     * @return the template engines manager
     */
    public TemplateEnginesManager getTemplateEnginesManager() {
        return templateEnginesManager;
    }

    /**
     * Generate file.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @param parameters the parameters
     * @return the list
     * @throws IOException Signals that an I/O exception has occurred
     */
    public List<File> generateFile(String workspace, String project, String path, GenerationTemplateParameters parameters)
            throws IOException {
        Workspace workspaceObject = getWorkspaceService().getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        List<File> generatedFiles = new ArrayList<>();
        if (parameters.getParameters()
                      .isEmpty()) {
            parameters.getParameters();
        }
        addStandardParameters(workspace, project, path, parameters.getParameters());

        getPublisherService().publish(workspace, project, "");
        GenerationTemplateMetadata metadata = getTemplateMetadata(parameters);

        for (GenerationTemplateMetadataSource source : metadata.getSources()) {
            String sourcePath = new RepositoryPath().append(IRepositoryStructure.PATH_REGISTRY_PUBLIC)
                                                    .append(source.getLocation())
                                                    .build();
            IResource sourceResource = projectObject.getRepository()
                                                    .getResource(sourcePath);
            if (sourceResource.exists()) {
                byte[] input = sourceResource.getContent();
                if (logger.isTraceEnabled()) {
                    logger.trace("Generating using template from the Registry: " + sourcePath);
                }
                generateWithTemplateIterable(parameters, projectObject, generatedFiles, source, input);
            } else {
                InputStream in = GenerationService.class.getResourceAsStream("/META-INF/dirigible" + source.getLocation());
                try {
                    if (in != null) {
                        byte[] input = IOUtils.toByteArray(in);
                        if (logger.isTraceEnabled()) {
                            logger.trace("Generating using built-in template: " + source.getLocation());
                        }
                        generateWithTemplateIterable(parameters, projectObject, generatedFiles, source, input);
                    } else {
                        throw new IOException(
                                format("Invalid source location of [{0}] in template definition file: [{1}] or the resource does not exist",
                                        source.getLocation(), parameters.getTemplate()));
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
        }
        return generatedFiles;
    }

    /**
     * Gets the template metadata.
     *
     * @param parameters the parameters
     * @return the template metadata
     */
    private static GenerationTemplateMetadata getTemplateMetadata(GenerationTemplateParameters parameters) {
        String templateModule = parameters.getTemplate();
        try (DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner()) {
            Module module = runner.run(Path.of(templateModule));
            Object res = runner.runMethod(module, "getTemplate", parameters.getParameters())
                               .as(Object.class);
            String serializedMetadata = GsonHelper.toJson(res);
            GenerationTemplateMetadata metadata = GsonHelper.fromJson(serializedMetadata, GenerationTemplateMetadata.class);
            return Objects.requireNonNull(metadata);
        } catch (Exception e) {
            throw new IllegalArgumentException(format("Invalid template definition file: [{0}]", parameters.getTemplate()));
        }
    }

    /**
     * Generate with template iterable.
     *
     * @param parameters the parameters
     * @param projectObject the project object
     * @param generatedFiles the generated files
     * @param source the source
     * @param input the input
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void generateWithTemplateIterable(GenerationTemplateParameters parameters, Project projectObject, List<File> generatedFiles,
            GenerationTemplateMetadataSource source, byte[] input) throws IOException {
        if (source.getCollection() != null) {
            List<Map<String, Object>> elements = (List<Map<String, Object>>) parameters.getParameters()
                                                                                       .get(source.getCollection());
            if (elements == null) {
                throw new IOException(
                        format("Invalid template definition file: [{0}]. Multiplicity element is set, but no actual parameter provided.",
                                parameters.getTemplate()));
            }
            // addStandardParameters(workspace, project, path, elements);
            for (Map<String, Object> elementParameters : elements) {
                generateWithTemplate(elementParameters, projectObject, generatedFiles, source, input);
            }
        } else {
            generateWithTemplate(parameters.getParameters(), projectObject, generatedFiles, source, input);
        }
    }

    /**
     * Generate with template.
     *
     * @param parameters the parameters
     * @param projectObject the project object
     * @param generatedFiles the generated files
     * @param source the source
     * @param input the input
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void generateWithTemplate(Map<String, Object> parameters, Project projectObject, List<File> generatedFiles,
            GenerationTemplateMetadataSource source, byte[] input) throws IOException {
        byte[] output = null;
        String action = source.getAction();
        parameters.put(GenerationParameters.PARAMETER_ENGINE, source.getEngine());
        parameters.put(GenerationParameters.PARAMETER_HANDLER, source.getHandler());
        if (action != null) {
            if (ACTION_GENERATE.equals(action)) {
                String sm = null;// MUSTACHE_DEFAULT_START_SYMBOL;
                String em = null;// MUSTACHE_DEFAULT_END_SYMBOL;
                if (source.getStart() != null && source.getEnd() != null) {
                    sm = source.getStart();
                    em = source.getEnd();
                }
                String engine = GENERATION_ENGINE_DEFAULT;
                if (source.getEngine() != null) {
                    engine = source.getEngine();
                }
                output = generateContent(parameters, source.getLocation(), input, sm, em, engine);
            } else if (ACTION_COPY.equals(action)) {
                output = input;
            } else {
                throw new IOException(format("Invalid action in template definition: [{0}]", action));
            }
        } else {
            output = input;
        }

        String generatedFileName;
        String rename = source.getRename();
        if (rename != null) {
            generatedFileName = generateName(parameters, source.getLocation() + "-name", rename);
        } else {
            generatedFileName = new RepositoryPath().append(source.getLocation())
                                                    .getLastSegment();
        }
        String generatedFilePath = new RepositoryPath().append(parameters.get("packagePath")
                                                                         .toString())
                                                       .append(generatedFileName)
                                                       .build();
        String contentType = ContentTypeHelper.getContentType(ContentTypeHelper.getExtension(generatedFileName));
        boolean isBinary = ContentTypeHelper.isBinary(contentType);
        File fileObject = projectObject.createFile(generatedFilePath, output, isBinary, contentType);
        generatedFiles.add(fileObject);
    }

    /**
     * Generate wrapper.
     *
     * @param parameters the parameters
     * @return the string
     */
    private String generateWrapper(GenerationTemplateParameters parameters) {
        String wrapper = new StringBuilder().append("var template = require('")
                                            .append(parameters.getTemplate())
                                            .append("');JSON.stringify(template.getTemplate(")
                                            .append(new Gson().toJson(parameters.getParameters()) + "));")
                                            .toString();
        return wrapper;
    }

    /**
     * Adds the standard parameters.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @param parameters the parameters
     */
    private void addStandardParameters(String workspace, String project, String path, Map<String, Object> parameters) {
        RepositoryPath filePath = new RepositoryPath().append(path);
        RepositoryPath packagePath = new RepositoryPath().append(path)
                                                         .getParentPath();
        String fileName = filePath.getLastSegment();
        String fileNameExt = FilenameUtils.getExtension(fileName);
        String fileNameBase = FilenameUtils.getBaseName(fileName);

        parameters.put(GenerationParameters.PARAMETER_WORKSPACE_NAME, workspace);
        parameters.put(GenerationParameters.PARAMETER_PROJECT_NAME, project);
        parameters.put(GenerationParameters.PARAMETER_FILE_NAME, fileName);
        parameters.put(GenerationParameters.PARAMETER_FILE_NAME_EXT, fileNameExt);
        parameters.put(GenerationParameters.PARAMETER_FILE_NAME_BASE, fileNameBase);
        parameters.put(GenerationParameters.PARAMETER_FILE_PATH, filePath.build());
        parameters.put(GenerationParameters.PARAMETER_PACKAGE_PATH, packagePath.build());
    }

    /**
     * Generate content.
     *
     * @param parameters the parameters
     * @param location the location
     * @param input the input
     * @param sm the sm
     * @param em the em
     * @param engine the engine type
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public byte[] generateContent(Map<String, Object> parameters, String location, byte[] input, String sm, String em, String engine)
            throws IOException {
        List<TemplateEngine> generationEngines = templateEnginesManager.getTemplateEngines();
        for (TemplateEngine next : generationEngines) {
            if (next.getName()
                    .equals(engine)) {
                return next.generate(parameters, location, input, sm, em);
            }
        }
        throw new GenerationException("Generation Engine not available: " + engine);
    }

    /**
     * Generate name.
     *
     * @param parameters the parameters
     * @param location the location
     * @param input the input
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private String generateName(Map<String, Object> parameters, String location, String input) throws IOException {
        List<TemplateEngine> generationEngines = templateEnginesManager.getTemplateEngines();
        for (TemplateEngine next : generationEngines) {
            if (next.getName()
                    .equals(TemplateEngine.TEMPLATE_ENGINE_DEFAULT)) {
                return new String(next.generate(parameters, location, input.getBytes()));
            }
        }
        throw new GenerationException("Generation Engine not available: " + TemplateEngine.TEMPLATE_ENGINE_DEFAULT);
    }

}
