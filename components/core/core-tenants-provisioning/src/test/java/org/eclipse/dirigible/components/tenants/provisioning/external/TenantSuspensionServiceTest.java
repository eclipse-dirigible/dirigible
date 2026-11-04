/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.tenant.TenantSuspensionParticipant;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The status route's transition table, cell by cell, and the order of a suspend and a resume: the
 * status before the first participant on suspend, the participants in reverse and then the status
 * and the {@code resumed} hook on resume (#8477).
 */
class TenantSuspensionServiceTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final List<String> calls = new ArrayList<>();

    private RecordingParticipant jobs;
    private RecordingParticipant listeners;
    private TenantSuspensionService service;

    @BeforeEach
    void wire() {
        jobs = new RecordingParticipant("jobs", 100);
        listeners = new RecordingParticipant("listeners", 200);
        TenantRegistrationService registrationService =
                new TenantRegistrationService(tenantService, mock(TenantInitializationStatusCalculator.class));
        // registered out of order on purpose: the service sorts them
        service = new TenantSuspensionService(tenantService, registrationService, List.of(listeners, jobs), "default-tenant");
        when(tenantService.save(any())).thenAnswer(invocation -> {
            Tenant saved = invocation.getArgument(0);
            calls.add("save " + saved.getStatus());
            return saved;
        });
    }

    @Test
    void suspendSavesTheStatusBeforeTheFirstParticipantAndRunsThemInOrder() {
        Tenant tenant = known("acme", TenantStatus.PROVISIONED);

        TenantProvisioningState state = service.changeStatus("acme", TenantStatus.SUSPENDED);

        assertEquals(List.of("save SUSPENDED", "suspend jobs", "suspend listeners"), calls);
        assertEquals(TenantStatus.SUSPENDED, tenant.getStatus());
        assertEquals(TenantStatus.SUSPENDED, state.status(), "answers the tenant as GET does");
    }

    @Test
    void resumeRunsTheParticipantsInReverseThenSavesTheStatusThenReconciles() {
        Tenant tenant = known("acme", TenantStatus.SUSPENDED);

        TenantProvisioningState state = service.changeStatus("acme", TenantStatus.PROVISIONED);

        assertEquals(List.of("resume listeners", "resume jobs", "save PROVISIONED", "resumed jobs", "resumed listeners"), calls);
        assertEquals(TenantStatus.PROVISIONED, tenant.getStatus());
        assertEquals(TenantStatus.PROVISIONED, state.status());
    }

    /**
     * Suspending a suspended tenant runs the suspend half again - how a half-done suspend is finished.
     */
    @Test
    void suspendingASuspendedTenantRunsTheSuspendHalfAgain() {
        known("acme", TenantStatus.SUSPENDED);

        service.changeStatus("acme", TenantStatus.SUSPENDED);

        assertEquals(List.of("suspend jobs", "suspend listeners"), calls, "nothing to save, every participant again");
    }

    /**
     * Resuming a provisioned tenant runs the resume half again - how a half-done resume is finished.
     */
    @Test
    void resumingAProvisionedTenantRunsTheResumeHalfAgain() {
        known("acme", TenantStatus.PROVISIONED);

        service.changeStatus("acme", TenantStatus.PROVISIONED);

        assertEquals(List.of("resume listeners", "resume jobs", "resumed jobs", "resumed listeners"), calls);
    }

    @Test
    void aFailingSuspendAnswers500NamingTheParticipantAndARepeatedCallFinishesIt() {
        Tenant tenant = known("acme", TenantStatus.PROVISIONED);
        listeners.failNext = true;

        ResponseStatusException stopped =
                assertThrows(ResponseStatusException.class, () -> service.changeStatus("acme", TenantStatus.SUSPENDED));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, stopped.getStatusCode());
        assertTrue(stopped.getReason()
                          .contains("RecordingParticipant")
                && stopped.getReason()
                          .contains("SUSPENDED"),
                stopped.getReason());
        assertEquals(TenantStatus.SUSPENDED, tenant.getStatus(), "the tenant stays suspended");

        calls.clear();
        service.changeStatus("acme", TenantStatus.SUSPENDED);
        assertEquals(List.of("suspend jobs", "suspend listeners"), calls);
    }

    @Test
    void aFailingResumeAnswers500AndLeavesTheTenantSuspendedAndARepeatedCallFinishesIt() {
        Tenant tenant = known("acme", TenantStatus.SUSPENDED);
        jobs.failNext = true;

        ResponseStatusException stopped =
                assertThrows(ResponseStatusException.class, () -> service.changeStatus("acme", TenantStatus.PROVISIONED));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, stopped.getStatusCode());
        assertTrue(stopped.getReason()
                          .contains("RecordingParticipant")
                && stopped.getReason()
                          .contains("SUSPENDED"),
                stopped.getReason());
        assertEquals(TenantStatus.SUSPENDED, tenant.getStatus(), "still refused: the safe side");
        assertEquals(List.of("resume listeners", "resume jobs"), calls, "never saved, never reconciled");

        calls.clear();
        service.changeStatus("acme", TenantStatus.PROVISIONED);
        assertEquals(TenantStatus.PROVISIONED, tenant.getStatus());
        assertEquals(List.of("resume listeners", "resume jobs", "save PROVISIONED", "resumed jobs", "resumed listeners"), calls);
    }

    /** The tenant is running once saved; a hook that fails then is left to its own next pass. */
    @Test
    void aFailingResumedHookDoesNotFailTheResume() {
        Tenant tenant = known("acme", TenantStatus.SUSPENDED);
        jobs.failResumed = true;

        service.changeStatus("acme", TenantStatus.PROVISIONED);

        assertEquals(TenantStatus.PROVISIONED, tenant.getStatus());
        assertTrue(calls.contains("resumed listeners"), "the other hooks still run: " + calls);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"INITIAL", "PENDING_ACTIVATION", "DEPROVISIONING"})
    void aTenantNeitherProvisionedNorSuspendedIsAConflictInBothDirections(TenantStatus status) {
        Tenant tenant = known("acme", status);

        for (TenantStatus requested : new TenantStatus[] {TenantStatus.SUSPENDED, TenantStatus.PROVISIONED}) {
            ResponseStatusException refused = assertThrows(ResponseStatusException.class, () -> service.changeStatus("acme", requested));
            assertEquals(HttpStatus.CONFLICT, refused.getStatusCode());
        }
        assertEquals(status, tenant.getStatus());
        assertTrue(calls.isEmpty());
    }

    @Test
    void anUnknownTenantIsNotFoundInBothDirections() {
        when(tenantService.findById("ghost")).thenReturn(Optional.empty());

        for (TenantStatus requested : new TenantStatus[] {TenantStatus.SUSPENDED, TenantStatus.PROVISIONED}) {
            ResponseStatusException refused = assertThrows(ResponseStatusException.class, () -> service.changeStatus("ghost", requested));
            assertEquals(HttpStatus.NOT_FOUND, refused.getStatusCode());
        }
        assertTrue(calls.isEmpty());
    }

    @Test
    void theDefaultTenantAnInvalidIdAndAnyOtherStatusAreBadRequests() {
        known("acme", TenantStatus.PROVISIONED);

        assertBadRequest("default-tenant", TenantStatus.SUSPENDED);
        assertBadRequest("default-tenant", TenantStatus.PROVISIONED);
        assertBadRequest("not.a.tenant", TenantStatus.SUSPENDED);
        assertBadRequest("acme", TenantStatus.DEPROVISIONING);
        assertBadRequest("acme", TenantStatus.PENDING_ACTIVATION);
        assertBadRequest("acme", TenantStatus.INITIAL);
        assertBadRequest("acme", null);

        assertTrue(calls.isEmpty());
        verify(tenantService, never()).save(any());
    }

    private void assertBadRequest(String tenantId, TenantStatus requested) {
        ResponseStatusException refused = assertThrows(ResponseStatusException.class, () -> service.changeStatus(tenantId, requested));
        assertEquals(HttpStatus.BAD_REQUEST, refused.getStatusCode(), tenantId + " -> " + requested);
    }

    private Tenant known(String id, TenantStatus status) {
        Tenant tenant = new Tenant("-", id, "", id, status);
        tenant.setId(id);
        when(tenantService.findById(id)).thenReturn(Optional.of(tenant));
        return tenant;
    }

    /** A participant that records its runs, and can fail once. */
    private final class RecordingParticipant implements TenantSuspensionParticipant {

        private final String name;
        private final int order;
        private boolean failNext;
        private boolean failResumed;

        RecordingParticipant(String name, int order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public void suspend(String tenantId) {
            record("suspend");
        }

        @Override
        public void resume(String tenantId) {
            record("resume");
        }

        @Override
        public void resumed() {
            calls.add("resumed " + name);
            if (failResumed) {
                throw new IllegalStateException("the reconciler is away");
            }
        }

        private void record(String action) {
            calls.add(action + " " + name);
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("the broker is away");
            }
        }
    }
}
