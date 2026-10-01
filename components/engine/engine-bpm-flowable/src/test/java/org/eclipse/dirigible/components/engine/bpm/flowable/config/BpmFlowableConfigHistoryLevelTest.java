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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The history level is configured explicitly (#7598): {@code audit} unless an operator says
 * otherwise, {@code none} accepted as the opt-out it is, anything else refused by name at boot
 * rather than failing somewhere inside the engine.
 */
class BpmFlowableConfigHistoryLevelTest {

    @AfterEach
    void restoreDefault() {
        DirigibleConfig.FLOWABLE_HISTORY_LEVEL.setStringValue(DirigibleConfig.FLOWABLE_HISTORY_LEVEL.getDefaultValue());
    }

    @Test
    void theDefaultRecordsTheAudit() {
        assertEquals(HistoryLevel.AUDIT, BpmFlowableConfig.historyLevel());
    }

    @Test
    void anOptOutIsHonoured() {
        DirigibleConfig.FLOWABLE_HISTORY_LEVEL.setStringValue("none");
        assertEquals(HistoryLevel.NONE, BpmFlowableConfig.historyLevel());
    }

    @Test
    void aValueThatIsNoHistoryLevelIsRefusedByName() {
        DirigibleConfig.FLOWABLE_HISTORY_LEVEL.setStringValue("verbose");
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, BpmFlowableConfig::historyLevel);
        assertTrue(refused.getMessage()
                          .contains("DIRIGIBLE_FLOWABLE_HISTORY_LEVEL is [verbose]"),
                refused.getMessage());
    }
}
