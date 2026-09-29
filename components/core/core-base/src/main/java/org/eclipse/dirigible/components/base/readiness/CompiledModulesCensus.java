/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.readiness;

/**
 * What the last AOT compiled-module discovery found (#7533): the modules carrying a
 * {@code META-INF/dirigible/<project>/.compiled} marker, the distinct classes their markers list,
 * and how many of those actually loaded. A module jar that loaded only partly is invisible to any
 * floor - only {@link #complete()} catches it.
 *
 * @param modules the projects carrying a marker
 * @param expectedClasses the distinct class names the markers list
 * @param registeredClasses the listed classes that loaded and were registered
 */
public record CompiledModulesCensus(int modules, int expectedClasses, int registeredClasses) {

    /**
     * Whether every listed class was registered.
     *
     * @return true when registered equals expected
     */
    public boolean complete() {
        return registeredClasses == expectedClasses;
    }
}
