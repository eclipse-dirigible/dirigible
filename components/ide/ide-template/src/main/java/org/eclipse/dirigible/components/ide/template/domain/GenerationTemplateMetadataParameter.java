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

/**
 * The Generation Template Metadata Parameter serialization object.
 */
public class GenerationTemplateMetadataParameter {

    /** The name. */
    private String name;

    /** The required. */
    private boolean required;

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
     * Checks if is required.
     *
     * @return true, if is required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Sets the required.
     *
     * @param required the new required
     */
    public void setRequired(boolean required) {
        this.required = required;
    }



}
