/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.extensions;

import org.eclipse.dirigible.components.api.extensions.ExtensionsFacade;

/**
 * Discovers extensions contributed to a named extension point. Returns the list of {@code module}
 * paths registered against the point — by user projects via {@code .extensionpoint} /
 * {@code .extension} artefacts, or by Java client classes annotated with
 * {@link org.eclipse.dirigible.sdk.extensions.Extension @Extension}.
 * <p>
 * Resolution is dynamic: a new contribution becomes visible to the next call after its synchronizer
 * cycle completes. The returned array is empty (never {@code null}) when no extensions match.
 */
public final class Extensions {

    private Extensions() {}

    public static String[] getExtensions(String extensionPointName) throws Exception {
        return ExtensionsFacade.getExtensions(extensionPointName);
    }
}
