/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

/**
 * Naming conventions shared by every intent generator. The physical table name in particular is
 * referenced from three artefacts (the {@code .edm} entity {@code dataName}, the {@code .report}
 * {@code baseTable} and the {@code .csvim} {@code table}) - all three must call
 * {@link #tableName(IntentGenerationContext, String)} so they can never drift apart.
 */
public final class IntentNaming {

    private IntentNaming() {}

    /**
     * The intent's base name used for single-file outputs ({@code <base>.edm}, {@code <base>.roles})
     * and as the physical table-name prefix. The YAML document's own {@code name:} field wins - the
     * file is conventionally called {@code app.intent}, so the name derived from the file name
     * ({@code app}) is a poor identity. Falls back to the intent file's base name, then the project
     * name, then the literal {@code intent}.
     *
     * @param context the generation context
     * @return the base name, never blank
     */
    public static String baseName(IntentGenerationContext context) {
        String declaredName = context.getModel()
                                     .getName();
        if (declaredName != null && !declaredName.isBlank()) {
            return declaredName;
        }
        String fallbackName = context.getFallbackName();
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }
        String project = context.getProjectName();
        return project.isEmpty() ? "intent" : project;
    }

    /**
     * Physical table name for an entity: {@code <INTENT>_<ENTITY>} in upper snake (codbex-style, e.g.
     * {@code ORDERS_COUNTRY}). The intent-name prefix keeps tables unique across projects sharing a
     * schema and away from SQL reserved words like {@code ORDER}.
     *
     * @param context the generation context
     * @param entityName the entity's declared name
     * @return the upper-snake, intent-prefixed table name
     */
    public static String tableName(IntentGenerationContext context, String entityName) {
        return upperSnake(baseName(context)) + "_" + upperSnake(entityName);
    }

    /**
     * Camel-/Pascal-case to upper snake. Handles {@code IDValue} -> {@code ID_VALUE}.
     *
     * @param name the identifier to convert (may be null)
     * @return the upper-snake form, empty for null/empty input
     */
    public static String upperSnake(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                out.append('_');
            }
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }
}
