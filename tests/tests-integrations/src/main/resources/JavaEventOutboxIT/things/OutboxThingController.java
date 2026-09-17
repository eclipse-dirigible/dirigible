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

    @Get("/retarget")
    public String retarget() {
        OutboxThing thing = things.findAll().get(0);
        return String.valueOf(things.move(thing.id));
    }

    /**
     * Deletes a row through a snapshot carrying nothing but its id — what a client repository
     * legitimately holds. The delete event must still announce the name the row was stored with.
     */
    @Get("/deletePartial")
    public String deletePartial() {
        OutboxThing stored = new OutboxThing();
        stored.name = "doomed";
        Integer id = things.save(stored).id;
        OutboxThing snapshot = new OutboxThing();
        snapshot.id = id;
        things.remove(snapshot);
        return String.valueOf(id);
    }
}
