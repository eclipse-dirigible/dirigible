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

import java.util.Optional;

/**
 * Extension point implemented by modules that can supply an instance for a given field type when
 * {@code ControllerClassConsumer} encounters a client-class field annotated with {@code @Inject}.
 * All beans implementing this interface are tried in order; the first one to return a non-empty
 * {@link Optional} wins.
 *
 * <p>
 * The canonical implementation lives in {@code data-store-java}'s {@code RepositoryRegistry}, which
 * returns the singleton repository instance whose runtime class matches (or is a subtype of) the
 * requested type. Additional resolvers can plug in without changing the engine.
 */
public interface DependencyResolver {

    /** Return an instance for {@code type}, or empty if this resolver does not know about it. */
    Optional<Object> resolve(Class<?> type);

}
