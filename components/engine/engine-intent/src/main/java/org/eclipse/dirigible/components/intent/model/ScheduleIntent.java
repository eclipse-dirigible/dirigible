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
 * A time-driven activity: on a cron schedule, query an entity and run an action per matching row.
 * The "overdue reminders" pattern of the declarative-glue catalog.
 *
 * <p>
 * Generates a client-Java {@code @Scheduled} {@code JobHandler} that queries {@link #entity} with a
 * typed {@code Criteria} built from {@link #where} and, for each matching row, performs exactly one
 * action:
 * <ul>
 * <li>{@link #notify} - send a mail per row (reusing the notification machinery against the row
 * entity); the notify's {@code event}/{@code channel} are unused here - the cron + {@code where}
 * are the trigger and filter; or</li>
 * <li>{@link #generate} - create a new record per row ("scheduled record generation"): map the row
 * onto a fresh target entity and save through the target's generated repository, so its create-time
 * logic (document numbering, status init, calculated fields) fires. The row is the source, so the
 * reused {@link GeneratesIntent#getFrom()} is the schedule's {@link #entity}; item cloning is out
 * of scope here (use an on-demand {@code generates} action for document-to-document cloning).</li>
 * </ul>
 * Exactly one of {@code notify} / {@code generate} must be set.
 */
public class ScheduleIntent {

    private String name;
    private String cron;
    private String entity;
    private List<ScheduleConditionIntent> where = new ArrayList<>();
    private NotificationIntent notify;
    private GeneratesIntent generate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public List<ScheduleConditionIntent> getWhere() {
        return where;
    }

    public void setWhere(List<ScheduleConditionIntent> where) {
        this.where = where == null ? new ArrayList<>() : where;
    }

    public NotificationIntent getNotify() {
        return notify;
    }

    public void setNotify(NotificationIntent notify) {
        this.notify = notify;
    }

    public GeneratesIntent getGenerate() {
        return generate;
    }

    public void setGenerate(GeneratesIntent generate) {
        this.generate = generate;
    }
}
