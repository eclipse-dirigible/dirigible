/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.engine.java.runtime.ClientBeanFactory;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.sdk.bpm.ProcessTrigger;
import org.junit.jupiter.api.Test;

/**
 * The record of a client process is gone only when the trigger that started the process confirms it
 * (issue #8108) - anything less leaves the task to be completed as before.
 */
class ProcessTriggerRecordGuardTest {

    private static final class Trigger implements ProcessTrigger {
        private final String process;
        private final Set<String> existing;

        Trigger(String process, Set<String> existing) {
            this.process = process;
            this.existing = existing;
        }

        @Override
        public String process() {
            return process;
        }

        @Override
        public String entity() {
            return "Ticket";
        }

        @Override
        public String restart(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean recordExists(String id) {
            if ("broken".equals(id)) {
                throw new IllegalStateException("database unavailable");
            }
            return existing.contains(id);
        }
    }

    private static final class LegacyTrigger implements ProcessTrigger {

        @Override
        public String process() {
            return "TicketHandling";
        }

        @Override
        public String entity() {
            return "Ticket";
        }

        @Override
        public String restart(String id) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void aRecordItsTriggerNoLongerFindsIsGone() {
        ProcessTriggerRecordGuard guard = guard(new Trigger("OtherFlow", Set.of()), new Trigger("TicketHandling", Set.of("149")));

        assertTrue(guard.isRecordGone("TicketHandling", "150"));
        assertFalse(guard.isRecordGone("TicketHandling", "149"), "a record its trigger still finds is not gone");
    }

    @Test
    void aProcessNoTriggerStartsHasNoRecordToMiss() {
        ProcessTriggerRecordGuard guard = guard(new Trigger("TicketHandling", Set.of()));

        assertFalse(guard.isRecordGone("HandAuthored", "150"));
    }

    @Test
    void aFailedLookupIsNotAMiss() {
        ProcessTriggerRecordGuard guard = guard(new Trigger("TicketHandling", Set.of()));

        assertFalse(guard.isRecordGone("TicketHandling", "broken"), "a lookup that fails must never cancel a process");
    }

    @Test
    void aTriggerGeneratedBeforeTheLookupExistedNeverReportsAMiss() {
        ProcessTriggerRecordGuard guard = guard(new LegacyTrigger());

        assertFalse(guard.isRecordGone("TicketHandling", "150"));
    }

    @Test
    void beforeAnyClientGenerationNothingIsGone() {
        ClientBeansHolder holder = mock(ClientBeansHolder.class);
        when(holder.current()).thenReturn(null);

        assertFalse(new ProcessTriggerRecordGuard(holder).isRecordGone("TicketHandling", "150"));
    }

    private static ProcessTriggerRecordGuard guard(ProcessTrigger... triggers) {
        ClientBeanFactory factory = mock(ClientBeanFactory.class);
        when(factory.getAll(ProcessTrigger.class)).thenReturn(List.of(triggers));
        ClientBeansHolder holder = mock(ClientBeansHolder.class);
        when(holder.current()).thenReturn(factory);
        return new ProcessTriggerRecordGuard(holder);
    }
}
