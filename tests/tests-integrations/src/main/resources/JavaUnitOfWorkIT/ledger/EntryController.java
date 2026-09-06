/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package ledger;

import org.eclipse.dirigible.components.data.store.java.repository.UnitOfWork;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.PathParam;

/**
 * The multi-write operation the platform's create-from is: several rows that only make sense
 * together. The failing variants write a good row first and a refused one second, which is exactly
 * the shape that used to leave a header behind with no lines.
 */
@Controller
public class EntryController {

    private final EntryRepository entries;

    public EntryController(EntryRepository entries) {
        this.entries = entries;
    }

    @Get("/count/{tag}")
    public String count(@PathParam("tag") String tag) {
        long matching = entries.findAll()
                               .stream()
                               .filter(entry -> tag.equals(entry.name))
                               .count();
        return String.valueOf(matching);
    }

    @Get("/unit/pass/{tag}")
    public String unitPasses(@PathParam("tag") String tag) {
        return UnitOfWork.call(() -> {
            entries.save(entry(tag, 1));
            entries.save(entry(tag, 2));
            return "written";
        });
    }

    @Get("/unit/fail/{tag}")
    public String unitFails(@PathParam("tag") String tag) {
        return UnitOfWork.call(() -> {
            entries.save(entry(tag, 1));
            entries.save(entry(tag, null));
            return "unreachable";
        });
    }

    @Get("/nounit/fail/{tag}")
    public String withoutUnitFails(@PathParam("tag") String tag) {
        entries.save(entry(tag, 1));
        entries.save(entry(tag, null));
        return "unreachable";
    }

    @Get("/unit/reads/{tag}")
    public String unitReadsItsOwnWrite(@PathParam("tag") String tag) {
        return UnitOfWork.call(() -> {
            Entry saved = entries.save(entry(tag, 1));
            Entry reloaded = entries.findById(saved.id);
            return reloaded == null ? "invisible" : reloaded.name;
        });
    }

    private static Entry entry(String tag, Integer amount) {
        Entry entry = new Entry();
        entry.name = tag;
        entry.amount = amount;
        return entry;
    }
}
