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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.base.context.ThreadLanguage;
import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

/**
 * A process step acts for the user who completed the instance's latest task (issue #8690), unless
 * the thread already acts for a user - and in that user's language (issue #8776).
 */
class ProcessActingUserTest {

    @Test
    void aStepActsForTheUserWhoCompletedTheLatestTask() {
        DelegateExecution execution = execution("approver");

        try (OriginatingUser.Binding binding = ProcessActingUser.bind(execution)) {
            assertEquals(Optional.of("approver"), OriginatingUser.get());
        }
        assertTrue(OriginatingUser.get()
                                  .isEmpty());
    }

    @Test
    void aUserTheThreadAlreadyActsForIsKept() {
        try (OriginatingUser.Binding raisedBy = OriginatingUser.bind("requester")) {
            try (OriginatingUser.Binding binding = ProcessActingUser.bind(execution("approver"))) {
                assertEquals(Optional.of("requester"), OriginatingUser.get());
            }
            assertEquals(Optional.of("requester"), OriginatingUser.get());
        }
    }

    @Test
    void anInstanceWithNoCompletedTaskBindsNobody() {
        try (OriginatingUser.Binding binding = ProcessActingUser.bind(execution(null))) {
            assertTrue(OriginatingUser.get()
                                      .isEmpty());
            assertTrue(ThreadLanguage.get()
                                     .isEmpty());
        }
    }

    @Test
    void aStepRunsInTheLanguageOfTheUserWhoCompletedTheLatestTask() {
        try (OriginatingUser.Binding binding = ProcessActingUser.bind(execution("approver", "bg"))) {
            assertEquals(Optional.of("bg"), ThreadLanguage.get());
        }
        assertTrue(ThreadLanguage.get()
                                 .isEmpty());
    }

    @Test
    void aLanguageTheThreadAlreadyHasIsKept() {
        try (OriginatingUser.Binding raisedBy = OriginatingUser.bind("requester", "en")) {
            try (OriginatingUser.Binding binding = ProcessActingUser.bind(execution("approver", "bg"))) {
                assertEquals(Optional.of("en"), ThreadLanguage.get());
            }
            assertEquals(Optional.of("en"), ThreadLanguage.get());
        }
    }

    private static DelegateExecution execution(String completedBy) {
        return execution(completedBy, null);
    }

    private static DelegateExecution execution(String completedBy, String language) {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable(BpmProviderFlowable.LAST_COMPLETED_BY)).thenReturn(completedBy);
        when(execution.getVariable(BpmProviderFlowable.LAST_COMPLETED_LANGUAGE)).thenReturn(language);
        return execution;
    }
}
