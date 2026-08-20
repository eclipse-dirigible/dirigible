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

@Controller
public class OutboxThingController {

    private final OutboxThingRepository things;

    public OutboxThingController(OutboxThingRepository things) {
        this.things = things;
    }

    @Get("/seed")
    public String seed() {
        OutboxThing thing = new OutboxThing();
        thing.name = "seeded";
        return String.valueOf(things.save(thing).id);
    }
}
