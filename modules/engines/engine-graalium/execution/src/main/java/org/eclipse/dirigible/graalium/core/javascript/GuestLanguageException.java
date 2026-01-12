/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.javascript;

public class GuestLanguageException extends RuntimeException {

    public GuestLanguageException(String path, String exMessage, String exClassName, Throwable cause) {
        super("Exception in file [" + path + "] - exception class [" + exClassName + "], exception message [" + exMessage + "]", cause);
    }
}
