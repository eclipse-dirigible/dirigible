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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class CommandDefinition.
 */
public class Command {

    /** The description. */
    private String description;

    /** The content type. */
    private String contentType;

    /** The commands. */
    private final List<CommandDescriptor> commands = new ArrayList<>();

    /** The set. */
    private final Map<String, String> set = new HashMap<>();

    /** The unset. */
    private final List<String> unset = new ArrayList<>();

    /** The target command. */
    private CommandDescriptor targetCommand;

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the content type.
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType the new content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the commands.
     *
     * @return the commands
     */
    public List<CommandDescriptor> getCommands() {
        return commands;
    }

    /**
     * Gets the sets the.
     *
     * @return the sets the
     */
    public Map<String, String> getSet() {
        return set;
    }

    /**
     * Gets the unset.
     *
     * @return the unset
     */
    public List<String> getUnset() {
        return unset;
    }

    /**
     * Gets the target command.
     *
     * @return the target command
     */
    public CommandDescriptor getTargetCommand() {
        return targetCommand;
    }

    /**
     * Sets the target command.
     *
     * @param targetCommand the new target command
     */
    public void setTargetCommand(CommandDescriptor targetCommand) {
        this.targetCommand = targetCommand;
    }

    /**
     * Validate.
     *
     * @throws IllegalArgumentException the illegal argument exception
     */
    public void validate() throws IllegalArgumentException {
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("Commands array is empty. Set appropriate command per target OS");
        }

        CommandDescriptor commandDescriptor = commands.stream()
                                                      .filter(CommandDescriptor::isCompatibleWithCurrentOS)
                                                      .findFirst()
                                                      .orElseThrow(
                                                              () -> new IllegalArgumentException("There is no command for current OS"));
        setTargetCommand(commandDescriptor);
    }

}
