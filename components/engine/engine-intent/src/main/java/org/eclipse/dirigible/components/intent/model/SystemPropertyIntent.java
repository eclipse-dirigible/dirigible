/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One generator-added property's setting under {@link SystemPropertiesIntent} (dirigible #8692).
 */
public class SystemPropertyIntent {

    /**
     * The roles the generated pages SHOW the property to; everybody else does not see it on the page.
     * Display only, unlike a field's {@code visibleTo:}: the REST responses keep serving the value,
     * because the page itself needs it (the process-task banner and buttons correlate on
     * {@code ProcessId}).
     */
    private List<String> visibleTo = new ArrayList<>();

    public List<String> getVisibleTo() {
        return visibleTo;
    }

    public void setVisibleTo(List<String> visibleTo) {
        this.visibleTo = visibleTo == null ? new ArrayList<>() : visibleTo;
    }
}
