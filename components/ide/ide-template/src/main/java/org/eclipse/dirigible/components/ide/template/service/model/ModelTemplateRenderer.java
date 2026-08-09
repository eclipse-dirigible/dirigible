/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.engine.template.GenerationException;
import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.eclipse.dirigible.components.engine.template.TemplateEnginesManager;
import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadataSource;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads template sources and renders them through the platform's template engines.
 *
 * <p>
 * Two details are contractual rather than incidental. First, a generated file's <b>path</b> is
 * always rendered with Mustache, whatever engine renders its body - the {@code rename} expressions
 * in every template descriptor are written in Mustache syntax. Second, the Mustache engine is
 * always invoked with explicit {@code &#123;&#123;} / {@code &#125;&#125;} markers while Velocity
 * and JavaScript are invoked with none, because that is what the descriptors were authored against;
 * a source may override the markers for its own body.
 */
@Component
class ModelTemplateRenderer {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ModelTemplateRenderer.class);

    /** The engine used when a source names none, and always used for output paths. */
    private static final String ENGINE_MUSTACHE = "mustache";

    /** The engine rendering Velocity bodies. */
    private static final String ENGINE_VELOCITY = "velocity";

    /** The engine rendering JavaScript bodies. */
    private static final String ENGINE_JAVASCRIPT = "javascript";

    /** The default Mustache start marker. */
    private static final String MUSTACHE_START = "{{";

    /** The default Mustache end marker. */
    private static final String MUSTACHE_END = "}}";

    /** The repository holding the published templates. */
    private final IRepository repository;

    /** The template engines. */
    private final TemplateEnginesManager templateEnginesManager;

    /**
     * Instantiates a new model template renderer.
     *
     * @param repository the repository
     * @param templateEnginesManager the template engines manager
     */
    ModelTemplateRenderer(IRepository repository, TemplateEnginesManager templateEnginesManager) {
        this.repository = repository;
        this.templateEnginesManager = templateEnginesManager;
    }

    /**
     * Reads a template source. Templates are served from the registry, where the template modules
     * publish their content; a template that is on the classpath but not yet published is read from
     * there, matching what file generation already does.
     *
     * @param location the registry-relative location
     * @return the template content
     * @throws IOException when the template exists nowhere
     */
    String readTemplate(String location) throws IOException {
        String registryPath = new RepositoryPath().append(IRepositoryStructure.PATH_REGISTRY_PUBLIC)
                                                  .append(location)
                                                  .build();
        IResource resource = repository.getResource(registryPath);
        if (resource.exists()) {
            return new String(resource.getContent(), StandardCharsets.UTF_8);
        }
        try (InputStream in = ModelTemplateRenderer.class.getResourceAsStream("/META-INF/dirigible" + location)) {
            if (in == null) {
                throw new IOException("Template file at location '" + location + "' does not exist.");
            }
            logger.debug("Rendering the built-in template [{}], which is not published in the registry", location);
            return new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8);
        }
    }

    /**
     * Renders a template body with the engine its source declares.
     *
     * @param source the template source
     * @param content the template content
     * @param parameters the template context
     * @return the rendered content
     * @throws IOException when rendering fails
     */
    String render(GenerationTemplateMetadataSource source, String content, Map<String, Object> parameters) throws IOException {
        String engine = resolveEngineName(source.getEngine());
        String startMarker = ENGINE_MUSTACHE.equals(engine) ? MUSTACHE_START : null;
        String endMarker = ENGINE_MUSTACHE.equals(engine) ? MUSTACHE_END : null;
        if (source.getStart() != null) {
            startMarker = source.getStart();
        }
        if (source.getEnd() != null) {
            endMarker = source.getEnd();
        }
        return generate(engine, source.getLocation(), content, parameters, startMarker, endMarker);
    }

    /**
     * Renders a generated file's path.
     *
     * @param location the template source location, for diagnostics
     * @param rename the path expression
     * @param parameters the template context
     * @return the rendered path
     * @throws IOException when rendering fails
     */
    String renderPath(String location, String rename, Map<String, Object> parameters) throws IOException {
        return generate(ENGINE_MUSTACHE, location, rename, parameters, MUSTACHE_START, MUSTACHE_END);
    }

    /**
     * Resolves the engine a source asks for, falling back to Mustache both when none is named and when
     * the named one does not exist.
     *
     * @param declaredEngine the declared engine name, may be null
     * @return the engine name to use
     */
    private static String resolveEngineName(String declaredEngine) {
        if (declaredEngine == null) {
            return ENGINE_MUSTACHE;
        }
        if (ENGINE_VELOCITY.equals(declaredEngine) || ENGINE_JAVASCRIPT.equals(declaredEngine) || ENGINE_MUSTACHE.equals(declaredEngine)) {
            return declaredEngine;
        }
        logger.error("Template engine [{}] does not exist, so the default Mustache engine will be used", declaredEngine);
        return ENGINE_MUSTACHE;
    }

    /**
     * Runs one engine.
     *
     * @param engineName the engine
     * @param location the template location
     * @param content the template content
     * @param parameters the template context
     * @param startMarker the start marker, may be null
     * @param endMarker the end marker, may be null
     * @return the rendered content
     * @throws IOException when rendering fails
     */
    private String generate(String engineName, String location, String content, Map<String, Object> parameters, String startMarker,
            String endMarker) throws IOException {
        for (TemplateEngine engine : templateEnginesManager.getTemplateEngines()) {
            if (engine.getName()
                      .equals(engineName)) {
                byte[] rendered =
                        engine.generate(context(parameters), location, content.getBytes(StandardCharsets.UTF_8), startMarker, endMarker);
                return new String(rendered, StandardCharsets.UTF_8);
            }
        }
        throw new GenerationException("Generation Engine not available: " + engineName);
    }

    /**
     * Copies the parameter graph for one engine invocation.
     *
     * <p>
     * An engine may write into the map it is handed - the Mustache one decorates every collection it
     * finds with an indexed twin under a {@code _}-suffixed key, recursively. Handing it the pipeline's
     * own graph would leave those artefacts in the parameters, and from there in the generation
     * descriptor, growing on every regeneration. A copy per invocation is what the engines have always
     * received, since the JavaScript pipeline reached them through a JSON round trip.
     *
     * @param parameters the template context
     * @return a deep copy of it
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> context(Map<String, Object> parameters) {
        return (Map<String, Object>) ModelJson.deepCopy(parameters);
    }

}
