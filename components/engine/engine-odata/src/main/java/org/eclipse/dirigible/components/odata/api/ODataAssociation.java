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

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.Expose;

/**
 * The Class ODataAssociationDefinition.
 */
public class ODataAssociation {

    /** The name. */
    @Expose
    private String name;

    /** The from. */
    @Expose
    private ODataAssociationEnd from;

    /** The to. */
    @Expose
    private ODataAssociationEnd to;

    /**
     * <p>
     * Define list of additional annotations for EntitySet element.
     * </p>
     * For example:
     * <code> &lt;AssociationSet Name="someName" Association="someName" sap:creatable="true" sap:updatable="true" sap:deletable="true"&gt; </code>
     */
    @Expose
    private Map<String, String> annotationsAssociationSet = new HashMap<>();

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
     * Gets the from.
     *
     * @return the from
     */
    public ODataAssociationEnd getFrom() {
        return from;
    }

    /**
     * Sets the from.
     *
     * @param from the new from
     */
    public void setFrom(ODataAssociationEnd from) {
        this.from = from;
    }

    /**
     * Gets the to.
     *
     * @return the to
     */
    public ODataAssociationEnd getTo() {
        return to;
    }

    /**
     * Sets the to.
     *
     * @param to the new to
     */
    public void setTo(ODataAssociationEnd to) {
        this.to = to;
    }

    /**
     * Gets the annotations association set.
     *
     * @return the annotations association set
     */
    public Map<String, String> getAnnotationsAssociationSet() {
        return annotationsAssociationSet;
    }

    /**
     * Sets the annotations association set.
     *
     * @param annotationsAssociationSet the annotations association set
     */
    public void setAnnotationsAssociationSet(Map<String, String> annotationsAssociationSet) {
        this.annotationsAssociationSet = annotationsAssociationSet;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "ODataAssociation [name=" + name + ", from=" + from + ", to=" + to + ", annotationsAssociationSet="
                + annotationsAssociationSet + "]";
    }

}
