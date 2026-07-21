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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Process / workflow declaration. Generates a {@code .bpmn} and any supporting forms / listeners.
 * {@link #trigger} is a free-form map ({@code onCreate: Order}, {@code onSchedule: "0 0 * * *"},
 * {@code onMessage: queue:invoices}) interpreted by the process generator.
 */
public class ProcessIntent {

    private String name;
    private String description;
    private Map<String, Object> trigger = new LinkedHashMap<>();
    private List<StepIntent> steps = new ArrayList<>();
    private Map<String, Object> abortOn = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getTrigger() {
        return trigger;
    }

    public void setTrigger(Map<String, Object> trigger) {
        this.trigger = trigger == null ? new LinkedHashMap<>() : trigger;
    }

    public List<StepIntent> getSteps() {
        return steps;
    }

    public void setSteps(List<StepIntent> steps) {
        this.steps = steps == null ? new ArrayList<>() : steps;
    }

    /**
     * Optional {@code abortOn: { status: [ids] | id, then: <step> }} - a {@code -transitioned} of the
     * trigger entity into any listed EntityStatus seed id cancels the in-flight process (its pending
     * user tasks, parked waits and armed timers), optionally running the {@code then} step chain first.
     * Free-form map, interpreted by the process generator and validator.
     */
    public Map<String, Object> getAbortOn() {
        return abortOn;
    }

    public void setAbortOn(Map<String, Object> abortOn) {
        this.abortOn = abortOn == null ? new LinkedHashMap<>() : abortOn;
    }
}
