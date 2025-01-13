/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework;

public enum HtmlElementType {
    BUTTON("button"), //
    INPUT("input"), //
    ANCHOR("a"), //
    HEADER5("h5"), //
    TITLE("title"), //
    IFRAME("iframe"), //
    SPAN("span"), //
    HEADER3("h3"), //
    DIV("div"), //
    FD_MESSAGE_PAGE_TITLE("fd-message-page-title"), //
    HEADER1("h1");

    private final String type;

    HtmlElementType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
