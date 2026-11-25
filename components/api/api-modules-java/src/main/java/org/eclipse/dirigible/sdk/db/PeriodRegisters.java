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

    /**
     * The topic a register announces on that one of its periods stands open again (dirigible #8755).
     *
     * <p>
     * A posting into a record locked by another model's register leaves a document dated in a closed
     * period unposted, and must re-run it once that period reopens. The register's own
     * {@code -transitioned} / {@code -updated} topics carry its project and perspective, which the
     * posting model cannot name, so the register's model relays every move of a period into a status
     * that is not closed onto this topic, keyed only by what an {@code immutableInPeriod.model} lock
     * already names. The message is a JSON object carrying {@code model}, {@code register}, {@code id},
     * {@code start} and {@code end}, the bounds as ISO dates. Like every application topic it is scoped
     * to the tenant that published it.
     *
     * @param model the model owning the register
     * @param register the register's entity name
     * @return the topic, e.g. {@code dirigible-period-journal-FiscalPeriod-reopened}
     */
    public static String reopenedTopic(String model, String register) {
        return "dirigible-period-" + model + "-" + register + "-reopened";
    }
}
