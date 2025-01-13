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
 * The Class DataTransfer.
 */
public class DataTransfer {

    /** The source. */
    private String source;

    /** The target. */
    private String target;

    /** The configuration. */
    private DataTransferConfiguration configuration;

    /**
     * Gets the source.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source.
     *
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the target.
     *
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the target.
     *
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Gets the configuration.
     *
     * @return the configuration
     */
    public DataTransferConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the configuration.
     *
     * @param configuration the configuration to set
     */
    public void setConfiguration(DataTransferConfiguration configuration) {
        this.configuration = configuration;
    }

}
