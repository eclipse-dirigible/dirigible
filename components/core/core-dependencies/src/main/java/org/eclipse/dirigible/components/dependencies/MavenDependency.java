/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

import java.util.List;
import java.util.Objects;

/**
 * A Maven dependency declared by a project - the exact coordinate to resolve, the scope it is
 * resolved for and the transitive exclusions to apply.
 *
 * @param coordinate the coordinate as a single {@code groupId:artifactId:version} string; exact
 *        versions only, version ranges are rejected
 * @param scope the scope the dependency is resolved for
 * @param exclusions the transitive exclusions as {@code groupId:artifactId} entries where the
 *        artifactId may be the {@code *} wildcard; never null, may be empty
 */
public record MavenDependency(String coordinate, Scope scope, List<String> exclusions) {

    /**
     * The scope a declared dependency is resolved for.
     */
    public enum Scope {

        /** The dependency joins the module classpath - the only scope supported in this phase. */
        MODULE,

        /** Reserved for a later phase - parsed, but rejected as unsupported. */
        PLATFORM;

        /**
         * Parses a scope as declared in {@code project.json} - blank means {@link #MODULE}.
         *
         * @param value the declared scope, may be null
         * @return the scope
         * @throws IllegalArgumentException when the value is neither {@code module} nor {@code platform}
         */
        public static Scope parse(String value) {
            if (value == null || value.isBlank()) {
                return MODULE;
            }
            return switch (value.trim()
                                .toLowerCase()) {
                case "module" -> MODULE;
                case "platform" -> PLATFORM;
                default -> throw new IllegalArgumentException("Unknown scope [" + value + "] - use module or platform");
            };
        }
    }

    /**
     * Validates the declaration.
     *
     * @param coordinate the coordinate as {@code groupId:artifactId:version}
     * @param scope the scope
     * @param exclusions the exclusions, null is treated as empty
     */
    public MavenDependency {
        Objects.requireNonNull(scope, "scope must not be null");
        exclusions = exclusions == null ? List.of() : List.copyOf(exclusions);
        validateCoordinate(coordinate);
        exclusions.forEach(MavenDependency::validateExclusion);
    }

    /**
     * Validate coordinate.
     *
     * @param coordinate the coordinate
     */
    private static void validateCoordinate(String coordinate) {
        Objects.requireNonNull(coordinate, "coordinate must not be null");
        String[] parts = coordinate.split(":", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("Invalid maven coordinate [" + coordinate + "] - expected groupId:artifactId:version");
        }
        String version = parts[2];
        if (version.chars()
                   .anyMatch(c -> c == '[' || c == ']' || c == '(' || c == ')' || c == ',')) {
            throw new IllegalArgumentException(
                    "Version ranges are not supported [" + coordinate + "] - declare an exact version, e.g. 1.4.0");
        }
    }

    /**
     * Validate exclusion.
     *
     * @param exclusion the exclusion
     */
    private static void validateExclusion(String exclusion) {
        String[] parts = exclusion.split(":", -1);
        if (parts.length != 2 || parts[0].isBlank() || "*".equals(parts[0]) || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid exclusion [" + exclusion + "] - expected groupId:artifactId, artifactId may be the * wildcard");
        }
    }

}
