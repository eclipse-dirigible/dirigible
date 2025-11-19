/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.domain;

/**
 * The Index Metadata transport object.
 */
public class IndexMetadata {

    /** The name. */
    private String name;

    /** The type. */
    private String type;

    /** The column. */
    private String column;

    /** The non unique. */
    private boolean nonUnique;

    /** The qualifier. */
    private String qualifier;

    /** The ordinal position. */
    private String ordinalPosition;

    /** The sort order. */
    private String sortOrder;

    /** The cardinality. */
    private int cardinality;

    /** The pages. */
    private int pages;

    /** The filter condition. */
    private String filterCondition;

    /** The kind. */
    private String kind = "index";

    /**
     * Instantiates a new index metadata.
     *
     * @param name the name
     * @param type the type
     * @param column the column
     * @param nonUnique the non unique
     * @param qualifier the qualifier
     * @param ordinalPosition the ordinal position
     * @param sortOrder the sort order
     * @param cardinality the cardinality
     * @param pages the pages
     * @param filterCondition the filter condition
     */
    public IndexMetadata(String name, String type, String column, boolean nonUnique, String qualifier, String ordinalPosition,
            String sortOrder, int cardinality, int pages, String filterCondition) {
        super();
        this.name = name;
        this.type = type;
        this.column = column;
        this.nonUnique = nonUnique;
        this.qualifier = qualifier;
        this.ordinalPosition = ordinalPosition;
        this.sortOrder = sortOrder;
        this.cardinality = cardinality;
        this.pages = pages;
        this.filterCondition = filterCondition;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the column.
     *
     * @return the column
     */
    public String getColumn() {
        return column;
    }

    /**
     * Sets the column.
     *
     * @param column the new column
     */
    public void setColumn(String column) {
        this.column = column;
    }

    /**
     * Checks if is non unique.
     *
     * @return true, if is non unique
     */
    public boolean isNonUnique() {
        return nonUnique;
    }

    /**
     * Sets the non unique.
     *
     * @param nonUnique the new non unique
     */
    public void setNonUnique(boolean nonUnique) {
        this.nonUnique = nonUnique;
    }

    /**
     * Gets the qualifier.
     *
     * @return the qualifier
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Sets the qualifier.
     *
     * @param qualifier the new qualifier
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Gets the ordinal position.
     *
     * @return the ordinal position
     */
    public String getOrdinalPosition() {
        return ordinalPosition;
    }

    /**
     * Sets the ordinal position.
     *
     * @param ordinalPosition the new ordinal position
     */
    public void setOrdinalPosition(String ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    /**
     * Gets the sort order.
     *
     * @return the sort order
     */
    public String getSortOrder() {
        return sortOrder;
    }

    /**
     * Sets the sort order.
     *
     * @param sortOrder the new sort order
     */
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Gets the cardinality.
     *
     * @return the cardinality
     */
    public int getCardinality() {
        return cardinality;
    }

    /**
     * Sets the cardinality.
     *
     * @param cardinality the new cardinality
     */
    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    /**
     * Gets the pages.
     *
     * @return the pages
     */
    public int getPages() {
        return pages;
    }

    /**
     * Sets the pages.
     *
     * @param pages the new pages
     */
    public void setPages(int pages) {
        this.pages = pages;
    }

    /**
     * Gets the filter condition.
     *
     * @return the filter condition
     */
    public String getFilterCondition() {
        return filterCondition;
    }

    /**
     * Sets the filter condition.
     *
     * @param filterCondition the new filter condition
     */
    public void setFilterCondition(String filterCondition) {
        this.filterCondition = filterCondition;
    }

    /**
     * Gets the kind.
     *
     * @return the kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the new kind
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

}
