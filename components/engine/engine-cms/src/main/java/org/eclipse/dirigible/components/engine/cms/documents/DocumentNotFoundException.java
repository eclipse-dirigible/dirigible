/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.documents;

/** Raised when a CMS path holds no document or folder. Surfaces as HTTP 404. */
class DocumentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DocumentNotFoundException(String path) {
        super("No document or folder at [" + path + "]");
    }
}
