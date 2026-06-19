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
 * typed {@code Criteria} built from {@link #where} and, for each row, performs {@link #notify}
 * (reusing the notification machinery against the row entity). The notify's {@code event}/
 * {@code channel} are unused here - the cron + {@code where} are the trigger and filter.
 */
public class ScheduleIntent {

    private String name;
    private String cron;
    private String entity;
    private List<ScheduleConditionIntent> where = new ArrayList<>();
    private NotificationIntent notify;

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
}
