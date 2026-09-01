/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.agent;

import org.apache.commons.lang3.StringUtils;

/**
 * One line of the proposal's requirement-coverage audit (dirigible #6997): a discrete requirement
 * from the developer's request, mapped to the construct(s) of the proposed YAML that carry it -
 * {@code "boundary"} when a boundaries entry carries it instead, {@code "none"} when the proposal
 * does not carry it at all.
 *
 * @param requirement the requirement, in the developer's own words
 * @param construct what carries it, or {@code "boundary"} / {@code "none"}
 */
record AgentCoverage(String requirement, String construct) {

    /** Whether the proposal, by its own audit, does not carry this requirement. */
    boolean uncovered() {
        return StringUtils.isBlank(construct) || "none".equalsIgnoreCase(construct.trim());
    }
}
