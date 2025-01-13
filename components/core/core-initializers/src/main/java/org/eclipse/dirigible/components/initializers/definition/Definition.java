/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.definition;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.DatatypeConverter;

import org.eclipse.dirigible.components.base.artefact.Auditable;
import org.springframework.data.annotation.Transient;

import com.google.gson.annotations.Expose;

/**
 * The Class Extension.
 */
@Entity
@Table(name = "DIRIGIBLE_DEFINITIONS")
public class Definition extends Auditable<String> implements Serializable {

    /** The Constant KEY_SEPARATOR. */
    public static final String KEY_SEPARATOR = ":";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DEFINITION_ID", nullable = false)
    private Long id;

    /** The location. */
    @Column(name = "DEFINITION_LOCATION", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String location;

    /** The name. */
    @Column(name = "DEFINITION_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String name;

    /** The key. */
    @Column(name = "DEFINITION_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String type;

    /**
     * The key e.g. table:/sales/domain/customer.table:customer
     */
    @Column(name = "DEFINITION_KEY", columnDefinition = "VARCHAR", nullable = false, length = 255, unique = true)
    @Expose
    protected String key;

    /** The checksum. */
    @Column(name = "DEFINITION_CHECKSUM", columnDefinition = "VARCHAR", nullable = true, length = 32)
    @Expose
    protected String checksum;

    /** The status. */
    @Column(name = "DEFINITION_STATE", columnDefinition = "VARCHAR", nullable = true, length = 32)
    @Enumerated(EnumType.STRING)
    @Expose
    protected DefinitionState state;

    /** The status. */
    @Column(name = "DEFINITION_MESSAGE", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Expose
    protected String message;

    /** The content. */
    @Transient
    private transient byte[] content;

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
     * Instantiates a new artefact.
     *
     * @param location the location
     * @param name the name
     * @param type the type
     * @param content the content
     */
    public Definition(String location, String name, String type, byte[] content) {
        super();
        this.location = location;
        this.name = name;
        this.type = type;
        this.content = content;
        updateKey();
        updateChecksum(content);
    }

    /**
     * Instantiates a new artefact.
     */
    public Definition() {}

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
        updateKey();
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
        updateKey();
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
     * Update key.
     *
     */
    public void updateKey() {
        if (this.type != null && this.location != null && this.name != null) {
            this.key = this.type + KEY_SEPARATOR + this.location + KEY_SEPARATOR + this.name;
        }
    }

    /**
     * Gets the checksum.
     *
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Sets the checksum.
     *
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * Gets the state.
     *
     * @return the state
     */
    public DefinitionState getState() {
        return state;
    }

    /**
     * Sets the state.
     *
     * @param state the state to set
     */
    public void setState(DefinitionState state) {
        this.state = state;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the new message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Update checksum.
     *
     * @param content the content
     */
    public void updateChecksum(byte[] content) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(content);
            byte[] digest = md.digest();
            this.checksum = DatatypeConverter.printHexBinary(digest)
                                             .toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            this.checksum = "";
        }
    }

    /**
     * Gets the content.
     *
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Sets the content.
     *
     * @param content the new content
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Definition [id=" + id + ", location=" + location + ", name=" + name + ", type=" + type + ", key=" + key + ", checksum="
                + checksum + ", state=" + state + ", message=" + message + ", createdBy=" + createdBy + ", createdAt=" + createdAt
                + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
    }

}
