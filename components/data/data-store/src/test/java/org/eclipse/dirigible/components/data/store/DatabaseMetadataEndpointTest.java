/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class DatabaseMetadataEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class DatabaseMetadataEndpointTest {

    // @Autowired
    // private DataSourceRepository datasourceRepository;
    //
    // @Autowired
    // private DataSourcesManager datasourcesManager;
    //
    // @Autowired
    // private MockMvc mockMvc;
    //
    // @Autowired
    // protected WebApplicationContext wac;
    //
    // @Autowired
    // private FilterChainProxy springSecurityFilterChain;
    //
    // @BeforeEach
    // public void setup() {
    // DataSource datasource = new DataSource("", "TestDB", "", "", "org.h2.Driver", "jdbc:h2:~/test",
    // "sa", "");
    // datasourceRepository.save(datasource);
    // }
    //
    // @Test
    // public void getDataSourceByName() throws Exception {
    //
    // mockMvc.perform(get("/services/data/metadata/{name}/{schema}/{structure}",
    // "TestDB", "INFORMATION_SCHEMA", "INDEXES"))
    // .andDo(print())
    // .andExpect(status().is2xxSuccessful())
    // ;
    // }
    //
    // @SpringBootApplication
    // static class TestConfiguration {
    // }
}
