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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

/**
 * One input of a {@code call:} action's dialog (issue #8090). The call has no target entity to type
 * the control from, so the control declares its own type.
 *
 * <p>
 * An input may open with a value ({@link #defaultValue}) and explain itself ({@link #help}); a
 * {@code multiselect} is a set of values chosen from its {@link #options} (issue #8296).
 */
public class ActionPromptIntent {

    /** The type whose value is a set of {@link #options}. */
    public static final String MULTISELECT = "multiselect";

    /** The input types a prompt control may declare. */
    public static final Set<String> TYPES = Set.of("string", "email", "text", "integer", "decimal", "boolean", "date", MULTISELECT);

    /** The input's name, referenced as {@code {name}} in the call's body. */
    private String name;

    /** One of {@link #TYPES}. */
    private String type;

    /** The control's caption; defaults to a humanized {@link #name}. */
    private String label;

    /** Whether the dialog refuses to run with the input empty - for a multiselect, with none ticked. */
    private boolean required;

    /**
     * The value the dialog opens with: a literal of the input's type (a list of option values for a
     * multiselect), or {@code "{property}"} - a property of the clicked record.
     */
    @SerializedName("default")
    private Object defaultValue;

    /** One line under the control. */
    private String help;

    /** The values a {@link #MULTISELECT} chooses from; refused on every other type. */
    private List<ActionPromptOptionIntent> options = new ArrayList<>();

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

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public List<ActionPromptOptionIntent> getOptions() {
        return options;
    }

    public void setOptions(List<ActionPromptOptionIntent> options) {
        this.options = options == null ? new ArrayList<>() : options;
    }

    /** Whether the input is a {@link #MULTISELECT}. */
    public boolean isMultiselect() {
        return MULTISELECT.equals(type);
    }
}
