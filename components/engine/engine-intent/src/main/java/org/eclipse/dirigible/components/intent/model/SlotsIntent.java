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

/**
 * Slots-view configuration for an entity rendered with {@code view: slots} - an appointment/booking
 * picker built on the Harmonia {@code x-h-slot-picker} (a 3-day grid of selectable time slots). The
 * entity's records occupy slots; a free slot is bookable and opens the create form prefilled with
 * the chosen date+time.
 *
 * <pre>
 * view: slots
 * slots:
 *   start: at            # the datetime field a picked slot writes to (required)
 *   open: "08:00"        # first slot of the day (default 08:00)
 *   close: "18:00"       # exclusive end of the day (default 18:00)
 *   step: 30             # slot length in minutes (default 30)
 *   disabledDays: [0, 6] # weekdays always closed (0 = Sunday .. 6 = Saturday)
 * </pre>
 */
public class SlotsIntent {

    /** The datetime field a picked slot writes to (required). */
    private String start;
    /** First slot of the day as {@code HH:MM} (default {@code 08:00}). */
    private String open;
    /** Exclusive end of the day as {@code HH:MM} (default {@code 18:00}). */
    private String close;
    /** Slot length in minutes (default {@code 30}). */
    private Integer step;
    /** Weekdays always closed ({@code 0} = Sunday .. {@code 6} = Saturday). */
    private List<Integer> disabledDays = new ArrayList<>();

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getOpen() {
        return open;
    }

    public void setOpen(String open) {
        this.open = open;
    }

    public String getClose() {
        return close;
    }

    public void setClose(String close) {
        this.close = close;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public List<Integer> getDisabledDays() {
        return disabledDays;
    }

    public void setDisabledDays(List<Integer> disabledDays) {
        this.disabledDays = disabledDays == null ? new ArrayList<>() : disabledDays;
    }
}
