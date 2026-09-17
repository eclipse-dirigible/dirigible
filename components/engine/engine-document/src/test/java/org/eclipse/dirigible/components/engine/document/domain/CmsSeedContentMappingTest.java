/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.Test;

/**
 * The seed content column must be inline binary on every dialect - never a PostgreSQL {@code oid}
 * large object. A {@code @Lob byte[]} renders as {@code oid} there, and pgjdbc's large-object API
 * throws "Large Objects may not be used in auto-commit mode" for exactly the auto-commit connection
 * the synchronization thread saves seeds on, so every seed save failed on a PostgreSQL SystemDB.
 */
class CmsSeedContentMappingTest {

    @Test
    void postgreSqlRendersInlineBytesNotALargeObject() {
        assertThat(contentColumnType("org.hibernate.dialect.PostgreSQLDialect")).isEqualTo("bytea");
    }

    /**
     * The other two dialects are unchanged by the mapping - they rendered (and keep rendering) an
     * inline binary column, so no deployed schema has to be migrated off a large object but
     * PostgreSQL's.
     */
    @Test
    void theOtherDialectsKeepTheColumnTheyAlreadyHave() {
        assertThat(contentColumnType("org.hibernate.dialect.H2Dialect")).isEqualTo("blob");
        assertThat(contentColumnType("org.hibernate.dialect.SQLServerDialect")).isEqualTo("varbinary(max)");
    }

    /** The DDL type Hibernate would give the content column on the given dialect. */
    private static String contentColumnType(String dialect) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySetting("hibernate.dialect", dialect)
                                                                               .build();
        try {
            Metadata metadata = new MetadataSources(registry).addAnnotatedClass(CmsSeed.class)
                                                             .buildMetadata();
            PersistentClass entity = metadata.getEntityBinding(CmsSeed.class.getName());
            Column content = (Column) entity.getProperty("content")
                                            .getSelectables()
                                            .get(0);

            return content.getSqlType(metadata)
                          .toLowerCase(Locale.ROOT);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
