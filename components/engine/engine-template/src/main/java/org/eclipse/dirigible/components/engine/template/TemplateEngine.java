/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.template;

import java.io.IOException;
import java.util.Map;

/**
 * The Interface TemplateEngine.
 */
public interface TemplateEngine {

    /** The Constant TEMPLATE_ENGINE_DEFAULT. */
    public static final String TEMPLATE_ENGINE_DEFAULT = "mustache";

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName();

    /**
     * Generate.
     *
     * @param parameters the parameters
     * @param location the location
     * @param input the input
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public byte[] generate(Map<String, Object> parameters, String location, byte[] input) throws IOException;

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
    public byte[] generate(Map<String, Object> parameters, String location, byte[] input, String sm, String em) throws IOException;

}
