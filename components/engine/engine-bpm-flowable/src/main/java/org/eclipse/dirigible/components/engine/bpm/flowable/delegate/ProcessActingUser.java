/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import java.util.Optional;

import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * The user a process step acts for (issue #8690): the one who completed the instance's latest user
 * task, bound as the thread's {@link OriginatingUser} while the step runs. A step after an
 * asynchronous boundary runs on the job executor, where no request carries a user, so a write it
 * makes - a workflow setter moving the record's status - was attributed to nobody. A user already
 * bound (the step runs inside a handler of an event a user raised) is kept, and the request's own
 * user wins over both, as it does everywhere.
 */
final class ProcessActingUser {

    private ProcessActingUser() {}

    /**
     * Binds the user the step acts for until the returned binding is closed.
     *
     * @param execution the step's execution
     * @return the binding to close
     */
    static OriginatingUser.Binding bind(DelegateExecution execution) {
        Optional<String> bound = OriginatingUser.get();
        if (bound.isPresent()) {
            return OriginatingUser.bind(bound.get());
        }
        Object completedBy = execution.getVariable(BpmProviderFlowable.LAST_COMPLETED_BY);
        return OriginatingUser.bind(completedBy instanceof String user ? user : null);
    }
}
