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

import org.eclipse.dirigible.engine.java.annotations.Documentation;
import org.eclipse.dirigible.engine.java.annotations.Inject;
import org.eclipse.dirigible.engine.java.annotations.http.Body;
import org.eclipse.dirigible.engine.java.annotations.http.Controller;
import org.eclipse.dirigible.engine.java.annotations.http.Delete;
import org.eclipse.dirigible.engine.java.annotations.http.Get;
import org.eclipse.dirigible.engine.java.annotations.http.PathParam;
import org.eclipse.dirigible.engine.java.annotations.http.Post;
import org.eclipse.dirigible.engine.java.annotations.http.Roles;

/**
 * REST surface for {@link Country}. Demonstrates the recommended client-code pattern: the
 * controller declares a {@code @Inject} {@link CountryRepository} field and never touches
 * {@code JavaEntityStore} or {@code BeanProvider} directly. The engine satisfies the field at
 * controller-load time via the data-store-java repository registry.
 */
@Controller
@Documentation("CRUD over the Country entity")
@Roles({"DEVELOPER"})
public class CountryController {

    @Inject
    private CountryRepository countries;

    @Get("/list")
    @Documentation("Lists every Country")
    public List<Country> list() {
        return countries.findAll();
    }

    @Get("/{id}")
    @Documentation("Fetches a single country by id")
    public Country byId(@PathParam("id") Long id) {
        return countries.findById(id);
    }

    @Post
    @Documentation("Creates a new country from a JSON body")
    public Country create(@Body Country country) {
        return countries.save(country);
    }

    @Delete("/{id}")
    @Documentation("Deletes the country with the given id")
    public void remove(@PathParam("id") Long id) {
        countries.deleteById(id);
    }
}
