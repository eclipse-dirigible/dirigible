/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.search;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryReadException;
import org.eclipse.dirigible.repository.api.RepositoryWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class RepositorySearcher.
 */
public class RepositorySearcher {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(RepositorySearcher.class);

    /** The Constant DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER. */
    public static final String DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER = "DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER"; //$NON-NLS-1$

    /** The Constant DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER_IS_ABSOLUTE. */
    public static final String DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER_IS_ABSOLUTE = "DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER_IS_ABSOLUTE"; //$NON-NLS-1$

    /** The Constant DIRIGIBLE_REPOSITORY_SEARCH_INDEX_LOCATION. */
    public static final String DIRIGIBLE_REPOSITORY_SEARCH_INDEX_LOCATION = "DIRIGIBLE_REPOSITORY_SEARCH_INDEX_LOCATION"; //$NON-NLS-1$

    /** The Constant CURRENT_DIR. */
    private static final String CURRENT_DIR = ".";

    /** The Constant CURRENT_INDEX. */
    private static final String CURRENT_INDEX = "dirigible" + IRepository.SEPARATOR + "repository" + IRepository.SEPARATOR + "index";

    /** The Constant FIELD_CONTENTS. */
    private static final String FIELD_CONTENTS = "contents";

    /** The Constant FIELD_MODIFIED. */
    private static final String FIELD_MODIFIED = "modified";

    /** The Constant FIELD_LOCATION. */
    private static final String FIELD_LOCATION = "location";

    /** The Constant MAX_RESULTS. */
    private static final int MAX_RESULTS = 1000;

    /** The repository. */
    private IRepository repository;

    /** The root. */
    private String root;

    /** The index. */
    private String index;

    /** The timer. */
    private Timer timer;

    /** The seconds. */
    private int seconds = 30;

    /** The last updated. */
    private Date lastUpdated = new Date(0);

    /** The count updated. */
    private volatile int countUpdated = 0;

    /**
     * Instantiates a new repository searcher.
     *
     * @param repository the repository
     */
    public RepositorySearcher(IRepository repository) {
        this.repository = repository;

        Configuration.loadModuleConfig("/dirigible-repository-search.properties");
        String rootFolder = Configuration.get(RepositorySearcher.DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER);
        boolean absolute = Boolean.parseBoolean(Configuration.get(RepositorySearcher.DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER_IS_ABSOLUTE));
        String indexLocation = Configuration.get(RepositorySearcher.DIRIGIBLE_REPOSITORY_SEARCH_INDEX_LOCATION, CURRENT_INDEX);

        if (absolute) {
            if (rootFolder != null) {
                this.root = rootFolder;
            } else {
                throw new IllegalStateException("Creating a Repository Searcher with absolute path flag, but the path itself is null");
            }
        } else {
            this.root = System.getProperty("user.dir");
            if ((rootFolder != null) && !rootFolder.equals(CURRENT_DIR)) {
                this.root += File.separator;
                this.root += rootFolder;
            }
        }

        this.index = indexLocation;

        timer = new Timer();
        timer.schedule(new ReindexTask(), 30000, seconds * 1000);
    }

    /**
     * The Class ReindexTask.
     */
    class ReindexTask extends TimerTask {

        /**
         * Run.
         */
        @Override
        public void run() {
            synchronized (RepositorySearcher.class) {
                if (countUpdated > 30) {
                    countUpdated = 0;
                    lastUpdated = new Date(0);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Full reindexing of the Repository Content...");
                    }
                }
                reindex();
                lastUpdated = new Date();
                countUpdated++;
            }
        }
    }

    /**
     * Adds the.
     *
     * @param location the location
     * @param contents the contents
     * @param lastModified the last modified
     * @param parameters the parameters
     * @throws RepositoryWriteException the repository write exception
     */
    private void add(String location, byte[] contents, long lastModified, Map<String, String> parameters) throws RepositoryWriteException {
        String indexName = index;

        try {
            Directory dir = FSDirectory.open(Paths.get(root + File.separator + indexName));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = null;
            try {
                writer = new IndexWriter(dir, iwc);
                Document doc = new Document();
                Field pathField = new StringField(FIELD_LOCATION, location, Field.Store.YES);
                doc.add(pathField);
                doc.add(new LongPoint(FIELD_MODIFIED, lastModified));
                if (parameters != null) {
                    for (String key : parameters.keySet()) {
                        doc.add(new StringField(key, parameters.get(key), Field.Store.YES));
                    }
                }
                doc.add(new TextField(FIELD_CONTENTS,
                        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), StandardCharsets.UTF_8))));
                writer.updateDocument(new Term(FIELD_LOCATION, location), doc);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryWriteException(e);
        }
    }

    /**
     * Search for a given term in the text files content.
     *
     * @param term the term
     * @return the list of the paths of resources which content matching the search term
     * @throws RepositoryReadException in case of an error
     */
    public List<String> search(String term) throws RepositoryReadException {
        List<String> results = new ArrayList<String>();
        String indexName = index;

        try {
            Directory dir = FSDirectory.open(Paths.get(root + File.separator + indexName));
            IndexReader reader = null;
            try {
                reader = DirectoryReader.open(dir);
                IndexSearcher searcher = new IndexSearcher(reader);
                Analyzer analyzer = new StandardAnalyzer();
                String field = FIELD_CONTENTS;
                QueryParser parser = new QueryParser(field, analyzer);
                Query query = parser.parse(term);
                TopDocs topDocs = searcher.search(query, MAX_RESULTS);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = searcher.doc(scoreDoc.doc);
                    for (IndexableField indexableField : document.getFields()) {
                        String name = indexableField.name();
                        if (FIELD_LOCATION.equals(name)) {
                            String value = indexableField.stringValue();
                            results.add(value);
                            break;
                        }
                    }

                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
            return results;
        } catch (IOException | ParseException e) {
            throw new RepositoryReadException(e);
        }
    }

    /**
     * Reindex.
     */
    private void reindex() {
        synchronized (RepositorySearcher.class) {
            long start = System.currentTimeMillis();
            List<String> paths = repository.getAllResourcePaths();
            for (String path : paths) {
                IResource resource = repository.getResource(path);
                if ((resource != null) && (resource.getInformation() != null) && (resource.getInformation()
                                                                                          .getModifiedAt() != null)) {
                    if (lastUpdated.before(resource.getInformation()
                                                   .getModifiedAt())) {
                        add(path, resource.getContent(), resource.getInformation()
                                                                 .getModifiedAt()
                                                                 .getTime(),
                                null);
                    }
                }
            }
            long end = System.currentTimeMillis();
            if (logger.isTraceEnabled()) {
                logger.trace("Reindexing of the Repository Content finished in: " + (end - start) + "ms");
            }
        }
    }

    /**
     * Force reindex.
     */
    public void forceReindex() {
        synchronized (RepositorySearcher.class) {
            this.lastUpdated = new Date(0);
            this.countUpdated = 0;
            reindex();
        }
    }

    /**
     * Gets the root.
     *
     * @return the root
     */
    public String getRoot() {
        return root;
    }

}
