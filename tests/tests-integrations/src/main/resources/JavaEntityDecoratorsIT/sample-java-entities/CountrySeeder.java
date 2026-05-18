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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;

public class CountrySeeder implements JavaHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JavaEntityStore store = BeanProvider.getBean(JavaEntityStore.class);

        // Idempotent: only seed when empty. Lets the IT call POST multiple times safely.
        if (store.count(Country.class) > 0) {
            response.setStatus(200);
            response.setContentType("text/plain");
            response.getWriter()
                    .write("already seeded");
            return;
        }

        store.save(country("AF", "AFG", "004", "Afghanistan"));
        store.save(country("AL", "ALB", "008", "Albania"));
        store.save(country("DZ", "DZA", "012", "Algeria"));

        response.setStatus(200);
        response.setContentType("text/plain");
        response.getWriter()
                .write("seeded");
    }

    private static Country country(String code2, String code3, String numericCode, String name) {
        Country c = new Country();
        c.code2 = code2;
        c.code3 = code3;
        c.numericCode = numericCode;
        c.name = name;
        return c;
    }

}
