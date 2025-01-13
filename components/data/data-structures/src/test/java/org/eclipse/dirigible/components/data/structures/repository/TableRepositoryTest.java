/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class TableRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class TableRepositoryTest {

    /** The table repository. */
    @Autowired
    private TableRepository tableRepository;

    /** The table column repository. */
    @Autowired
    private TableColumnRepository tableColumnRepository;

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {

        cleanup();

        // create test Tables
        createTable(tableRepository, tableColumnRepository, "/a/b/c/t1.table", "t1", "description", null);
        createTable(tableRepository, tableColumnRepository, "/a/b/c/t2.table", "t2", "description", null);
        createTable(tableRepository, tableColumnRepository, "/a/b/c/t3.table", "t3", "description", null);
        createTable(tableRepository, tableColumnRepository, "/a/b/c/t4.table", "t4", "description", null);
        createTable(tableRepository, tableColumnRepository, "/a/b/c/t5.table", "t5", "description", null);
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        // delete test Tables
        tableRepository.deleteAll();
    }


    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = tableRepository.findAll()
                                 .get(0)
                                 .getId();
        Optional<Table> optional = tableRepository.findById(id);
        Table table = optional.isPresent() ? optional.get() : null;
        assertNotNull(table);
        assertNotNull(table.getLocation());
        assertNotNull(table.getCreatedBy());
        assertEquals("SYSTEM", table.getCreatedBy());
        assertNotNull(table.getCreatedAt());
        assertNotNull(table.getColumns());
        assertNotNull(table.getColumns()
                           .get(0));
        assertEquals(table.getName() + "_1", table.getColumns()
                                                  .get(0)
                                                  .getName());
        assertNotNull(table.getIndexes());
        assertNotNull(table.getIndexes()
                           .get(0));
        assertEquals(table.getName() + "_1", table.getIndexes()
                                                  .get(0)
                                                  .getName());
        // assertEquals("table:/a/b/c/t1.table:t1", table.getKey());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = tableRepository.findAll()
                                 .get(0)
                                 .getId();
        Table table = entityManager.getReference(Table.class, id);
        assertNotNull(table);
        assertNotNull(table.getLocation());
    }

    /**
     * Creates the table.
     *
     * @param tableRepository the table repository
     * @param tableColumnRepository the table column repository
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @return the table
     */
    public static Table createTable(TableRepository tableRepository, TableColumnRepository tableColumnRepository, String location,
            String name, String description, Set<String> dependencies) {
        Table table = new Table(location, name, description, dependencies, "TABLE", null);
        table.addColumn(name + "_1", "VARCHAR", "20", true, false, "", "0", false);
        table.addColumn(name + "_2", "VARCHAR", "20", true, false, "", "0", false);
        table.addIndex(name + "_1", "", true, null, new String[] {name + "_1"});
        tableRepository.save(table);
        return table;
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
