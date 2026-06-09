/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import org.eclipse.dirigible.components.api.utils.Xml2JsonFacade;

/**
 * XML &harr; JSON conversion via the platform's Jackson XML mapper. Use this to consume legacy SOAP
 * responses without writing schema-derived bindings, or to emit XML payloads from data already
 * shaped as JSON.
 * <p>
 * The conversion is structural — element / attribute names become JSON keys, text nodes become
 * string values, repeated elements become arrays. Round-tripping is reliable for tree-shaped
 * documents; mixed-content XML (text interleaved with elements) loses some fidelity.
 */
public final class Xml {

    private Xml() {}

    public static String toJson(String xml) throws Exception {
        return Xml2JsonFacade.toJson(xml);
    }

    public static String fromJson(String json) throws Exception {
        return Xml2JsonFacade.fromJson(json);
    }
}
