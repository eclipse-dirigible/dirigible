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
 * The launcher agent the JVM invokes before {@code main} - its only job is to capture the
 * {@link Instrumentation} instance so the platform can append {@code scope: "platform"} dependency
 * JARs to the system classloader at runtime (JDBC drivers, JNI-bearing libraries).
 *
 * <p>
 * Two delivery paths invoke it:
 * <ul>
 * <li>the {@code Launcher-Agent-Class} manifest attribute of the executable jar - a {@code java
 * -jar} launch calls {@link #agentmain(String, Instrumentation)} with no command-line flag;</li>
 * <li>an explicit {@code -javaagent:} flag - used by the integration tests and by launches that do
 * not go through the executable jar - which calls {@link #premain(String, Instrumentation)}.</li>
 * </ul>
 *
 * Without either, the holder simply stays empty and the platform reports platform-scoped
 * dependencies as pending-restart - the agent is never required for boot.
 */
public final class DirigibleLauncherAgent {

    /**
     * Instantiation is not needed - the JVM calls the static entry points.
     */
    private DirigibleLauncherAgent() {
        // static entry points only
    }

    /**
     * The {@code Launcher-Agent-Class} entry point.
     *
     * @param args the agent arguments, unused
     * @param instrumentation the instrumentation
     */
    public static void agentmain(String args, Instrumentation instrumentation) {
        install(instrumentation);
    }

    /**
     * The {@code -javaagent} entry point.
     *
     * @param args the agent arguments, unused
     * @param instrumentation the instrumentation
     */
    public static void premain(String args, Instrumentation instrumentation) {
        install(instrumentation);
    }

    /**
     * Install.
     *
     * @param instrumentation the instrumentation
     */
    private static void install(Instrumentation instrumentation) {
        InstrumentationHolder.set(instrumentation);
        // The agent runs before any logging framework exists in this JVM, so a single stdout line
        // is the only possible boot marker - the launch-mode integration test greps for it, and
        // operators can confirm the delivery from the container log. Deliberately NOT a logger.
        System.out.println("Dirigible launcher agent installed - platform-scope dependencies can be appended at runtime");
    }

}
