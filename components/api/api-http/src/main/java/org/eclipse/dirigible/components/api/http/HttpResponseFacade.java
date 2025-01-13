/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.dirigible.commons.api.context.ContextException;
import org.eclipse.dirigible.commons.api.context.InvalidStateException;
import org.eclipse.dirigible.commons.api.context.ThreadContextFacade;
import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.http.access.UserResponseVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Java facade for working HttpServletResponse.
 */
@Component
public class HttpResponseFacade {

    /** The Constant NO_VALID_RESPONSE. */
    private static final String NO_VALID_RESPONSE = "Trying to use HTTP Response Facade without a valid Response";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseFacade.class);

    /**
     * Returns the HttpServletResponse associated with the current thread context.
     *
     * @return the response
     */
    public static final HttpServletResponse getResponse() {
        return UserResponseVerifier.getResponse();
    }

    /**
     * Checks if there is a HttpServletResponse associated with the current thread context.
     *
     * @return true, if there is a HttpServletResponse associated with the current thread context
     */
    public static final boolean isValid() {
        return UserResponseVerifier.isValid();
    }

    /**
     * Prints the text.
     *
     * @param text the text
     */
    public static final void print(String text) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        try {
            response.getOutputStream()
                    .print(text);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Prints the object as text.
     *
     * @param o the object to be printed
     */
    public static final void print(Object o) {
        if (o != null) {
            print(o.toString());
        }
    }

    /**
     * Prints the int primitive as text.
     *
     * @param i the integer to be printed
     */
    public static final void print(int i) {
        print(i + "");
    }

    /**
     * Prints the double primitive as text.
     *
     * @param d the double to be printed
     */
    public static final void print(double d) {
        print(d + "");
    }

    /**
     * Prints the integer as text.
     *
     * @param i the integer to be printed
     */
    public static final void print(Integer i) {
        print(i + "");
    }

    /**
     * Prints the double as text.
     *
     * @param d the double to be printed
     */
    public static final void print(Double d) {
        print(d + "");
    }

    /**
     * Prints the text with a carriage return.
     *
     * @param text the text
     */
    public static final void println(String text) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        try {
            response.getOutputStream()
                    .println(text);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Prints the object as text with a carriage return.
     *
     * @param o the object
     */
    public static final void println(Object o) {
        if (o != null) {
            println(o.toString());
        }
    }

    /**
     * Prints the int primitive as text with a carriage return.
     *
     * @param i the int primitive
     */
    public static final void println(int i) {
        println(i + "");
    }

    /**
     * Prints the double primitive as text with a carriage return.
     *
     * @param d the double primitive
     */
    public static final void println(double d) {
        println(d + "");
    }

    /**
     * Prints the Integer as text with a carriage return.
     *
     * @param i the integer
     */
    public static final void println(Integer i) {
        println(i + "");
    }

    /**
     * Prints the Double as text with a carriage return.
     *
     * @param d the double
     */
    public static final void println(Double d) {
        println(d + "");
    }

    /**
     * Writes the bytes to the output stream.
     *
     * @param bytes the bytes
     */
    public static final void write(byte[] bytes) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        try {
            response.getOutputStream()
                    .write(bytes);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Writes the string bytes to the output stream.
     *
     * @param input the input
     */
    public static final void write(String input) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        try {
            byte[] bytes = BytesHelper.jsonToBytes(input);
            response.getOutputStream()
                    .write(bytes);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Checks if the response is committed.
     *
     * @return true, if is committed
     */
    public static boolean isCommitted() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.isCommitted();
    }

    /**
     * Sets the content type.
     *
     * @param contentType the new content type
     */
    public static final void setContentType(String contentType) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setContentType(contentType);
    }

    /**
     * Flushes the response.
     */
    public static final void flush() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        try {
            response.getOutputStream()
                    .flush();
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Closes the response output stream.
     */
    public static final void close() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        try {
            response.getOutputStream()
                    .close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Adds a cookie.
     *
     * @param cookieJson the cookie in JSON format
     */
    public static final void addCookie(String cookieJson) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        Cookie cookie = GsonHelper.fromJson(cookieJson, Cookie.class);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    /**
     * Checks if the response contains a header with the specified name.
     *
     * @param name the name
     * @return true, if the header has already been added
     */
    public static final boolean containsHeader(String name) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.containsHeader(name);
    }

    /**
     * Encodes the specified URL.
     *
     * @param url the url
     * @return the URL encoded
     */
    public static final String encodeURL(String url) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.encodeURL(url);
    }

    /**
     * Returns the character encoding.
     *
     * @return the character encoding
     */
    public static final String getCharacterEncoding() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.getCharacterEncoding();
    }

    /**
     * Encode redirect URL.
     *
     * @param url the url
     * @return the string
     */
    public static final String encodeRedirectURL(String url) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.encodeRedirectURL(url);
    }

    /**
     * Returns the content type.
     *
     * @return the content type
     */
    public static final String getContentType() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.getContentType();
    }

    /**
     * Sends and error.
     *
     * @param sc the sc
     * @param msg the msg
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final void sendError(int sc, String msg) throws IOException {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.sendError(sc, msg);
    }

    /**
     * Sends error.
     *
     * @param sc the sc
     * @param msg the msg
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final void sendError(Double sc, String msg) throws IOException {
        sendError(sc.intValue(), msg);
    }

    /**
     * Sends error.
     *
     * @param sc the sc
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final void sendError(int sc) throws IOException {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.sendError(sc);
    }

    /**
     * Sends error.
     *
     * @param sc the sc
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final void sendError(Double sc) throws IOException {
        sendError(sc.intValue());
    }

    /**
     * Sets the character encoding.
     *
     * @param charset the new character encoding
     */
    public static final void setCharacterEncoding(String charset) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setCharacterEncoding(charset);
    }

    /**
     * Sends redirect.
     *
     * @param location the location
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final void sendRedirect(String location) throws IOException {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.sendRedirect(location);
    }

    /**
     * Sets the content length.
     *
     * @param len the new content length
     */
    public static final void setContentLength(int len) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setContentLength(len);
    }

    /**
     * Sets the content length.
     *
     * @param len the new content length
     */
    public static final void setContentLength(Double len) {
        setContentLength(len.intValue());
    }

    /**
     * Sets the header.
     *
     * @param name the name
     * @param value the value
     */
    public static final void setHeader(String name, String value) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setHeader(name, value);
    }

    /**
     * Adds the header.
     *
     * @param name the name
     * @param value the value
     */
    public static final void addHeader(String name, String value) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.addHeader(name, value);
    }

    /**
     * Sets the status.
     *
     * @param sc the new status
     */
    public static final void setStatus(int sc) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setStatus(sc);
    }

    /**
     * Sets the status.
     *
     * @param sc the new status
     */
    public static final void setStatus(Double sc) {
        setStatus(sc.intValue());
    }

    /**
     * Reset the buffer.
     */
    public static final void reset() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.reset();
    }

    /**
     * Returns the value of the header with the specified name.
     *
     * @param name the name
     * @return the header
     */
    public static final String getHeader(String name) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.getHeader(name);
    }

    /**
     * Sets the locale.
     *
     * @param language the new locale
     */
    public static final void setLocale(String language) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setLocale(new Locale(language));
    }

    /**
     * Sets the locale.
     *
     * @param language the language
     * @param country the country
     */
    public static final void setLocale(String language, String country) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setLocale(new Locale(language, country));
    }

    /**
     * Sets the locale.
     *
     * @param language the language
     * @param country the country
     * @param variant the variant
     */
    public static final void setLocale(String language, String country, String variant) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        response.setLocale(new Locale(language, country, variant));
    }

    /**
     * Returns the headers.
     *
     * @param name the name
     * @return the headers
     */
    public static final String getHeaders(String name) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return GsonHelper.toJson(response.getHeaders(name)
                                         .toArray());
    }

    /**
     * Returns the header names.
     *
     * @return the header names
     */
    public static final String getHeaderNames() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return GsonHelper.toJson(response.getHeaderNames()
                                         .toArray());
    }

    /**
     * Returns the locale.
     *
     * @return the locale
     */
    public static final String getLocale() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return GsonHelper.toJson(response.getLocale());
    }

    /**
     * Open the output stream of the current servlet response.
     *
     * @return the created output stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final OutputStream getOutputStream() throws IOException {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new InvalidStateException(NO_VALID_RESPONSE);
        }
        return response.getOutputStream();
    }

}
