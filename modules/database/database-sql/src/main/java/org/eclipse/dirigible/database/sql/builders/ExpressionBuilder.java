/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.database.sql.ISqlDialect;

/**
 * The Expression Builder.
 */
public class ExpressionBuilder extends AbstractSqlBuilder {

    /** The expressions. */
    private List<String> expressions = new ArrayList<String>();

    /**
     * Instantiates a new expression builder.
     *
     * @param dialect the dialect
     */
    public ExpressionBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * And.
     *
     * @param name the name
     * @return the expression builder
     */
    public ExpressionBuilder and(String name) {
        if (this.expressions.isEmpty()) {
            this.expressions.add(name);
        } else {
            this.expressions.add(KEYWORD_AND + SPACE + name);
        }
        return this;
    }

    /**
     * Or.
     *
     * @param name the name
     * @return the expression builder
     */
    public ExpressionBuilder or(String name) {
        if (this.expressions.isEmpty()) {
            this.expressions.add(name);
        } else {
            this.expressions.add(KEYWORD_OR + SPACE + name);
        }
        return this;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        return generateExpressions();
    }

    /**
     * Generate expressions.
     *
     * @return the string
     */
    protected String generateExpressions() {
        StringBuilder snippet = new StringBuilder();
        for (String expression : this.expressions) {
            snippet.append(expression)
                   .append(SPACE);
        }
        return snippet.toString()
                      .substring(0, snippet.length() - 1);
    }

    /**
     * Gets the expressions.
     *
     * @return the expressions
     */
    public List<String> getExpressions() {
        return expressions;
    }

}
