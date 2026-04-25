/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

public class InvalidConfigException extends RuntimeException {
    private final String configKey;

    public InvalidConfigException(String message, String configKey) {
        super(message);
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
