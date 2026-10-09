/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.qldb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

/**
 * The Amazon QLDB driver is an add-on (#7783). Without it, {@link QLDBRepository} must still load
 * and be introspectable (the JavaScript host reflects on it), and opening a repository must throw
 * {@link QldbNotAvailableException} naming the add-on - never a {@link NoClassDefFoundError}.
 * <p>
 * The test classpath carries the driver, so the repository is loaded in a class loader that hides
 * the driver, Ion and the AWS SDK.
 */
class QLDBRepositoryWithoutDriverTest {

    private static final String OWN_PACKAGE = QLDBRepository.class.getPackageName() + ".";

    private static final String[] HIDDEN_PACKAGES = {"software.amazon.qldb.", "software.amazon.awssdk.", "com.amazon.ion.",
            "com.amazonaws.", "com.fasterxml.jackson.dataformat.ion."};

    @Test
    void the_repository_is_introspectable_without_the_driver() throws Exception {
        Class<?> repository = repositoryWithoutDriver();

        assertTrue(repository.getMethods().length > 0);
        assertTrue(repository.getDeclaredMethods().length > 0);
        assertTrue(repository.getDeclaredFields().length > 0);
    }

    @Test
    void opening_a_repository_throws_the_add_on_exception_without_the_driver() throws Exception {
        Constructor<?> constructor = repositoryWithoutDriver().getConstructor(String.class, String.class);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> constructor.newInstance("ledger", "table"));

        Throwable cause = thrown.getCause();
        assertEquals(QldbNotAvailableException.class.getName(), cause.getClass()
                                                                     .getName(),
                () -> "threw " + cause);
        assertTrue(cause.getMessage()
                        .contains(QldbNotAvailableException.ADD_ON_ARTIFACT),
                cause::getMessage);
    }

    @Test
    void the_add_on_exception_is_a_repository_exception() {
        assertTrue(QLDBRepositoryException.class.isAssignableFrom(QldbNotAvailableException.class));
    }

    private static Class<?> repositoryWithoutDriver() throws ClassNotFoundException {
        return Class.forName(QLDBRepository.class.getName(), true, new WithoutDriver(QLDBRepository.class.getClassLoader()));
    }

    /**
     * Defines this module's classes itself, refuses the driver's packages, and delegates everything
     * else.
     */
    private static final class WithoutDriver extends ClassLoader {

        WithoutDriver(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                for (String hidden : HIDDEN_PACKAGES) {
                    if (name.startsWith(hidden)) {
                        throw new ClassNotFoundException(name);
                    }
                }
                if (!name.startsWith(OWN_PACKAGE)) {
                    return super.loadClass(name, resolve);
                }
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = define(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> define(String name) throws ClassNotFoundException {
            try (InputStream stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (stream == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = stream.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
