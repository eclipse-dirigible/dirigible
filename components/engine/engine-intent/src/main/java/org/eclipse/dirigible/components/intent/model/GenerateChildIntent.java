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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A child block of a scheduled generation ({@code schedules[].generate.children[]}): for each
 * element of a source collection, one child row of the just-generated parent. Two collection kinds:
 * {@code forEach: &#123; entity: X, match: &#123; childField: sourceField &#125; &#125;} (one child
 * per matching row of a LOCAL entity) and {@code forEach: &#123; days: workingDays
 * &#125;} (one child per working day - Mon to Fri - of the month the job runs in, the date written
 * to {@code dayField}). Children nest one more level (e.g. timesheet line to its day allocations);
 * depth is capped at two.
 */
public class GenerateChildIntent {

    /** The child entity to create (resolved in the same model as the generation target). */
    private String to;

    /** The child's to-one relation back to the generated parent record. */
    private String parent;

    /** The collection source: {@code entity} + {@code match}, or {@code days: workingDays}. */
    private Map<String, Object> forEach = new LinkedHashMap<>();

    /** Child property (PascalCased downstream) to a property of the forEach row. */
    private Map<String, String> map = new LinkedHashMap<>();

    /** Child property to a literal (or {@code now}). */
    private Map<String, String> defaults = new LinkedHashMap<>();

    /** For a {@code days} source: the child date field receiving each generated day. */
    private String dayField;

    /** Nested child blocks (one more level at most). */
    private List<GenerateChildIntent> children;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Map<String, Object> getForEach() {
        return forEach;
    }

    public void setForEach(Map<String, Object> forEach) {
        this.forEach = forEach;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults;
    }

    public String getDayField() {
        return dayField;
    }

    public void setDayField(String dayField) {
        this.dayField = dayField;
    }

    public List<GenerateChildIntent> getChildren() {
        return children;
    }

    public void setChildren(List<GenerateChildIntent> children) {
        this.children = children;
    }
}
