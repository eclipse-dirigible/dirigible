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

import org.eclipse.dirigible.commons.utils.xml2json.Xml2Json;
import org.springframework.stereotype.Component;

/**
 * The Class Xml2JsonFacade.
 */
@Component
public class Xml2JsonFacade {

    /**
     * Converts JSON to XML.
     *
     * @param json the JSON contents
     * @return the JSON as XML
     * @throws Exception in case of parsing failure
     */
    public static final String fromJson(String json) throws Exception {
        return Xml2Json.toXml(json);
    }

    /**
     * Converts XML to JSON.
     *
     * @param xml the XML contents
     * @return the XML as JSON
     * @throws Exception in case of parsing failure
     */
    public static final String toJson(String xml) throws Exception {
        return Xml2Json.toJson(xml);
    }
}
