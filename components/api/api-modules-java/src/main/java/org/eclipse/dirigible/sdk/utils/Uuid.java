/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import org.eclipse.dirigible.components.api.utils.UuidFacade;

/**
 * UUID generation and validation. {@link #random()} produces a Version 4 (random) UUID using the
 * JDK's {@link java.util.UUID#randomUUID()}; {@link #validate(String)} checks string conformance
 * without throwing — useful for trusting / rejecting an inbound identifier before parsing it.
 * <p>
 * Use this rather than {@code java.util.UUID.randomUUID().toString()} when you want the same output
 * format ASCII clients in TS / JS see, byte-for-byte.
 */
public final class Uuid {

    private Uuid() {}

    public static String random() {
        return UuidFacade.random();
    }

    public static boolean validate(String uuid) {
        return UuidFacade.validate(uuid);
    }
}
