/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.launcher.agent;

import java.lang.instrument.Instrumentation;

/**
 * Holds the {@link Instrumentation} instance the {@link DirigibleLauncherAgent} captured before
 * {@code main}. Under a {@code java -jar} launch this class sits at the executable jar's root, so
 * the system classloader defines it; application code linking against it resolves parent-first to
 * that same definition and reads the same static field the agent wrote.
 */
public final class InstrumentationHolder {

    /** The instrumentation, null until an agent delivery installs it. */
    private static volatile Instrumentation instrumentation;

    /**
     * Instantiation is not needed.
     */
    private InstrumentationHolder() {
        // static holder only
    }

    /**
     * Called by the agent's entry points.
     *
     * @param captured the instrumentation
     */
    static void set(Instrumentation captured) {
        instrumentation = captured;
    }

    /**
     * The captured instrumentation.
     *
     * @return the instrumentation, or null when no agent delivery ran in this JVM
     */
    public static Instrumentation get() {
        return instrumentation;
    }

}
