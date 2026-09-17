/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every JPA entity scanned into the SystemDB owes a {@code createTable} changeset in
 * {@code db/changelog/dirigible-system.json} - see {@code core-liquibase/CLAUDE.md}. Hibernate's
 * {@code hbm2ddl=update} fallback hides an omission on a fresh local H2 and produces a
 * dialect-rendered CREATE against a real deployment's vendor, which is how
 * {@code DIRIGIBLE_CMS_SEEDS} reached production missing on PostgreSQL. This test is the build-time
 * guard: it runs where the whole platform is on the classpath, so it sees every module's entities.
 */
class SystemSchemaChangelogCoverageTest {

    /** The JPA scan packages of the SystemDB entity manager factory. */
    private static final List<String> SCAN_PACKAGES = List.of("org.eclipse.dirigible.components", "org.eclipse.dirigible.engine");

    /** The system changelog resource. */
    private static final String CHANGELOG = "db/changelog/dirigible-system.json";

    @Test
    void everySystemEntityIsCreatedByTheChangelog() throws IOException {
        Set<String> entities = entityTables();
        // a scan that finds nothing would pass this test for the wrong reason
        assertTrue(entities.contains("DIRIGIBLE_SECURITY_ROLES"),
                "The entity scan found no known system entity - it scanned " + entities.size() + " table(s): " + entities);

        Set<String> created = createdTables();
        Set<String> missing = new TreeSet<>(entities);
        missing.removeAll(created);

        assertTrue(missing.isEmpty(), "Every @Entity scanned into the SystemDB must have a createTable changeset in " + CHANGELOG
                + " - these have none: " + missing);
    }

    private Set<String> entityTables() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Set<String> tables = new TreeSet<>();
        for (String scanPackage : SCAN_PACKAGES) {
            for (BeanDefinition definition : scanner.findCandidateComponents(scanPackage)) {
                Class<?> entity = ClassUtils.resolveClassName(definition.getBeanClassName(), getClass().getClassLoader());
                Table table = entity.getAnnotation(Table.class);
                tables.add(table != null && !table.name()
                                                  .isBlank() ? table.name() : entity.getSimpleName());
            }
        }
        return tables;
    }

    @SuppressWarnings("unchecked")
    private Set<String> createdTables() throws IOException {
        Set<String> tables = new TreeSet<>();
        try (InputStream in = new ClassPathResource(CHANGELOG).getInputStream()) {
            Map<String, Object> changelog = new ObjectMapper().readValue(in, Map.class);
            for (Map<String, Object> entry : (List<Map<String, Object>>) changelog.get("databaseChangeLog")) {
                Map<String, Object> changeSet = (Map<String, Object>) entry.get("changeSet");
                if (changeSet == null) {
                    continue;
                }
                for (Map<String, Object> change : (List<Map<String, Object>>) changeSet.get("changes")) {
                    Map<String, Object> createTable = (Map<String, Object>) change.get("createTable");
                    if (createTable != null) {
                        tables.add((String) createTable.get("tableName"));
                    }
                }
            }
        }
        return tables;
    }

}
