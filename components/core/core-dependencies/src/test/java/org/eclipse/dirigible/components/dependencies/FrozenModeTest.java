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

import org.eclipse.dirigible.components.dependencies.FrozenResolution.FrozenPlan;
import org.eclipse.dirigible.components.dependencies.MavenDependency.Scope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Frozen mode ({@code DIRIGIBLE_DEPENDENCIES_FROZEN=true}): the locked set resolves from the local
 * repository alone - by construction no remote repository is ever consulted - and a coordinate the
 * lockfile does not carry is rejected with a pointed error.
 */
class FrozenModeTest {

    @TempDir
    Path tempDir;

    @Test
    void the_locked_set_resolves_without_consulting_any_remote_repository() throws IOException {
        Path moduleJar = deploy("com.example", "mid", "1.0.0");
        Path platformJar = deploy("com.example", "driver", "2.0.0");
        Lockfile lockfile = new Lockfile("2026-08-18T09:00:00Z", "15.0.0",
                List.of(locked("com.example:mid:1.0.0", moduleJar, "module"), locked("com.example:driver:2.0.0", platformJar, "platform")),
                List.of());

        FrozenPlan plan = FrozenResolution.plan(lockfile, declared(module("com.example:mid:1.0.0")), localRepository());

        assertThat(plan.failures()).isEmpty();
        assertThat(plan.mismatched()).isEmpty();
        assertThat(plan.artifacts("module")).containsExactly(moduleJar);
        assertThat(plan.artifacts("platform")).containsExactly(platformJar);
    }

    @Test
    void a_new_coordinate_is_rejected_with_a_pointed_error() throws IOException {
        Path moduleJar = deploy("com.example", "mid", "1.0.0");
        Lockfile lockfile =
                new Lockfile("2026-08-18T09:00:00Z", "15.0.0", List.of(locked("com.example:mid:1.0.0", moduleJar, "module")), List.of());

        FrozenPlan plan = FrozenResolution.plan(lockfile, declared(module("com.example:mid:1.0.0"), module("com.example:brand-new:1.0.0")),
                localRepository());

        assertThat(plan.mismatched()).containsExactly("com.example:brand-new:1.0.0");
        assertThat(plan.failures()
                       .get("com.example:brand-new:1.0.0")).contains("frozen mode")
                                                           .contains("DIRIGIBLE_DEPENDENCIES_FROZEN")
                                                           .contains("project-lock.json");
        // the locked set still activates - a mismatch is per-declaration, not a total failure
        assertThat(plan.artifacts("module")).containsExactly(moduleJar);
    }

    @Test
    void a_missing_locked_artifact_fails_with_the_coordinate_in_the_error() {
        Lockfile lockfile = new Lockfile("2026-08-18T09:00:00Z", "15.0.0",
                List.of(new Lockfile.LockedArtifact("com.example:gone:1.0.0", "aa11", List.of("my-project"), null, "module")), List.of());

        FrozenPlan plan = FrozenResolution.plan(lockfile, declared(), localRepository());

        assertThat(plan.activations()).isEmpty();
        assertThat(plan.failures()
                       .get("com.example:gone:1.0.0")).contains("missing from the local repository");
    }

    private Path localRepository() {
        return tempDir.resolve("local-repo");
    }

    private Path deploy(String groupId, String artifactId, String version) throws IOException {
        Path directory = Files.createDirectories(localRepository().resolve(groupId.replace('.', '/'))
                                                                  .resolve(artifactId)
                                                                  .resolve(version));
        Path jar = directory.resolve(artifactId + "-" + version + ".jar");
        Files.writeString(jar, groupId + ":" + artifactId + ":" + version);
        return jar;
    }

    private static Lockfile.LockedArtifact locked(String id, Path jar, String scope) throws IOException {
        return new Lockfile.LockedArtifact(id, LockfileStore.sha256(jar), List.of("my-project"), null, scope);
    }

    private static MavenDependency module(String coordinate) {
        return new MavenDependency(coordinate, Scope.MODULE, List.of());
    }

    private static DeclaredDependencies declared(MavenDependency... dependencies) {
        Set<MavenDependency> set = new LinkedHashSet<>(List.of(dependencies));
        Map<String, Set<String>> declaredBy = new LinkedHashMap<>();
        for (MavenDependency dependency : dependencies) {
            declaredBy.computeIfAbsent(dependency.coordinate(), key -> new LinkedHashSet<>())
                      .add("my-project");
        }
        return new DeclaredDependencies(set, Map.of(), declaredBy);
    }

}
