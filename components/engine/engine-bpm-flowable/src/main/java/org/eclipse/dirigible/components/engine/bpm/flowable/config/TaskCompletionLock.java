/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * Makes a task completion take the task's row lock BEFORE the process moves on from the task, so of
 * two concurrent completions of one task only the one that answers 2xx runs the service steps after
 * it (issue #8547).
 *
 * <p>
 * The engine writes nothing until its command ends: two COMPLETEs of the same task (the double
 * click, or an Approve racing a Reject) both read the task, both run the service tasks that follow
 * it synchronously, and only the flush at the end tells them apart - the loser fails the revision
 * check and its command rolls back. The engine's own rows roll back with it, but the
 * {@code JavaTask} delegates write the application's records through the data store, on another
 * database and in transactions of their own: the losing completion's number stamp, status setter
 * and the events they publish were already committed. A 409 answer (#8104 / #8416) on top of a
 * document approved and numbered by that very click.
 *
 * <p>
 * The delegates cannot join the engine's transaction - the engine runs on the system database and
 * the records live elsewhere - so the loser is stopped before any of them runs. On the task's
 * {@code TASK_COMPLETED} event, which the engine fires after the task's own complete listeners and
 * before it plans the step that follows the task, this touches the task row with
 * {@code UPDATE ... SET REV_ = REV_ WHERE ID_ = ? AND REV_ = ?} on the command's own connection.
 * The statement changes nothing, but it takes the row lock and keeps it until the command ends:
 * <ul>
 * <li>the first completion holds it while its service steps run, and its own flush is unaffected
 * (the revision is untouched);</li>
 * <li>a concurrent completion waits for that commit, then finds the row deleted (or at another
 * revision) and fails with Flowable's optimistic-lock failure - before any delegate of its own ran.
 * The endpoints already answer that failure, on a task that is gone, with 409 (#8416).</li>
 * </ul>
 * A task created in the same command has no row to lock yet, and no other session can see it, so it
 * is skipped.
 */
public final class TaskCompletionLock implements FlowableEventListener {

    private static final String TASK_TABLE = "ACT_RU_TASK";

    /**
     * Registers the lock on an engine configuration, keeping any typed listeners already registered.
     *
     * @param configuration the engine configuration, before the engine is built
     */
    public static void register(AbstractEngineConfiguration configuration) {
        Map<String, List<FlowableEventListener>> typed =
                configuration.getTypedEventListeners() == null ? new HashMap<>() : new HashMap<>(configuration.getTypedEventListeners());
        List<FlowableEventListener> listeners =
                new ArrayList<>(typed.getOrDefault(FlowableEngineEventType.TASK_COMPLETED.name(), List.of()));
        listeners.add(new TaskCompletionLock());
        typed.put(FlowableEngineEventType.TASK_COMPLETED.name(), listeners);
        configuration.setTypedEventListeners(typed);
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableEntityEvent entityEvent) || !(entityEvent.getEntity() instanceof TaskEntity task)
                || task.isInserted()) {
            return;
        }
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            return;
        }
        DbSqlSession session = commandContext.getSession(DbSqlSession.class);
        String table = session.getDbSqlSessionFactory()
                              .getDatabaseTablePrefix()
                + TASK_TABLE;
        Connection connection = session.getSqlSession()
                                       .getConnection();
        int locked;
        try (PreparedStatement statement = connection.prepareStatement("UPDATE " + table + " SET REV_ = REV_ WHERE ID_ = ? AND REV_ = ?")) {
            statement.setString(1, task.getId());
            statement.setInt(2, task.getRevision());
            locked = statement.executeUpdate();
        } catch (SQLException e) {
            throw new FlowableException("Could not lock task [" + task.getId() + "] for its completion", e);
        }
        if (locked == 0) {
            throw new FlowableOptimisticLockingException("Task [" + task.getId()
                    + "] was completed or changed by another transaction concurrently - this completion runs none of its steps");
        }
    }

    /** The failure must reach the command, so that it rolls back before the next step runs. */
    @Override
    public boolean isFailOnException() {
        return true;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
