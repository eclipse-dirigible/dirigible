/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.api.v3.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;


/**
 * The Class UTF8Facade.
 */
public class UTF8Facade {

    /**
     * UTF8 encode.
     *
     * @param input
     *            the input
     * @return the utf8encoded input
     */
    public static final byte[] encode(String input) throws UnsupportedEncodingException {
        return input.getBytes(StandardCharsets.UTF_8);
    }



    /**
     * UTF8 decode.
     *
     * @param input
     *            the input
     * @return the utf8 decoded output
     */
    public static final String decode(byte[] input) {
        return new String(input, StandardCharsets.UTF_8);
    }

    public static final String bytesToString(byte[] bytes, int offset, int length) throws UnsupportedEncodingException {
        return new String(bytes, offset, length, "UTF-8");
    }


}
