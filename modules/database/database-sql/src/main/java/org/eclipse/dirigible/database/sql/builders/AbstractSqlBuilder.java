/*
 * Copyright (c) 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.database.sql.ISqlBuilder;
import org.eclipse.dirigible.database.sql.ISqlDialect;

/**
 * The Abstract SQL Builder.
 */
public abstract class AbstractSqlBuilder implements ISqlBuilder {

	private ISqlDialect dialect;
	
	
	/**
	 * Instantiates a new abstract sql builder.
	 *
	 * @param dialect
	 *            the dialect
	 */
	protected AbstractSqlBuilder(ISqlDialect dialect) {
		this.dialect = dialect;
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
	 * Whether the names of tables, columns, indices are case sensitive
	 * 
	 * @return true if set
	 */
	protected boolean isCaseSensitive() {
		return Boolean.parseBoolean(Configuration.get("DIRIGIBLE_DATABASE_NAMES_CASE_SENSITIVE", "false"));
	}
	
	/**
	 * Encapsulate the name within qutes
	 * 
	 * @param name the name
	 * @return the encapsulated name
	 */
	protected String encapsulate(String name) {
		if ("*".equals(name.trim())) {
			return name;
		}
		if (!name.startsWith("\"")) {
			if (isColumn(name.trim())) {
				name = "\"" + name + "\"";
			} else {
				name = encapsulateMany(name);
			}
		}
		return name;
	}
	
	private Pattern columnPattern = Pattern.compile("^(?![0-9]*$)[a-zA-Z0-9_#$]+$");
	
	/**
	 * Check whether the name is a column (one word) or it is complex expression containing functions, etc. (count(*))
	 * @param name the name of the eventual column
	 * @return true if it is one word
	 */
	protected boolean isColumn(String name) {
		if (name == null) {
	        return false; 
	    }
	    return columnPattern.matcher(name).matches();
	}
	
	/**
	 * Encapsulate all the non-function and non-numeric words
	 * 
	 * @param line the input string
	 * @return the transformed string
	 */
	protected String encapsulateMany(String line) {
		String regex = "([^a-zA-Z0-9_#$::']+)'*\\1*";
		String[] words = line.split(regex);
		Set<Set> functionsNames = getDialect().getFunctionsNames();
		for (String word : words) {
			if (isNumeric(word) || isValue(word)) {
				continue;
			}
			if (!"".equals(word.trim())
					&& !functionsNames.contains(word.toLowerCase())) {
				line = line.replace(word, "\"" + word + "\"");
			}
		}
		return line;
	}
	
	private Pattern numericPattern = Pattern.compile("-?\\d+(\\.\\d+)?");
	
	/**
	 * Check whether the string is a number
	 * @param s the input
	 * @return true if it is a number
	 */
	protected boolean isNumeric(String s) {
	    if (s == null) {
	        return false; 
	    }
	    return numericPattern.matcher(s).matches();
	}

	protected boolean isValue(String s) {
		if (s == null) {
			return false;
		}
		return s.startsWith("'") || s.endsWith("'");
	}
}
