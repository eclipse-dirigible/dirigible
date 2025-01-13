/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.synonym.CreateSynonymBuilder;

/**
 * The Class HanaCreatePublicSynonymBuilder.
 */
public class HanaCreatePublicSynonymBuilder extends CreateSynonymBuilder {

    /**
     * Instantiates a new hana create public synonym builder.
     *
     * @param dialect the dialect
     * @param synonym the synonym
     */
    public HanaCreatePublicSynonymBuilder(ISqlDialect dialect, String synonym) {
        super(dialect, synonym);
    }

    /**
     * Generate synonym.
     *
     * @param sql the sql
     */
    @Override
    protected void generateSynonym(StringBuilder sql) {
        String synonymName = encapsulate(this.getSynonym(), true);
        sql.append(SPACE)
           .append(KEYWORD_PUBLIC)
           .append(SPACE)
           .append(KEYWORD_SYNONYM)
           .append(SPACE)
           .append(synonymName);
    }

}
