/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package delegateinjection;

import org.eclipse.dirigible.sdk.component.Component;

/** Recompiled by the test to prove an injected delegate is rewired on a client rebuild. */
@Component
public class VersionProvider {

    public String version() {
        return "v1";
    }
}
