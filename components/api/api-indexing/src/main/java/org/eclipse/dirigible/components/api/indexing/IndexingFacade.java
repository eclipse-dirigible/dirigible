/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.indexing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.api.indexing.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class IndexingFacade.
 */
@Component
public class IndexingFacade implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(IndexingFacade.class);

    /** The indexing facade. */
    private static IndexingFacade INSTANCE;

    /** The Constant indexingService. */
    private final IndexingService indexingService;

    @Autowired
    private IndexingFacade(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the database facade
     */
    public static IndexingFacade get() {
        return INSTANCE;
    }

    public IndexingService getIndexingService() {
        return indexingService;
    }

    /**
     * Adds an index.
     *
     * @param index the index
     * @param location the location
     * @param contents the contents
     * @param lastModified the last modified
     * @param parameters the parameters
     * @throws IOException the indexing exception
     */
    public static final void add(String index, String location, String contents, String lastModified, String parameters)
            throws IOException {
        Map map = GsonHelper.fromJson(parameters, Map.class);
        IndexingFacade.get()
                      .getIndexingService()
                      .add(index, location, contents.getBytes(StandardCharsets.UTF_8), Long.parseLong(lastModified), map);
    }

    /**
     * Search an index by term.
     *
     * @param index the index
     * @param term the term
     * @return the values as JSON
     * @throws IOException the indexing exception
     */
    public static final String search(String index, String term) throws IOException {
        return IndexingFacade.get()
                             .getIndexingService()
                             .search(index, term);
    }

    /**
     * Search an index by date before.
     *
     * @param index the index
     * @param date the date
     * @return the values as JSON
     * @throws IOException the indexing exception
     */
    public static final String before(String index, String date) throws IOException {
        return IndexingFacade.get()
                             .getIndexingService()
                             .before(index, Long.parseLong(date));
    }

    /**
     * Search an index by date after.
     *
     * @param index the index
     * @param date the date
     * @return the values as JSON
     * @throws IOException the indexing exception
     */
    public static final String after(String index, String date) throws IOException {
        return IndexingFacade.get()
                             .getIndexingService()
                             .after(index, Long.parseLong(date));
    }

    /**
     * Search an index by date between.
     *
     * @param index the index
     * @param lower the lower
     * @param upper the upper
     * @return the values as JSON
     * @throws IOException the indexing exception
     */
    public static final String between(String index, String lower, String upper) throws IOException {
        return IndexingFacade.get()
                             .getIndexingService()
                             .between(index, Long.parseLong(lower), Long.parseLong(upper));
    }

}
