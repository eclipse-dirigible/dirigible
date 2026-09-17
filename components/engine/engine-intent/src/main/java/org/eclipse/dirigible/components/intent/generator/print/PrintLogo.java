/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.print;

/**
 * The issuer's logo slot every generated {@code .print} scaffold carries.
 *
 * <p>
 * One path for the whole application, not one per document: a company has a logo, not an
 * invoice-logo and a separate statement-logo, so branding a deployment is a single upload rather
 * than one per printed artifact. The file lives in the tenant's own content store, which makes it a
 * <b>per-tenant</b> value on a shared deployment - exactly like the print templates it sits beside,
 * and unlike anything that could be modeled, generated or committed.
 *
 * <p>
 * The slot is emitted unconditionally because a missing image renders <b>nothing</b>: a deployment
 * that never uploads a logo prints exactly what it prints today, and one that does needs no
 * regeneration of a scaffold it may already have customized.
 */
final class PrintLogo {

    /** The content-store path of the shared logo, relative to the store root. */
    static final String CMS_PATH = "Templates/Print/logo.png";

    private PrintLogo() {}

    /**
     * Appends the logo image element.
     *
     * @param template the template being built
     * @param indent the indentation to emit it at
     */
    static void append(StringBuilder template, String indent) {
        template.append(indent)
                .append("<image src=\"")
                .append(CMS_PATH)
                .append("\" width=\"120\"/>\n");
    }

    /**
     * The comment lines explaining the slot, emitted into the scaffold's header comment so the first
     * person to open the template learns where the logo comes from.
     *
     * @return the comment body lines, each already newline-terminated
     */
    static String comment() {
        return "     The logo is read from the tenant's content store at " + CMS_PATH + " - upload it there\n"
                + "     through the Documents perspective, or ship a default as doc/" + CMS_PATH + " in the\n"
                + "     project. A missing file prints nothing. An <image src=\"...\"> may equally name a file of the\n"
                + "     record itself (an Attachment row's StoragePath), or carry the image inline as a data: URI.\n";
    }
}
