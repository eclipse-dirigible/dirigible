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

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.engine.java.annotations.Documentation;
import org.eclipse.dirigible.engine.java.annotations.http.Controller;
import org.eclipse.dirigible.engine.java.annotations.http.Post;

/**
 * Stand-alone @Controller that seeds a small fixture set of {@link Country} rows. Kept separate
 * from {@code CountryController} so the IT can prove deleting one controller does not affect the
 * other.
 */
@Controller
@Documentation("Seeds the sample country fixture")
public class CountrySeeder {

    @Post
    @Documentation("Idempotently seeds three countries; returns either 'seeded' or 'already seeded'")
    public String seed() {
        JavaEntityStore store = BeanProvider.getBean(JavaEntityStore.class);
        if (store.count(Country.class) > 0) {
            return "already seeded";
        }
        store.save(country("AF", "AFG", "004", "Afghanistan"));
        store.save(country("AL", "ALB", "008", "Albania"));
        store.save(country("DZ", "DZA", "012", "Algeria"));
        return "seeded";
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
