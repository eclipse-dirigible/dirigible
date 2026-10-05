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

import java.util.Map;

/**
 * The in-process test slice's access to the entity registration of {@link JavaEntityManager}
 * (dirigible #7643). It lives in the manager's package so that a change of the package-private API
 * it reaches breaks this module's compilation, not a downstream test run.
 */
public final class IntentSliceEntities {

    private IntentSliceEntities() {}

    /**
     * Registers every entity of an application and builds the session factory once over all of them -
     * the public {@link JavaEntityManager#register(String, Class)} rebuilds it per entity.
     *
     * @param manager the entity manager
     * @param entities the entity classes by registration key ({@code <project>::<fqn>})
     */
    public static void registerAll(JavaEntityManager manager, Map<String, Class<?>> entities) {
        entities.forEach(manager::registerWithoutRebuild);
        manager.getSessionFactory();
    }
}
