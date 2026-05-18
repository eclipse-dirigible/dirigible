/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.spi;

/**
 * Descriptor for a client class that the engine has just loaded into the shared {@link ClassLoader}
 * or is about to drop.
 *
 * @param project owning project (first segment under the registry root); the same FQN can only be
 *        declared in a single project at a time
 * @param fqn fully-qualified binary class name
 * @param type the loaded {@link Class} object — valid until the next rebuild; consumers must not
 *        cache it past the matching {@link JavaClassConsumer#onClassUnloaded} callback
 * @param loader the {@link ClassLoader} that defined {@link #type} — kept here so consumers that
 *        need to set the thread-context classloader (Hibernate, Jackson, etc.) can do so
 */
public record LoadedClass(String project, String fqn, Class<?> type, ClassLoader loader) {
}
