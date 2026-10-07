/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * One parsed {@code .migration} file: {@code <project>/.../<version>__<description>.migration},
 * SQL, optionally headed by comment lines declaring how it applies:
 *
 * <pre>
 * -- tenant: each        (default; once per tenant schema) | system (once, on the system database)
 * -- idempotent: false   (default; an edit after it applied FAILS it) | true (an edit re-applies it)
 * UPDATE ORDERS SET STATUS = 'OPEN' WHERE STATUS IS NULL;
 * </pre>
 *
 * The version is one or more dot-separated numbers, optionally prefixed with {@code V}; versions
 * compare numerically segment by segment, so {@code 2} precedes {@code 10}. Leading zeros are not
 * part of a version: {@code 001} and {@code 1} are one version, so renaming {@code V001__x} to
 * {@code V1__x} keeps its ledger record. A header line - a comment naming {@code tenant} or
 * {@code idempotent} followed by {@code :} or {@code =} - must parse in full, the value alone on
 * the line; one that does not is refused rather than read as a comment. The checksum is taken over
 * the content with line endings normalized, so a checkout that converts them does not read as an
 * edit.
 *
 * @param project the project the file belongs to - the first segment of its location
 * @param version the version, without the optional {@code V} prefix and with each segment's leading
 *        zeros stripped
 * @param description the part of the file name after {@code __}
 * @param scope where the migration applies
 * @param idempotent whether the migration may run again when its content changes
 * @param checksum the SHA-256 of the content, hex-encoded
 * @param sql the content, as it is executed
 */
record MigrationScript(String project, String version, String description, Scope scope, boolean idempotent, String checksum, String sql) {

    /** Where a migration applies. */
    enum Scope {
        /** Once in every tenant's schema, through the tenant-routed default datasource. */
        EACH,
        /** Once, on the system database. */
        SYSTEM
    }

    static final String FILE_EXTENSION = ".migration";

    private static final Pattern FILE_NAME = Pattern.compile("[Vv]?(\\d+(?:\\.\\d+)*)__([A-Za-z0-9][A-Za-z0-9_.\\-]*)\\.migration");

    /** What makes a comment line a header: matched at its start, so anything may follow. */
    private static final Pattern HEADER_NAME = Pattern.compile("--\\s*(?:tenant|idempotent)\\s*[:=]", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEADER = Pattern.compile("--\\s*(tenant|idempotent)\\s*:\\s*(\\S+)\\s*", Pattern.CASE_INSENSITIVE);

    /**
     * Parses a migration file.
     *
     * @param location the registry-relative location, {@code /<project>/.../<name>.migration}
     * @param content the file content
     * @return the parsed migration
     * @throws ParseException when the file name, a header or the content is not a valid migration
     */
    static MigrationScript parse(String location, byte[] content) throws ParseException {
        String[] segments = location.split("/");
        if (segments.length < 3 || segments[1].isEmpty()) {
            throw new ParseException("The migration [" + location + "] must live inside a project", 0);
        }
        String fileName = segments[segments.length - 1];
        Matcher name = FILE_NAME.matcher(fileName);
        if (!name.matches()) {
            throw new ParseException("The migration file name [" + fileName + "] in [" + location
                    + "] must be <version>__<description>.migration, the version being dot-separated numbers (e.g. 001__backfill_status.migration or V1.2__split_name.migration)",
                    0);
        }
        String sql = new String(content, StandardCharsets.UTF_8).replace("\r\n", "\n");

        Scope scope = Scope.EACH;
        boolean idempotent = false;
        boolean hasStatement = false;
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!trimmed.startsWith("--")) {
                hasStatement = true;
                break;
            }
            if (!HEADER_NAME.matcher(trimmed)
                            .lookingAt()) {
                continue;
            }
            Matcher header = HEADER.matcher(trimmed);
            if (!header.matches()) {
                throw new ParseException("The migration [" + location + "] has the header line [" + trimmed
                        + "] that does not parse - write it as [-- tenant: each|system] or [-- idempotent: true|false], with nothing after the value",
                        0);
            }
            String value = header.group(2)
                                 .toLowerCase();
            if ("tenant".equalsIgnoreCase(header.group(1))) {
                scope = parseScope(location, value);
            } else {
                idempotent = parseIdempotent(location, value);
            }
        }
        if (!hasStatement) {
            throw new ParseException("The migration [" + location + "] contains no SQL statement", 0);
        }
        return new MigrationScript(segments[1], canonicalVersion(name.group(1)), name.group(2), scope, idempotent, checksum(sql), sql);
    }

    /**
     * Strips each segment's leading zeros, keeping a lone {@code 0}, so that the ledger key, the
     * duplicate check and the order all see one spelling of a version.
     */
    private static String canonicalVersion(String version) {
        return Arrays.stream(version.split("\\."))
                     .map(MigrationScript::stripLeadingZeros)
                     .collect(Collectors.joining("."));
    }

    /**
     * Compares two versions numerically, segment by segment; a version that is a prefix of the other
     * precedes it ({@code 1} before {@code 1.0}).
     *
     * @param left a version
     * @param right a version
     * @return negative, zero or positive as {@code left} precedes, equals or follows {@code right}
     */
    static int compareVersions(String left, String right) {
        String[] leftSegments = left.split("\\.");
        String[] rightSegments = right.split("\\.");
        for (int i = 0; i < Math.min(leftSegments.length, rightSegments.length); i++) {
            int compared = compareNumbers(leftSegments[i], rightSegments[i]);
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(leftSegments.length, rightSegments.length);
    }

    /**
     * Compares two unbounded decimal numbers without parsing them, so a timestamp version cannot
     * overflow.
     */
    private static int compareNumbers(String left, String right) {
        String leftDigits = stripLeadingZeros(left);
        String rightDigits = stripLeadingZeros(right);
        if (leftDigits.length() != rightDigits.length()) {
            return Integer.compare(leftDigits.length(), rightDigits.length());
        }
        return leftDigits.compareTo(rightDigits);
    }

    private static String stripLeadingZeros(String digits) {
        int start = 0;
        while (start < digits.length() - 1 && digits.charAt(start) == '0') {
            start++;
        }
        return digits.substring(start);
    }

    private static Scope parseScope(String location, String value) throws ParseException {
        return switch (value) {
            case "each" -> Scope.EACH;
            case "system" -> Scope.SYSTEM;
            default -> throw new ParseException("The migration [" + location + "] declares [tenant: " + value
                    + "] - expected [each] (once per tenant schema) or [system] (once, on the system database)", 0);
        };
    }

    private static boolean parseIdempotent(String location, String value) throws ParseException {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new ParseException(
                    "The migration [" + location + "] declares [idempotent: " + value + "] - expected [true] or [false]", 0);
        };
    }

    private static String checksum(String normalizedContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                            .formatHex(digest.digest(normalizedContent.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is a mandatory JDK algorithm", ex);
        }
    }
}
