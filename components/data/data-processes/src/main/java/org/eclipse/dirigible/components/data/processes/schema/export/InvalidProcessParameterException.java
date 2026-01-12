/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.export;

public class InvalidProcessParameterException extends RuntimeException {

    public InvalidProcessParameterException(String message) {
        super(message);
    }

    public InvalidProcessParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
