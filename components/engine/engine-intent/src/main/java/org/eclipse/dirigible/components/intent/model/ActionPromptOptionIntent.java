/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

/**
 * One value a {@code multiselect} prompt input chooses from (issue #8296). Authored as {@code {
 * value, label?, help? }} or as a bare string, which is its value and its label.
 */
public class ActionPromptOptionIntent {

    /** The value sent in the call's body when the option is ticked. */
    private String value;

    /** The caption beside the option's box; the {@link #value} when absent. */
    private String label;

    /** One line under the option. */
    private String help;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** The caption shown beside the box: the {@link #label}, else the {@link #value}. */
    public String caption() {
        return label == null || label.isBlank() ? value : label;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }
}
