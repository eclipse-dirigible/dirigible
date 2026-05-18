/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package demo;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;

public class CountryController implements JavaHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JavaEntityStore store = BeanProvider.getBean(JavaEntityStore.class);
        List<Country> countries = store.findAll(Country.class);

        response.setStatus(200);
        response.setContentType("application/json");
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Country c : countries) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("{")
                .append("\"id\":")
                .append(c.id)
                .append(",")
                .append("\"code2\":\"")
                .append(c.code2)
                .append("\",")
                .append("\"code3\":\"")
                .append(c.code3)
                .append("\",")
                .append("\"numericCode\":\"")
                .append(c.numericCode)
                .append("\",")
                .append("\"name\":\"")
                .append(c.name)
                .append("\"}");
        }
        json.append("]");
        response.getWriter()
                .write(json.toString());
    }

}
