/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.indexing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * The Class IndexingCoreServiceTest.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration
@WebAppConfiguration
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
public class IndexingServiceTest {

    /** The Constant TEST_INDEX. */
    private static final String TEST_INDEX = "test_index";

    /** The indexing service. */
    @Autowired
    private IndexingService indexingService;

    /**
     * Search test.
     *
     * @throws IOException the indexing exception
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void searchTest() throws IOException {

        Map<String, String> parameters = new HashMap<String, String>();
        indexingService.add(TEST_INDEX, "/root/folder/subfolder/file1.txt",
                "Apache LuceneTM is a high-performance, full-featured text search engine library written entirely in Java.".getBytes(),
                new Date().getTime(), parameters);
        indexingService.add(TEST_INDEX, "/root/folder/subfolder/file2.txt",
                "It is a technology suitable for nearly any application that requires full-text search, especially cross-platform.".getBytes(),
                new Date().getTime(), parameters);
        indexingService.add(TEST_INDEX, "/root/folder/subfolder/file3.txt",
                "Apache Lucene is an open source project available for free download. Please use the links on the right to access Lucene.".getBytes(),
                new Date().getTime(), parameters);

        List matches = GsonHelper.fromJson(indexingService.search(TEST_INDEX, "Lucene"), List.class);
        assertNotNull(matches);
        assertEquals(1, matches.size());

        matches = GsonHelper.fromJson(indexingService.search(TEST_INDEX, "Apache"), List.class);
        assertNotNull(matches);
        assertEquals(2, matches.size());

        matches = GsonHelper.fromJson(indexingService.search(TEST_INDEX, "apache"), List.class);
        assertNotNull(matches);
        assertEquals(2, matches.size());

        matches = GsonHelper.fromJson(indexingService.search(TEST_INDEX, "NoMatches"), List.class);
        assertNotNull(matches);
        assertEquals(0, matches.size());
    }

    /**
     * Between test.
     *
     * @throws IOException the indexing exception
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void betweenTest() throws IOException {

        Map<String, String> parameters = new HashMap<String, String>();
        indexingService.add(TEST_INDEX, "/root/folder/subfolder/file4.txt",
                "Apache LuceneTM is a high-performance, full-featured text search engine library written entirely in Java.".getBytes(),
                new Date(123).getTime(), parameters);
        indexingService.add(TEST_INDEX, "/root/folder/subfolder/file5.txt",
                "It is a technology suitable for nearly any application that requires full-text search, especially cross-platform.".getBytes(),
                new Date(456).getTime(), parameters);
        indexingService.add(TEST_INDEX, "/root/folder/subfolder/file6.txt",
                "Apache Lucene is an open source project available for free download. Please use the links on the right to access Lucene.".getBytes(),
                new Date(789).getTime(), parameters);

        List matches =
                GsonHelper.fromJson(indexingService.between(TEST_INDEX, new Date(124).getTime(), new Date(689).getTime()), List.class);
        assertNotNull(matches);
        assertEquals(1, matches.size());
    }

}
