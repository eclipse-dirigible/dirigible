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

import java.util.List;

/**
 * The Generation Template Metadata serialization object.
 */
public class GenerationTemplateMetadata {

    /** The id. */
    private String id;

    /** The name. */
    private String name;

    /** The description. */
    private String description;

    /** The model. */
    private boolean model;

    /** The sources. */
    private List<GenerationTemplateMetadataSource> sources;

    /** The parameters. */
    private List<GenerationTemplateMetadataParameter> parameters;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for the model flag.
     *
     * @return true if it is model based
     */
    public boolean isModel() {
        return model;
    }

    /**
     * Sets the model flag.
     *
     * @param model the flag
     */
    public void setModel(boolean model) {
        this.model = model;
    }

    /**
     * Gets the sources.
     *
     * @return the sources
     */
    public List<GenerationTemplateMetadataSource> getSources() {
        return sources;
    }

    /**
     * Sets the sources.
     *
     * @param sources the new sources
     */
    public void setSources(List<GenerationTemplateMetadataSource> sources) {
        this.sources = sources;
    }

    /**
     * Gets the parameters.
     *
     * @return the parameters
     */
    public List<GenerationTemplateMetadataParameter> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the new parameters
     */
    public void setParameters(List<GenerationTemplateMetadataParameter> parameters) {
        this.parameters = parameters;
    }

}
