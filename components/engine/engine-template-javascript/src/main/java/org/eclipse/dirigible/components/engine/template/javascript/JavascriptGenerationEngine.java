/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.template.javascript;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.eclipse.dirigible.repository.api.IRepository;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class JavascriptGenerationEngine.
 */
@Component
public class JavascriptGenerationEngine implements TemplateEngine {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JavascriptGenerationEngine.class);

    /** The Constant ENGINE_NAME. */
    public static final String ENGINE_NAME = "javascript";

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    /**
     * Generate.
     *
     * @param parameters the parameters
     * @param location the location
     * @param input the input
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public byte[] generate(Map<String, Object> parameters, String location, byte[] input) throws IOException {
        return generate(parameters, location, input, null, null);
    }

    /**
     * Generate.
     *
     * @param parameters the parameters
     * @param location the location
     * @param input the input
     * @param sm the sm
     * @param em the em
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public byte[] generate(Map<String, Object> parameters, String location, byte[] input, String sm, String em) throws IOException {
        try {
            // Map<Object, Object> context = new HashMap<Object, Object>();
            // BiConsumer<Object, Object> action = new ContextBiConsumer(context);
            // parameters.forEach(action);

            try (DirigibleJavascriptCodeRunner runner = createJSCodeRunner()) {
                String handlerPath = location.startsWith(IRepository.SEPARATOR) ? location.substring(1) : location;
                Module module = runner.run(handlerPath);
                Value result = runner.runMethod(module, "generate", GsonHelper.toJson(parameters));
                return (result != null && result.asString() != null) ? result.asString()
                                                                             .getBytes()
                        : new byte[] {};
            }

        } catch (Exception ex) {
            String errorMessage = "Could not evaluate template by Javascript: " + location;
            LOGGER.error(errorMessage, ex);
            throw new IOException(errorMessage, ex);
        }
    }

    /**
     * The Class ContextBiConsumer.
     */
    class ContextBiConsumer implements BiConsumer<Object, Object> {

        /** The context. */
        Map<Object, Object> context;

        /**
         * Instantiates a new context bi consumer.
         *
         * @param context the context
         */
        ContextBiConsumer(Map<Object, Object> context) {
            this.context = context;
        }

        /**
         * Accept.
         *
         * @param k the k
         * @param v the v
         */
        @Override
        public void accept(Object k, Object v) {
            this.context.put(k, v);
        }
    }

    /**
     * Creates the JS code runner.
     *
     * @return the dirigible javascript code runner
     */
    DirigibleJavascriptCodeRunner createJSCodeRunner() {
        return new DirigibleJavascriptCodeRunner();
    }

}
