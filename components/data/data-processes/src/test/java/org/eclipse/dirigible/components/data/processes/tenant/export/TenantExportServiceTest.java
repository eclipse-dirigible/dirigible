/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

/**
 * The rules of the tenant exports: one at a time, a tenant sees its own only, an archive is there
 * from completion to expiry, and an export whose process will not complete it ends as failed.
 */
class TenantExportServiceTest {

    private static final String ACME = "acme";
    private static final String GLOBEX = "globex";
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    /** The rows, by id - what the mocked repository reads and writes. */
    private final Map<Long, TenantExport> rows = new LinkedHashMap<>();

    private TenantExportRepository exports;
    private TenantExportProcess process;
    private TenantExportArchives archives;
    private TenantContext tenantContext;
    private Instant now;
    private TenantExportService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Throwable {
        now = NOW;
        exports = mock(TenantExportRepository.class);
        when(exports.save(any())).thenAnswer(invocation -> {
            TenantExport export = invocation.getArgument(0);
            if (export.getId() == null) {
                export.setId(rows.size() + 1L);
            }
            rows.put(export.getId(), export);
            return export;
        });
        when(exports.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(rows.get(invocation.<Long>getArgument(0))));
        when(exports.findByIdAndTenantId(anyLong(), anyString())).thenAnswer(
                invocation -> Optional.ofNullable(rows.get(invocation.<Long>getArgument(0)))
                                      .filter(export -> export.getTenantId()
                                                              .equals(invocation.getArgument(1))));
        when(exports.existsByTenantIdAndStatusIn(anyString(), any())).thenAnswer(invocation -> rows.values()
                                                                                                   .stream()
                                                                                                   .anyMatch(export -> export.getTenantId()
                                                                                                                             .equals(invocation.getArgument(
                                                                                                                                     0))
                                                                                                           && invocation.<Collection<TenantExportStatus>>getArgument(
                                                                                                                   1)
                                                                                                                        .contains(
                                                                                                                                export.getStatus())));
        when(exports.countByTenantIdAndStatus(anyString(), any())).thenAnswer(invocation -> rows.values()
                                                                                                .stream()
                                                                                                .filter(export -> export.getTenantId()
                                                                                                                        .equals(invocation.getArgument(
                                                                                                                                0))
                                                                                                        && export.getStatus() == invocation.getArgument(
                                                                                                                1))
                                                                                                .count());
        when(exports.findByStatusIn(any())).thenAnswer(invocation -> rows.values()
                                                                         .stream()
                                                                         .filter(export -> invocation.<Collection<TenantExportStatus>>getArgument(
                                                                                 0)
                                                                                                     .contains(export.getStatus()))
                                                                         .toList());
        when(exports.findByExpiresAtBefore(any())).thenAnswer(invocation -> rows.values()
                                                                                .stream()
                                                                                .filter(export -> export.getExpiresAt() != null
                                                                                        && export.getExpiresAt()
                                                                                                 .isBefore(invocation.getArgument(0)))
                                                                                .toList());
        when(exports.findByTenantIdOrderByRequestedAtDescIdDesc(anyString())).thenAnswer(invocation -> {
            List<TenantExport> found = new ArrayList<>(rows.values()
                                                           .stream()
                                                           .filter(export -> export.getTenantId()
                                                                                   .equals(invocation.getArgument(0)))
                                                           .toList());
            found.sort((left, right) -> right.getId()
                                             .compareTo(left.getId()));
            return found;
        });
        org.mockito.Mockito.doAnswer(invocation -> rows.remove(invocation.<Long>getArgument(0)))
                           .when(exports)
                           .deleteById(anyLong());

        process = mock(TenantExportProcess.class);
        when(process.start(anyLong())).thenAnswer(invocation -> "process-" + invocation.getArgument(0));
        when(process.failure(anyString())).thenReturn(Optional.empty());
        archives = mock(TenantExportArchives.class);
        tenantContext = mock(TenantContext.class);
        when(tenantContext.execute(anyString(), any(CallableResultAndException.class))).thenAnswer(
                invocation -> invocation.<CallableResultAndException<Object, Throwable>>getArgument(1)
                                        .call());
        // A mock manager runs every TransactionTemplate callback inline.
        service = new TenantExportService(exports, process, archives, tenantContext, mock(PlatformTransactionManager.class), () -> now);
    }

    @Test
    void anExportIsRequestedAndItsProcessStarted() {
        TenantExportView export = service.request(ACME, "ann@example.com");

        assertEquals(TenantExportStatus.REQUESTED, export.status());
        assertEquals("ann@example.com", export.requestedBy());
        assertEquals(NOW, export.requestedAt());
        assertEquals("process-1", rows.get(export.id())
                                      .getProcessInstanceId());
    }

