/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.net;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Minimal SOAP envelope helpers — create an empty message, parse one from an XML string, or
 * synchronously invoke a remote SOAP endpoint. Built directly on {@link jakarta.xml.soap}, which is
 * part of every Dirigible runtime; no separate WSDL tooling is needed for simple integrations.
 * <p>
 * Reach for a generated client (Apache CXF, JAX-WS) when the service is complex and you want
 * type-safe stubs — these helpers shine for one-off integrations and for inspecting / repairing
 * SOAP traffic from an existing pipeline.
 */
public final class Soap {

    private Soap() {}

    public static SOAPMessage createMessage() throws SOAPException {
        return MessageFactory.newInstance()
                             .createMessage();
    }

    public static SOAPMessage parseMessage(String xml) throws SOAPException, IOException {
        return MessageFactory.newInstance()
                             .createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    public static SOAPMessage call(String endpoint, SOAPMessage request) throws SOAPException {
        try (SOAPConnection connection = SOAPConnectionFactory.newInstance()
                                                              .createConnection()) {
            return connection.call(request, endpoint);
        }
    }
}
