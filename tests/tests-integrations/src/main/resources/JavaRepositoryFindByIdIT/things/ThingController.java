/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package things;

import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.PathParam;
import org.eclipse.dirigible.sdk.http.Response;

/**
 * The shape the SDK javadoc documents: look a record up by a path parameter and answer 404 when it
 * is not there. The null guard is only reachable if findById actually returns null for an unknown
 * id - a throw would make the same request a 500.
 */
@Controller
public class ThingController {

    private final ThingRepository things;

    public ThingController(ThingRepository things) {
        this.things = things;
    }

    @Get("/seed")
    public String seed() {
        Thing thing = new Thing();
        thing.name = "seeded";
        return String.valueOf(things.save(thing).id);
    }

    @Get("/byId/{id}")
    public String byId(@PathParam("id") Integer id) {
        Thing thing = things.findById(id);
        if (thing == null) {
            Response.setStatus(404);
            return "no such thing";
        }
        return thing.name;
    }

    @Get("/byOne/{id}")
    public String byOne(@PathParam("id") Integer id) {
        return things.findOne(id)
                     .map(thing -> thing.name)
                     .orElseGet(() -> {
                         Response.setStatus(404);
                         return "no such thing";
                     });
    }
}
