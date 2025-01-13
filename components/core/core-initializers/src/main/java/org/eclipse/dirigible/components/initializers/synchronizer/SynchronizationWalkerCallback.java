/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * The Interface SynchronizationWalkerCallback.
 */
public interface SynchronizationWalkerCallback {

    /**
     * Visit file.
     *
     * @param file the file
     * @param attrs the attrs
     * @param location the location
     */
    public void visitFile(Path file, BasicFileAttributes attrs, String location);

}
