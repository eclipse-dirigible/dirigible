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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.stereotype.Component;

/**
 * The Class IndexingCoreService.
 */
@Component
public class IndexingService {

    /** The Constant DIRIGIBLE_INDEXING_ROOT_FOLDER. */
    private static final String DIRIGIBLE_INDEXING_ROOT_FOLDER = "DIRIGIBLE_INDEXING_ROOT_FOLDER";

    /** The Constant DIRIGIBLE_INDEXING_MAX_RESULTS. */
    private static final String DIRIGIBLE_INDEXING_MAX_RESULTS = "DIRIGIBLE_INDEXING_MAX_RESULTS";

    /** The Constant FIELD_CONTENTS. */
    private static final String FIELD_CONTENTS = "contents";

    /** The Constant FIELD_MODIFIED. */
    private static final String FIELD_MODIFIED = "modified";

    /** The Constant FIELD_LOCATION. */
    private static final String FIELD_LOCATION = "location";

    /** The Constant US. */
    private static final String US = "_";

    /** The Constant BS. */
    private static final String BS = "\\";

    /** The Constant SLASH. */
    private static final String SLASH = "/";

    /** The Constant DOT. */
    private static final String DOT = ".";

    /** The root folder. */
    private static String ROOT_FOLDER;

    /** The max results. */
    private static int MAX_RESULTS;

    static {
        Configuration.loadModuleConfig("/dirigible-indexing.properties");
        ROOT_FOLDER = Configuration.get(DIRIGIBLE_INDEXING_ROOT_FOLDER);
        MAX_RESULTS = Configuration.getAsInt(DIRIGIBLE_INDEXING_MAX_RESULTS, 100);
    }

    /**
     * Adds the.
     *
     * @param index the index
     * @param location the location
     * @param contents the contents
     * @param lastModified the last modified
     * @param parameters the parameters
     * @throws IOException the indexing exception
     */
    public void add(String index, String location, byte[] contents, long lastModified, Map<String, String> parameters) throws IOException {
        String indexName = index;
        if (index != null) {
            indexName = flattenizeIndexName(indexName);
        } else {
            throw new IOException("Index name may not be null");
        }

        Directory dir = FSDirectory.open(Paths.get(ROOT_FOLDER + File.separator + indexName));
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
    }

    /**
     * Search.
     *
     * @param index the index
     * @param term the term
     * @return the string
     * @throws IOException the indexing exception
     */
    public String search(String index, String term) throws IOException {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        String indexName = index;
        if (index != null) {
            indexName = flattenizeIndexName(indexName);
        } else {
            throw new IOException("Index name may not be null");
        }
        try {
            Directory dir = FSDirectory.open(Paths.get(ROOT_FOLDER + File.separator + indexName));
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
                    Map<String, String> map = new HashMap<String, String>();
                    for (IndexableField indexableField : document.getFields()) {
                        map.put(indexableField.name(), indexableField.stringValue());
                    }
                    results.add(map);
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
            return GsonHelper.toJson(results);
        } catch (IOException | ParseException e) {
            throw new IOException(e);
        }
    }

    /**
     * Before.
     *
     * @param index the index
     * @param date the date
     * @return the string
     * @throws IOException the indexing exception
     */
    public String before(String index, long date) throws IOException {
        return between(index, new Date(0).getTime(), date);
    }

    /**
     * After.
     *
     * @param index the index
     * @param date the date
     * @return the string
     * @throws IOException the indexing exception
     */
    public String after(String index, long date) throws IOException {
        return between(index, date, new Date().getTime());
    }

    /**
     * Between.
     *
     * @param index the index
     * @param lower the lower
     * @param upper the upper
     * @return the string
     * @throws IOException the indexing exception
     */
    public String between(String index, long lower, long upper) throws IOException {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        String indexName = index;
        if (index != null) {
            indexName = flattenizeIndexName(indexName);
        } else {
            throw new IOException("Index name may not be null");
        }
        Directory dir = FSDirectory.open(Paths.get(ROOT_FOLDER + File.separator + indexName));
        IndexReader reader = null;
        try {
            reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = LongPoint.newRangeQuery(FIELD_MODIFIED, lower, upper);
            TopDocs topDocs = searcher.search(query, MAX_RESULTS);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = searcher.doc(scoreDoc.doc);
                Map<String, String> map = new HashMap<String, String>();
                for (IndexableField indexableField : document.getFields()) {
                    map.put(indexableField.name(), indexableField.stringValue());
                }
                results.add(map);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return GsonHelper.toJson(results);
    }

    /**
     * Flattenize index name.
     *
     * @param index the index
     * @return the string
     */
    private String flattenizeIndexName(String index) {
        String indexName = index;
        indexName = indexName.replace(DOT, US);
        indexName = indexName.replace(SLASH, US);
        indexName = indexName.replace(BS, US);
        return indexName;
    }

}
