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

import org.eclipse.dirigible.components.dependencies.MavenDependency.Scope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenDependencyTest {

    @Test
    void accepts_an_exact_coordinate_and_normalizes_null_exclusions() {
        MavenDependency dependency = new MavenDependency("com.example:widget:2.1.0", Scope.MODULE, null);

        assertThat(dependency.coordinate()).isEqualTo("com.example:widget:2.1.0");
        assertThat(dependency.exclusions()).isEmpty();
    }

    @Test
    void rejects_a_version_range_with_a_clear_error() {
        assertThatThrownBy(() -> new MavenDependency("com.example:widget:[1.0,2.0)", Scope.MODULE, List.of())).isInstanceOf(
                IllegalArgumentException.class)
                                                                                                              .hasMessageContaining(
                                                                                                                      "exact version");
    }

    @Test
    void rejects_a_malformed_coordinate() {
        assertThatThrownBy(() -> new MavenDependency("com.example:widget", Scope.MODULE, List.of()))
                                                                                                    .isInstanceOf(
                                                                                                            IllegalArgumentException.class)
                                                                                                    .hasMessageContaining(
                                                                                                            "groupId:artifactId:version");
    }

    @Test
    void rejects_a_malformed_exclusion() {
        assertThatThrownBy(() -> new MavenDependency("com.example:widget:2.1.0", Scope.MODULE, List.of("com.example"))).isInstanceOf(
                IllegalArgumentException.class);
        assertThatThrownBy(() -> new MavenDependency("com.example:widget:2.1.0", Scope.MODULE, List.of("*:widget"))).isInstanceOf(
                IllegalArgumentException.class);
    }

    @Test
    void parses_the_scope_leniently() {
        assertThat(Scope.parse(null)).isEqualTo(Scope.MODULE);
        assertThat(Scope.parse("")).isEqualTo(Scope.MODULE);
        assertThat(Scope.parse("module")).isEqualTo(Scope.MODULE);
        assertThat(Scope.parse("PLATFORM")).isEqualTo(Scope.PLATFORM);
        assertThatThrownBy(() -> Scope.parse("global")).isInstanceOf(IllegalArgumentException.class)
                                                       .hasMessageContaining("global");
    }

}
