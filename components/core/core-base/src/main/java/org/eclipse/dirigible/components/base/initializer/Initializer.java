/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.initializer;

/**
 * Base interface for initializers that would be called after all classpath content is expanded on
 * the filesystem.
 */
public interface Initializer {

    /**
     * Method that would be called for initialization work to be done.
     */
    void initialize();
}
