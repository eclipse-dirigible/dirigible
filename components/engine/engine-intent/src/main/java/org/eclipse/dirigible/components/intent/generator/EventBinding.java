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

import java.util.Map;

/**
 * Shared reading of an {@code event: { onCreate|onUpdate|onDelete: <Entity> }} binding map - the
 * common entity-lifecycle hook used by notifications, integrations and other declarative glue. Maps
 * the event kind to the per-operation topic suffix the Java DAO publishes to (create = unsuffixed
 * base topic; update/delete = {@code -updated}/{@code -deleted}).
 */
public final class EventBinding {

    private static final String[] KINDS = {"onCreate", "onUpdate", "onDelete"};

    private EventBinding() {}

    /**
     * @param event the binding map (may be {@code null})
     * @return the lifecycle event kind present, or {@code null}
     */
    public static String kind(Map<String, Object> event) {
        if (event == null) {
            return null;
        }
        for (String kind : KINDS) {
            if (event.get(kind) != null) {
                return kind;
            }
        }
        return null;
    }

    /**
     * @param event the binding map (may be {@code null})
     * @return the entity named by the bound event, or {@code null}
     */
    public static String entity(Map<String, Object> event) {
        String kind = kind(event);
        Object target = kind == null || event == null ? null : event.get(kind);
        return target == null ? null : target.toString();
    }

    /**
     * @param kind the lifecycle event kind
     * @return the topic suffix ({@code ""} for create, {@code -updated}/{@code -deleted} otherwise)
     */
    public static String topicSuffix(String kind) {
        if ("onUpdate".equals(kind)) {
            return "-updated";
        }
        if ("onDelete".equals(kind)) {
            return "-deleted";
        }
        return "";
    }
}
