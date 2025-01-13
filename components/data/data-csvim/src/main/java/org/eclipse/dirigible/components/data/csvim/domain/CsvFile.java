/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.domain;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Set;

/**
 * The Class CsvFile.
 */
@Entity
@Table(name = "DIRIGIBLE_CSV_FILE")
public class CsvFile extends Artefact {

    /**
     * The Constant ARTEFACT_TYPE.
     */
    public static final String ARTEFACT_TYPE = "csvfile";

    /**
     * The id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CSV_FILE_ID", nullable = false)
    private Long id;

    /**
     * The table.
     */
    @Column(name = "CSV_FILE_TABLE", columnDefinition = "VARCHAR", nullable = false)
    @Expose
    private String table;

    /**
     * The schema.
     */
    @Column(name = "CSV_FILE_SCHEMA", columnDefinition = "VARCHAR", nullable = false)
    @Expose
    private String schema;

    /**
     * The file.
     */
    @Column(name = "CSV_FILE_FILE", columnDefinition = "VARCHAR", nullable = false)
    @Expose
    private String file;

    /**
     * The header.
     */
    @Column(name = "CSV_FILE_HEADER", columnDefinition = "BOOLEAN")
    @Expose
    private Boolean header;

    /**
     * The use header names.
     */
    @Column(name = "CSV_FILE_USE_HEADER_NAMES", columnDefinition = "BOOLEAN")
    @Expose
    private Boolean useHeaderNames;

    /**
     * The delim field.
     */
    @Column(name = "CSV_FILE_DELIM_FIELD", columnDefinition = "VARCHAR")
    @Expose
    private String delimField;

    /**
     * The delim enclosing.
     */
    @Column(name = "CSV_FILE_DELIM_ENCLOSING", columnDefinition = "VARCHAR")
    @Expose
    private String delimEnclosing;

    /**
     * The imported.
     */
    @Column(name = "CSV_FILE_IMPORTED", columnDefinition = "BOOLEAN", nullable = false)
    private boolean imported;

    /** The sequence. */
    @Column(name = "CSV_FILE_SEQUENCE", columnDefinition = "VARCHAR")
    @Expose
    private String sequence;

    /**
     * The distinguish empty from null.
     */
    @Column(name = "CSV_FILE_DISTINGUISH_EMPTY_FROM_NULL", columnDefinition = "BOOLEAN")
    @Expose
    private Boolean distinguishEmptyFromNull;

    /** The upsert. */
    @Column(name = "CSV_FILE_UPSERT", columnDefinition = "boolean", nullable = false)
    @Expose
    private Boolean upsert = true; // default true

    /**
     * The csvim.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "CSVIM_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Csvim csvim;

    /**
     * Instantiates a new csv file.
     *
     * @param location the location
     * @param name the name
     * @param type the type
     * @param description the description
     * @param dependencies the dependencies
     * @param id the id
     * @param table the table
     * @param schema the schema
     * @param file the file
     * @param header the header
     * @param useHeaderNames the use header names
     * @param delimField the delim field
     * @param delimEnclosing the delim enclosing
     * @param sequence the sequence
     * @param distinguishEmptyFromNull the distinguish empty from null
     * @param csvim the csvim
     */
    public CsvFile(String location, String name, String type, String description, Set<String> dependencies, Long id, String table,
            String schema, String file, Boolean header, Boolean useHeaderNames, String delimField, String delimEnclosing, String sequence,
            Boolean distinguishEmptyFromNull, Csvim csvim) {
        super(location, name, type, description, dependencies);
        this.id = id;
        this.table = table;
        this.schema = schema;
        this.file = file;
        this.header = header;
        this.useHeaderNames = useHeaderNames;
        this.delimField = delimField;
        this.delimEnclosing = delimEnclosing;
        this.sequence = sequence;
        this.distinguishEmptyFromNull = distinguishEmptyFromNull;
        this.csvim = csvim;
    }

