/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.springframework.stereotype.Component;

import com.google.common.net.UrlEscapers;

/**
 * The Class UrlFacade.
 */
@Component
public class UrlFacade {

    /**
     * Translates a string into application/x-www-form-urlencoded format using a specific encoding
     * scheme. This method uses the supplied encoding scheme to obtain the bytes for unsafe characters.
     *
     * @param input the input string
     * @param charset the input charset
     * @return the translated input
     * @throws UnsupportedEncodingException in case of problem with encoding
     */
    public static final String encode(String input, String charset) throws UnsupportedEncodingException {
        if (charset == null) {
            charset = StandardCharsets.UTF_8.name();
        }
        return URLEncoder.encode(input, charset);
    }

    /**
     * Translates a string into application/x-www-form-urlencoded format using a specific encoding
     * scheme. This method uses the supplied encoding scheme to obtain the bytes for unsafe characters.
     *
     * @param input the input string
     * @return the translated input
     * @throws UnsupportedEncodingException in case of problem with encoding
     */
    public static final String encode(String input) throws UnsupportedEncodingException {
        return encode(input, null);
    }

    /**
     * Decodes a application/x-www-form-urlencoded string using a specific encoding scheme. The supplied
     * encoding is used to determine what characters are represented by any consecutive sequences of the
     * form "%xy".
     *
     * @param input the input string
     * @param charset the input charset
     * @return the decoded input
     * @throws DecoderException in case of decoding failure
     * @throws UnsupportedEncodingException in case of problem with encoding
     */
    public static final String decode(String input, String charset) throws DecoderException, UnsupportedEncodingException {
        if (charset == null) {
            charset = StandardCharsets.UTF_8.name();
        }
        return URLDecoder.decode(input, charset);
    }

    /**
     * Decodes a application/x-www-form-urlencoded string using a specific encoding scheme. The supplied
     * encoding is used to determine what characters are represented by any consecutive sequences of the
     * form "%xy".
     *
     * @param input the input string
     * @return the decoded input
     * @throws DecoderException in case of decoding failure
     * @throws UnsupportedEncodingException in case of problem with encoding
     */
    public static final String decode(String input) throws DecoderException, UnsupportedEncodingException {
        return decode(input, null);
    }

    /**
     * Escape URL fragments.
     *
     * @param input the input string
     * @return escaped input
     */
    public static final String escape(String input) {
        return UrlEscapers.urlFragmentEscaper()
                          .escape(input);

    }

    /**
     * Escape URL path.
     *
     * @param input the input string
     * @return escaped input
     */
    public static final String escapePath(String input) {
        return UrlEscapers.urlPathSegmentEscaper()
                          .escape(input);

    }

    /**
     * Escape URL fragments.
     *
     * @param input the input string
     * @return escaped input
     */
    public static final String escapeForm(String input) {
        return UrlEscapers.urlFormParameterEscaper()
                          .escape(input);

    }

}
