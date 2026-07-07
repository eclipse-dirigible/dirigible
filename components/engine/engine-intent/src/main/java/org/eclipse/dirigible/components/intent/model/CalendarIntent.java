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

/**
 * Calendar-view configuration for an entity rendered with {@code view: calendar}. The entity's
 * records become events on a Harmonia {@code x-h-calendar}; this block names which fields supply each
 * part of an event. Only {@link #start} is required.
 *
 * <pre>
 * view: calendar
 * calendar:
 *   start: date          # the date / datetime field placed on the timeline (required)
 *   end: endDate         # optional end field for multi-hour / multi-day events
 *   title: description   # field or to-one relation whose label titles the event pill
 *   color: Status        # field or to-one relation the event colour is keyed by (categorical)
 *   initialView: month   # month (default) | week | day
 * </pre>
 */
public class CalendarIntent {

    /** The date or datetime field placed on the timeline (required). */
    private String start;
    /** Optional end date/datetime field; when absent the event is a point / all-day on {@link #start}. */
    private String end;
    /** Field or to-one relation whose label titles the event pill; defaults to the document title / name. */
    private String title;
    /** Field or to-one relation the event colour is keyed by (categorical palette). */
    private String color;
    /** Initial view: {@code month} (default), {@code week} or {@code day}. */
    private String initialView;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getInitialView() {
        return initialView;
    }

    public void setInitialView(String initialView) {
        this.initialView = initialView;
    }
}
