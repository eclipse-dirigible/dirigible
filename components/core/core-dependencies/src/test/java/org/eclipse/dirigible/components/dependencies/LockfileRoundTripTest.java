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

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The lockfile round trip: written and parsed back byte-faithfully, serialized deterministically so
 * the diff between two locks is reviewable, and a tampered artifact (one flipped byte) fails the
 * checksum verification with the coordinate in the error.
 */
class LockfileRoundTripTest {

    @TempDir
    Path tempDir;

    private LockfileStore store;
    private Path lockfilePath;

    @BeforeEach
    void setUp() {
        lockfilePath = tempDir.resolve("project-lock.json");
        Configuration.set("DIRIGIBLE_DEPENDENCIES_LOCKFILE", lockfilePath.toString());
        store = new LockfileStore(new ResolvedModulesLinker());
    }

    @AfterEach
    void tearDown() {
        Configuration.set("DIRIGIBLE_DEPENDENCIES_LOCKFILE", "");
    }

    @Test
    void writes_and_reads_the_lock_back_faithfully() {
        Lockfile lockfile = new Lockfile("2026-08-18T09:00:00Z", "15.0.0",
                List.of(new Lockfile.LockedArtifact("com.example:mid:1.0.0", "aa11", List.of("my-project"), null, "module"),
                        new Lockfile.LockedArtifact("com.example:leaf:1.0.0", "bb22", null, "com.example:mid:1.0.0", "module")),
                List.of(new Lockfile.LockedMediation("com.example:leaf", "1.0.0", List.of("0.9.0"),
                        Map.of("1.0.0", List.of("my-project")))));

        store.write(lockfile);

        assertThat(store.read()).contains(lockfile);
    }

    @Test
    void serializes_deterministically_for_reviewable_diffs() {
        Lockfile lockfile = new Lockfile("2026-08-18T09:00:00Z", "15.0.0",
                List.of(new Lockfile.LockedArtifact("com.example:leaf:1.0.0", "bb22", List.of(), null, "module")), List.of());

        store.write(lockfile);
        String first = content();
        store.write(lockfile);

        assertThat(content()).isEqualTo(first);
        // the transitive attribution fields are omitted, not written as nulls - the sketch format
        assertThat(first).doesNotContain("null");
    }

    @Test
    void a_tampered_artifact_fails_verification_with_the_coordinate_in_the_error() throws IOException {
        Path jar = deploy("com.example", "lib", "1.0.0", "original bytes".getBytes());
        String sha = LockfileStore.sha256(jar);
        Lockfile lockfile = new Lockfile("2026-08-18T09:00:00Z", "15.0.0",
                List.of(new Lockfile.LockedArtifact("com.example:lib:1.0.0", sha, List.of("my-project"), null, "module")), List.of());

        byte[] tampered = Files.readAllBytes(jar);
        tampered[0] ^= 0x01; // one flipped byte
        Files.write(jar, tampered);

        FrozenResolution.FrozenPlan plan = FrozenResolution.plan(lockfile, declared(), tempDir.resolve("repo"), new ProvidedBom(Map.of()));

        assertThat(plan.activations()).isEmpty();
        assertThat(plan.failures()).hasSize(1);
        assertThat(plan.failures()
                       .get("com.example:lib:1.0.0")).contains("Checksum mismatch")
                                                     .contains("com.example:lib:1.0.0")
                                                     .contains(sha);
    }

    @Test
    void an_untampered_artifact_verifies_cleanly() throws IOException {
        Path jar = deploy("com.example", "lib", "1.0.0", "original bytes".getBytes());
        Lockfile lockfile = new Lockfile("2026-08-18T09:00:00Z", "15.0.0", List.of(
                new Lockfile.LockedArtifact("com.example:lib:1.0.0", LockfileStore.sha256(jar), List.of("my-project"), null, "module")),
                List.of());

        FrozenResolution.FrozenPlan plan = FrozenResolution.plan(lockfile, declared(), tempDir.resolve("repo"), new ProvidedBom(Map.of()));

        assertThat(plan.failures()).isEmpty();
        assertThat(plan.activations()).hasSize(1);
        assertThat(plan.activations()
                       .get(0)
                       .path()).isEqualTo(jar);
    }

    private String content() {
        try {
            return Files.readString(lockfilePath);
        } catch (IOException e) {
            throw new IllegalStateException("the lockfile must exist", e);
        }
    }

    private Path deploy(String groupId, String artifactId, String version, byte[] content) throws IOException {
        Path directory = Files.createDirectories(tempDir.resolve("repo")
                                                        .resolve(groupId.replace('.', '/'))
                                                        .resolve(artifactId)
                                                        .resolve(version));
        Path jar = directory.resolve(artifactId + "-" + version + ".jar");
        Files.write(jar, content);
        return jar;
    }

    private static DeclaredDependencies declared(MavenDependency... dependencies) {
        Set<MavenDependency> set = new LinkedHashSet<>(List.of(dependencies));
        return new DeclaredDependencies(set, Map.of(), Map.of());
    }

}
