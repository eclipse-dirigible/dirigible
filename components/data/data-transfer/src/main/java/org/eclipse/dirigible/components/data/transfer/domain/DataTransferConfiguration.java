/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.transfer.domain;

/**
 * The Class DataTransferConfiguration.
 */
public class DataTransferConfiguration {

    /** The source schema. */
    private String sourceSchema;

    /** The target schema. */
    private String targetSchema;

    /**
     * Gets the source schema.
     *
     * @return the sourceSchema
     */
    public String getSourceSchema() {
        return sourceSchema;
    }

    /**
     * Sets the source schema.
     *
     * @param sourceSchema the sourceSchema to set
     */
    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    /**
     * Gets the target schema.
     *
     * @return the targetSchema
     */
    public String getTargetSchema() {
        return targetSchema;
    }

    /**
     * Sets the target schema.
     *
     * @param targetSchema the targetSchema to set
     */
    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

}
