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

/**
 * Extension point through which a period register (intent {@code period:}) answers whether a date
 * falls in a CLOSED period, for records of models that cannot reference it at compile time (intent
 * {@code immutableInPeriod: { period: <Register>, model: <owner model> }}, dirigible #8687).
 *
 * <p>
 * The ledger keeps one register, while the source documents that post into it live in their own
 * models, which the ledger's model uses - so they cannot use it back. The register's generated
 * repository implements this interface instead, and a locked record asks every contribution through
 * {@link PeriodRegisters#isClosed(String, String, LocalDate)}, keyed by the owner model and the
 * register's name. A register that is not deployed contributes nothing, and the record stays open.
 */
public interface PeriodRegister {

    /**
     * The model that owns this register - its generated folder name, the name an
     * {@code immutableInPeriod.model} of another model gives.
     *
     * @return the owner model, e.g. {@code journal}
     */
    String periodModel();

    /**
     * The register's entity name.
     *
     * @return the register, e.g. {@code FiscalPeriod}
     */
    String periodRegister();

    /**
     * Whether a period of this register covering the date is closed. A date no period covers is open.
     *
     * @param date the date, never {@code null}
     * @return {@code true} when a covering period is closed
     */
    boolean isClosedOn(LocalDate date);
}
