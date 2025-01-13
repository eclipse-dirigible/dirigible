/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.api;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class ODataEntityDefinition.
 */
public class ODataEntity {

    /** The name. */
    @Expose
    private String name;

    /** The alias. */
    @Expose
    private String alias;

    /** The table. */
    @Expose
    private String table;

    @Expose
    private String schema;

    /** The key generated. */
    @Expose
    private String keyGenerated;

    /** The properties. */
    @Expose
    private List<ODataProperty> properties = new ArrayList<>();

    /** The parameters. */
    @Expose
    private List<ODataParameter> parameters = new ArrayList<>();

    /** The navigations. */
    @Expose
    private List<ODataNavigation> navigations = new ArrayList<>();

    /** The handlers. */
    @Expose
    private List<ODataHandler> handlers = new ArrayList<>();

    /**
     * For VIEW type the keys need to be specified explicitly, because on DB side there will be no keys
     * definitions.
     */
    @Expose
    private List<String> keys = new ArrayList<>();

    /**
     * <p>
     * Define list of additional annotations for EntitySet element.
     * </p>
     * For example:
     * <code> &lt;EntitySet Name="SomeName" EntityType="someType" sap:creatable="true" sap:updatable-path="Updatable"&gt; </code>
     */
    @Expose
    private Map<String, String> annotationsEntitySet = new HashMap<>();

    /**
     * <p>
     * Define list of additional annotations for EntityType element.
     * </p>
     * For example: <code> &lt;EntityType Name="SomeTypeName" sap:semantics="aggregate""&gt; </code>
     */
    @Expose
    private Map<String, String> annotationsEntityType = new HashMap<>();

    /**
     * <p>
     * Define list of aggregation types for the columns.
     * </p>
     * For example: <code> &lt;SUM="NUMBER"&gt; </code>
     */
    @Expose
    private Map<String, String> aggregationsTypeAndColumn = new HashMap<>();

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
     * Gets the alias.
     *
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Sets the alias.
     *
     * @param alias the new alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
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
     * @param table the new table
     */
    public void setTable(String table) {
        this.table = table;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Gets the key generated.
     *
     * @return the key generated
     */
    public String getKeyGenerated() {
        return keyGenerated;
    }

    /**
     * Sets the key generated.
     *
     * @param keyGenerated the new key generated
     */
    public void setKeyGenerated(String keyGenerated) {
        this.keyGenerated = keyGenerated;
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public List<ODataProperty> getProperties() {
        return properties;
    }

    /**
     * Sets the properties.
     *
     * @param properties the new properties
     */
    public void setProperties(List<ODataProperty> properties) {
        this.properties = properties;
    }

    /**
     * Gets the parameters.
     *
     * @return the parameters
     */
    public List<ODataParameter> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the new parameters
     */
    public void setParameters(List<ODataParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Gets the navigations.
     *
     * @return the navigations
     */
    public List<ODataNavigation> getNavigations() {
        return navigations;
    }

    /**
     * Sets the navigations.
     *
     * @param navigations the new navigations
     */
    public void setNavigations(List<ODataNavigation> navigations) {
        this.navigations = navigations;
    }

    /**
     * Gets the handlers.
     *
     * @return the handlers
     */
    public List<ODataHandler> getHandlers() {
        return handlers;
    }

    /**
     * Sets the handlers.
     *
     * @param handlers the new handlers
     */
    public void setHandlers(List<ODataHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * Gets the keys.
     *
     * @return the keys
     */
    public List<String> getKeys() {
        return keys;
    }

    /**
     * Sets the keys.
     *
     * @param keys the new keys
     */
    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    /**
     * Gets the annotations entity set.
     *
     * @return the annotations entity set
     */
    public Map<String, String> getAnnotationsEntitySet() {
        return annotationsEntitySet;
    }

    /**
     * Sets the annotations entity set.
     *
     * @param annotationsEntitySet the annotations entity set
     */
    public void setAnnotationsEntitySet(Map<String, String> annotationsEntitySet) {
        this.annotationsEntitySet = annotationsEntitySet;
    }

    /**
     * Gets the annotations entity type.
     *
     * @return the annotations entity type
     */
    public Map<String, String> getAnnotationsEntityType() {
        return annotationsEntityType;
    }

    /**
     * Sets the annotations entity type.
     *
     * @param annotationsEntityType the annotations entity type
     */
    public void setAnnotationsEntityType(Map<String, String> annotationsEntityType) {
        this.annotationsEntityType = annotationsEntityType;
    }

    /**
     * Gets the aggregations type and column.
     *
     * @return the aggregations type and column
     */
    public Map<String, String> getAggregationsTypeAndColumn() {
        return aggregationsTypeAndColumn;
    }

    /**
     * Sets the aggregations type and column.
     *
     * @param aggregationsTypeAndColumn the aggregations type and column
     */
    public void setAggregationsTypeAndColumn(Map<String, String> aggregationsTypeAndColumn) {
        this.aggregationsTypeAndColumn = aggregationsTypeAndColumn;
    }

    @Override
    public String toString() {
        return "ODataEntity{" + "name='" + name + '\'' + ", alias='" + alias + '\'' + ", table='" + table + '\'' + ", schema='" + schema
                + '\'' + ", keyGenerated='" + keyGenerated + '\'' + ", properties=" + properties + ", parameters=" + parameters
                + ", navigations=" + navigations + ", handlers=" + handlers + ", keys=" + keys + ", annotationsEntitySet="
                + annotationsEntitySet + ", annotationsEntityType=" + annotationsEntityType + ", aggregationsTypeAndColumn="
                + aggregationsTypeAndColumn + '}';
    }
}
