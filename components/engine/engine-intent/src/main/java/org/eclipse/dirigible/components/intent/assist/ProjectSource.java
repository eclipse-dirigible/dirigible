/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

/**
 * One Java source found in the workspace project.
 *
 * @param path the project-relative path, e.g. {@code custom/InvoiceNumber.java}
 * @param fqn the top-level type's binary name, e.g. {@code custom.InvoiceNumber}
 * @param source the source text
 */
record ProjectSource(String path, String fqn, String source) {

    /** The type's simple name. */
    String simpleName() {
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }
}
