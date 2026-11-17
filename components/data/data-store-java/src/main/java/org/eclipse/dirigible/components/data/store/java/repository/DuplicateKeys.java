/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.dirigible.sdk.db.ValidationException;

/**
 * Recognises a database failure as a collision with one of an entity's business keys and finds the
 * refusal authored for that key (dirigible #7098, #7138, #8661). Matching is on the key's name
 * inside the driver's message, which is the only handle the database gives back: a composite
 * {@code unique:} key by the constraint name the model emitted, a single-column {@code unique} by
 * its column name, which is what every dialect builds its own generated constraint name out of
 * ("VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_key").
 */
final class DuplicateKeys {

    /**
     * The markers the drivers append the executed statement to their message behind - Hibernate's
     * "[insert into ...]" and H2's "SQL statement:". Everything from there on is the SQL, not the
     * violation.
     */
    private static final List<String> STATEMENT_MARKERS = List.of("SQL STATEMENT:", "[INSERT ", "[UPDATE ", "[DELETE ", "[MERGE ");

    /** SQLSTATE of a uniqueness violation - which a primary-key collision reports as well. */
    private static final String UNIQUE_VIOLATION = "23505";

    /**
     * The longer key first, so a column whose name is a prefix of a longer one keeps its own answer.
     */
    private static final Comparator<String> MOST_SPECIFIC_FIRST = Comparator.comparingInt(String::length)
                                                                            .reversed()
                                                                            .thenComparing(Comparator.naturalOrder());

    private DuplicateKeys() {}

    /**
     * The refusal of the business key the failure collided with.
     *
     * @param failure the failure a write raised
     * @param keys the refusal of each business key, by the name the database puts into its violation
     * @return the refusal, resolved for the reader's language - or {@code null} when the failure is not
     *         a uniqueness violation, or names none of the keys (a primary key, a key of another table)
     */
    static ValidationException refusalFor(Throwable failure, Map<String, Supplier<ValidationException>> keys) {
        if (keys.isEmpty() || !isConstraintViolation(failure) || !isDuplicateViolation(failure)) {
            return null;
        }
        String violation = violationText(failure);
        if (violation == null) {
            return null;
        }
        String upper = violation.toUpperCase(Locale.ROOT);
        return keys.keySet()
                   .stream()
                   .sorted(MOST_SPECIFIC_FIRST)
                   .filter(key -> upper.contains(key.toUpperCase(Locale.ROOT)))
                   .findFirst()
                   .map(key -> keys.get(key)
                                   .get())
                   .orElse(null);
    }

    /** Whether a persistence failure is (caused by) a database constraint violation. */
    private static boolean isConstraintViolation(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = next(cause)) {
            String type = cause.getClass()
                               .getName();
            if (type.contains("ConstraintViolation") || type.contains("DataIntegrityViolation")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the violation is specifically about UNIQUENESS. A column name matches the text of a
     * foreign-key violation too ("Key (COMPANY_ID)=(5) is not present in table ..."), which is a
     * different conflict and must not be answered as a duplicate. Every supported dialect either
     * reports SQLSTATE 23505 or says so in words - PostgreSQL "duplicate key value violates unique
     * constraint", H2 "Unique index or primary key violation", MySQL "Duplicate entry", Oracle "unique
     * constraint (...) violated" - and says it in the violation itself, so the words are read off the
     * same anchored text the key match uses rather than off the statement appended to it.
     */
    private static boolean isDuplicateViolation(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = next(cause)) {
            if (cause instanceof SQLException sqlException && UNIQUE_VIOLATION.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        String violation = violationText(failure);
        if (violation == null) {
            return false;
        }
        String upper = violation.toUpperCase(Locale.ROOT);
        return upper.contains("DUPLICATE") || upper.contains("UNIQUE");
    }

    /**
     * The text of the violation the database raised: the message of the INNERMOST {@link SQLException}
     * in the chain - the one that carries the constraint name and the Detail line - with the statement
     * the driver appends to it cut off.
     * <p>
     * Anchoring the match there is load-bearing, not tidiness. Hibernate re-reports the failure with
     * the executed SQL attached ("... [insert into PRODUCT (PRODUCT_CODE, PRODUCT_NAME, ...)]") and H2
     * appends its own "SQL statement:" block, so a message read whole lists EVERY column of the table:
     * a collision on one unique field would be answered with the message of another, and a primary-key
     * violation - SQLSTATE 23505 as well - with a business key's.
     */
    private static String violationText(Throwable failure) {
        String text = null;
        for (Throwable cause = failure; cause != null; cause = next(cause)) {
            if (cause instanceof SQLException && cause.getMessage() != null) {
                text = cause.getMessage();
            }
        }
        if (text == null) {
            text = failure.getMessage();
        }
        return text == null ? null : withoutStatement(text);
    }

    /** The message up to the first statement marker in it, which is where the column list starts. */
    private static String withoutStatement(String message) {
        String upper = message.toUpperCase(Locale.ROOT);
        int cut = message.length();
        for (String marker : STATEMENT_MARKERS) {
            int at = upper.indexOf(marker);
            if (at >= 0 && at < cut) {
                cut = at;
            }
        }
        return message.substring(0, cut);
    }

    private static Throwable next(Throwable cause) {
        return cause.getCause() == cause ? null : cause.getCause();
    }
}
