/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * The Generation Template Parameters serialization object.
 */
public class GenerationTemplateParameters {

    /** The template. */
    private String template;

    /** The parameters. */
    private Map<String, Object> parameters = new HashMap<String, Object>();

    /**
     * Gets the template.
     *
     * @return the template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Sets the template.
     *
     * @param template the new template
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Gets the parameters.
     *
     * @return the parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }



}
