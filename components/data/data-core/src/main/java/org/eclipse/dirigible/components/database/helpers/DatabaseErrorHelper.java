/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.helpers;

import com.google.gson.Gson;

/**
 * Database Error Helper.
 */
public class DatabaseErrorHelper {

    /**
     * Serialize the error as JSON.
     *
     * @param t the error
     * @return the error as JSON
     */
    public static String toJson(Throwable t) {
        return new Gson().toJson(new ErrorMessage(t.getMessage()));
    }

    /**
     * Serialize the error as JSON.
     *
     * @param errorMessage the error message
     * @return the error as JSON
     */
    public static String toJson(String errorMessage) {
        return new Gson().toJson(new ErrorMessage(errorMessage));
    }

    /**
     * Return the error as plain text.
     *
     * @param t the error
     * @return the error as plain text
     */
    public static String print(Throwable t) {
        return t.getMessage();
    }

    /**
     * Return the error as plain text.
     *
     * @param errorMessage the error message
     * @return the error as plain text
     */
    public static String print(String errorMessage) {
        return errorMessage;
    }

    /**
     * The Class ErrorMessage.
     */
    private static class ErrorMessage {

        /** The error message. */
        @SuppressWarnings("unused")
        private final String errorMessage;

        /**
         * Instantiates a new error message.
         *
         * @param errorMessage the error message
         */
        public ErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