    @Test
    void aSecondExportIsRefusedWhileOneIsRequestedOrRunning() {
        long first = service.request(ACME, "ann@example.com")
                            .id();
        assertEquals(HttpStatus.CONFLICT, refusal(() -> service.request(ACME, "ben@example.com")));

        service.markRunning(first);
        assertEquals(HttpStatus.CONFLICT, refusal(() -> service.request(ACME, "ben@example.com")));

        // another tenant's export is no reason to refuse, and an ended one is none either
        service.request(GLOBEX, "cid@example.com");
        service.complete(first, "/__EXPORTS/tenant-exports/1/acme.zip", 10);
        assertEquals(TenantExportStatus.REQUESTED, service.request(ACME, "ben@example.com")
                                                          .status());
    }

    @Test
    void anExportIsRefusedWhileTheTenantKeepsAsManyArchivesAsItMay() {
        int limit = TenantExportService.maxArchives();
        long last = 0;
        for (int i = 0; i < limit; i++) {
            last = service.request(ACME, "ann@example.com")
                          .id();
            service.complete(last, "/__EXPORTS/tenant-exports/" + last + "/acme.zip", 10);
        }
        assertEquals(HttpStatus.CONFLICT, refusal(() -> service.request(ACME, "ann@example.com")));
        // the limit is each tenant's own
        service.request(GLOBEX, "cid@example.com");

        // deleting one makes room for the next
        service.delete(ACME, last);
        assertEquals(TenantExportStatus.REQUESTED, service.request(ACME, "ann@example.com")
                                                          .status());
    }

