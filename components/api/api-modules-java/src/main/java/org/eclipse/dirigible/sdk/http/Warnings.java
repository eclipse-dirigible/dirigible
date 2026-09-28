/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.http;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.sdk.db.ConfirmationRequiredException;
import org.eclipse.dirigible.sdk.db.Warning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The confirmation half of the soft {@code severity: warn} tier (issue #7466): a generated
 * controller collects the warnings a write raises and hands them here before it persists.
 *
 * <p>
 * The transport is stateless. A write that raises a warning its caller has not confirmed is
 * answered {@code 428 Precondition Required} listing every warning with its code; the caller
 * repeats the same request with {@value #CONFIRM_HEADER} naming the codes it accepts
 * (comma-separated). Confirmation is per code, so a warning that appears only on the second attempt
 * - the data changed in between - is asked about again instead of riding through on an earlier yes.
 * A write with no inbound HTTP request (a process step, a job) has nobody to ask and is not
 * stopped.
 *
 * <p>
 * Every confirmed warning is logged with the user who confirmed it - the audit trace until the
 * activity stream (#7472) records it as an event.
 */
public final class Warnings {

    /** The request header a caller names the confirmed warning codes in. */
    public static final String CONFIRM_HEADER = "X-Confirm-Warnings";

    private static final Logger LOGGER = LoggerFactory.getLogger(Warnings.class);

    private Warnings() {}

    /**
     * Lets the write proceed when every warning is confirmed by the current request, otherwise throws.
     *
     * @param warnings the warnings the write raised, possibly empty
     * @throws ConfirmationRequiredException when a warning is not confirmed
     */
    public static void requireConfirmed(List<Warning> warnings) {
        if (warnings == null || warnings.isEmpty() || !Request.isValid()) {
            return;
        }
        Set<String> confirmed = confirmedCodes(Request.getHeader(CONFIRM_HEADER));
        for (Warning warning : warnings) {
            if (!confirmed.contains(warning.code())) {
                throw new ConfirmationRequiredException(warnings);
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("User [{}] confirmed warnings {} on {} {}", sanitize(Request.getRemoteUser(), "anonymous"), warnings.stream()
                                                                                                                            .map(Warning::code)
                                                                                                                            .toList(),
                    Request.getMethod(), sanitize(Request.getPathInfo(), ""));
        }
    }

    static Set<String> confirmedCodes(String header) {
        if (header == null || header.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(header.split(","))
                     .map(String::trim)
                     .filter(code -> !code.isEmpty())
                     .collect(Collectors.toSet());
    }

    private static String sanitize(String value, String fallback) {
        return value == null ? fallback : value.replaceAll("[\\r\\n\\t]", "_");
    }

}
