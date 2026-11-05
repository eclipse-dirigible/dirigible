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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * A report's movable date window (dirigible #8506): {@code period: { target: orderedOn, default:
 * last30Days, presets: [currentMonth, previousMonth] }}.
 *
 * <p>
 * Shorthand for the from/to {@link ReportParameterIntent} pair - {@code { name: fromDate, target,
 * op: ge }} and {@code { name: toDate, target, op: le }} - whose {@code initial}s are the relative
 * moments of the {@link #getDefaultPreset() default preset}. The parser appends that pair to the
 * report's {@code parameters} before validating it, so everything downstream (the query, the attach
 * binding, the export pass-through) sees two ordinary parameters. The {@code .report} also carries
 * the period itself, with each preset's moments, so the report page can offer the presets beside
 * the two date inputs and name the active window in its title.
 */
public class ReportPeriodIntent {

    /** A preset's window, as the two relative moments of its bounds - both inclusive. */
    public record Window(String from, String to) {
    }

    /**
     * The preset that leaves the two inputs to the user: no moments, the all-time window by default.
     */
    public static final String CUSTOM = "custom";

    /** The known presets, in the order the report page lists them. */
    public static final Map<String, Window> PRESETS;

    static {
        Map<String, Window> presets = new LinkedHashMap<>();
        presets.put("last7Days", new Window("CURRENT_DATE-P6D", "CURRENT_DATE"));
        presets.put("last30Days", new Window("CURRENT_DATE-P29D", "CURRENT_DATE"));
        presets.put("currentMonth", new Window("START_OF_MONTH", "START_OF_MONTH+P1M-P1D"));
        presets.put("previousMonth", new Window("START_OF_MONTH-P1M", "START_OF_MONTH-P1D"));
        presets.put("currentQuarter", new Window("START_OF_QUARTER", "START_OF_QUARTER+P3M-P1D"));
        presets.put("lastQuarter", new Window("START_OF_QUARTER-P3M", "START_OF_QUARTER-P1D"));
        presets.put("yearToDate", new Window("START_OF_YEAR", "CURRENT_DATE"));
        presets.put("currentYear", new Window("START_OF_YEAR", "START_OF_YEAR+P1Y-P1D"));
        presets.put("previousYear", new Window("START_OF_YEAR-P1Y", "START_OF_YEAR-P1D"));
        PRESETS = Collections.unmodifiableMap(presets);
    }

    /** The default preset when none is declared. */
    public static final String DEFAULT_PRESET = "last30Days";

    /** The presets offered when none are declared - the windows an accounting reader asks for first. */
    public static final List<String> DEFAULT_PRESETS = List.of("last30Days", "currentMonth", "previousMonth", "lastQuarter", "yearToDate");

    /** The filtered date: a {@code date}/{@code timestamp} field of the source, or a one-hop path. */
    private String target;
    /** The preset the report opens with; {@code custom} opens on all time. Absent -> last30Days. */
    @SerializedName("default")
    private String defaultPreset;
    /** The presets the report page offers, in order; {@code custom} is always offered on top. */
    private List<String> presets;
    /** The name of the generated lower-bound parameter. Absent -> {@code fromDate}. */
    private String from;
    /** The name of the generated upper-bound parameter. Absent -> {@code toDate}. */
    private String to;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDefaultPreset() {
        return defaultPreset;
    }

    public void setDefaultPreset(String defaultPreset) {
        this.defaultPreset = defaultPreset;
    }

    public List<String> getPresets() {
        return presets;
    }

    public void setPresets(List<String> presets) {
        this.presets = presets;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    /** The default preset as authored (trimmed), or {@link #DEFAULT_PRESET}. */
    public String getEffectiveDefault() {
        return defaultPreset == null || defaultPreset.isBlank() ? DEFAULT_PRESET : defaultPreset.trim();
    }

    /** The offered presets as authored (trimmed, blanks dropped), or {@link #DEFAULT_PRESETS}. */
    public List<String> getEffectivePresets() {
        if (presets == null || presets.isEmpty()) {
            return DEFAULT_PRESETS;
        }
        List<String> names = new ArrayList<>();
        for (String preset : presets) {
            if (preset != null && !preset.isBlank()) {
                names.add(preset.trim());
            }
        }
        return names;
    }

    /** The lower-bound parameter's name. */
    public String getFromName() {
        return from == null || from.isBlank() ? "fromDate" : from.trim();
    }

    /** The upper-bound parameter's name. */
    public String getToName() {
        return to == null || to.isBlank() ? "toDate" : to.trim();
    }
}
