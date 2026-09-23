/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The run-time half of a notify block's {@code html:} alternative: what an interpolated value may
 * contribute to the markup.
 */
class HtmlTest {

    @Test
    void theMarkupCharactersBecomeReferences() {
        assertEquals("Smith &amp; Sons &lt;Ltd&gt;", Html.escape("Smith & Sons <Ltd>"));
        assertEquals("&quot;quoted&quot; &#39;single&#39;", Html.escape("\"quoted\" 'single'"));
    }

    @Test
    void anythingElsePassesThroughAsConcatenationRendersIt() {
        assertEquals("Иван 42", Html.escape("Иван 42"));
        assertEquals("42", Html.escape(42));
        assertEquals("null", Html.escape(null));
    }
}
