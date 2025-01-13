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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class TemplateEnginesManager.
 */
@Component
public class TemplateEnginesManager {

    /** The template engines. */
    private List<TemplateEngine> templateEngines;

    /**
     * Instantiates a new template engines manager.
     *
     * @param templateEngines the template generation engines
     */
    @Autowired
    public TemplateEnginesManager(List<TemplateEngine> templateEngines) {
        this.templateEngines = templateEngines;
    }

    /**
     * Gets the template engines.
     *
     * @return the template engines
     */
    public List<TemplateEngine> getTemplateEngines() {
        return templateEngines;
    }

    /**
     * Select a Template Engine by Name.
     *
     * @param name the name
     * @return the engine
     */
    public final TemplateEngine getTemplateEngine(String name) {
        for (TemplateEngine next : getTemplateEngines()) {
            if (next.getName()
                    .equalsIgnoreCase(name)) {
                return next;
            }
        }
        return null;
    }

}
