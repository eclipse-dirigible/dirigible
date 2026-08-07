/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The Class ArtefactStatusEndpointTest.
 */
class ArtefactStatusEndpointTest {

    @Test
    void aggregatesEveryArtefactServiceInTheContext() {
        ArtefactStatusEndpoint endpoint = new ArtefactStatusEndpoint(List.of(
                serviceOf(artefact("/project/first.job", "first", "job", ArtefactPhase.CREATE, ArtefactLifecycle.CREATED, null)),
                serviceOf(artefact("/project/second.table", "second", "table", ArtefactPhase.UPDATE, ArtefactLifecycle.FAILED, "boom"))));

        List<ArtefactStatus> statuses = endpoint.getArtefacts()
                                                .getBody();

        assertIterableEquals(
                List.of(new ArtefactStatus("/project/first.job", "first", "job", "CREATE", "CREATED", null, Boolean.TRUE),
                        new ArtefactStatus("/project/second.table", "second", "table", "UPDATE", "FAILED", "boom", Boolean.TRUE)),
                statuses);
    }

    @Test
    void anUnreadableArtefactTypeDoesNotTakeDownTheInventory() {
        ArtefactService<?, ?> failing = new TestArtefactService(null) {
            @Override
            public List<TestArtefact> getAll() {
                throw new IllegalStateException("table is missing");
            }
        };
        ArtefactStatusEndpoint endpoint = new ArtefactStatusEndpoint(List.of(failing,
                serviceOf(artefact("/project/first.job", "first", "job", ArtefactPhase.CREATE, ArtefactLifecycle.CREATED, null))));

        List<ArtefactStatus> statuses = endpoint.getArtefacts()
                                                .getBody();

        assertEquals(1, statuses.size());
        assertEquals("first", statuses.get(0)
                                      .name());
    }

    @Test
    void aNeverSynchronizedArtefactReportsNoPhase() {
        TestArtefact artefact = artefact("/project/third.csvim", "third", "csvim", null, null, null);
        ArtefactStatusEndpoint endpoint = new ArtefactStatusEndpoint(List.of(serviceOf(artefact)));

        ArtefactStatus status = endpoint.getArtefacts()
                                        .getBody()
                                        .get(0);

        assertNull(status.phase());
        assertNull(status.status());
    }

    private static TestArtefact artefact(String location, String name, String type, ArtefactPhase phase, ArtefactLifecycle lifecycle,
            String error) {
        TestArtefact artefact = new TestArtefact(location, name, type);
        artefact.setPhase(phase);
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        artefact.setRunning(true);
        return artefact;
    }

    private static ArtefactService<?, ?> serviceOf(TestArtefact artefact) {
        return new TestArtefactService(artefact);
    }

    /** A minimal concrete artefact - {@link Artefact} itself is abstract. */
    private static final class TestArtefact extends Artefact {

        private TestArtefact(String location, String name, String type) {
            super(location, name, type, null, null);
        }
    }

    /** A service over a single artefact; only {@link #getAll()} is exercised by the endpoint. */
    private static class TestArtefactService implements ArtefactService<TestArtefact, Long> {

        private final TestArtefact artefact;

        private TestArtefactService(TestArtefact artefact) {
            this.artefact = artefact;
        }

        @Override
        public List<TestArtefact> getAll() {
            return List.of(artefact);
        }

        @Override
        public Page<TestArtefact> getPages(Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TestArtefact findById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TestArtefact findByName(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TestArtefact> findByLocation(String location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TestArtefact findByKey(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TestArtefact save(TestArtefact a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(TestArtefact a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRunningToAll(boolean running) {
            throw new UnsupportedOperationException();
        }
    }
}
