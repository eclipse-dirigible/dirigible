/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.domain;

import com.google.gson.annotations.Expose;

public class Lifecycle {

    @Expose
    private LifecycleStart start;

    @Expose
    private LifecycleStop stop;

    public LifecycleStart getStart() {
        return start;
    }

    public void setStart(LifecycleStart start) {
        this.start = start;
    }

    public LifecycleStop getStop() {
        return stop;
    }

    public void setStop(LifecycleStop stop) {
        this.stop = stop;
    }
}
