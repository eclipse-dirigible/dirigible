/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.utils;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * The Class UuidFacade.
 */
@Component
public class UuidFacade {

    /**
     * Generates random UUID.
     *
     * @return the string
     */
    public static final String random() {
        return UUID.randomUUID()
                   .toString();
    }

    /**
     * Check if it's a valid UUID.
     *
     * @param uuid the uuid
     * @return true, if it's a valid UUID
     */
    public static final boolean validate(String uuid) {
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

}
