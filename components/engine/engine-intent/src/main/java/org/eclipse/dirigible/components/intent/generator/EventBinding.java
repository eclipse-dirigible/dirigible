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
 *
 * <p>
 * {@code onPhase} is the ENRICHMENT axis, and its suffix comes from the binding rather than the
 * kind: a phase is a name the entity declares ({@code phases: [costed]}) and the topic is
 * {@code -<phase>}. It exists because an enrichment a listener computes and writes back
 * event-silently publishes nothing at all, so a consumer bound to {@code onCreate} races it and
 * reads the un-enriched row - with every step green. Reading the suffix therefore needs the whole
 * binding map, which is what {@link #topicSuffix(Map)} is for; {@link #topicSuffix(String)} answers
 * for the kind-only channels and cannot resolve a phase.
 *
 * <p>
 * {@code onNotifyFailed} is the DELIVERY axis (dirigible #7023): a notify block is fail-soft, so a
 * mail that never leaves used to be a server log line and nothing else - the document sat in SENT
 * and no construct in the DSL could observe it. A notify block declaring {@code outcome:} stamps
 * the failure on the record and rides {@code -notifyFailed} into the outbox with that write, so the
 * flip of the trace and its announcement commit together. Only the FAILURE is announced: a delivery
 * that worked is the normal path and needs no channel of its own.
 */
public final class EventBinding {

    /**
     * The enrichment axis: the bound entity, with the phase named by the sibling {@link #PHASE_KEY}.
     */
    public static final String ON_PHASE = "onPhase";

    /** The key naming which declared phase an {@link #ON_PHASE} binding observes. */
    public static final String PHASE_KEY = "phase";

    /** The delivery axis: a notify block about the bound entity could not send its message. */
    public static final String ON_NOTIFY_FAILED = "onNotifyFailed";

    /**
     * The topic suffix {@link #ON_NOTIFY_FAILED} binds - published so the senders cannot mistype it.
     */
    public static final String NOTIFY_FAILED_SUFFIX = "-notifyFailed";

    private static final String[] KINDS = {"onCreate", "onUpdate", "onDelete", "onTransition", ON_NOTIFY_FAILED, ON_PHASE};

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
     * @param event the binding map (may be {@code null})
     * @return the phase an {@link #ON_PHASE} binding names, or {@code null} for any other binding
     */
    public static String phase(Map<String, Object> event) {
        if (event == null || event.get(ON_PHASE) == null) {
            return null;
        }
        Object phase = event.get(PHASE_KEY);
        return phase == null ? null : phase.toString();
    }

    /**
     * The topic suffix of a whole binding - the only form that can resolve an {@code onPhase} one,
     * whose suffix is the declared phase name rather than a constant of the kind.
     *
     * @param event the binding map (may be {@code null})
     * @return the topic suffix the bound event's publisher uses
     */
    public static String topicSuffix(Map<String, Object> event) {
        String phase = phase(event);
        return phase == null ? topicSuffix(kind(event)) : "-" + phase;
    }

    /**
     * @param kind the event kind
     * @return the topic suffix ({@code ""} for create, else
     *         {@code -updated}/{@code -deleted}/{@code -transitioned}/{@code -notifyFailed})
     * @throws IllegalArgumentException for {@code onPhase}, whose suffix is the phase name and lives in
     *         the binding - a consumer that can bind a phase must read {@link #topicSuffix(Map)}, and
     *         answering the create topic here would silently bind the un-enriched moment, which is the
     *         very failure the phase axis exists to remove
     */
    public static String topicSuffix(String kind) {
        if (ON_PHASE.equals(kind)) {
            throw new IllegalArgumentException("onPhase has no kind-only topic suffix - read topicSuffix(event)");
        }
        if ("onUpdate".equals(kind)) {
            return "-updated";
        }
        if ("onDelete".equals(kind)) {
            return "-deleted";
        }
        if ("onTransition".equals(kind)) {
            return "-transitioned";
        }
        if (ON_NOTIFY_FAILED.equals(kind)) {
            return NOTIFY_FAILED_SUFFIX;
        }
        return "";
    }
}
