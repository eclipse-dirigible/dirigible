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
 * The shipped print template's content column is inline binary on every dialect, for the reason
 * {@link CmsSeedContentMappingTest} pins for the CMS seed - and matches the per-vendor changesets
 * that create it.
 */
class PrintTemplateSeedContentMappingTest {

    @Test
    void theContentColumnMatchesItsChangesets() {
        assertThat(contentColumnType("org.hibernate.dialect.PostgreSQLDialect")).isEqualTo("bytea");
        assertThat(contentColumnType("org.hibernate.dialect.H2Dialect")).isEqualTo("blob");
        assertThat(contentColumnType("org.hibernate.dialect.SQLServerDialect")).isEqualTo("varbinary(max)");
    }

    private static String contentColumnType(String dialect) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySetting("hibernate.dialect", dialect)
                                                                               .build();
        try {
            Metadata metadata = new MetadataSources(registry).addAnnotatedClass(PrintTemplateSeed.class)
                                                             .buildMetadata();
            PersistentClass entity = metadata.getEntityBinding(PrintTemplateSeed.class.getName());
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
