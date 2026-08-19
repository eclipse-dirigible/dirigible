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

import org.eclipse.dirigible.components.data.store.java.repository.JavaRepository;
import org.eclipse.dirigible.sdk.component.Repository;

/**
 * Publishes its create event the way a generated repository does: by handing the topic to the write,
 * which records the event in the tenant's outbox inside the insert's own transaction.
 */
@Repository
public class OutboxThingRepository extends JavaRepository<OutboxThing> {

    public static final String CREATED_TOPIC = "event-outbox-it-thing";

    public OutboxThingRepository() {
        super(OutboxThing.class);
    }

    @Override
    public OutboxThing save(OutboxThing thing) {
        return super.save(thing, CREATED_TOPIC);
    }
}
