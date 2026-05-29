/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.process;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Command;

import java.util.List;
import java.util.Optional;

final class OsCommandResolver {

    static final String OS_MAC = "mac";
    static final String OS_LINUX = "linux";
    static final String OS_WINDOWS = "windows";

    private OsCommandResolver() {}

    /**
     * Returns the lower-case OS token matching the running platform, or {@code null} if it isn't one of
     * the supported values.
     */
    static String currentOs() {
        if (SystemUtils.IS_OS_MAC) {
            return OS_MAC;
        }
        if (SystemUtils.IS_OS_LINUX) {
            return OS_LINUX;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            return OS_WINDOWS;
        }
        return null;
    }

    /**
     * Picks the command from the configured list whose {@code os} field matches the running platform.
     */
    static Optional<Command> pickForCurrentOs(List<Command> commands) {
        if (commands == null || commands.isEmpty()) {
            return Optional.empty();
        }
        String os = currentOs();
        if (os == null) {
            return Optional.empty();
        }
        return commands.stream()
                       .filter(c -> os.equalsIgnoreCase(c.getOs()))
                       .findFirst();
    }
}
