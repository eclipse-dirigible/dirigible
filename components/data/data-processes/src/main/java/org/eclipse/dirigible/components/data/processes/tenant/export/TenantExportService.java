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

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * The exports of a tenant: one at a time and no more archives kept than the limit, each a row that
 * its process moves from {@code REQUESTED} through {@code RUNNING} to {@code COMPLETED} or
 * {@code FAILED}.
 *
 * <p>
 * The routes act on the caller's tenant only - a row of another tenant is "not found". The process
 * steps report through {@link #markRunning}, {@link #complete} and the housekeeping through
 * {@link #failStuck} and {@link #expire}. Every write is a transaction of its own, so a row says
 * where the export stands whatever becomes of the process step that wrote it.
 */
@Service
@Conditional(TenantExportEnabledCondition.class)
class TenantExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantExportService.class);

    /** The statuses of an export that still runs or waits to. */
    static final List<TenantExportStatus> ACTIVE = List.of(TenantExportStatus.REQUESTED, TenantExportStatus.RUNNING);

    /**
     * How long a requested export may be without a process before it is taken to have never started.
     */
    static final Duration START_GRACE = Duration.ofMinutes(10);

    private final TenantExportRepository exports;
    private final TenantExportProcess process;
    private final TenantExportArchives archives;
    private final TenantContext tenantContext;
    private final TransactionTemplate writes;
    private final Supplier<Instant> clock;

    @Autowired
    TenantExportService(TenantExportRepository exports, TenantExportProcess process, TenantExportArchives archives,
            TenantContext tenantContext, @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        this(exports, process, archives, tenantContext, transactionManager, Instant::now);
    }

    TenantExportService(TenantExportRepository exports, TenantExportProcess process, TenantExportArchives archives,
            TenantContext tenantContext, PlatformTransactionManager transactionManager, Supplier<Instant> clock) {
        this.exports = exports;
        this.process = process;
        this.archives = archives;
        this.tenantContext = tenantContext;
        this.writes = new TransactionTemplate(transactionManager);
        this.writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /**
     * How many days an archive is kept.
     *
     * @return the days, at least one
     */
    static int retentionDays() {
        return Math.max(1, DirigibleConfig.TENANT_EXPORT_RETENTION_DAYS.getIntValue());
    }

    /**
     * How many archives a tenant may keep at a time.
     *
     * @return the limit, at least one
     */
    static int maxArchives() {
        return Math.max(1, DirigibleConfig.TENANT_EXPORT_MAX_ARCHIVES.getIntValue());
    }

    /**
     * Starts an export of the tenant, which must be the current one. It is refused while another one
     * runs, and while the tenant keeps as many archives as it may - one has to be deleted first.
     *
     * @param tenantId the tenant
     * @param requestedBy who asks
     * @return the new export
     */
    TenantExportView request(String tenantId, String requestedBy) {
        TenantExport export;
        synchronized (this) {
            export = writes.execute(status -> {
                if (exports.existsByTenantIdAndStatusIn(tenantId, ACTIVE)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "An export of this tenant is still running");
                }
                int limit = maxArchives();
                if (exports.countByTenantIdAndStatus(tenantId, TenantExportStatus.COMPLETED) >= limit) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "This tenant keeps " + limit + " exports already - delete one to start another");
                }
                return exports.save(new TenantExport(tenantId, requestedBy, clock.get()));
            });
        }
        long exportId = export.getId();
        try {
            String processInstanceId = process.start(exportId);
            writes.executeWithoutResult(status -> exports.findById(exportId)
                                                         .ifPresent(started -> {
                                                             started.setProcessInstanceId(processInstanceId);
                                                             exports.save(started);
                                                         }));
        } catch (RuntimeException ex) {
            LOGGER.error("Could not start export [{}] of tenant [{}]", exportId, LogSanitizer.sanitize(tenantId), ex);
            fail(exportId, "The export could not be started: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The export could not be started", ex);
        }
        LOGGER.info("Started export [{}] of tenant [{}]", exportId, LogSanitizer.sanitize(tenantId));
        return TenantExportView.of(exports.findById(exportId)
                                          .orElse(export));
    }

    /**
     * The exports of a tenant, newest first.
     *
     * @param tenantId the tenant
     * @return the exports
     */
    List<TenantExportView> list(String tenantId) {
        return exports.findByTenantIdOrderByRequestedAtDescIdDesc(tenantId)
                      .stream()
                      .map(TenantExportView::of)
                      .toList();
    }

    /**
     * One export of a tenant.
     *
     * @param tenantId the tenant
     * @param id the export
     * @return the export
     */
    TenantExportView get(String tenantId, long id) {
        return TenantExportView.of(require(tenantId, id));
    }

    /**
     * The export whose archive may be downloaded now.
     *
     * @param tenantId the tenant
     * @param id the export
     * @return the export, completed and not expired
     */
    TenantExport downloadable(String tenantId, long id) {
        TenantExport export = require(tenantId, id);
        if (export.getStatus() != TenantExportStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The export has no archive: it is " + export.getStatus());
        }
        if (export.getExpiresAt() != null && !clock.get()
                                                   .isBefore(export.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "The export has expired");
        }
        return export;
    }

    /**
     * Opens the archive of a downloadable export. The row and the archive can part - the CMS storage of
     * a replaced instance, a restore from an older backup, a file removed by hand - and an archive that
     * is gone is the caller's answer, not a server fault: the export is expired now, so it is no longer
     * offered and the housekeeping removes it.
     *
     * @param export the export, as {@link #downloadable} returned it
     * @return the archive's content
     * @throws IOException when the CMS cannot be read
     */
    InputStream openArchive(TenantExport export) throws IOException {
        Optional<InputStream> content = archives.open(export.getArchivePath());
        if (content.isPresent()) {
            return content.get();
        }
        Instant now = clock.get();
        writes.executeWithoutResult(status -> exports.findById(export.getId())
                                                     .ifPresent(missing -> {
                                                         missing.setExpiresAt(now);
                                                         exports.save(missing);
                                                     }));
        LOGGER.warn("The archive [{}] of export [{}] of tenant [{}] is no longer in the CMS - the export is expired",
                export.getArchivePath(), export.getId(), LogSanitizer.sanitize(export.getTenantId()));
        throw new ResponseStatusException(HttpStatus.GONE, "The export's archive is no longer available");
    }

    /**
     * Deletes an export of the tenant, which must be the current one, with its archive.
     *
     * @param tenantId the tenant
     * @param id the export
     */
    void delete(String tenantId, long id) {
        TenantExport export = require(tenantId, id);
        if (export.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The export is still running");
        }
        try {
            archives.deleteExportFolder(id);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The export's archive could not be deleted", ex);
        }
        writes.executeWithoutResult(status -> exports.deleteById(id));
        LOGGER.info("Deleted export [{}] of tenant [{}]", id, LogSanitizer.sanitize(tenantId));
    }

    private TenantExport require(String tenantId, long id) {
        return exports.findByIdAndTenantId(id, tenantId)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no export [" + id + "]"));
    }

    // ---- what the process reports ----

    /**
     * The process took the export up.
     *
     * @param id the export
     * @return the export
     */
    TenantExport markRunning(long id) {
        return writes.execute(status -> {
            TenantExport export = exports.findById(id)
                                         .orElseThrow(() -> new IllegalStateException("There is no export [" + id + "]"));
            if (!export.isActive()) {
                throw new IllegalStateException("Export [" + id + "] is " + export.getStatus() + " - it cannot run");
            }
            export.setStatus(TenantExportStatus.RUNNING);
            return exports.save(export);
        });
    }

    /**
     * The archive is stored.
     *
     * @param id the export
     * @param archivePath where the archive is
     * @param sizeBytes its size
     */
    void complete(long id, String archivePath, long sizeBytes) {
        writes.executeWithoutResult(status -> {
            TenantExport export = exports.findById(id)
                                         .orElseThrow(() -> new IllegalStateException("There is no export [" + id + "]"));
            Instant now = clock.get();
            export.setStatus(TenantExportStatus.COMPLETED);
            export.setArchivePath(archivePath);
            export.setSizeBytes(sizeBytes);
            export.setCompletedAt(now);
            export.setExpiresAt(now.plus(Duration.ofDays(retentionDays())));
            export.setError(null);
            exports.save(export);
        });
        LOGGER.info("Export [{}] completed: [{}] bytes at [{}]", id, sizeBytes, archivePath);
    }

    /**
     * Ends an export that still runs as failed; an export that already ended is left as it is.
     *
     * @param id the export
     * @param error why
     * @return whether the export was failed by this call
     */
    boolean fail(long id, String error) {
        return Boolean.TRUE.equals(writes.execute(status -> {
            Optional<TenantExport> found = exports.findById(id);
            if (found.isEmpty() || !found.get()
                                         .isActive()) {
                return false;
            }
            TenantExport export = found.get();
            Instant now = clock.get();
            export.setStatus(TenantExportStatus.FAILED);
            export.setError(error);
            export.setCompletedAt(now);
            export.setExpiresAt(now.plus(Duration.ofDays(retentionDays())));
            exports.save(export);
            return true;
        }));
    }

    // ---- the housekeeping ----

    /**
     * Ends as failed every export whose process will not complete it: one of its steps ran out of
     * retries, the process is gone, or it never started. The process is stopped and what it wrote is
     * deleted.
     */
    void failStuck() {
        for (TenantExport export : exports.findByStatusIn(ACTIVE)) {
            inTenant(export, () -> failIfStuck(export));
        }
    }

    private void failIfStuck(TenantExport export) {
        String processInstanceId = export.getProcessInstanceId();
        Optional<String> failure;
        if (processInstanceId == null) {
            boolean overdue = export.getRequestedAt()
                                    .plus(START_GRACE)
                                    .isBefore(clock.get());
            failure = overdue ? Optional.of("The export did not start") : Optional.empty();
        } else {
            failure = process.failure(processInstanceId);
        }
        if (failure.isEmpty()) {
            return;
        }
        if (!fail(export.getId(), failure.get())) {
            return;
        }
        LOGGER.warn("Export [{}] of tenant [{}] failed: {}", export.getId(), LogSanitizer.sanitize(export.getTenantId()), failure.get());
        if (processInstanceId != null) {
            process.cancel(processInstanceId);
        }
        deleteFiles(export);
    }

    /**
     * Deletes every export past its expiry, with its archive.
     */
    void expire() {
        for (TenantExport export : exports.findByExpiresAtBefore(clock.get())) {
            if (export.isActive()) {
                continue;
            }
            inTenant(export, () -> {
                deleteFiles(export);
                writes.executeWithoutResult(status -> exports.deleteById(export.getId()));
                LOGGER.info("Deleted the expired export [{}] of tenant [{}]", export.getId(), LogSanitizer.sanitize(export.getTenantId()));
            });
        }
    }

    private void deleteFiles(TenantExport export) {
        try {
            archives.deleteExportFolder(export.getId());
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("Could not delete the files of export [{}] of tenant [{}]", export.getId(),
                    LogSanitizer.sanitize(export.getTenantId()), ex);
        }
    }

    /**
     * Runs the housekeeping of one export in its tenant; a failure is logged and leaves the others
     * their turn. An export whose tenant is gone is deleted.
     */
    private void inTenant(TenantExport export, Runnable action) {
        try {
            tenantContext.execute(export.getTenantId(), () -> {
                action.run();
                return null;
            });
        } catch (TenantNotFoundException ex) {
            writes.executeWithoutResult(status -> exports.deleteById(export.getId()));
            LOGGER.info("Deleted export [{}]: its tenant [{}] is gone", export.getId(), LogSanitizer.sanitize(export.getTenantId()));
        } catch (RuntimeException ex) {
            LOGGER.error("The housekeeping of export [{}] of tenant [{}] failed", export.getId(),
                    LogSanitizer.sanitize(export.getTenantId()), ex);
        }
    }
}
