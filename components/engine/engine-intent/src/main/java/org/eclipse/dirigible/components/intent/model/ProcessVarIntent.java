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

/**
 * A declared process variable ({@code vars: [{ name: dbPassword, clearAfter: provisionApp }]}) -
 * step data a {@code serviceTask} delegate {@code produces:} and a later one {@code uses:}. An
 * undeclared name in either list is a parse error, so step data is always written down. The
 * optional {@code clearAfter} names the step whose normal completion removes the value from the
 * instance data, so a generated credential does not survive in the process history.
 */
public class ProcessVarIntent {

    private String name;
    private String clearAfter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClearAfter() {
        return clearAfter;
    }

    public void setClearAfter(String clearAfter) {
        this.clearAfter = clearAfter;
    }
}
