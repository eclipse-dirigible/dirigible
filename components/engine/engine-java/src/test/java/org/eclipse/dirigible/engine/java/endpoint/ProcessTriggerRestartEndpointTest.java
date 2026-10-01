/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.eclipse.dirigible.engine.java.runtime.ClientBeanFactory;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.sdk.bpm.ProcessTrigger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The restart endpoint routes to the trigger of the named process and turns the trigger's two
 * refusals into the two HTTP answers an operator can act on.
 */
class ProcessTriggerRestartEndpointTest {

    private static final class RecordingTrigger implements ProcessTrigger {
        private final String process;
        private String restartedId;

        RecordingTrigger(String process) {
            this.process = process;
        }

        @Override
        public String process() {
            return process;
        }

        @Override
        public String entity() {
            return "Timesheet";
        }

        @Override
        public String restart(String id) {
            if ("missing".equals(id)) {
                throw new IllegalArgumentException("No Timesheet [missing]");
            }
            if ("running".equals(id)) {
                throw new IllegalStateException("Timesheet [running] still has instance [77] running");
            }
            restartedId = id;
            return "instance-of-" + id;
        }
    }

    @Test
    void theTriggerOfTheNamedProcessIsRestarted() {
        RecordingTrigger other = new RecordingTrigger("OtherFlow");
        RecordingTrigger wanted = new RecordingTrigger("TimesheetApproval");
        ProcessTriggerRestartEndpoint endpoint = endpoint(other, wanted);

        assertEquals("instance-of-42", endpoint.restart("TimesheetApproval", "42")
                                               .getBody());
        assertEquals("42", wanted.restartedId);
        assertEquals(null, other.restartedId, "the other process's trigger is left alone");
    }

    @Test
    void anUnknownProcessIsNotFound() {
        ProcessTriggerRestartEndpoint endpoint = endpoint(new RecordingTrigger("TimesheetApproval"));

        ResponseStatusException refused = assertThrows(ResponseStatusException.class, () -> endpoint.restart("NoSuchFlow", "42"));
        assertEquals(HttpStatus.NOT_FOUND, refused.getStatusCode());
        assertTrue(refused.getReason()
                          .contains("NoSuchFlow"));
    }

    @Test
    void anUnknownRecordIsNotFound() {
        ProcessTriggerRestartEndpoint endpoint = endpoint(new RecordingTrigger("TimesheetApproval"));

        ResponseStatusException refused =
                assertThrows(ResponseStatusException.class, () -> endpoint.restart("TimesheetApproval", "missing"));
        assertEquals(HttpStatus.NOT_FOUND, refused.getStatusCode());
    }

    @Test
    void aStillRunningInstanceIsAConflict() {
        ProcessTriggerRestartEndpoint endpoint = endpoint(new RecordingTrigger("TimesheetApproval"));

        ResponseStatusException refused =
                assertThrows(ResponseStatusException.class, () -> endpoint.restart("TimesheetApproval", "running"));
        assertEquals(HttpStatus.CONFLICT, refused.getStatusCode());
        assertTrue(refused.getReason()
                          .contains("[77]"));
    }

    @Test
    void beforeAnyClientGenerationThereIsNoTrigger() {
        ClientBeansHolder holder = mock(ClientBeansHolder.class);
        when(holder.current()).thenReturn(null);
        ProcessTriggerRestartEndpoint endpoint = new ProcessTriggerRestartEndpoint(holder);

        ResponseStatusException refused = assertThrows(ResponseStatusException.class, () -> endpoint.restart("TimesheetApproval", "42"));
        assertEquals(HttpStatus.NOT_FOUND, refused.getStatusCode());
    }

    private static ProcessTriggerRestartEndpoint endpoint(ProcessTrigger... triggers) {
        ClientBeanFactory factory = mock(ClientBeanFactory.class);
        when(factory.getAll(ProcessTrigger.class)).thenReturn(List.of(triggers));
        ClientBeansHolder holder = mock(ClientBeansHolder.class);
        when(holder.current()).thenReturn(factory);
        return new ProcessTriggerRestartEndpoint(holder);
    }
}