    @Test
    void anExportThatCannotStartIsFailed() {
        when(process.start(anyLong())).thenThrow(new IllegalStateException("no definition"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, refusal(() -> service.request(ACME, "ann@example.com")));

        TenantExport failed = rows.get(1L);
        assertEquals(TenantExportStatus.FAILED, failed.getStatus());
        assertTrue(failed.getError()
                         .contains("no definition"));
        // so the next request is not refused for it
        org.mockito.Mockito.doReturn("process-2")
                           .when(process)
                           .start(anyLong());
        assertEquals(TenantExportStatus.REQUESTED, service.request(ACME, "ann@example.com")
                                                          .status());
    }

    @Test
    void anUnknownExportAndOneOfAnotherTenantAreNotFound() {
        long acmes = service.request(ACME, "ann@example.com")
                            .id();

        assertEquals(HttpStatus.NOT_FOUND, refusal(() -> service.get(ACME, 99)));
        assertEquals(HttpStatus.NOT_FOUND, refusal(() -> service.get(GLOBEX, acmes)));
        assertEquals(HttpStatus.NOT_FOUND, refusal(() -> service.downloadable(GLOBEX, acmes)));
        assertEquals(HttpStatus.NOT_FOUND, refusal(() -> service.delete(GLOBEX, acmes)));
        assertEquals(List.of(), service.list(GLOBEX));
        assertEquals(1, service.list(ACME)
                               .size());
    }

    @Test
    void anArchiveIsThereFromCompletionToExpiry() {
        long id = service.request(ACME, "ann@example.com")
                         .id();
        assertEquals(HttpStatus.CONFLICT, refusal(() -> service.downloadable(ACME, id)), "requested: no archive yet");
        service.markRunning(id);
        assertEquals(HttpStatus.CONFLICT, refusal(() -> service.downloadable(ACME, id)), "running: no archive yet");

        service.complete(id, "/__EXPORTS/tenant-exports/1/acme.zip", 2048);

        TenantExport completed = service.downloadable(ACME, id);
        assertEquals("/__EXPORTS/tenant-exports/1/acme.zip", completed.getArchivePath());
        assertEquals(2048L, completed.getSizeBytes());
        assertEquals(NOW, completed.getCompletedAt());
        assertEquals(NOW.plus(Duration.ofDays(7)), completed.getExpiresAt(), "the default retention is seven days");

        now = NOW.plus(Duration.ofDays(7));
        assertEquals(HttpStatus.GONE, refusal(() -> service.downloadable(ACME, id)));
    }

    @Test
    void aRunningExportCannotBeDeletedAndAnEndedOneGoesWithItsArchive() throws IOException {
        long id = service.request(ACME, "ann@example.com")
                         .id();
        assertEquals(HttpStatus.CONFLICT, refusal(() -> service.delete(ACME, id)));
        service.markRunning(id);
        assertEquals(HttpStatus.CONFLICT, refusal(() -> service.delete(ACME, id)));
        verify(archives, never()).deleteExportFolder(anyLong());

        service.complete(id, "/__EXPORTS/tenant-exports/1/acme.zip", 10);
        service.delete(ACME, id);

        verify(archives).deleteExportFolder(id);
        assertFalse(rows.containsKey(id));
    }

    @Test
    void anExportWhoseProcessFailedEndsAsFailedAndLeavesNothingBehind() throws IOException {
        long id = service.request(ACME, "ann@example.com")
                         .id();
        service.markRunning(id);

        service.failStuck();
        assertEquals(TenantExportStatus.RUNNING, rows.get(id)
                                                     .getStatus(),
                "a process that still runs is left alone");

        when(process.failure("process-1")).thenReturn(Optional.of("Failed to export table [LIBRARY_BOOK]"));
        service.failStuck();

        TenantExport failed = rows.get(id);
        assertEquals(TenantExportStatus.FAILED, failed.getStatus());
        assertEquals("Failed to export table [LIBRARY_BOOK]", failed.getError());
        assertNotNull(failed.getExpiresAt(), "a failed export is cleared away like any other");
        verify(process).cancel("process-1");
        verify(archives).deleteExportFolder(id);
        verify(tenantContext, org.mockito.Mockito.atLeastOnce()).execute(org.mockito.ArgumentMatchers.eq(ACME),
                any(CallableResultAndException.class));
    }

    @Test
    void anExportThatNeverGotAProcessFailsOnlyAfterTheGracePeriod() {
        TenantExport orphan = exports.save(new TenantExport(ACME, "ann@example.com", NOW));

        service.failStuck();
        assertEquals(TenantExportStatus.REQUESTED, orphan.getStatus());

        now = NOW.plus(TenantExportService.START_GRACE)
                 .plusSeconds(1);
        service.failStuck();
        assertEquals(TenantExportStatus.FAILED, orphan.getStatus());
        verify(process, never()).cancel(anyString());
    }

    @Test
    void aCompletedExportIsNotFailedByALateLookAtItsProcess() {
        long id = service.request(ACME, "ann@example.com")
                         .id();
        service.complete(id, "/__EXPORTS/tenant-exports/1/acme.zip", 10);

        assertFalse(service.fail(id, "its process is gone"));
        assertEquals(TenantExportStatus.COMPLETED, rows.get(id)
                                                       .getStatus());
        assertNull(rows.get(id)
                       .getError());
    }

    @Test
    void expiredExportsAreDeletedWithTheirArchivesAndTheOthersKept() throws IOException {
        long expired = service.request(ACME, "ann@example.com")
                              .id();
        service.complete(expired, "/__EXPORTS/tenant-exports/1/acme.zip", 10);
        now = NOW.plus(Duration.ofDays(3));
        long fresh = service.request(ACME, "ann@example.com")
                            .id();
        service.complete(fresh, "/__EXPORTS/tenant-exports/2/acme.zip", 10);
        long running = service.request(GLOBEX, "cid@example.com")
                              .id();

        now = NOW.plus(Duration.ofDays(7))
                 .plusSeconds(1);
        service.expire();

        assertFalse(rows.containsKey(expired));
        assertTrue(rows.containsKey(fresh));
        assertTrue(rows.containsKey(running));
        verify(archives).deleteExportFolder(expired);
        verify(archives, never()).deleteExportFolder(fresh);
    }

    @Test
    void anArchiveThatCannotBeDeletedDoesNotKeepAnExpiredRow() throws IOException {
        long id = service.request(ACME, "ann@example.com")
                         .id();
        service.complete(id, "/__EXPORTS/tenant-exports/1/acme.zip", 10);
        doThrow(new IOException("the CMS is away")).when(archives)
                                                   .deleteExportFolder(id);

        now = NOW.plus(Duration.ofDays(8));
        service.expire();

        assertFalse(rows.containsKey(id));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theRowsOfATenantThatIsGoneAreDeleted() throws Throwable {
        long id = service.request(ACME, "ann@example.com")
                         .id();
        when(tenantContext.execute(anyString(), any(CallableResultAndException.class))).thenThrow(new TenantNotFoundException(ACME));

        service.failStuck();

        assertFalse(rows.containsKey(id));
    }

    @Test
    void theProcessCannotTakeUpAnExportThatAlreadyEnded() {
        long id = service.request(ACME, "ann@example.com")
                         .id();
        service.fail(id, "stopped");

        assertThrows(IllegalStateException.class, () -> service.markRunning(id));
        assertThrows(IllegalStateException.class, () -> service.markRunning(99));
    }

    private static HttpStatus refusal(Runnable call) {
        ResponseStatusException refused = assertThrows(ResponseStatusException.class, call::run);
        return HttpStatus.resolve(refused.getStatusCode()
                                         .value());
    }
}
