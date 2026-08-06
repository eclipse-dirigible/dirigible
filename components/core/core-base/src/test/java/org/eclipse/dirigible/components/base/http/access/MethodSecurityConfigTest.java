/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Guards the platform-wide authorization invariant.
 * <p>
 * Most endpoints authorize with {@code @RolesAllowed}, which Spring enforces only when JSR-250
 * method security is enabled. That switch used to live on the basic-authentication configuration,
 * which every single-sign-on profile disables - so under Keycloak / Cognito / GitHub / Snowflake
 * every {@code @RolesAllowed} silently became a no-op and administrative endpoints were open to any
 * authenticated user. These tests fail if that shape ever returns.
 */
class MethodSecurityConfigTest {

    @Test
    void jsr250IsEnabledSoRolesAllowedIsEnforced() {
        EnableMethodSecurity annotation = MethodSecurityConfig.class.getAnnotation(EnableMethodSecurity.class);

        assertTrue(annotation != null, "MethodSecurityConfig must carry @EnableMethodSecurity");
        assertTrue(annotation.jsr250Enabled(), "jsr250Enabled must be true - every @RolesAllowed on the platform depends on it");
    }

    @Test
    void theConfigurationIsUnconditionalSoNoAuthenticationProfileCanDisableIt() {
        assertNull(MethodSecurityConfig.class.getAnnotation(Profile.class),
                "method security must not be bound to an authentication profile");
        assertNull(MethodSecurityConfig.class.getAnnotation(ConditionalOnProperty.class),
                "method security must not be conditional - a disabled condition silently unguards every @RolesAllowed endpoint");
    }

    /**
     * A second declaration would reintroduce the gap from the other side: Spring reads the switches
     * from the annotated class, so an authentication configuration carrying its own (attribute-less,
     * hence JSR-250-less) {@code @EnableMethodSecurity} is exactly how the defect shipped.
     *
     * @throws IOException when the source tree cannot be read
     */
    @Test
    void methodSecurityIsDeclaredExactlyOnceInTheSourceTree() throws IOException {
        Path sourceRoot = repositoryRoot();
        if (sourceRoot == null) {
            return; // not running from a source checkout - nothing to scan
        }
        List<Path> declarations = new ArrayList<>();
        for (String module : new String[] {"components", "modules", "build"}) {
            Path moduleRoot = sourceRoot.resolve(module);
            if (!Files.isDirectory(moduleRoot)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(moduleRoot)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString()
                                         .endsWith(".java"))
                     .filter(path -> !path.toString()
                                          .contains("/target/"))
                     .filter(path -> !path.toString()
                                          .contains("/src/test/"))
                     .filter(MethodSecurityConfigTest::declaresMethodSecurity)
                     .forEach(declarations::add);
            }
        }
        assertEquals(1, declarations.size(),
                "@EnableMethodSecurity must be declared exactly once (in MethodSecurityConfig); found: " + declarations);
    }

    private static boolean declaresMethodSecurity(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8)
                        .contains("@EnableMethodSecurity");
        } catch (IOException e) {
            return false;
        }
    }

    /** The checkout root, located by walking up from the module directory; null when not found. */
    private static Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"))
                           .toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("components")) && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}
