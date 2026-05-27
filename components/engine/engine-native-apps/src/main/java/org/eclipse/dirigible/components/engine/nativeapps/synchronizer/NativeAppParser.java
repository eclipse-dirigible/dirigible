/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.synchronizer;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Authentication;
import org.eclipse.dirigible.components.engine.nativeapps.domain.BasicAuthCredentials;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Command;
import org.eclipse.dirigible.components.engine.nativeapps.domain.CommandArgument;
import org.eclipse.dirigible.components.engine.nativeapps.domain.ExposedPath;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Lifecycle;
import org.eclipse.dirigible.components.engine.nativeapps.domain.LifecycleStart;
import org.eclipse.dirigible.components.engine.nativeapps.domain.LifecycleStop;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Security;
import org.eclipse.dirigible.components.engine.nativeapps.domain.StartMode;

/**
 * Pure helper for {@link NativeAppSynchronizer#parseImpl}: turns a raw {@code .native-app} JSON
 * payload into a fully-hydrated {@link NativeApp} with placeholders expanded and entity columns
 * lifted from the typed config tree.
 */
public final class NativeAppParser {

    private NativeAppParser() {}

    /**
     * Deserializes the JSON, walks the typed object tree applying {@link Configuration#configureObject}
     * to every node, validates the result and lifts derived fields onto entity columns. The returned
     * entity is detached and not yet persisted.
     */
    public static NativeApp parse(String json) {
        NativeAppFile file = JsonHelper.fromJson(json, NativeAppFile.class);
        if (file == null) {
            throw new IllegalArgumentException("Native app JSON deserialized to null.");
        }
        expandPlaceholdersDeep(file);
        validate(file);

        NativeApp app = new NativeApp();
        app.setName(file.getName() != null ? file.getName() : file.getId());
        app.setDescription(file.getDescription());
        app.setBasePath(file.getBasePath());
        app.setKind(file.getType());
        app.setConfig(file.getConfig());
        liftConfigOntoColumns(app);
        app.setConfigJson(JsonHelper.toJson(app.getConfig()));
        return app;
    }

    /** Recreates the typed {@link NativeAppConfig} tree from the persisted {@code configJson}. */
    public static void rehydrateConfig(NativeApp app) {
        if (app == null || app.getConfigJson() == null || app.getConfig() != null) {
            return;
        }
        app.setConfig(JsonHelper.fromJson(app.getConfigJson(), NativeAppConfig.class));
    }

    private static void expandPlaceholdersDeep(NativeAppFile file) {
        Configuration.configureObject(file);
        NativeAppConfig config = file.getConfig();
        if (config == null) {
            return;
        }
        Configuration.configureObject(config);

        Lifecycle lifecycle = config.getLifecycle();
        if (lifecycle != null) {
            Configuration.configureObject(lifecycle);
            LifecycleStart start = lifecycle.getStart();
            if (start != null) {
                Configuration.configureObject(start);
                for (Command cmd : start.getCommands()) {
                    expandCommand(cmd);
                }
            }
            LifecycleStop stop = lifecycle.getStop();
            if (stop != null) {
                Configuration.configureObject(stop);
                for (Command cmd : stop.getCommands()) {
                    expandCommand(cmd);
                }
            }
        }

        Security security = config.getSecurity();
        if (security != null) {
            Configuration.configureObject(security);
            Authentication auth = security.getAuthentication();
            if (auth != null) {
                Configuration.configureObject(auth);
                BasicAuthCredentials cred = auth.getCredentials();
                if (cred != null) {
                    Configuration.configureObject(cred);
                }
            }
            for (ExposedPath exposed : security.getExposedPaths()) {
                Configuration.configureObject(exposed);
            }
        }
    }

    private static void expandCommand(Command cmd) {
        if (cmd == null) {
            return;
        }
        Configuration.configureObject(cmd);
        for (CommandArgument arg : cmd.getArguments()) {
            Configuration.configureObject(arg);
        }
    }

    private static void validate(NativeAppFile file) {
        if (file.getType() == null) {
            throw new IllegalArgumentException("Native app is missing required field [type] (local|remote).");
        }
        String displayName = file.getName() != null ? file.getName() : file.getId();
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Native app is missing required field [name] (and no [id] fallback).");
        }
        if (file.getBasePath() == null) {
            throw new IllegalArgumentException("Native app [" + displayName + "] is missing required field [basePath].");
        }
        NativeAppConfig config = file.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("Native app [" + displayName + "] is missing required field [config].");
        }
        if (file.getType() == NativeAppKind.REMOTE) {
            if (config.getUrl() == null || config.getUrl()
                                                 .isBlank()) {
                throw new IllegalArgumentException("Remote native app [" + displayName + "] must declare [config.url].");
            }
        } else if (file.getType() == NativeAppKind.LOCAL) {
            if (config.getLifecycle() == null || config.getLifecycle()
                                                       .getStart() == null
                    || config.getLifecycle()
                             .getStart()
                             .getCommands()
                             .isEmpty()) {
                throw new IllegalArgumentException(
                        "Local native app [" + displayName + "] must declare at least one [config.lifecycle.start.commands] entry.");
            }
        }
    }

    private static void liftConfigOntoColumns(NativeApp app) {
        NativeAppConfig config = app.getConfig();
        if (config == null) {
            return;
        }
        app.setDefaultPort(config.getDefaultPort());
        if (app.getKind() == NativeAppKind.REMOTE) {
            app.setRemoteUrl(config.getUrl());
            app.setStartMode(null);
        } else if (app.getKind() == NativeAppKind.LOCAL) {
            app.setRemoteUrl(null);
            LifecycleStart start = config.getLifecycle() == null ? null
                    : config.getLifecycle()
                            .getStart();
            StartMode mode = start == null ? null : start.getMode();
            app.setStartMode(mode == null ? StartMode.LAZY : mode);
        }
    }
}
