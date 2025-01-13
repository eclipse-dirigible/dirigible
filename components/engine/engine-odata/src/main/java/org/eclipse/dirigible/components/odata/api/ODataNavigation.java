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
 * The Class ODataNavigation.
 */
public class ODataNavigation {

    /** The name. */
    @Expose
    private String name;

    /** The association. */
    @Expose
    private String association;

    /**
     * <p>
     * Define list of additional annotations for NavigationProperty element.
     * </p>
     * For example:
     * <code> &lt;NavigationProperty Name="SomeName" Relationship="SomeRel" FromRole="FromRoleName" ToRole="ToRoleName" sap:filterable="false"/&gt; </code>
     */
    @Expose
    private Map<String, String> annotationsNavigationProperty = new HashMap<>();

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
     * Gets the association.
     *
     * @return the association
     */
    public String getAssociation() {
        return association;
    }

    /**
     * Sets the association.
     *
     * @param association the new association
     */
    public void setAssociation(String association) {
        this.association = association;
    }

    /**
     * Gets the annotations navigation property.
     *
     * @return the annotations navigation property
     */
    public Map<String, String> getAnnotationsNavigationProperty() {
        return annotationsNavigationProperty;
    }

    /**
     * Sets the annotations navigation property.
     *
     * @param annotationsNavigationProperty the annotations navigation property
     */
    public void setAnnotationsNavigationProperty(Map<String, String> annotationsNavigationProperty) {
        this.annotationsNavigationProperty = annotationsNavigationProperty;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "ODataNavigation [name=" + name + ", association=" + association + ", annotationsNavigationProperty="
                + annotationsNavigationProperty + "]";
    }

}
