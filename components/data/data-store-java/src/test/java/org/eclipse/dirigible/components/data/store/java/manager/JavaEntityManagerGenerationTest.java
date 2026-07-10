/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;

import org.eclipse.dirigible.components.data.store.java.store.EntityBeanMapper;
import org.junit.jupiter.api.Test;

/**
 * {@link JavaEntityManager#findForClass(Class)} across client-classloader GENERATIONS.
 *
 * <p>
 * Client classes are recompiled into a fresh {@code ClientClassLoader} on every rebuild, but a
 * caller can outlive the swap — Flowable caches delegate instances with the process definition, so
 * after a module republish a workflow delegate still passes the OLD generation's entity class. The
 * regression this guards: the FQN fallback used to return the CURRENT registration whose reflective
 * {@code Field}s cannot be applied to the caller's instances — {@code EntityBeanMapper.fromMap}
 * threw {@code IllegalArgumentException} ("Can not set … field …") on every job retry and the
 * workflow stalled until a server restart. The fallback now serves a caller-generation metadata
 * view, so a stale-generation caller keeps working (persistence is name-keyed dynamic-map — the
 * Java generation is irrelevant to Hibernate).
 */
class JavaEntityManagerGenerationTest {

    /**
     * Re-defines ONLY the fixture entity from its bytecode, delegating everything else to the parent —
     * two loads through two instances yield same-FQN classes with different identities, exactly like
     * two {@code ClientClassLoader} generations.
     */
    private static final class GenerationClassLoader extends ClassLoader {
        GenerationClassLoader() {
            super(JavaEntityManagerGenerationTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(GenerationFixtureInvoice.class.getName())) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }
                String resource = name.replace('.', '/') + ".class";
                try (InputStream in = getParent().getResourceAsStream(resource)) {
                    byte[] bytes = in.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (Exception e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }
    }

    private static JavaEntityManager managerWith(Class<?> registeredClass) {
        JavaEntityManager manager = new JavaEntityManager(null, null, null);
        manager.registerWithoutRebuild("proj::" + registeredClass.getName(), registeredClass);
        return manager;
    }

    @Test
    void identityMatchReturnsTheRegistrationItself() {
        JavaEntityManager manager = managerWith(GenerationFixtureInvoice.class);

        RegisteredEntity meta = manager.findForClass(GenerationFixtureInvoice.class)
                                       .orElseThrow();

        assertSame(GenerationFixtureInvoice.class, meta.entityClass());
    }

    @Test
    void staleGenerationCallerGetsACallerGenerationView() throws Exception {
        Class<?> otherGeneration = new GenerationClassLoader().loadClass(GenerationFixtureInvoice.class.getName());
        assertNotSame(GenerationFixtureInvoice.class, otherGeneration, "the fixture must load with a distinct identity");
        JavaEntityManager manager = managerWith(GenerationFixtureInvoice.class);

        RegisteredEntity view = manager.findForClass(otherGeneration)
                                       .orElseThrow();

        // The view's whole reflective surface belongs to the CALLER's generation …
        assertSame(otherGeneration, view.entityClass());
        assertSame(otherGeneration, view.idField()
                                        .getDeclaringClass());
        // … while the persistence coordinates match the registration (name-keyed dynamic-map).
        RegisteredEntity registered = manager.findForClass(GenerationFixtureInvoice.class)
                                             .orElseThrow();
        assertEquals(registered.entityName(), view.entityName());
        assertEquals(registered.tableName(), view.tableName());
        // Repeat lookups reuse the cached view.
        assertSame(view, manager.findForClass(otherGeneration)
                                .orElseThrow());
    }

    @Test
    void staleGenerationCallerCanMaterializeRows() throws Exception {
        // The end-to-end regression shape: findById in a stale-generation delegate maps a Hibernate
        // row onto a bean of the CALLER's class. With the old fallback (current-generation meta) this
        // threw IllegalArgumentException from Field.set; with the view it materializes cleanly.
        Class<?> otherGeneration = new GenerationClassLoader().loadClass(GenerationFixtureInvoice.class.getName());
        JavaEntityManager manager = managerWith(GenerationFixtureInvoice.class);
        RegisteredEntity view = manager.findForClass(otherGeneration)
                                       .orElseThrow();

        Object bean = EntityBeanMapper.fromMap(otherGeneration, Map.of("Id", 42, "Number", "SI00000042"), view);

        assertTrue(otherGeneration.isInstance(bean));
        assertEquals(42, otherGeneration.getField("Id")
                                        .get(bean));
        assertEquals("SI00000042", otherGeneration.getField("Number")
                                                  .get(bean));
    }
}
