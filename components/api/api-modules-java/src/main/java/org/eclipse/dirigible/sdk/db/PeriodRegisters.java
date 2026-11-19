/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.time.LocalDate;

import org.eclipse.dirigible.sdk.extensions.Extensions;

/**
 * Asks the period register of ANOTHER model whether a date is closed (intent
 * {@code immutableInPeriod: { period, model, date }}, dirigible #8687) - see
 * {@link PeriodRegister}.
 */
public final class PeriodRegisters {

    private PeriodRegisters() {}

    /**
     * Whether the named register closes the date.
     *
     * @param model the model owning the register
     * @param register the register's entity name
     * @param date the date; {@code null} falls in no period
     * @return {@code true} when a deployed register of that model and name has a closed period covering
     *         the date; {@code false} for an uncovered date, a {@code null} date, or a register that is
     *         not deployed
     */
    public static boolean isClosed(String model, String register, LocalDate date) {
        if (date == null) {
            return false;
        }
        for (PeriodRegister contribution : Extensions.find(PeriodRegister.class)) {
            if (model.equals(contribution.periodModel()) && register.equals(contribution.periodRegister())) {
                return contribution.isClosedOn(date);
            }
        }
        return false;
    }
}
