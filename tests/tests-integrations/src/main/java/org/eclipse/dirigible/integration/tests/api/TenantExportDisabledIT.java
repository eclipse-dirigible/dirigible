/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Without {@code DIRIGIBLE_TENANT_EXPORT_ENABLED} there is no tenant data export: no route answers,
 * and neither the endpoint nor its service is a bean.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantExportDisabledIT extends IntegrationTest {

    private static final String EXPORTS = "/services/data/tenant-export";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void noRouteAnswers() throws Exception {
        mvc.perform(get(EXPORTS + "/context").with(user("admin").roles("ADMINISTRATOR")))
           .andExpect(status().isNotFound());
        mvc.perform(get(EXPORTS).with(user("admin").roles("ADMINISTRATOR")))
           .andExpect(status().isNotFound());
        mvc.perform(post(EXPORTS).with(user("admin").roles("ADMINISTRATOR")))
           .andExpect(status().isNotFound());
        mvc.perform(get(EXPORTS + "/1").with(user("admin").roles("ADMINISTRATOR")))
           .andExpect(status().isNotFound());
        mvc.perform(get(EXPORTS + "/1/archive").with(user("admin").roles("ADMINISTRATOR")))
           .andExpect(status().isNotFound());
        mvc.perform(delete(EXPORTS + "/1").with(user("admin").roles("ADMINISTRATOR")))
           .andExpect(status().isNotFound());
    }

    @Test
    void noExportBeanExists() {
        for (String type : new String[] {"TenantExportEndpoint", "TenantExportService", "TenantExportHousekeeping"}) {
            boolean present = Arrays.stream(applicationContext.getBeanDefinitionNames())
                                    .map(applicationContext::getType)
                                    .anyMatch(found -> found != null && found.getName()
                                                                             .equals("org.eclipse.dirigible.components.data.processes.tenant.export."
                                                                                     + type));
            if (present) {
                throw new AssertionError(type + " must not exist while the tenant data export is off");
            }
        }
    }
}
