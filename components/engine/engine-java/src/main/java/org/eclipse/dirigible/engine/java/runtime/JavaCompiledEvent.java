/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * Published by {@link JavaLoader} after each successful rebuild. Carries the FQNs that were
 * compiled (and written to disk) and the FQNs that were removed (and whose {@code .class} files
 * were deleted).
 */
public class JavaCompiledEvent extends ApplicationEvent {

    private final Set<String> compiledFqns;
    private final Set<String> removedFqns;

    public JavaCompiledEvent(Object source, Set<String> compiledFqns, Set<String> removedFqns) {
        super(source);
        this.compiledFqns = Set.copyOf(compiledFqns);
        this.removedFqns = Set.copyOf(removedFqns);
    }

    /** FQNs that were successfully compiled and whose {@code .class} files were written to disk. */
    public Set<String> getCompiledFqns() {
        return compiledFqns;
    }

    /** FQNs that were removed from the registry and whose {@code .class} files were deleted. */
    public Set<String> getRemovedFqns() {
        return removedFqns;
    }
}
