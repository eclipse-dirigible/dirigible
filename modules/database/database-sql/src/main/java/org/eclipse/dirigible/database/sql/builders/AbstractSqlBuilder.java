/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.dirigible.database.sql.ISqlBuilder;
import org.eclipse.dirigible.database.sql.ISqlDialect;

/**
 * The Abstract SQL Builder.
 */
public abstract class AbstractSqlBuilder implements ISqlBuilder {

    /**
     * The Regex find the content between single quotes.
     */
    private static final Pattern contentBetweenSingleQuotes = Pattern.compile("'([^']*?)'");
    /** The dialect. */
    private final ISqlDialect dialect;
    /** The column pattern. */
    private final Pattern columnPattern = Pattern.compile("^(?![0-9]*$)[a-zA-Z0-9_#$]+$");
    /** The numeric pattern. */
    private final Pattern numericPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    /**
     * Instantiates a new abstract sql builder.
     *
     * @param dialect the dialect
     */
    protected AbstractSqlBuilder(ISqlDialect dialect) {
        this.dialect = dialect;
    }

    /**
     * Usually returns the default generated snippet.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return build();
    }

    /**
     * Returns the default generated snippet.
     *
     * @return the string
     */
    @Override
    public String build() {
        return generate();
    }

    /**
     * Encapsulate the name within quotes.
     *
     * @param name the name
     * @return the encapsulated name
     */
    protected String encapsulate(String name) {
        return encapsulate(name, false);
    }

    /**
     * Encapsulate the name within quotes.
     *
     * @param name the name
     * @param isDataStructureName to check if encapsulating a data structure name
     * @return the encapsulated name
     */
    protected String encapsulate(String name, boolean isDataStructureName) {
        if (name == null)
            return null;
        String escapeSymbol = String.valueOf(getEscapeSymbol());
        if ("*".equals(name.trim())) {
            return name;
        }
        if (!name.startsWith(escapeSymbol)) {
            if (isDataStructureName || isColumn(name.trim())) {
                name = escapeSymbol + name + escapeSymbol;
            } else {
                name = encapsulateMany(name);
            }
        }
        return name;
    }

    /**
     * Gets the escape symbol.
     *
     * @return the escape symbol
     */
    public char getEscapeSymbol() {
        return getDialect().getEscapeSymbol();
    }

    /**
     * Gets the dialect.
     *
     * @return the dialect
     */
    protected ISqlDialect getDialect() {
        return dialect;
    }

    /**
     * Check whether the name is a column (one word) or it is complex expression containing functions,
     * etc. (count(*))
     *
     * @param name the name of the eventual column
     * @return true if it is one word
     */
    protected boolean isColumn(String name) {
        if (name == null) {
            return false;
        }
        return columnPattern.matcher(name)
                            .matches();
    }

    /**
     * Encapsulate all the non-function and non-numeric words.
     *
     * @param line the input string
     * @return the transformed string
     */
    protected String encapsulateMany(String line) {
        return encapsulateMany(line, getEscapeSymbol());
    }

    /**
     * Encapsulate many.
     *
     * @param line the line
     * @param escapeChar the escape char
     * @return the string
     */
    protected String encapsulateMany(String line, char escapeChar) {
        String lineWithoughContentBetweenSingleQuotes = String.join("", line.split(contentBetweenSingleQuotes.toString()));
        String regex = "([^a-zA-Z0-9_#$::'/]+)'*\\1*";
        String[] words = lineWithoughContentBetweenSingleQuotes.split(regex);
        Set<String> wordsSet = new HashSet<>(Arrays.asList(words));
        Set<Set> functionsNames = getDialect().getFunctionsNames();
        for (String word : wordsSet) {
            if (isNumeric(word) || isValue(word)) {
                continue;
            }
            if (!"".equals(word.trim()) && !(functionsNames.contains(word.toLowerCase()) || functionsNames.contains(word.toUpperCase()))) {
                line = line.replace(word, escapeChar + word + escapeChar);
            }
        }
        return line;
    }

    /**
     * Check whether the string is a number.
     *
     * @param s the input
     * @return true if it is a number
     */
    protected boolean isNumeric(String s) {
        if (s == null) {
            return false;
        }
        return numericPattern.matcher(s)
                             .matches();
    }

    /**
     * Checks if is value.
     *
     * @param s the s
     * @return true, if is value
     */
    protected boolean isValue(String s) {
        if (s == null) {
            return false;
        }
        return s.startsWith("'") || s.endsWith("'");
    }

    /**
     * Encapsulate where.
     *
     * @param where the where
     * @return the string
     */
    protected String encapsulateWhere(String where) {
        return encapsulateMany(where, getEscapeSymbol());
    }
}
