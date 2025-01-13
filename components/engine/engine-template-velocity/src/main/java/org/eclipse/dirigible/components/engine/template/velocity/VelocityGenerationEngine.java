/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.template.velocity;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/**
 * The Class VelocityGenerationEngine.
 */
@Component
public class VelocityGenerationEngine implements TemplateEngine {

    /** The Constant ENGINE_NAME. */
    public static final String ENGINE_NAME = "velocity";

    /** The engine. */
    private final VelocityEngine engine;

    /**
     * Instantiates a new velocity generation engine.
     */
    public VelocityGenerationEngine() {
        engine = new VelocityEngine();
        try {
            engine.setProperty(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, true);
            engine.init();
        } catch (Throwable e) {
            // if (logger.isErrorEnabled()) {logger.error(e.getMessage(), e);}
            e.printStackTrace();
        }
    }

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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        try {
            final VelocityContext context = new VelocityContext();
            prepareContextData(parameters, context);
            engine.evaluate(context, writer, location, new InputStreamReader(in, StandardCharsets.UTF_8));
            writer.flush();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new IOException("Could not evaluate template by Velocity [" + location + "]: " + ex.getMessage(), ex);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    /**
     * Put the input parameters to the Velocity Context for processing.
     *
     * @param parameters the parameters
     * @param context the context
     */
    private void prepareContextData(Map<String, Object> parameters, VelocityContext context) {
        if (parameters == null) {
            return;
        }
        Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet()
                                                                 .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            context.put(entry.getKey(), entry.getValue());
        }
    }

}
