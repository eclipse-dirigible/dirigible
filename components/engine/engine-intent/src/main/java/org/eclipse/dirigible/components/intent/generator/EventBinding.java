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
 * Shared reading of an {@code event: { onCreate|onUpdate|onDelete|onTransition: <Entity> }} binding
 * map - the common entity-event hook used by notifications, integrations, departures, process
 * triggers and {@code wait} steps. Maps the event kind to the topic suffix its publisher uses
 * (create = unsuffixed base topic; update/delete = {@code -updated}/{@code -deleted}).
 *
 * <p>
 * {@code onTransition} is the STATUS axis, and it is a separate channel rather than a flavour of
 * update: a workflow setter, a {@code transitions:} button and a {@code generates} completion hook
 * all write the status through the targeted primitives and publish {@code -transitioned}, never
 * {@code -updated} - deliberately, so a system write cannot re-fire the onUpdate reactions meant
 * for a person's edit. The consequence was that the {@code -updated} half of the DSL could not
 * observe a status change at all; binding to it is what this kind exists for.
 */
public final class EventBinding {

    private static final String[] KINDS = {"onCreate", "onUpdate", "onDelete", "onTransition"};

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
     * @return the entity named by the bound event, or {@code null} - including when the binding is not
     *         an entity lifecycle one at all (a {@code onStepReached}/{@code onStepCompleted} binding
     *         names a process step, not an entity; see {@link StepEventSupport})
     */
    public static String entity(Map<String, Object> event) {
        String kind = kind(event);
        Object target = kind == null || event == null ? null : event.get(kind);
        return target instanceof Map<?, ?> || target == null ? null : target.toString();
    }

    /**
     * @param kind the event kind
     * @return the topic suffix ({@code ""} for create, else
     *         {@code -updated}/{@code -deleted}/{@code -transitioned})
     */
    public static String topicSuffix(String kind) {
        if ("onUpdate".equals(kind)) {
            return "-updated";
        }
        if ("onDelete".equals(kind)) {
            return "-deleted";
        }
        if ("onTransition".equals(kind)) {
            return "-transitioned";
        }
        return "";
    }
}
