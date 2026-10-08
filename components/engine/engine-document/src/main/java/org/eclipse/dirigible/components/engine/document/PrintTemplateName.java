/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The name of a print template in a language folder, which is all that tells the two kinds apart:
 * <ul>
 * <li>a <b>shipped version</b> is {@code <name>@<version>.print} - written by
 * {@link PrintTemplateSynchronizer}, never edited or deleted through the print engine;</li>
 * <li>a <b>tenant template</b> is {@code <name>.print} - the tenant's own, editable.</li>
 * </ul>
 * The reference of a template (what the tenant configuration and the {@code template} request
 * parameter carry) is the file name without the extension: {@code standard@1.28.0} or
 * {@code acme-blue}.
 *
 * @param name the template name
 * @param version the shipped version, {@code null} for a tenant template
 */
record PrintTemplateName(String name, String version) {

    /** The file extension of a print template. */
    static final String EXTENSION = ".print";

    private static final char VERSION_SEPARATOR = '@';

    /**
     * Letters, digits, dot, dash and underscore, starting with a letter or digit - a safe file name on
     * every CMS backend, never a path.
     */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");

    /** A version additionally allows {@code +} (semver build metadata). */
    private static final Pattern VERSION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.+_-]{0,63}");

    /**
     * A release version: at least two dot-separated numbers (so an all-digit content hash never reads
     * as one), an optional pre-release and optional build metadata.
     */
    private static final Pattern SEMVER = Pattern.compile("(\\d+(?:\\.\\d+)+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?");

    PrintTemplateName {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid print template name [" + name + "]");
        }
        if (version != null && !isValidVersion(version)) {
            throw new IllegalArgumentException("Invalid print template version [" + version + "]");
        }
    }

    /**
     * A tenant template.
     *
     * @param name the template name
     * @return the name
     */
    static PrintTemplateName tenant(String name) {
        return new PrintTemplateName(name, null);
    }

    /**
     * A shipped version.
     *
     * @param name the template name
     * @param version the version
     * @return the name
     */
    static PrintTemplateName shipped(String name, String version) {
        return new PrintTemplateName(name, version);
    }

    /**
     * Parses a reference ({@code standard@1.28.0}, {@code acme-blue}).
     *
     * @param reference the reference
     * @return the name, empty when the reference is not a valid one
     */
    static Optional<PrintTemplateName> parse(String reference) {
        if (reference == null) {
            return Optional.empty();
        }
        String trimmed = reference.trim();
        int separator = trimmed.indexOf(VERSION_SEPARATOR);
        String name = separator < 0 ? trimmed : trimmed.substring(0, separator);
        String version = separator < 0 ? null : trimmed.substring(separator + 1);
        if (!isValidName(name) || (version != null && !isValidVersion(version))) {
            return Optional.empty();
        }
        return Optional.of(new PrintTemplateName(name, version));
    }

    /**
     * Parses a document name of a language folder.
     *
     * @param fileName the document name
     * @return the template name, empty when the document is not a print template
     */
    static Optional<PrintTemplateName> fromFileName(String fileName) {
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT)
                                         .endsWith(EXTENSION)) {
            return Optional.empty();
        }
        return parse(fileName.substring(0, fileName.length() - EXTENSION.length()));
    }

    /**
     * Whether a template name is valid.
     *
     * @param name the name
     * @return true for a non-blank name of letters, digits, dot, dash and underscore
     */
    static boolean isValidName(String name) {
        return name != null && NAME.matcher(name)
                                   .matches();
    }

    /**
     * Whether a version is valid.
     *
     * @param version the version
     * @return true for a version a file name can carry
     */
    static boolean isValidVersion(String version) {
        return version != null && VERSION.matcher(version)
                                         .matches();
    }

    /**
     * Whether this is a shipped version.
     *
     * @return true for {@code <name>@<version>}
     */
    boolean isShipped() {
        return version != null;
    }

    /**
     * The reference - the file name without the extension.
     *
     * @return {@code name@version} or {@code name}
     */
    String reference() {
        return isShipped() ? name + VERSION_SEPARATOR + version : name;
    }

    /**
     * The document name in the language folder.
     *
     * @return the reference plus {@code .print}
     */
    String fileName() {
        return reference() + EXTENSION;
    }

    /**
     * Whether this version is a release version, which is ordered by its numbers rather than by when it
     * was shipped.
     *
     * @return true for a version such as {@code 1.28.0} or {@code 2.0.0-rc.1}
     */
    boolean hasReleaseVersion() {
        return isShipped() && SEMVER.matcher(version)
                                    .matches();
    }

    /**
     * Compares two release versions by their numbers; a pre-release ranks below its release.
     *
     * @param other the other shipped name, which must have a release version as well
     * @return negative, zero or positive as this version is older, equal or newer
     */
    int compareReleaseVersion(PrintTemplateName other) {
        Matcher left = SEMVER.matcher(version);
        Matcher right = SEMVER.matcher(other.version);
        if (!left.matches() || !right.matches()) {
            throw new IllegalStateException("Not release versions: [" + version + "], [" + other.version + "]");
        }
        String[] leftNumbers = left.group(1)
                                   .split("\\.");
        String[] rightNumbers = right.group(1)
                                     .split("\\.");
        for (int i = 0; i < Math.max(leftNumbers.length, rightNumbers.length); i++) {
            int compared = compareNumbers(i < leftNumbers.length ? leftNumbers[i] : "0", i < rightNumbers.length ? rightNumbers[i] : "0");
            if (compared != 0) {
                return compared;
            }
        }
        String leftPre = left.group(2);
        String rightPre = right.group(2);
        if (leftPre == null || rightPre == null) {
            return leftPre == null ? (rightPre == null ? 0 : 1) : -1;
        }
        return leftPre.compareTo(rightPre);
    }

    /** Compares two digit strings as numbers of any length. */
    private static int compareNumbers(String left, String right) {
        String l = left.replaceFirst("^0+(?=.)", "");
        String r = right.replaceFirst("^0+(?=.)", "");
        return l.length() != r.length() ? Integer.compare(l.length(), r.length()) : l.compareTo(r);
    }
}
