/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

/**
 * The Class BytesHelper.
 */
public class BytesHelper {

    /**
     * Transform a json string to bytes array.
     *
     * @param input the input
     * @return the byte[]
     */
    public static byte[] jsonToBytes(String input) {
        return GsonHelper.fromJson(input, byte[].class);
    }

    /**
     * Transform a bytes array to json string.
     *
     * @param bytes the bytes
     * @return the string
     */
    public static String bytesToJson(byte[] bytes) {
        return GsonHelper.toJson(bytes);
    }

}
