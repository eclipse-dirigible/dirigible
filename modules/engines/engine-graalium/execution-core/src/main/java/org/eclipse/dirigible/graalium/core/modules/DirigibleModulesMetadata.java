/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.modules;

import java.util.List;

/**
 * The Class DirigibleModulesMetadata.
 */
public class DirigibleModulesMetadata {

    /** The Constant PURE_ESM_MODULES. */
    private static final List<String> PURE_ESM_MODULES = List.of("@aerokit/sdk/http", "@aerokit/sdk/io", "@aerokit/sdk/bpm",
            "@aerokit/sdk/cache", "@aerokit/sdk/cms", "@aerokit/sdk/component", "@aerokit/sdk/core", "@aerokit/sdk/db", "@aerokit/sdk/etcd",
            "@aerokit/sdk/extensions", "@aerokit/sdk/git", "@aerokit/sdk/indexing", "@aerokit/sdk/job", "@aerokit/sdk/kafka",
            "@aerokit/sdk/log", "@aerokit/sdk/mail", "@aerokit/sdk/messaging", "@aerokit/sdk/mongodb", "@aerokit/sdk/net",
            "@aerokit/sdk/pdf", "@aerokit/sdk/platform", "@aerokit/sdk/qldb", "@aerokit/sdk/rabbitmq", "@aerokit/sdk/redis",
            "@aerokit/sdk/user", "@aerokit/sdk/template", "@aerokit/sdk/utils", "@aerokit/sdk/junit", "@aerokit/sdk/integrations",
            "@aerokit/sdk/security", //
            // add old apis as well for compatibility
            "sdk/http", "sdk/io", "sdk/bpm", "sdk/cache", "sdk/cms", "sdk/component", "sdk/core", "sdk/db", "sdk/etcd", "sdk/extensions",
            "sdk/git", "sdk/indexing", "sdk/job", "sdk/kafka", "sdk/log", "sdk/mail", "sdk/messaging", "sdk/mongodb", "sdk/net", "sdk/pdf",
            "sdk/platform", "sdk/qldb", "sdk/rabbitmq", "sdk/redis", "sdk/user", "sdk/template", "sdk/utils", "sdk/junit",
            "sdk/integrations", "sdk/security");

    /**
     * Checks if is pure esm module.
     *
     * @param module the module
     * @return true, if is pure esm module
     */
    static boolean isPureEsmModule(String module) {
        return PURE_ESM_MODULES.stream()
                               .anyMatch(module::startsWith);
    }
}
