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

    private OsCommandResolver() {}

    /**
     * Returns the {@link Os} matching the running platform, or {@link Os#UNSUPPORTED} when the host is
     * none of mac / linux / windows.
     */
    static Os currentOs() {
        if (SystemUtils.IS_OS_MAC) {
            return Os.MAC;
        }
        if (SystemUtils.IS_OS_LINUX) {
            return Os.LINUX;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            return Os.WINDOWS;
        }
        return Os.UNSUPPORTED;
    }

    /**
     * Picks the command from the configured list whose {@code os} field matches the running platform.
     * An unrecognised host ({@link Os#UNSUPPORTED}) or a command whose {@code os} token doesn't parse
     * cleanly never matches.
     */
    static Optional<Command> pickForCurrentOs(List<Command> commands) {
        if (commands == null || commands.isEmpty()) {
            return Optional.empty();
        }
        Os current = currentOs();
        if (current == Os.UNSUPPORTED) {
            return Optional.empty();
        }
        return commands.stream()
                       .filter(c -> Os.fromTokenIfKnown(c.getOs())
                                      .map(parsed -> parsed == current)
                                      .orElse(false))
                       .findFirst();
    }
}