    /**
     * Instantiates a new csv file.
     *
     * @param id the id
     * @param table the table
     * @param schema the schema
     * @param file the file
     * @param header the header
     * @param useHeaderNames the use header names
     * @param delimField the delim field
     * @param delimEnclosing the delim enclosing
     * @param sequence the sequence
     * @param distinguishEmptyFromNull the distinguish empty from null
     * @param csvim the csvim
     */
    public CsvFile(Long id, String table, String schema, String file, Boolean header, Boolean useHeaderNames, String delimField,
            String delimEnclosing, String sequence, Boolean distinguishEmptyFromNull, Csvim csvim) {
        this.id = id;
        this.table = table;
        this.schema = schema;
        this.file = file;
        this.header = header;
        this.useHeaderNames = useHeaderNames;
        this.delimField = delimField;
        this.delimEnclosing = delimEnclosing;
        this.sequence = sequence;
        this.distinguishEmptyFromNull = distinguishEmptyFromNull;
        this.csvim = csvim;
    }

    /**
     * Instantiates a new csv file.
     */
    public CsvFile() {

    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the table.
     *
     * @return the table
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the table.
     *
     * @param table the table to set
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Gets the schema.
     *
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the schema.
     *
     * @param schema the schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Gets the file.
     *
     * @return the file
     */
    public String getFile() {
        return file;
    }

    /**
     * Sets the file.
     *
     * @param file the file to set
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Gets the header.
     *
     * @return the header
     */
    public Boolean getHeader() {
        return header;
    }

    /**
     * Sets the header.
     *
     * @param header the header to set
     */
    public void setHeader(Boolean header) {
        this.header = header;
    }

    /**
     * Gets the use header names.
     *
     * @return the useHeaderNames
     */
    public Boolean getUseHeaderNames() {
        return useHeaderNames;
    }

    /**
     * Sets the use header names.
     *
     * @param useHeaderNames the useHeaderNames to set
     */
    public void setUseHeaderNames(Boolean useHeaderNames) {
        this.useHeaderNames = useHeaderNames;
    }

    /**
     * Gets the delim field.
     *
     * @return the delimField
     */
    public String getDelimField() {
        return delimField;
    }

    /**
     * Sets the delim field.
     *
     * @param delimField the delimField to set
     */
    public void setDelimField(String delimField) {
        this.delimField = delimField;
    }

    /**
     * Gets the delim enclosing.
     *
     * @return the delimEnclosing
     */
    public String getDelimEnclosing() {
        return delimEnclosing;
    }

    /**
     * Sets the delim enclosing.
     *
     * @param delimEnclosing the delimEnclosing to set
     */
    public void setDelimEnclosing(String delimEnclosing) {
        this.delimEnclosing = delimEnclosing;
    }

    /**
     * Gets the sequence.
     *
     * @return sequence
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * Sets the sequence.
     *
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    /**
     * Gets the distinguish empty from null.
     *
     * @return the distinguishEmptyFromNull
     */
    public Boolean getDistinguishEmptyFromNull() {
        return distinguishEmptyFromNull;
    }

    /**
     * Sets the distinguish empty from null.
     *
     * @param distinguishEmptyFromNull the distinguishEmptyFromNull to set
     */
    public void setDistinguishEmptyFromNull(Boolean distinguishEmptyFromNull) {
        this.distinguishEmptyFromNull = distinguishEmptyFromNull;
    }

    /**
     * Gets the csvim.
     *
     * @return the csvim
     */
    public Csvim getCsvim() {
        return csvim;
    }

    /**
     * Sets the csvim.
     *
     * @param csvim the new csvim
     */
    public void setCsvim(Csvim csvim) {
        this.csvim = csvim;
    }

    /**
     * Gets the upsert.
     *
     * @return the upsert
     */
    public Boolean getUpsert() {
        return upsert;
    }

    /**
     * Sets the upsert.
     *
     * @param upsert the new upsert
     */
    public void setUpsert(Boolean upsert) {
        this.upsert = upsert;
    }

    @Override
    public void updateKey() {
        if ((this.type == null) || (this.location == null) || (this.name == null) || (this.table == null)) {
            String errMessage =
                    String.format("Attempt to generate an artefact key by type=[%s], location=[%s], name=[%s], table [%s], schema=[%s]",
                            this.type, this.location, this.name, this.table, this.schema);
            throw new IllegalArgumentException(errMessage);
        }
        this.key = this.type + KEY_SEPARATOR + this.location + KEY_SEPARATOR + this.name + KEY_SEPARATOR + this.table + KEY_SEPARATOR
                + this.schema;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }
}
