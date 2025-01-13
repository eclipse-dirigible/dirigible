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

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.eclipse.dirigible.graalium.core.graal.configuration.Configuration;
import org.eclipse.dirigible.graalium.core.python.PythonCodeRunner;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;

import jakarta.annotation.Nullable;

public class ContextCreator {

    private final Engine engine;
    private final Path workingDirectoryPath;
    private final Path projectDirectoryPath;
    private final @Nullable Path pythonModulesPath;
    private final @Nullable Consumer<Context.Builder> onBeforeContextCreatedHook;
    private final @Nullable Consumer<Context> onAfterContextCreatedHook;
    private final FileSystem fileSystem;

    /** The Constant PYTHON_ENABLED. */
    private static final boolean PYTHON_ENABLED = ServiceLoader.load(PythonCodeRunner.class)
                                                               .iterator()
                                                               .hasNext();

    public ContextCreator(Engine engine, Path workingDirectoryPath, Path projectDirectoryPath, Path pythonModulesPath,
            FileSystem fileSystem) {
        this(engine, workingDirectoryPath, projectDirectoryPath, pythonModulesPath, null, null, fileSystem);
    }

    public ContextCreator(Engine engine, Path workingDirectoryPath, Path projectDirectoryPath, Path pythonModulesPath,
            Consumer<Context.Builder> onBeforeContextCreatedHook, Consumer<Context> onAfterContextCreatedHook, FileSystem fileSystem) {
        this.engine = engine;
        this.workingDirectoryPath = workingDirectoryPath;
        this.projectDirectoryPath = projectDirectoryPath;
        this.pythonModulesPath = pythonModulesPath;
        this.onBeforeContextCreatedHook = onBeforeContextCreatedHook;
        this.onAfterContextCreatedHook = onAfterContextCreatedHook;
        this.fileSystem = fileSystem;
    }

    public Context createContext() {
        Context.Builder contextBuilder = Context.newBuilder()
                                                .engine(engine)
                                                .allowEnvironmentAccess(EnvironmentAccess.INHERIT)
                                                .allowExperimentalOptions(true)
                                                .currentWorkingDirectory(workingDirectoryPath)
                                                .allowHostClassLookup(s -> true)
                                                .allowHostAccess(HostAccess.ALL)
                                                .allowAllAccess(true)
                                                .allowCreateThread(true)
                                                .allowCreateProcess(true)
                                                .allowIO(IOAccess.newBuilder()
                                                                 .fileSystem(fileSystem)
                                                                 .allowHostSocketAccess(true)
                                                                 .build())
                                                .option("js.nashorn-compat", "true")
                                                .option("js.import-attributes", "true")
                                                .option("js.json-modules", "true")
                                                .option("js.operator-overloading", "true")
                                                .option("js.intl-402", "true")
                                                .option("js.ecmascript-version", "staging")
                                                .option("js.esm-eval-returns-exports", "true");

        if (PYTHON_ENABLED) {
            contextBuilder.option("python.PythonPath", createPythonPath())
                          .option("python.ForceImportSite", "true");
        }

        if (onBeforeContextCreatedHook != null) {
            onBeforeContextCreatedHook.accept(contextBuilder);
        }

        Context context = contextBuilder.build();

        if (onAfterContextCreatedHook != null) {
            onAfterContextCreatedHook.accept(context);
        }

        return context;
    }

    private String createPythonPath() {
        if (pythonModulesPath == null) {
            return projectDirectoryPath.toString();
        }
        return projectDirectoryPath + ":" + pythonModulesPath;
    }
}
