/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.synchronizer;

import jakarta.persistence.EntityManager;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.repository.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import static org.eclipse.dirigible.components.security.repository.RoleRepositoryTest.createSecurityRole;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class SecurityRoleSynchronizerTest.
 */
@SpringBootTest(classes = {RoleRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class SecurityRolesSynchronizerTest {
    /**
     * The entity manager.
     */
    @Autowired
    EntityManager entityManager;
    /**
     * The security role repository.
     */
    @Autowired
    private RoleRepository roleRepository;
    /**
     * The sercurity role synchronizer.
     */
    @Autowired
    private RolesSynchronizer rolesSynchronizer;


    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {

        cleanup();

        try {
            // Create test security accesses
            roleRepository.save(createSecurityRole("/a/b/c/test1.access", "test1", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test2.access", "test2", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test3.access", "test3", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test4.access", "test4", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test5.access", "test5", "description"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // Delete test security roles
        roleRepository.deleteAll();
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void testIsAcceptedPath() {
        assertTrue(rolesSynchronizer.isAccepted(Path.of("/a/b/c/test.roles"), null));
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void testIsAcceptedArtefact() {
        assertTrue(rolesSynchronizer.isAccepted(createSecurityRole("/a/b/c/test.roles", "test", "description").getType()));
    }

    /**
     * Load the artefact.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @Test
    public void testLoad() throws IOException, ParseException {
        byte[] content = SecurityRolesSynchronizerTest.class.getResourceAsStream("/META-INF/dirigible/test/test.roles")
                                                            .readAllBytes();
        List<Role> list = rolesSynchronizer.parse("/META-INF/dirigible/test/test.roles", content);
        assertNotNull(list);
        assertEquals("/META-INF/dirigible/test/test.roles", list.get(0)
                                                                .getLocation());
    }

    /**
     * A {@code .roles} entry may mark a role a tenant's Owner can give and say how it reads (#8728); an
     * entry without the marks stays unassignable.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void testAssignableAndLabelArePersisted() throws ParseException {
        String content = "[{\"name\":\"Accountant\",\"label\":\"Accountant\",\"description\":\"Enters invoices.\",\"assignable\":true},"
                + "{\"name\":\"acme.Invoices.InvoiceReadOnly\"}]";
        List<Role> list = rolesSynchronizer.parse("/acme/assignable.roles", content.getBytes(StandardCharsets.UTF_8));

        Role accountant = rolesSynchronizer.getService()
                                           .findByKey(list.get(0)
                                                          .getKey());
        assertTrue(accountant.isAssignable());
        assertEquals("Accountant", accountant.getLabel());
        assertEquals("Enters invoices.", accountant.getDescription());
        Role convention = rolesSynchronizer.getService()
                                           .findByKey(list.get(1)
                                                          .getKey());
        assertFalse(convention.isAssignable());
        assertNull(convention.getLabel());
    }

    /**
     * A role taken out of a {@code .roles} file that stays is removed when the file is parsed again
     * (#8854); the roles it still declares keep their rows, and a role of the same name declared in
     * another file keeps that file's row.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void testRoleNoLongerDeclaredIsRemoved() throws ParseException {
        String before = "[{\"name\":\"Accountant\",\"assignable\":true},{\"name\":\"acme.Invoices.InvoiceReadOnly\",\"assignable\":true},"
                + "{\"name\":\"acme.Invoices.InvoiceFullAccess\"}]";
        rolesSynchronizer.parse("/acme/app.roles", before.getBytes(StandardCharsets.UTF_8));
        rolesSynchronizer.parse("/acme/default-roles.roles",
                "[{\"name\":\"acme.Invoices.InvoiceReadOnly\"}]".getBytes(StandardCharsets.UTF_8));

        String after = "[{\"name\":\"Accountant\",\"assignable\":true},{\"name\":\"acme.Invoices.InvoiceFullAccess\"}]";
        rolesSynchronizer.parse("/acme/app.roles", after.getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("Accountant", "acme.Invoices.InvoiceFullAccess"), namesAt("/acme/app.roles"));
        assertEquals(List.of("acme.Invoices.InvoiceReadOnly"), namesAt("/acme/default-roles.roles"));
        assertFalse(rolesSynchronizer.getService()
                                     .findByKey("role:/acme/default-roles.roles:acme.Invoices.InvoiceReadOnly")
                                     .isAssignable());
    }

    /**
     * A {@code .roles} file that declares nothing any more leaves no role of its own behind, and the
     * roles of other files are not touched.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void testFileDeclaringNothingKeepsNoRole() throws ParseException {
        rolesSynchronizer.parse("/acme/app.roles", "[{\"name\":\"Accountant\"}]".getBytes(StandardCharsets.UTF_8));

        rolesSynchronizer.parse("/acme/app.roles", "[]".getBytes(StandardCharsets.UTF_8));

        assertTrue(namesAt("/acme/app.roles").isEmpty());
        assertEquals(List.of("test1"), namesAt("/a/b/c/test1.access"));
    }

    private List<String> namesAt(String location) {
        return rolesSynchronizer.retrieve(location)
                                .stream()
                                .map(Role::getName)
                                .sorted()
                                .toList();
    }
}
