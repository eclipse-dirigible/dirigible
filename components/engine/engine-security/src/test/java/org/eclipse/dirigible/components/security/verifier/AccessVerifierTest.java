/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.verifier;

import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.repository.AccessRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dirigible.components.security.repository.AccessRepositoryTest.createSecurityAccess;
import static org.eclipse.dirigible.components.security.repository.RoleRepositoryTest.createSecurityRole;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class AccessVerifierTest.
 */
@SpringBootTest(classes = {AccessRepository.class, RoleRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class AccessVerifierTest {

    /** The security access verifier. */
    @Autowired
    private AccessVerifier securityAccessVerifier;

    /** The security access repository. */
    @Autowired
    private AccessRepository securityAccessRepository;

    /** The security role repository. */
    @Autowired
    private RoleRepository securityRoleRepository;


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

        // Create test security roles and accesses
        securityRoleRepository.save(createSecurityRole("/a/b/c/test1.role", "test1", "description"));
        securityRoleRepository.save(createSecurityRole("/a/b/c/test2.role", "test2", "description"));
        securityAccessRepository.save(
                createSecurityAccess("/a/b/c/test.access", "test1", "description", "HTTP", "/a" + "/b/c/test.txt", "GET", "test1"));
        securityAccessRepository.save(
                createSecurityAccess("/a/b/c/test.access", "test2", "description", "HTTP", "/a" + "/b/c/test.txt", "GET", "test2"));
        securityAccessVerifier.refreshCache(true);
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // Delete test security roles and accesses
        securityRoleRepository.deleteAll();
        securityAccessRepository.deleteAll();
    }

    /**
     * Test get matching security accesses.
     */
    @Test
    void testGetMatchingSecurityAccesses() {
        List<Access> matchingSecurityAccesses = securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/a/b/c", "GET");
        assertTrue(matchingSecurityAccesses.isEmpty());

        matchingSecurityAccesses = securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/a/b/c/test.txt", "GET");
        assertFalse(matchingSecurityAccesses.isEmpty());
        assertEquals(2, matchingSecurityAccesses.size());
    }

    @Test
    void testGetMatchingSecurityAccessesWithAntPatterns() {
        securityAccessRepository.save(createSecurityAccess("/ant/pattern/access.access", "antPattern", "description", "HTTP",
                "/this/is/ant/pattern/*/test/**", "GET", "somerole"));
        securityAccessVerifier.refreshCache(true);
        List<Access> matchingSecurityAccesses =
                securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/this/is/ant/pattern/1234/test/api/v2/123", "GET");
        assertThat(matchingSecurityAccesses).hasSize(1);
        Access access = matchingSecurityAccesses.iterator()
                                                .next();
        assertThat(access.getRole()).isEqualTo("somerole");
    }

    /** The most specific matching Ant pattern (the longest path string) wins over a shorter one. */
    @Test
    void mostSpecificAntPatternWins() {
        securityAccessRepository.save(createSecurityAccess("/m/broad.access", "broad", "description", "HTTP", "/m/**", "GET", "broadRole"));
        securityAccessRepository.save(
                createSecurityAccess("/m/narrow.access", "narrow", "description", "HTTP", "/m/a/**", "GET", "narrowRole"));
        securityAccessVerifier.refreshCache(true);

        List<Access> matches = securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/m/a/b", "GET");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0)
                          .getRole()).isEqualTo("narrowRole");
    }

    /** An exact definition is matched (via the O(1) index) and outranks a shorter Ant pattern. */
    @Test
    void exactMatchOutranksAShorterAntPattern() {
        securityAccessRepository.save(
                createSecurityAccess("/e/exact.access", "exact", "description", "HTTP", "/aa/bb/cc", "GET", "exactRole"));
        securityAccessRepository.save(
                createSecurityAccess("/e/pattern.access", "pattern", "description", "HTTP", "/aa/*/cc", "GET", "patternRole"));
        securityAccessVerifier.refreshCache(true);

        List<Access> matches = securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/aa/bb/cc", "GET");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0)
                          .getRole()).isEqualTo("exactRole");
    }

    /** A longer Ant pattern still out-specifies an exact definition of the same path (longest wins). */
    @Test
    void longerAntPatternOutranksAnExactMatch() {
        securityAccessRepository.save(
                createSecurityAccess("/o/exact.access", "exact", "description", "HTTP", "/a/b/c", "GET", "exactRole"));
        securityAccessRepository.save(
                createSecurityAccess("/o/pattern.access", "pattern", "description", "HTTP", "/{seg}/b/c", "GET", "varRole"));
        securityAccessVerifier.refreshCache(true);

        List<Access> matches = securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/a/b/c", "GET");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0)
                          .getRole()).isEqualTo("varRole");
    }

    /** The wildcard HTTP method bucket (*) contributes alongside the specific method. */
    @Test
    void wildcardMethodBucketIsIncluded() {
        securityAccessRepository.save(
                createSecurityAccess("/w/any.access", "any", "description", "HTTP", "/w/resource", "*", "anyMethodRole"));
        securityAccessVerifier.refreshCache(true);

        assertThat(securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/w/resource", "GET")).hasSize(1);
        assertThat(securityAccessVerifier.getMatchingSecurityAccesses("HTTP", "/w/resource", "DELETE")).hasSize(1);
    }
}
