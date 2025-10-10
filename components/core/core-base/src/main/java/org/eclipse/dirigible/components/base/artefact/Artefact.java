/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.converters.SetOfStringsToCsvConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The Class Artefact.
 */
@MappedSuperclass
public abstract class Artefact extends Auditable<String> implements Serializable {

    /** The Constant KEY_SEPARATOR. */
    public static final String KEY_SEPARATOR = ":";
    public static final int ERROR_LENGTH = 2000;

    private static final Logger LOGGER = LoggerFactory.getLogger(Artefact.class);
    /** The location. */
    @Column(name = "ARTEFACT_LOCATION", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String location;

    /** The name. */
    @Column(name = "ARTEFACT_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String name;

    /** The key. */
    @Column(name = "ARTEFACT_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String type;

    /** The description. */
    @Column(name = "ARTEFACT_DESCRIPTION")
    @Lob()
    @Expose
    protected String description;

    /**
     * The key e.g. table:/sales/domain/customer.table:customer
     */
    @Column(name = "ARTEFACT_KEY", columnDefinition = "VARCHAR", nullable = false, length = 255, unique = true)
    @Expose
    protected String key;

    /** The dependencies as comma separated keys. */
    @Column(name = "ARTEFACT_DEPENDENCIES", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Expose
    @Nullable
    @Convert(converter = SetOfStringsToCsvConverter.class)
    protected Set<String> dependencies;

    /** The lifecycle. */
    @Column(name = "ARTEFACT_STATUS", columnDefinition = "VARCHAR", nullable = true, length = 32)
    @Enumerated(EnumType.STRING)
    @JsonIgnore
    protected ArtefactLifecycle lifecycle;

    /** The phase. */
    @Column(name = "ARTEFACT_PHASE", columnDefinition = "VARCHAR", nullable = true, length = 32)
    @Enumerated(EnumType.STRING)
    @JsonIgnore
    protected ArtefactPhase phase;

    /** The description. */
    @Column(name = "ARTEFACT_ERROR", columnDefinition = "VARCHAR", nullable = true, length = ERROR_LENGTH)
    @Expose
    protected String error;

    /** The running. */
    @Column(name = "ARTEFACT_RUNNING", columnDefinition = "BOOLEAN")
    @JsonIgnore
    private Boolean running;

    /**
     * Instantiates a new artefact.
     *
     * @param location the location
     * @param name the name
     * @param type the type
     * @param description the description
     * @param dependencies the dependencies
     */
    public Artefact(String location, String name, String type, String description, Set<String> dependencies) {
        this.location = location;
        this.name = name;
        this.type = type;
        this.description = description;
        this.dependencies = dependencies;
        this.lifecycle = ArtefactLifecycle.NEW;
        updateKey();
    }

    /**
     * Update key.
     */
    public void updateKey() {
        if ((this.type == null) || (this.location == null) || (this.name == null)) {
            throw new IllegalArgumentException(
                    String.format("Attempt to generate an artefact key by type=[%s], location=[%s], name=[%s]", type, location, name));
        }
        this.key = this.type + KEY_SEPARATOR + this.location + KEY_SEPARATOR + this.name;
    }

    /**
     * Instantiates a new artefact.
     */
    public Artefact() {}

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
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
        // updateKey();
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
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
        // updateKey();
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
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key.
     *
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the dependencies.
     *
     * @return the dependencies
     */
    public Set<String> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the dependencies.
     *
     * @param dependencies the dependencies to set
     */
    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Adds the dependency.
     *
     * @param location the location
     * @param name the name
     * @param type the type
     */
    public void addDependency(String location, String name, String type) {
        addDependency(type + KEY_SEPARATOR + location + KEY_SEPARATOR + name);
    }

    /**
     * Adds the dependency.
     *
     * @param dependency the dependency
     */
    public void addDependency(String dependency) {
        if (dependencies == null) {
            dependencies = new HashSet<String>();
        }
        dependencies.add(dependency);
    }

    /**
     * Gets the lifecycle.
     *
     * @return the lifecycle
     */
    public ArtefactLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Sets the lifecycle.
     *
     * @param lifecycle the new lifecycle
     */
    public void setLifecycle(ArtefactLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * Gets the phase.
     *
     * @return the phase
     */
    public ArtefactPhase getPhase() {
        return phase;
    }

    /**
     * Sets the phase.
     *
     * @param phase the new phase
     */
    public void setPhase(ArtefactPhase phase) {
        this.phase = phase;
    }

    /**
     * Gets the error.
     *
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error.
     *
     * @param error the new error
     */
    public void setError(String error) {
        if (null != error && error.length() >= ERROR_LENGTH) {
            LOGGER.warn("Passed error with length [{}] which exceeds the maximum allowed [{}]. The value will be cutted before it is set.",
                    error.length(), ERROR_LENGTH);
            error = error.substring(0, ERROR_LENGTH);
        }
        this.error = error;
    }

    /**
     * Gets the running.
     *
     * @return the running
     */
    public Boolean getRunning() {
        return running;
    }

    /**
     * Sets the running.
     *
     * @param running the running to set
     */
    public void setRunning(Boolean running) {
        this.running = running;
    }

    /**
     * Construct key.
     *
     * @param typeA the type A
     * @param locationA the location A
     * @param nameA the name A
     * @return the string
     */
    public String constructKey(String typeA, String locationA, String nameA) {
        if (typeA != null && locationA != null && nameA != null) {
            return typeA + KEY_SEPARATOR + locationA + KEY_SEPARATOR + nameA;
        }
        return null;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Artefact [location=" + location + ", name=" + name + ", type=" + type + ", description=" + description + ", key=" + key
                + ", dependencies=" + dependencies + ", lifecycle=" + lifecycle + ", phase=" + phase + ", error=" + error + ", createdBy="
                + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
    }

}
