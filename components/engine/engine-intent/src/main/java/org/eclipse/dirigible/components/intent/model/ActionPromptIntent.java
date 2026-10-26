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

import java.util.Set;

/**
 * One input of a {@code call:} action's dialog (issue #8090). The call has no target entity to type
 * the control from, so the control declares its own type.
 */
public class ActionPromptIntent {

    /** The input types a prompt control may declare. */
    public static final Set<String> TYPES = Set.of("string", "email", "text", "integer", "decimal", "boolean", "date");

    /** The input's name, referenced as {@code {name}} in the call's body. */
    private String name;

    /** One of {@link #TYPES}. */
    private String type;

    /** The control's caption; defaults to a humanized {@link #name}. */
    private String label;

    /** Whether the dialog refuses to run with the input empty. */
    private boolean required;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
