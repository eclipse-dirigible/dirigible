/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.auth;

import org.eclipse.dirigible.components.engine.nativeapps.domain.Authentication;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * SPI for attaching outbound authentication to native-app proxy requests.
 *
 * <p>
 * Implementations declare the {@link #type()} string they handle (matching
 * {@link Authentication#getType()}) and mutate the request with the appropriate credentials. Add a
 * new implementation as a Spring {@code @Component} to extend the supported schemes.
 */
public interface AuthenticationInjector {

    /** The {@code authentication.type} value this injector handles (e.g. {@code "basic"}). */
    String type();

    /** Returns a request with the appropriate auth header attached. */
    ServerRequest inject(ServerRequest request, Authentication auth);
}
