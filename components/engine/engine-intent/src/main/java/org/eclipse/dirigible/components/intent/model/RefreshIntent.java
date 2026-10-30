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

import com.google.gson.annotations.SerializedName;

/**
 * An entity's {@code refresh:} (issue #8375): its pages re-read themselves while a record they show
 * is in one of the listed statuses - a status the server moves on its own (a scheduled job, an
 * inbound message, another user), which would otherwise look stuck until the user pressed Refresh.
 *
 * <pre>
 * refresh:
 *   while: [BOOKED, IN_TRANSIT]   # required: the entity's own statuses, by seeded name or id
 *   every: PT10S                  # optional ISO-8601 duration, PT5S..PT5M, default PT10S
 * </pre>
 *
 * The names are resolved to seed ids on the raw tree ({@code StatusSymbolResolver}), so after the
 * parse {@link #getStatuses()} holds numbers; the parser refuses anything else.
 */
public class RefreshIntent {

    /** The poll interval when {@code every:} is not declared. */
    public static final String DEFAULT_EVERY = "PT10S";

    /** The statuses a record is "moving" in - seed ids once the parse resolved the names. */
    @SerializedName("while")
    private List<Object> statuses = new ArrayList<>();

    /** The poll interval, an ISO-8601 duration. */
    private String every;

    public List<Object> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<Object> statuses) {
        this.statuses = statuses;
    }

    public String getEvery() {
        return every;
    }

    public void setEvery(String every) {
        this.every = every;
    }

    /**
     * The statuses as seed ids, in the declared order - a value that is no integer is left out (the
     * parser reports it).
     *
     * @return the seed ids, never {@code null}
     */
    public List<Integer> statusIds() {
        List<Integer> ids = new ArrayList<>();
        if (statuses == null) {
            return ids;
        }
        for (Object status : statuses) {
            Integer id = idOf(status);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * One {@code while:} entry as a seed id, or {@code null} when it is none.
     *
     * @param status the entry - a number after the parse, or a string
     * @return the seed id, or {@code null}
     */
    public static Integer idOf(Object status) {
        if (status instanceof Number number) {
            double value = number.doubleValue();
            return value == Math.rint(value) ? (int) value : null;
        }
        if (status == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(status)
                                         .trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * The poll interval in seconds - {@code every:} or the {@link #DEFAULT_EVERY}. Only meaningful once
     * the parser accepted the duration.
     *
     * @return the interval in seconds
     */
    public long everySeconds() {
        String duration = every == null || every.isBlank() ? DEFAULT_EVERY : every.trim();
        return java.time.Duration.parse(duration)
                                 .getSeconds();
    }
}
