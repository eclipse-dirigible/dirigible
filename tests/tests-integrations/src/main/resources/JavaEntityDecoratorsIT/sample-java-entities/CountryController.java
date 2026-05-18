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

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.engine.java.annotations.Documentation;
import org.eclipse.dirigible.engine.java.annotations.http.Body;
import org.eclipse.dirigible.engine.java.annotations.http.Controller;
import org.eclipse.dirigible.engine.java.annotations.http.Delete;
import org.eclipse.dirigible.engine.java.annotations.http.Get;
import org.eclipse.dirigible.engine.java.annotations.http.PathParam;
import org.eclipse.dirigible.engine.java.annotations.http.Post;
import org.eclipse.dirigible.engine.java.annotations.http.Roles;

/**
 * REST surface for {@link Country}. Exercises every method-level HTTP decorator the engine
 * supports, plus {@code @Body}, {@code @PathParam}, and class-level {@code @Roles}.
 */
@Controller
@Documentation("CRUD over the Country entity")
@Roles({"DEVELOPER"})
public class CountryController {

    @Get("/list")
    @Documentation("Lists every Country")
    public List<Country> list() {
        return store().findAll(Country.class);
    }

    @Get("/{id}")
    @Documentation("Fetches a single country by id")
    public Country byId(@PathParam("id") Long id) {
        return store().findById(Country.class, id);
    }

    @Post
    @Documentation("Creates a new country from a JSON body")
    public Country create(@Body Country country) {
        return store().save(country);
    }

    @Delete("/{id}")
    @Documentation("Deletes the country with the given id")
    public void remove(@PathParam("id") Long id) {
        store().deleteById(Country.class, id);
    }

    private static JavaEntityStore store() {
        return BeanProvider.getBean(JavaEntityStore.class);
    }
}
