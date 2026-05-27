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

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AuthenticationInjectorRegistry {

    private final Map<String, AuthenticationInjector> byType;

    public AuthenticationInjectorRegistry(List<AuthenticationInjector> injectors) {
        Map<String, AuthenticationInjector> map = new HashMap<>();
        for (AuthenticationInjector injector : injectors) {
            map.put(injector.type(), injector);
        }
        this.byType = Map.copyOf(map);
    }

    public Optional<AuthenticationInjector> findByType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byType.get(type));
    }
}
