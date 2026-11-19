/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.retention;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.domain.JobStatus;
import org.eclipse.dirigible.components.jobs.repository.JobLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class JobLogRetentionTest {

    private static final String RETENTION_KEY = DirigibleConfig.SCHEDULER_LOGS_RETENTION_PERIOD.getKey();
    private static final String TENANT = "tenant-a";
    private static final String OTHER_TENANT = "tenant-b";
    private static final String HANDLER = "handler";

    @Autowired
    private JobLogRepository jobLogs;

    @Autowired
    private JobLogRetention retention;

    private long sequence;

    @SpringBootApplication
    static class TestConfiguration {
    }

    @BeforeEach
    void setup() {
        jobLogs.deleteAll();
    }

    @AfterEach
    void cleanup() {
        Configuration.remove(RETENTION_KEY);
        jobLogs.deleteAll();
    }

    @Test
    void theRunsOlderThanTheDefaultWeekArePurgedAndTheNewerKept() {
        run(TENANT, "active", daysAgo(10));
        Timestamp withinTheWeek = daysAgo(6);
        run(TENANT, "active", withinTheWeek);
        Timestamp recent = hoursAgo(1);
        run(TENANT, "active", recent);

        assertEquals(3, retention.purge());

        assertEquals(List.of(withinTheWeek, withinTheWeek, withinTheWeek, recent, recent, recent), triggeredAtOf(TENANT, "active"));
    }

    @Test
    void theConfiguredPeriodIsApplied() {
        Configuration.set(RETENTION_KEY, "24");
        run(TENANT, "active", daysAgo(2));
        Timestamp recent = hoursAgo(1);
        run(TENANT, "active", recent);

        assertEquals(3, retention.purge());

        assertEquals(List.of(recent, recent, recent), triggeredAtOf(TENANT, "active"));
    }

    @Test
    void theLastRunOfAJobIsKeptHoweverOldItIs() {
        run(TENANT, "idle", daysAgo(30));
        Timestamp lastRun = daysAgo(20);
        Timestamp loggedDuringLastRun = new Timestamp(lastRun.getTime() + 5_000);
        log(TENANT, "idle", JobStatus.TRIGGRED, lastRun);
        log(TENANT, "idle", JobStatus.LOGGED, loggedDuringLastRun);
        log(TENANT, "idle", JobStatus.FAILED, lastRun);

        assertEquals(3, retention.purge());

        assertEquals(List.of(lastRun, lastRun, loggedDuringLastRun), triggeredAtOf(TENANT, "idle"));
        assertEquals(0, retention.purge());
    }

    @Test
    void theLastRunIsKeptPerTenant() {
        run(TENANT, "shared", daysAgo(30));
        Timestamp recent = hoursAgo(1);
        run(TENANT, "shared", recent);
        run(OTHER_TENANT, "shared", daysAgo(40));
        Timestamp lastRunOfOtherTenant = daysAgo(30);
        run(OTHER_TENANT, "shared", lastRunOfOtherTenant);

        assertEquals(6, retention.purge());

        assertEquals(List.of(recent, recent, recent), triggeredAtOf(TENANT, "shared"));
        assertEquals(List.of(lastRunOfOtherTenant, lastRunOfOtherTenant, lastRunOfOtherTenant), triggeredAtOf(OTHER_TENANT, "shared"));
    }

    @Test
    void theOldLogsOfAJobWithoutARunArePurged() {
        log(TENANT, "never-run", JobStatus.INFO, daysAgo(10));
        Timestamp recent = hoursAgo(1);
        log(TENANT, "never-run", JobStatus.INFO, recent);

        assertEquals(1, retention.purge());

        assertEquals(List.of(recent), triggeredAtOf(TENANT, "never-run"));
    }

    @Test
    void aPeriodOfZeroKeepsEveryLog() {
        Configuration.set(RETENTION_KEY, "0");
        run(TENANT, "active", daysAgo(400));
        run(TENANT, "active", hoursAgo(1));

        assertEquals(0, retention.purge());

        assertEquals(6, jobLogs.count());
    }

    /** A run as the engine logs it: the TRIGGRED entry, what the handler logged, and the outcome. */
    private void run(String tenantId, String jobName, Timestamp triggeredAt) {
        log(tenantId, jobName, JobStatus.TRIGGRED, triggeredAt);
        log(tenantId, jobName, JobStatus.LOGGED, triggeredAt);
        log(tenantId, jobName, JobStatus.FINISHED, triggeredAt);
    }

    private void log(String tenantId, String jobName, JobStatus status, Timestamp triggeredAt) {
        String name = jobName + "-" + sequence++;
        jobLogs.save(new JobLog("/" + name, name, null, null, jobName, HANDLER, triggeredAt, null, null, status, null, tenantId));
    }

    private List<Timestamp> triggeredAtOf(String tenantId, String jobName) {
        return jobLogs.findAll()
                      .stream()
                      .filter(log -> tenantId.equals(log.getTenantId()) && jobName.equals(log.getJobName()))
                      .map(JobLog::getTriggeredAt)
                      .sorted()
                      .toList();
    }

    private static Timestamp daysAgo(int days) {
        return hoursAgo(days * 24);
    }

    private static Timestamp hoursAgo(int hours) {
        return Timestamp.from(Instant.now()
                                     .minus(Duration.ofHours(hours))
                                     .truncatedTo(ChronoUnit.MILLIS));
    }
}
