/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.etcd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * The etcd client library is an add-on (#7783). Without it, the facade must still load and be
 * introspectable (Spring scans it, the JavaScript host reflects on it), and every call must throw
 * {@link EtcdNotAvailableException} naming the add-on - never a {@link NoClassDefFoundError}.
 * <p>
 * The test classpath carries jetcd, so the facade is loaded in a class loader that hides it.
 */
class EtcdFacadeWithoutClientLibraryTest {

    private static final String OWN_PACKAGE = EtcdFacade.class.getPackageName() + ".";

    private static final String HIDDEN_PACKAGE = "io.etcd.";

    @Test
    void the_facade_is_introspectable_without_the_client_library() throws Exception {
        Class<?> facade = facadeWithoutClientLibrary();

        Method[] methods = facade.getDeclaredMethods();

        assertTrue(methods.length > 0);
    }

    @Test
    void every_call_throws_the_add_on_exception_without_the_client_library() throws Exception {
        Class<?> facade = facadeWithoutClientLibrary();

        assertAddOnMissing(facade.getMethod("getClient"));
        assertAddOnMissing(facade.getMethod("stringToByteSequence", String.class), "foo");
        assertAddOnMissing(facade.getMethod("byteArrayToByteSequence", byte[].class), (Object) new byte[] {1});
        assertAddOnMissing(facade.getMethod("byteSequenceToString", Object.class), "foo");
    }

    @Test
    void the_calls_reach_the_client_library_when_it_is_present() {
        Object sequence = EtcdFacade.stringToByteSequence("foo");

        assertEquals("foo", EtcdFacade.byteSequenceToString(sequence));
    }

    private static void assertAddOnMissing(Method method, Object... args) {
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> method.invoke(null, args));
        Throwable cause = thrown.getCause();
        assertEquals(EtcdNotAvailableException.class.getName(), cause.getClass()
                                                                     .getName(),
                () -> method.getName() + " threw " + cause);
        assertTrue(cause.getMessage()
                        .contains(EtcdNotAvailableException.ADD_ON_ARTIFACT),
                cause::getMessage);
        assertFalse(cause instanceof LinkageError);
    }

    private static Class<?> facadeWithoutClientLibrary() throws ClassNotFoundException {
        return Class.forName(EtcdFacade.class.getName(), true, new WithoutClientLibrary(EtcdFacade.class.getClassLoader()));
    }

    /**
     * Defines this module's classes itself, refuses jetcd, and delegates everything else.
     */
    private static final class WithoutClientLibrary extends ClassLoader {

        WithoutClientLibrary(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (name.startsWith(HIDDEN_PACKAGE)) {
                    throw new ClassNotFoundException(name);
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
