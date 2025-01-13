/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.ftp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.apache.ftpserver.ftplet.Authority;
import org.eclipse.dirigible.components.engine.ftp.domain.FtpUser;
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
 * The Class FtpRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class FtpRepositoryTest {

    /** The ftp user repository. */
    @Autowired
    private FtpUserRepository ftpUserRepository;

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        // Create test ftp users
        ftpUserRepository.save(createFtpUser("user1", "pass1", true, null, 300, "./", true));
        ftpUserRepository.save(createFtpUser("user2", "pass2", true, null, 300, "./", true));
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // Delete test security roles
        ftpUserRepository.findAll()
                         .stream()
                         .forEach(u -> ftpUserRepository.delete(u));
    }

    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = ftpUserRepository.findAll()
                                   .get(0)
                                   .getId();
        Optional<FtpUser> optional = ftpUserRepository.findById(id);
        FtpUser ftpuser = optional.isPresent() ? optional.get() : null;
        assertNotNull(ftpuser);
        assertNotNull(ftpuser.getUsername());
    }

    /**
     * Find all by username.
     */
    @Test
    public void findAllByUsername() {
        List<FtpUser> list = ftpUserRepository.findAllByUsername("user1");
        FtpUser ftpuser = list.size() > 0 ? list.get(0) : null;
        assertNotNull(ftpuser);
        assertNotNull(ftpuser.getUsername());
    }

    /**
     * Gets the all usernames.
     *
     * @return the all usernames
     */
    @Test
    public void getAllUsernames() {
        List<String> list = ftpUserRepository.findAll()
                                             .stream()
                                             .map(FtpUser::getUsername)
                                             .collect(Collectors.toList());
        assertEquals(3, list.size());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = ftpUserRepository.findAll()
                                   .get(0)
                                   .getId();
        FtpUser ftpuser = entityManager.getReference(FtpUser.class, id);
        assertNotNull(ftpuser);
        assertNotNull(ftpuser.getUsername());
    }

    /**
     * Creates the ftp user.
     *
     * @param username the username
     * @param password the password
     * @param enabled the enabled
     * @param auths the auths
     * @param maxIdleTime the max idle time
     * @param homeDirectory the home directory
     * @param admin the admin
     * @return the ftp user
     */
    public static FtpUser createFtpUser(String username, String password, boolean enabled, List<? extends Authority> auths, int maxIdleTime,
            String homeDirectory, boolean admin) {
        FtpUser ftpuser = new FtpUser(username, password, true, auths, maxIdleTime, homeDirectory, admin);
        return ftpuser;
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
