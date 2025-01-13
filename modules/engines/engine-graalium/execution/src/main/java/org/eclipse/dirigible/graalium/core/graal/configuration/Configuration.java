/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.graal.configuration;

/**
 * The Class Configuration.
 */
public class Configuration {

    /**
     * Gets the.
     *
     * @param environmentVariableName the environment variable name
     * @param defaultValue the default value
     * @return the string
     */
    public static String get(String environmentVariableName, String defaultValue) {
        String maybeEnvironmentVariableValue = System.getenv(environmentVariableName);
        return maybeEnvironmentVariableValue != null ? maybeEnvironmentVariableValue : defaultValue;
    }
}
