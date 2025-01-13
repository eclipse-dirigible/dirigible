/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.domain;

/**
 * The Generation Template Metadata Source serialization object.
 */
public class GenerationTemplateMetadataSource {

    /** The location. */
    private String location;

    /** The action. */
    private String action;

    /** The rename. */
    private String rename;

    /** The start. */
    private String start;

    /** The end. */
    private String end;

    /** The collection. */
    private String collection;

    /** The type. */
    private String type;

    /** The engine. */
    private String engine;

    /** The handler. */
    private String handler;

    /**
     * Gets the location.
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location.
     *
     * @param name the new location
     */
    public void setLocation(String name) {
        this.location = name;
    }

    /**
     * Gets the action.
     *
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the action.
     *
     * @param action the new action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Gets the rename.
     *
     * @return the rename
     */
    public String getRename() {
        return rename;
    }

    /**
     * Sets the rename.
     *
     * @param rename the new rename
     */
    public void setRename(String rename) {
        this.rename = rename;
    }

    /**
     * Gets the start.
     *
     * @return the start
     */
    public String getStart() {
        return start;
    }

    /**
     * Sets the start.
     *
     * @param start the new start
     */
    public void setStart(String start) {
        this.start = start;
    }

    /**
     * Gets the end.
     *
     * @return the end
     */
    public String getEnd() {
        return end;
    }

    /**
     * Sets the end.
     *
     * @param end the new end
     */
    public void setEnd(String end) {
        this.end = end;
    }

    /**
     * Gets the collection element.
     *
     * @return collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Sets the collection element.
     *
     * @param collection the collection element
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * Gets the named type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the named type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the named engine.
     *
     * @return the engine
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Sets the named engine.
     *
     * @param engine the engine
     */
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Gets the handler.
     *
     * @return the handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * Sets the handler.
     *
     * @param handler the handler to set
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

}
