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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
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
     * Separates a version from its revision: {@code 1.28.0_v1} is the first content shipped under
     * 1.28.0 after 1.28.0 itself.
     */
    static final String REVISION_SEPARATOR = "_v";

    /** A revision fits an {@code int}. */
    private static final int MAX_REVISION_DIGITS = 9;

    /**
     * A release version: at least two dot-separated numbers (so an all-digit content hash never reads
     * as one), an optional pre-release, optional build metadata and an optional revision.
     */
    private static final Pattern SEMVER = Pattern.compile("(\\d+(?:\\.\\d+)+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?(?:"
            + REVISION_SEPARATOR + "(\\d{1," + MAX_REVISION_DIGITS + "}))?");

    /**
     * What an overwrite through the Documents perspective leaves behind on a CMS whose rename does
     * nothing (S3): {@code standard.print-1696000000000} - the upload, under its temporary name.
     */
    private static final Pattern OVERWRITE_LEFTOVER = Pattern.compile("(?i)(.+)\\.print-\\d+");

    /** The longest template name, as {@link #NAME} admits it. */
    private static final int MAX_NAME_LENGTH = 100;

    /** A character a template name cannot carry. */
    private static final Pattern NOT_A_NAME_CHARACTER = Pattern.compile("[^A-Za-z0-9._-]+");

    /** A combining mark, dropped so an accented letter keeps its base letter. */
    private static final Pattern COMBINING_MARK = Pattern.compile("\\p{M}+");

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
     * The base name a document of a language folder carries when it is a print template at all - also
     * one whose name is not a valid template name ({@code Invoice template.print},
     * {@code фактура.print}) and the leftover of an overwrite on a CMS whose rename does nothing
     * ({@code standard.print-1696000000000}). The old resolution printed such documents, so the
     * catalogue keeps them visible rather than dropping them.
     *
     * @param documentName the document name
     * @return the name without the {@code .print} extension, empty when the document is not a print
     *         template
     */
    static Optional<String> templateBase(String documentName) {
        if (documentName == null) {
            return Optional.empty();
        }
        String base = null;
        if (documentName.toLowerCase(Locale.ROOT)
                        .endsWith(EXTENSION)) {
            base = documentName.substring(0, documentName.length() - EXTENSION.length());
        } else {
            Matcher leftover = OVERWRITE_LEFTOVER.matcher(documentName);
            if (leftover.matches()) {
                base = leftover.group(1);
            }
        }
        return base == null || base.isBlank() ? Optional.empty() : Optional.of(base);
    }

    /**
     * Turns any document base name into a valid tenant template name, deterministically: accents are
     * dropped, every run of other characters - {@code @} included, so the result is never a shipped
     * version - becomes a dash, and a name with nothing left is named after a hash of the original.
     *
     * @param base the base name
     * @return a valid template name
     */
    static String sanitize(String base) {
        String plain = COMBINING_MARK.matcher(Normalizer.normalize(base, Normalizer.Form.NFKD))
                                     .replaceAll("");
        String name = NOT_A_NAME_CHARACTER.matcher(plain)
                                          .replaceAll("-")
                                          .replaceFirst("^[._-]+", "");
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        return name.isEmpty() ? "template-" + shortHash(base) : name;
    }

    /**
     * The first 8 hex digits of the SHA-256 of a text - a short, stable discriminator.
     *
     * @param text the text
     * @return 8 lower-case hex digits
     */
    static String shortHash(String text) {
        return shortHash(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The first 8 hex digits of the SHA-256 of some bytes - the content version of a shipped template
     * whose module declares no release version.
     *
     * @param content the bytes
     * @return 8 lower-case hex digits
     */
    static String shortHash(byte[] content) {
        return contentHash(content).substring(0, 8);
    }

    /**
     * The SHA-256 of some bytes, in hex.
     *
     * @param content the bytes
     * @return 64 lower-case hex digits
     */
    static String contentHash(byte[] content) {
        try {
            return HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256")
                                                    .digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
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
     * Compares two release versions by their numbers; a pre-release ranks below its release, and a
     * revision ({@code 1.28.0_v2}) above the version it revises.
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
            if (leftPre != null || rightPre != null) {
                return leftPre == null ? 1 : -1;
            }
        } else {
            int compared = comparePreRelease(leftPre.split("\\."), rightPre.split("\\."));
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(revisionNumber(left.group(3)), revisionNumber(right.group(3)));
    }

    /**
     * A revision of a version - the label a content gets when its version already names other bytes.
     *
     * @param label the version
     * @param revision the revision, from 1
     * @return {@code <label>_v<revision>}
     */
    static String revise(String label, int revision) {
        return label + REVISION_SEPARATOR + revision;
    }

    /**
     * Which revision of a label a version is.
     *
     * @param version the version
     * @param label the label
     * @return 0 for the label itself, N for {@code <label>_vN}, empty for any other version
     */
    static OptionalInt revisionOf(String version, String label) {
        if (version.equals(label)) {
            return OptionalInt.of(0);
        }
        String prefix = label + REVISION_SEPARATOR;
        if (version.startsWith(prefix)) {
            String revision = version.substring(prefix.length());
            if (isNumeric(revision) && revision.length() <= MAX_REVISION_DIGITS) {
                return OptionalInt.of(Integer.parseInt(revision));
            }
        }
        return OptionalInt.empty();
    }

    private static int revisionNumber(String revision) {
        return revision == null ? 0 : Integer.parseInt(revision);
    }

    /**
     * Compares two pre-releases the SemVer way: identifier by identifier, numerically when both are
     * numeric ({@code rc.2} below {@code rc.10}), a numeric one below an alphanumeric one, otherwise by
     * their text; a pre-release that is a prefix of the other ranks below it.
     */
    private static int comparePreRelease(String[] left, String[] right) {
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            boolean leftNumeric = isNumeric(left[i]);
            boolean rightNumeric = isNumeric(right[i]);
            int compared;
            if (leftNumeric && rightNumeric) {
                compared = compareNumbers(left[i], right[i]);
            } else if (leftNumeric != rightNumeric) {
                compared = leftNumeric ? -1 : 1;
            } else {
                compared = left[i].compareTo(right[i]);
            }
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    private static boolean isNumeric(String identifier) {
        return !identifier.isEmpty() && identifier.chars()
                                                  .allMatch(Character::isDigit);
    }

    /** Compares two digit strings as numbers of any length. */
    private static int compareNumbers(String left, String right) {
        String l = left.replaceFirst("^0+(?=.)", "");
        String r = right.replaceFirst("^0+(?=.)", "");
        return l.length() != r.length() ? Integer.compare(l.length(), r.length()) : l.compareTo(r);
    }
}
