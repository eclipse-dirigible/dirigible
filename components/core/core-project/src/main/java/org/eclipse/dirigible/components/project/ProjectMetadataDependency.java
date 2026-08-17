/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.project;

import java.util.List;

/**
 * The ProjectMetadataDependency serialization object.
 */
public class ProjectMetadataDependency {

    /** Dependency type of a sibling git repository, consumed by the IDE workspace. */
    public static final String TYPE_GIT = "git";

    /** Dependency type of a Maven artifact, resolved by the platform's dependency resolver. */
    public static final String TYPE_MAVEN = "maven";

    /** The guid. */
    private String guid;

    /** The type. */
    private String type;

    /** The url. */
    private String url;

    /** The branch. */
    private String branch;

    /** The Maven coordinate as a single groupId:artifactId:version string (maven type only). */
    private String id;

    /** The scope - module (default) or platform (maven type only). */
    private String scope;

    /**
     * The exclusions as groupId:artifactId entries, artifactId may be the * wildcard (maven type only).
     */
    private List<String> exclusions;

    /**
     * Gets the guid.
     *
     * @return the guid
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Sets the guid.
     *
     * @param guid the new guid
     */
    public void setGuid(String guid) {
        this.guid = guid;
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
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url the new url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the branch.
     *
     * @return the branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Sets the branch.
     *
     * @param branch the new branch
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * Gets the Maven coordinate.
     *
     * @return the coordinate as groupId:artifactId:version
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the Maven coordinate.
     *
     * @param id the coordinate as groupId:artifactId:version
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the scope.
     *
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope.
     *
     * @param scope the new scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Gets the exclusions.
     *
     * @return the exclusions as groupId:artifactId entries
     */
    public List<String> getExclusions() {
        return exclusions;
    }

    /**
     * Sets the exclusions.
     *
     * @param exclusions the exclusions as groupId:artifactId entries
     */
    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions;
    }

}
