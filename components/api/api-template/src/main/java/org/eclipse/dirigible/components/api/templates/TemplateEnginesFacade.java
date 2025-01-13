/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.templates;

import java.io.IOException;
import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.eclipse.dirigible.components.engine.template.TemplateEnginesManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class TemplateEnginesFacade.
 */
@Component
public class TemplateEnginesFacade implements InitializingBean {

    /** The engine mustache. */
    private final TemplateEngine ENGINE_MUSTACHE;

    /** The engine velocity. */
    private final TemplateEngine ENGINE_VELOCITY;

    /** The engine javascript. */
    private final TemplateEngine ENGINE_JAVASCRIPT;

    /** The template engine mustache. */
    private final TemplateEngineFacade TEMPLATE_ENGINE_MUSTACHE;

    /** The template engine velocity. */
    private final TemplateEngineFacade TEMPLATE_ENGINE_VELOCITY;

    /** The template engine javascript. */
    private final TemplateEngineFacade TEMPLATE_ENGINE_JAVASCRIPT;

    /** The generation engines manager. */
    private final TemplateEnginesManager generationEnginesManager;

    /** The instance. */
    private static TemplateEnginesFacade INSTANCE;

    /**
     * Instantiates a new template engines facade.
     *
     * @param generationEnginesManager the generation engines manager
     */
    @Autowired
    private TemplateEnginesFacade(TemplateEnginesManager generationEnginesManager) {
        this.generationEnginesManager = generationEnginesManager;
        this.ENGINE_MUSTACHE = generationEnginesManager.getTemplateEngine("mustache");
        this.ENGINE_VELOCITY = generationEnginesManager.getTemplateEngine("velocity");
        this.ENGINE_JAVASCRIPT = generationEnginesManager.getTemplateEngine("javascript");
        this.TEMPLATE_ENGINE_MUSTACHE = new TemplateEngineFacade(ENGINE_MUSTACHE);
        this.TEMPLATE_ENGINE_VELOCITY = new TemplateEngineFacade(ENGINE_VELOCITY);
        this.TEMPLATE_ENGINE_JAVASCRIPT = new TemplateEngineFacade(ENGINE_JAVASCRIPT);
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the database facade
     */
    public static TemplateEnginesFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the generation engines manager.
     *
     * @return the generation engines manager
     */
    public TemplateEnginesManager getGenerationEnginesManager() {
        return generationEnginesManager;
    }

    /**
     * Gets the default engine.
     *
     * @return the default engine
     */
    public static TemplateEngineFacade getDefaultEngine() {
        return TemplateEnginesFacade.get().TEMPLATE_ENGINE_VELOCITY;
    }

    /**
     * Gets the mustache engine.
     *
     * @return the mustache engine
     */
    public static TemplateEngineFacade getMustacheEngine() {
        return TemplateEnginesFacade.get().TEMPLATE_ENGINE_MUSTACHE;
    }

    /**
     * Gets the velocity engine.
     *
     * @return the velocity engine
     */
    public static TemplateEngineFacade getVelocityEngine() {
        return TemplateEnginesFacade.get().TEMPLATE_ENGINE_VELOCITY;
    }

    /**
     * Gets the javascript engine.
     *
     * @return the javascript engine
     */
    public static TemplateEngineFacade getJavascriptEngine() {
        return TemplateEnginesFacade.get().TEMPLATE_ENGINE_JAVASCRIPT;
    }

    /**
     * The Class TemplateEngine.
     */
    public static class TemplateEngineFacade {

        /** The engine. */
        private TemplateEngine engine;

        /**
         * Instantiates a new template engine.
         *
         * @param engine the engine
         */
        public TemplateEngineFacade(TemplateEngine engine) {
            this.engine = engine;
        }

        /**
         * Generate.
         *
         * @param template the template
         * @param parametersJson the parameters json
         * @return the string
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @SuppressWarnings("unchecked")
        public String generate(String location, String template, String parametersJson) throws IOException {
            Map<String, Object> parameters = GsonHelper.fromJson(parametersJson, Map.class);
            byte[] result = engine.generate(parameters, location, template.getBytes());
            return new String(result);
        }

        /**
         * Generate.
         *
         * @param template the template
         * @param parametersJson the parameters json
         * @param sm the sm
         * @param em the em
         * @return the string
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @SuppressWarnings("unchecked")
        public String generate(String location, String template, String parametersJson, String sm, String em) throws IOException {
            Map<String, Object> parameters = GsonHelper.fromJson(parametersJson, Map.class);
            byte[] result = engine.generate(parameters, location, template.getBytes(), sm, em);
            return new String(result);
        }
    }
}
