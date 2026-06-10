/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.platform;

import java.util.Collections;
import java.util.Map;
import org.eclipse.dirigible.components.api.platform.EnginesFacade;

/**
 * Invokes another scripting engine on a file in the registry — most often GraalJS, occasionally
 * Python or another GraalVM polyglot engine. The {@code parameters} map is forwarded into the
 * engine's global scope and the engine's natural return value is propagated back.
 * <p>
 * Useful for hybrid workflows where a Java controller delegates a step to TS/JS code (or vice
 * versa). For pure in-Java composition prefer regular method calls; spinning up an engine has
 * non-trivial cost compared to invoking a {@code Component} bean directly.
 */
public final class Engine {

    private Engine() {}

    public static Object execute(String type, String project, String filePath) throws Exception {
        return EnginesFacade.execute(type, project, filePath, null, Collections.emptyMap(), false);
    }

    public static Object execute(String type, String project, String filePath, Map<Object, Object> parameters) throws Exception {
        return EnginesFacade.execute(type, project, filePath, null, parameters, false);
    }

    public static Object execute(String type, String project, String filePath, String pathParam, Map<Object, Object> parameters,
            boolean debug) throws Exception {
        return EnginesFacade.execute(type, project, filePath, pathParam, parameters, debug);
    }

    public static String listEngines() {
        return EnginesFacade.getEngineTypes();
    }
}
