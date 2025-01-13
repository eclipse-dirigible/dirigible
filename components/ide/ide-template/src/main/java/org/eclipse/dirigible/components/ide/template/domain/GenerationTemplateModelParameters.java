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
 * The Generation Template Model Parameters serialization object.
 */
public class GenerationTemplateModelParameters extends GenerationTemplateParameters {

    /** The model. */
    private String model;

    /**
     * Gets the model.
     *
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the model.
     *
     * @param model the model
     */
    public void setModel(String model) {
        this.model = model;
    }

}
