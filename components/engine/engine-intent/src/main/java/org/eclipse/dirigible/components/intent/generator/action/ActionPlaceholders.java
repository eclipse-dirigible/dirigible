/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.action;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.generator.CheckSupport;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.model.ActionIntent;
import org.eclipse.dirigible.components.intent.model.ActionPromptIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;

/**
 * The clicked record's properties a {@code call:} action's dialog names as {@code {property}}
 * (issue #8296): in its {@code description}, its {@code confirm} text and a prompt input's
 * {@code default}. A placeholder names a field of the {@code forEntity}, a to-one relation (shown
 * by its display text) or the {@code Name} a {@code label:} expression generates.
 */
public final class ActionPlaceholders {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]*)\\}");

    private ActionPlaceholders() {}

    /**
     * The placeholders a dialog text names, in order of first appearance.
     *
     * @param text the authored text, may be null
     * @return the placeholder names
     */
    public static Set<String> in(String text) {
        Set<String> names = new LinkedHashSet<>();
        if (text != null) {
            Matcher placeholder = PLACEHOLDER.matcher(text);
            while (placeholder.find()) {
                names.add(placeholder.group(1));
            }
        }
        return names;
    }

    /**
     * The property a {@code default:} reads off the clicked record, or {@code null} when the default is
     * a literal - only a whole {@code "{property}"} string is a placeholder.
     *
     * @param value the authored default
     * @return the placeholder name, or null
     */
    public static String ofDefault(Object value) {
        if (value instanceof String text) {
            Matcher whole = PLACEHOLDER.matcher(text);
            return whole.matches() ? whole.group(1) : null;
        }
        return null;
    }

    /**
     * Every placeholder the action's dialog reads off the clicked record.
     *
     * @param action the call action
     * @return the placeholder names, in authoring order
     */
    public static Set<String> of(ActionIntent action) {
        Set<String> names = new LinkedHashSet<>(in(action.getDescription()));
        names.addAll(in(action.getConfirm()));
        for (ActionPromptIntent input : action.getPrompt()) {
            String property = ofDefault(input.getDefaultValue());
            if (property != null) {
                names.add(property);
            }
        }
        return names;
    }

    /**
     * The record property a placeholder names, PascalCase as the record carries it.
     *
     * @param entity the action's {@code forEntity}
     * @param name the placeholder name
     * @return the property, or {@code null} when the entity has none of that name
     */
    public static String property(EntityIntent entity, String name) {
        FieldIntent field = CheckSupport.field(entity, name);
        if (field != null) {
            return IntentNaming.pascalCase(field.getName());
        }
        RelationIntent relation = CheckSupport.toOne(entity, name);
        if (relation != null) {
            return IntentNaming.pascalCase(relation.getName());
        }
        if (entity != null && "name".equalsIgnoreCase(name) && entity.getLabel() != null && !entity.getLabel()
                                                                                                   .isBlank()) {
            return "Name"; // the stored label property the expression generates
        }
        return null;
    }

    /**
     * The to-one relation a placeholder names, whose display text the dialog shows, or {@code null}
     * when it names a field (a field of the name wins, as in {@link #property}).
     *
     * @param entity the action's {@code forEntity}
     * @param name the placeholder name
     * @return the relation, or null
     */
    public static RelationIntent relation(EntityIntent entity, String name) {
        return CheckSupport.field(entity, name) != null ? null : CheckSupport.toOne(entity, name);
    }
}
