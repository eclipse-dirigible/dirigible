/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.browser;

public enum HtmlElementType {
    PARAGRAPH("p"), //
    BUTTON("button"), //
    INPUT("input"), //
    ANCHOR("a"), //
    HEADER1("h1"), //
    HEADER2("h2"), //
    HEADER3("h3"), //
    HEADER4("h4"), //
    HEADER5("h5"), //
    HEADER6("h6"), //
    TITLE("title"), //
    IFRAME("iframe"), //
    SPAN("span"), //
    DIV("div"), //
    FD_MESSAGE_PAGE_TITLE("bk-message-page-title"), //
    CANVAS("canvas"), //
    LI("li"), //
    UL("ul"), //
    TH("th"), //
    TR("tr"); //

    private final String type;

    HtmlElementType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
