/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.graal;

import org.eclipse.dirigible.graalium.core.graal.configuration.Configuration;
import org.graalvm.polyglot.Engine;

import java.io.PrintStream;

import static org.eclipse.dirigible.graalium.core.graal.Logging.errorStream;
import static org.eclipse.dirigible.graalium.core.graal.Logging.outputStream;

public class EngineCreator {
    private static Engine ENGINE = null;
    private static Engine DEBUGGABLE_ENGINE = null;

    public static Engine getOrCreateEngine() {
        if (ENGINE == null) {
            ENGINE = getDefaultEngineBuilder().build();
        }

        return ENGINE;
    }

    public static Engine getOrCreateDebuggableEngine() {
        if (DEBUGGABLE_ENGINE == null) {
            DEBUGGABLE_ENGINE = getDefaultEngineBuilder().option("inspect", Configuration.get("DIRIGIBLE_GRAALIUM_DEBUG_PORT", "8081"))
                                                         .option("inspect.Secure",
                                                                 Configuration.get("DIRIGIBLE_GRAALIUM_DEBUG_SECURE", "false"))
                                                         .option("inspect.Suspend",
                                                                 Configuration.get("DIRIGIBLE_GRAALIUM_DEBUG_SUSPEND", "true"))
                                                         .option("inspect.Path",
                                                                 Configuration.get("DIRIGIBLE_GRAALIUM_DEBUG_PATH", "debug"))
                                                         .build();
        }

        return DEBUGGABLE_ENGINE;
    }

    private static Engine.Builder getDefaultEngineBuilder() {
        return Engine.newBuilder()
                     .allowExperimentalOptions(true)
                     .out(outputStream())
                     .err(errorStream())
                     .option("engine.WarnInterpreterOnly", "false");
    }
}
