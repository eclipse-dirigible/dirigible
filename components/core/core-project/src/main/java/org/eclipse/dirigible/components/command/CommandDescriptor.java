/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.command;

import org.apache.commons.lang3.SystemUtils;

/**
 * The Class CommandLine.
 */
public class CommandDescriptor {

    /** The os. */
    private final CommandOS os;

    /** The command. */
    private final String command;

    /**
     * Instantiates a new command line.
     *
     * @param os the os
     * @param command the command
     */
    public CommandDescriptor(CommandOS os, String command) {
        this.os = os;
        this.command = command;
    }

    /**
     * Gets the os.
     *
     * @return the os
     */
    public CommandOS getOS() {
        return os;
    }

    /**
     * Gets the command.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Checks if is compatible with current OS.
     *
     * @return true, if is compatible with current OS
     */
    public boolean isCompatibleWithCurrentOS() {
        if (os == null) {
            return true; // treat as command with no explicit OS set, so use this command as universal
        }

        switch (os) {
            case UNIX:
                return SystemUtils.IS_OS_UNIX;
            case LINUX:
                return SystemUtils.IS_OS_LINUX;
            case MAC:
                return SystemUtils.IS_OS_MAC;
            case WINDOWS:
                return SystemUtils.IS_OS_WINDOWS;
            default:
                throw new IllegalArgumentException("Unsupported OS type: " + os);
        }
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "CommandLine{" + "os=" + os + ", command='" + command + '\'' + '}';
    }
}
