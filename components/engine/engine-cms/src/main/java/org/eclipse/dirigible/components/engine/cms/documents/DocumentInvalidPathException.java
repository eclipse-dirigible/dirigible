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

/**
 * Raised when a requested path holds characters a CMS path cannot contain. Surfaces as HTTP 400,
 * and deliberately carries no echo of the offending value.
 */
class DocumentInvalidPathException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DocumentInvalidPathException() {
        super("The path contains characters that are not allowed");
    }
}
