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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.function.Supplier;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The engine's job lock times are configurable (#8871): Flowable's one hour unless an operator says
 * otherwise, a positive whole number of seconds honoured, anything else refused by name at boot.
 */
class BpmFlowableConfigLockTimeTest {

    @AfterEach
    void restoreDefaults() {
        DirigibleConfig.FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS.setStringValue(
                DirigibleConfig.FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS.getDefaultValue());
        DirigibleConfig.FLOWABLE_TIMER_JOB_LOCK_TIME_SECONDS.setStringValue(
                DirigibleConfig.FLOWABLE_TIMER_JOB_LOCK_TIME_SECONDS.getDefaultValue());
    }

    @Test
    void theDefaultsAreOneHour() {
        assertEquals(Duration.ofHours(1), BpmFlowableConfig.asyncJobLockTime());
        assertEquals(Duration.ofHours(1), BpmFlowableConfig.timerLockTime());
    }

    @Test
    void unsetKeysLeaveTheEngineAtItsOwnLockTimes() {
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        Duration engineAsync = config.getAsyncExecutorConfiguration()
                                     .getAsyncJobLockTime();
        Duration engineTimer = config.getAsyncExecutorConfiguration()
                                     .getTimerLockTime();

        BpmFlowableConfig.applyJobLockTimes(config);

        assertEquals(engineAsync, config.getAsyncExecutorConfiguration()
                                        .getAsyncJobLockTime());
        assertEquals(engineTimer, config.getAsyncExecutorConfiguration()
                                        .getTimerLockTime());
    }

    @Test
    void setValuesAreHonouredAndApplied() {
        DirigibleConfig.FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS.setStringValue("120");
        DirigibleConfig.FLOWABLE_TIMER_JOB_LOCK_TIME_SECONDS.setStringValue("300");
        assertEquals(Duration.ofSeconds(120), BpmFlowableConfig.asyncJobLockTime());
        assertEquals(Duration.ofSeconds(300), BpmFlowableConfig.timerLockTime());

        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        BpmFlowableConfig.applyJobLockTimes(config);
        assertEquals(Duration.ofSeconds(120), config.getAsyncExecutorConfiguration()
                                                    .getAsyncJobLockTime());
        assertEquals(Duration.ofSeconds(300), config.getAsyncExecutorConfiguration()
                                                    .getTimerLockTime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "abc", "2147484"})
    void anAsyncLockTimeThatIsNoPositiveWholeNumberIsRefusedByName(String value) {
        DirigibleConfig.FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS.setStringValue(value);
        assertRefused(BpmFlowableConfig::asyncJobLockTime, "DIRIGIBLE_FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS is [" + value + "]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "abc", "2147484"})
    void aTimerLockTimeThatIsNoPositiveWholeNumberIsRefusedByName(String value) {
        DirigibleConfig.FLOWABLE_TIMER_JOB_LOCK_TIME_SECONDS.setStringValue(value);
        assertRefused(BpmFlowableConfig::timerLockTime, "DIRIGIBLE_FLOWABLE_TIMER_JOB_LOCK_TIME_SECONDS is [" + value + "]");
    }

    @Test
    void anInvalidValueStopsTheEngineConfiguration() {
        DirigibleConfig.FLOWABLE_TIMER_JOB_LOCK_TIME_SECONDS.setStringValue("1.5");
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        assertRefused(() -> {
            BpmFlowableConfig.applyJobLockTimes(config);
            return null;
        }, "DIRIGIBLE_FLOWABLE_TIMER_JOB_LOCK_TIME_SECONDS is [1.5]");
    }

    private static void assertRefused(Supplier<?> read, String expected) {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, read::get);
        assertTrue(refused.getMessage()
                          .contains(expected),
                refused.getMessage());
    }
}
