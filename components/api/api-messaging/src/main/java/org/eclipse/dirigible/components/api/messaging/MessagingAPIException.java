/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.messaging;

/**
 * MessagingAPIException is used to throw an error when something unexpected occur
 */
public class MessagingAPIException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create instance of MessagingAPIException
     *
     * @param message error message
     * @param cause exception cause
     */
    public MessagingAPIException(String message, Throwable cause) {
        super(message, cause);
    }

}
